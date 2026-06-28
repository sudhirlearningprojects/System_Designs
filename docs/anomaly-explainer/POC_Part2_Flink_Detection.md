# POC Implementation - Part 2: Flink Anomaly Detection

## 1. Flink Job (Python - PyFlink)

For the POC, we use PyFlink for faster iteration. In production, you'd use Java/Scala.

```python
# flink_anomaly_detector.py
import json
import math
from collections import deque
from datetime import datetime, timezone

from kafka import KafkaConsumer, KafkaProducer


class SlidingWindowStats:
    """Maintains sliding window statistics for a metric."""

    def __init__(self, window_size=60):
        """window_size = number of data points to keep (5min / 5s interval = 60 points)."""
        self.window_size = window_size
        self.values = deque(maxlen=window_size)

    def add(self, value: float):
        self.values.append(value)

    def mean(self) -> float:
        if not self.values:
            return 0
        return sum(self.values) / len(self.values)

    def stddev(self) -> float:
        if len(self.values) < 2:
            return 1  # Avoid division by zero
        m = self.mean()
        variance = sum((x - m) ** 2 for x in self.values) / (len(self.values) - 1)
        return math.sqrt(variance) if variance > 0 else 1

    def z_score(self, value: float) -> float:
        return (value - self.mean()) / self.stddev()

    def is_ready(self) -> bool:
        """Need at least 20 data points for meaningful stats."""
        return len(self.values) >= 20


class AnomalyDetector:
    """Detects anomalies using Z-score on sliding windows per service."""

    def __init__(self, threshold=3.0):
        self.threshold = threshold
        # service -> metric_name -> SlidingWindowStats
        self.windows: dict[str, dict[str, SlidingWindowStats]] = {}

    def process(self, metric_event: dict) -> dict | None:
        """Process a metric event. Returns AnomalyEvent if anomaly detected, else None."""
        service = metric_event["service"]
        metrics = metric_event["metrics"]
        timestamp = metric_event["timestamp"]

        if service not in self.windows:
            self.windows[service] = {
                "cpu_percent": SlidingWindowStats(),
                "memory_percent": SlidingWindowStats(),
                "error_rate": SlidingWindowStats(),
                "latency_p99_ms": SlidingWindowStats(),
            }

        anomalies = []
        for metric_name, window in self.windows[service].items():
            value = metrics.get(metric_name, 0)
            
            if window.is_ready():
                z = window.z_score(value)
                if abs(z) > self.threshold:
                    anomalies.append({
                        "metric": metric_name,
                        "value": round(value, 2),
                        "mean": round(window.mean(), 2),
                        "stddev": round(window.stddev(), 2),
                        "z_score": round(z, 2),
                    })

            window.add(value)

        if anomalies:
            # Find the primary anomaly (highest z-score)
            primary = max(anomalies, key=lambda a: abs(a["z_score"]))
            
            anomaly_type_map = {
                "cpu_percent": "CPU_SPIKE",
                "memory_percent": "MEMORY_SPIKE",
                "error_rate": "ERROR_SPIKE",
                "latency_p99_ms": "LATENCY_SPIKE",
            }

            return {
                "anomaly_id": f"anom-{service}-{datetime.now().strftime('%Y%m%d%H%M%S')}",
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "service": service,
                "severity": self._calculate_severity(primary["z_score"]),
                "anomaly_type": anomaly_type_map.get(primary["metric"], "UNKNOWN"),
                "primary_metric": primary["metric"],
                "current_value": primary["value"],
                "baseline_mean": primary["mean"],
                "baseline_stddev": primary["stddev"],
                "z_score": primary["z_score"],
                "correlated_signals": [a for a in anomalies if a["metric"] != primary["metric"]],
                "all_metrics": {k: round(v, 2) for k, v in metrics.items()},
                "labels": metric_event.get("labels", {}),
            }

        return None

    def _calculate_severity(self, z_score: float) -> str:
        abs_z = abs(z_score)
        if abs_z > 6:
            return "CRITICAL"
        elif abs_z > 5:
            return "HIGH"
        elif abs_z > 4:
            return "MEDIUM"
        return "LOW"


def run_detector():
    """Main loop: consume from Kafka, detect anomalies, produce to output topic."""
    consumer = KafkaConsumer(
        "metrics.raw",
        bootstrap_servers=["localhost:9092"],
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        group_id="anomaly-detector",
        auto_offset_reset="latest",
    )

    producer = KafkaProducer(
        bootstrap_servers=["localhost:9092"],
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8"),
    )

    detector = AnomalyDetector(threshold=3.0)
    print("🔍 Anomaly Detector running. Waiting for metrics...")

    for message in consumer:
        metric_event = message.value
        anomaly = detector.process(metric_event)

        if anomaly:
            producer.send("anomalies.detected", key=anomaly["service"], value=anomaly)
            producer.flush()
            print(f"\n🚨 ANOMALY DETECTED!")
            print(f"   Service: {anomaly['service']}")
            print(f"   Type: {anomaly['anomaly_type']}")
            print(f"   Severity: {anomaly['severity']}")
            print(f"   Z-Score: {anomaly['z_score']}")
            print(f"   Value: {anomaly['current_value']} (baseline: {anomaly['baseline_mean']} ± {anomaly['baseline_stddev']})")
            if anomaly['correlated_signals']:
                print(f"   Correlated: {[s['metric'] for s in anomaly['correlated_signals']]}")


if __name__ == "__main__":
    run_detector()
```

## 2. Production Flink Job (Java - for reference)

```java
// AnomalyDetectionJob.java
package org.sudhir512kj.anomalyexplainer.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

public class AnomalyDetectionJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000); // Checkpoint every 60s

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setTopics("metrics.raw")
                .setGroupId("flink-anomaly-detector")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> metrics = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Metrics");

        DataStream<String> anomalies = metrics
                .map(MetricEvent::fromJson)
                .keyBy(MetricEvent::getService)
                .window(SlidingEventTimeWindows.of(Time.minutes(5), Time.seconds(30)))
                .process(new AnomalyProcessFunction(3.0)) // Z-score threshold
                .filter(event -> event != null)
                .map(AnomalyEvent::toJson);

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("anomalies.detected")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();

        anomalies.sinkTo(sink);
        env.execute("Anomaly Detection Pipeline");
    }
}
```

## 3. Testing the Detector

```python
# test_detector.py
"""Unit tests for the anomaly detector logic."""

from flink_anomaly_detector import AnomalyDetector, SlidingWindowStats


def test_sliding_window_stats():
    window = SlidingWindowStats(window_size=10)
    # Add normal values
    for v in [10, 11, 9, 10, 12, 8, 10, 11, 9, 10]:
        window.add(v)
    
    assert abs(window.mean() - 10.0) < 0.5
    assert window.stddev() < 2.0
    assert window.is_ready() is False  # Need 20 points

    # Add more
    for v in [10, 11, 9, 10, 12, 8, 10, 11, 9, 10]:
        window.add(v)
    
    assert window.is_ready() is True
    # Z-score of an outlier
    assert window.z_score(30) > 3.0


def test_anomaly_detection():
    detector = AnomalyDetector(threshold=3.0)

    # Feed 25 normal data points to warm up
    for i in range(25):
        event = {
            "service": "test-service",
            "timestamp": f"2024-01-15T10:{i:02d}:00Z",
            "metrics": {"cpu_percent": 45 + (i % 5), "memory_percent": 60, "error_rate": 0.01, "latency_p99_ms": 200},
            "labels": {},
        }
        result = detector.process(event)

    # Now inject anomaly
    anomaly_event = {
        "service": "test-service",
        "timestamp": "2024-01-15T10:30:00Z",
        "metrics": {"cpu_percent": 95, "memory_percent": 60, "error_rate": 0.01, "latency_p99_ms": 200},
        "labels": {},
    }
    result = detector.process(anomaly_event)
    
    assert result is not None
    assert result["anomaly_type"] == "CPU_SPIKE"
    assert result["z_score"] > 3.0
    print(f"✅ Anomaly detected: {result['anomaly_type']} (z={result['z_score']})")


if __name__ == "__main__":
    test_sliding_window_stats()
    test_anomaly_detection()
    print("\n✅ All tests passed!")
```

## 4. Advanced: Multi-Signal Correlation

```python
# correlation_engine.py
"""Correlates multiple anomalous signals to identify compound issues."""

from dataclasses import dataclass
from datetime import datetime, timedelta


@dataclass
class CorrelatedAnomaly:
    primary_service: str
    primary_metric: str
    correlated_services: list[str]
    time_window: str
    pattern: str  # "cascading", "simultaneous", "delayed"


class CorrelationEngine:
    """Detects patterns across multiple services and metrics."""

    def __init__(self, correlation_window_sec=60):
        self.correlation_window = timedelta(seconds=correlation_window_sec)
        self.recent_anomalies: list[dict] = []

    def add_anomaly(self, anomaly: dict) -> CorrelatedAnomaly | None:
        """Add anomaly and check for correlations."""
        now = datetime.fromisoformat(anomaly["timestamp"].replace("Z", "+00:00"))
        
        # Remove old anomalies outside window
        self.recent_anomalies = [
            a for a in self.recent_anomalies
            if (now - datetime.fromisoformat(a["timestamp"].replace("Z", "+00:00"))) < self.correlation_window
        ]

        self.recent_anomalies.append(anomaly)

        # Check for correlations
        services_affected = set(a["service"] for a in self.recent_anomalies)
        if len(services_affected) > 1:
            # Multiple services affected = possible cascading failure
            return CorrelatedAnomaly(
                primary_service=anomaly["service"],
                primary_metric=anomaly["primary_metric"],
                correlated_services=list(services_affected - {anomaly["service"]}),
                time_window=f"{self.correlation_window.seconds}s",
                pattern="cascading" if self._is_cascading() else "simultaneous",
            )

        return None

    def _is_cascading(self) -> bool:
        """Check if anomalies appeared sequentially (cascading) vs simultaneously."""
        if len(self.recent_anomalies) < 2:
            return False
        times = sorted(
            datetime.fromisoformat(a["timestamp"].replace("Z", "+00:00"))
            for a in self.recent_anomalies
        )
        # If gap between first and last > 10s, likely cascading
        return (times[-1] - times[0]).total_seconds() > 10
```
