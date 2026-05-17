package org.sudhir512kj.cronjob.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sudhir512kj.cronjob.model.CronJob;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TaskResult execute(CronJob job) {
        return switch (job.getTaskType()) {
            case HTTP_WEBHOOK -> executeHttp(job);
            case SHELL_COMMAND -> executeShell(job);
            case KAFKA_PUBLISH -> executeKafka(job);
            case GRPC_CALL -> executeGrpc(job);
        };
    }

    @SuppressWarnings("unchecked")
    private TaskResult executeHttp(CronJob job) {
        try {
            Map<String, Object> config = objectMapper.readValue(job.getTaskConfig(), Map.class);
            String url = (String) config.get("url");
            String method = (String) config.getOrDefault("method", "POST");
            String body = config.containsKey("body") ? objectMapper.writeValueAsString(config.get("body")) : "";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(job.getTimeoutSeconds()));

            if (config.containsKey("headers")) {
                Map<String, String> headers = (Map<String, String>) config.get("headers");
                headers.forEach(reqBuilder::header);
            }

            HttpRequest request = switch (method.toUpperCase()) {
                case "GET" -> reqBuilder.GET().build();
                case "PUT" -> reqBuilder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                case "DELETE" -> reqBuilder.DELETE().build();
                default -> reqBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            };

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            return new TaskResult(success, response.statusCode(), response.body(), null);
        } catch (Exception e) {
            log.error("HTTP task failed for job {}: {}", job.getId(), e.getMessage());
            return new TaskResult(false, null, null, e.getMessage());
        }
    }

    private TaskResult executeShell(CronJob job) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(job.getTaskConfig(), Map.class);
            String command = (String) config.get("command");

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(job.getTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new TaskResult(false, null, null, "Command timed out");
            }

            String output = new String(process.getInputStream().readAllBytes());
            boolean success = process.exitValue() == 0;
            return new TaskResult(success, process.exitValue(), output, success ? null : output);
        } catch (Exception e) {
            return new TaskResult(false, null, null, e.getMessage());
        }
    }

    private TaskResult executeKafka(CronJob job) {
        // Placeholder - would publish to Kafka topic
        log.info("Kafka publish task for job: {}", job.getId());
        return new TaskResult(true, null, "Message published", null);
    }

    private TaskResult executeGrpc(CronJob job) {
        // Placeholder - would make gRPC call
        log.info("gRPC call task for job: {}", job.getId());
        return new TaskResult(true, null, "gRPC call completed", null);
    }

    public record TaskResult(boolean success, Integer statusCode, String output, String error) {}
}
