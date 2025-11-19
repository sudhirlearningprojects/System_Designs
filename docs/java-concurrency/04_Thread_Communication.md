# Thread Communication

## Table of Contents
- [Inter-Thread Communication](#inter-thread-communication)
- [wait(), notify(), and notifyAll()](#wait-notify-and-notifyall)
- [Producer-Consumer Pattern](#producer-consumer-pattern)
- [BlockingQueue](#blockingqueue)
- [CountDownLatch](#countdownlatch)
- [CyclicBarrier](#cyclicbarrier)
- [Semaphore](#semaphore)
- [Exchanger](#exchanger)
- [Phaser](#phaser)

## Inter-Thread Communication

Thread communication allows threads to coordinate their activities and share information safely. Java provides several mechanisms for threads to communicate with each other.

### Why Thread Communication is Needed

```java
public class WithoutCommunication {
    private boolean dataReady = false;
    private String data;
    
    public void producer() {
        // Simulate data preparation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        data = "Important Data";
        dataReady = true;
        System.out.println("Data produced");
    }
    
    public void consumer() {
        // Inefficient polling
        while (!dataReady) {
            try {
                Thread.sleep(100); // Waste CPU cycles
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("Consumed: " + data);
    }
    
    public static void main(String[] args) {
        WithoutCommunication example = new WithoutCommunication();
        
        Thread producer = new Thread(example::producer);
        Thread consumer = new Thread(example::consumer);
        
        consumer.start();
        producer.start();
    }
}
```

## wait(), notify(), and notifyAll()

These methods provide the foundation for thread communication in Java. They must be called within a synchronized block.

### Basic wait/notify Example

```java
public class WaitNotifyExample {
    private boolean dataReady = false;
    private String data;
    private final Object lock = new Object();
    
    public void producer() {
        synchronized (lock) {
            try {
                Thread.sleep(2000); // Simulate data preparation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            data = "Important Data";
            dataReady = true;
            System.out.println("Data produced, notifying consumer");
            lock.notify(); // Wake up waiting consumer
        }
    }
    
    public void consumer() {
        synchronized (lock) {
            while (!dataReady) {
                try {
                    System.out.println("Consumer waiting for data...");
                    lock.wait(); // Release lock and wait
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println("Consumed: " + data);
        }
    }
    
    public static void main(String[] args) {
        WaitNotifyExample example = new WaitNotifyExample();
        
        Thread consumer = new Thread(example::consumer);
        Thread producer = new Thread(example::producer);
        
        consumer.start();
        producer.start();
    }
}
```

### wait() vs sleep()

```java
public class WaitVsSleep {
    private final Object lock = new Object();
    
    public void demonstrateWait() {
        synchronized (lock) {
            System.out.println("Before wait() - holding lock");
            try {
                lock.wait(2000); // Releases lock, waits for notification or timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("After wait() - reacquired lock");
        }
    }
    
    public void demonstrateSleep() {
        synchronized (lock) {
            System.out.println("Before sleep() - holding lock");
            try {
                Thread.sleep(2000); // Keeps holding the lock!
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("After sleep() - still holding lock");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        WaitVsSleep example = new WaitVsSleep();
        
        // Demonstrate wait() - releases lock
        Thread thread1 = new Thread(example::demonstrateWait);
        Thread thread2 = new Thread(() -> {
            synchronized (example.lock) {
                System.out.println("Thread2 acquired lock while thread1 is waiting");
            }
        });
        
        thread1.start();
        Thread.sleep(500); // Let thread1 start waiting
        thread2.start(); // This will acquire the lock
        
        thread1.join();
        thread2.join();
        
        System.out.println("---");
        
        // Demonstrate sleep() - keeps lock
        Thread thread3 = new Thread(example::demonstrateSleep);
        Thread thread4 = new Thread(() -> {
            synchronized (example.lock) {
                System.out.println("Thread4 acquired lock after thread3 finished sleeping");
            }
        });
        
        thread3.start();
        Thread.sleep(500); // Let thread3 start sleeping
        thread4.start(); // This will wait until thread3 releases lock
        
        thread3.join();
        thread4.join();
    }
}
```

### notify() vs notifyAll()

```java
public class NotifyVsNotifyAll {
    private final Object lock = new Object();
    private boolean condition = false;
    
    public void waitingMethod(String threadName) {
        synchronized (lock) {
            while (!condition) {
                try {
                    System.out.println(threadName + " is waiting");
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println(threadName + " proceeded");
        }
    }
    
    public void notifyOneThread() {
        synchronized (lock) {
            condition = true;
            System.out.println("Condition set to true, notifying one thread");
            lock.notify(); // Only one waiting thread will be awakened
        }
    }
    
    public void notifyAllThreads() {
        synchronized (lock) {
            condition = true;
            System.out.println("Condition set to true, notifying all threads");
            lock.notifyAll(); // All waiting threads will be awakened
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        NotifyVsNotifyAll example = new NotifyVsNotifyAll();
        
        // Create multiple waiting threads
        for (int i = 1; i <= 3; i++) {
            Thread thread = new Thread(() -> 
                example.waitingMethod(Thread.currentThread().getName()));
            thread.setName("Waiter-" + i);
            thread.start();
        }
        
        Thread.sleep(1000);
        
        // Use notify() - only one thread will proceed
        example.notifyOneThread();
        
        Thread.sleep(2000);
        
        // Reset condition for second demonstration
        example.condition = false;
        
        // Create more waiting threads
        for (int i = 4; i <= 6; i++) {
            Thread thread = new Thread(() -> 
                example.waitingMethod(Thread.currentThread().getName()));
            thread.setName("Waiter-" + i);
            thread.start();
        }
        
        Thread.sleep(1000);
        
        // Use notifyAll() - all threads will proceed
        example.notifyAllThreads();
    }
}
```

## Producer-Consumer Pattern

The producer-consumer pattern is a classic concurrency pattern where producers generate data and consumers process it.

### Simple Producer-Consumer with wait/notify

```java
import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumer {
    private final Queue<Integer> buffer = new LinkedList<>();
    private final int capacity = 5;
    private final Object lock = new Object();
    
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (lock) {
                while (buffer.size() == capacity) {
                    System.out.println("Buffer full, producer waiting...");
                    lock.wait();
                }
                
                buffer.offer(value);
                System.out.println("Produced: " + value + ", Buffer size: " + buffer.size());
                value++;
                
                lock.notifyAll(); // Notify consumers
            }
            Thread.sleep(1000); // Simulate production time
        }
    }
    
    public void consume() throws InterruptedException {
        while (true) {
            synchronized (lock) {
                while (buffer.isEmpty()) {
                    System.out.println("Buffer empty, consumer waiting...");
                    lock.wait();
                }
                
                int value = buffer.poll();
                System.out.println("Consumed: " + value + ", Buffer size: " + buffer.size());
                
                lock.notifyAll(); // Notify producers
            }
            Thread.sleep(1500); // Simulate consumption time
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();
        
        Thread producer = new Thread(() -> {
            try {
                pc.produce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                pc.consume();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
    }
}
```

### Multiple Producers and Consumers

```java
public class MultipleProducerConsumer {
    private final Queue<String> buffer = new LinkedList<>();
    private final int capacity = 10;
    private final Object lock = new Object();
    
    public void produce(String producerId) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            synchronized (lock) {
                while (buffer.size() == capacity) {
                    lock.wait();
                }
                
                String item = producerId + "-Item-" + i;
                buffer.offer(item);
                System.out.println("Produced: " + item + " [Buffer: " + buffer.size() + "]");
                
                lock.notifyAll();
            }
            Thread.sleep(500);
        }
    }
    
    public void consume(String consumerId) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            synchronized (lock) {
                while (buffer.isEmpty()) {
                    lock.wait();
                }
                
                String item = buffer.poll();
                System.out.println("Consumed: " + item + " by " + consumerId + 
                    " [Buffer: " + buffer.size() + "]");
                
                lock.notifyAll();
            }
            Thread.sleep(700);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        MultipleProducerConsumer mpc = new MultipleProducerConsumer();
        
        // Create multiple producers
        Thread producer1 = new Thread(() -> {
            try {
                mpc.produce("Producer-1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread producer2 = new Thread(() -> {
            try {
                mpc.produce("Producer-2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Create multiple consumers
        Thread consumer1 = new Thread(() -> {
            try {
                mpc.consume("Consumer-1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer2 = new Thread(() -> {
            try {
                mpc.consume("Consumer-2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();
        
        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();
    }
}
```

## BlockingQueue

BlockingQueue provides a thread-safe queue with blocking operations, making producer-consumer implementation much simpler.

### ArrayBlockingQueue Example

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueExample {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
    
    public void producer(String producerId) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            String item = producerId + "-Item-" + i;
            queue.put(item); // Blocks if queue is full
            System.out.println("Produced: " + item + " [Queue size: " + queue.size() + "]");
            Thread.sleep(300);
        }
    }
    
    public void consumer(String consumerId) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            String item = queue.take(); // Blocks if queue is empty
            System.out.println("Consumed: " + item + " by " + consumerId + 
                " [Queue size: " + queue.size() + "]");
            Thread.sleep(500);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        BlockingQueueExample example = new BlockingQueueExample();
        
        Thread producer = new Thread(() -> {
            try {
                example.producer("Producer");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                example.consumer("Consumer");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
}
```

### Different BlockingQueue Implementations

```java
import java.util.concurrent.*;

public class BlockingQueueTypes {
    public static void demonstrateArrayBlockingQueue() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(3);
        
        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String item = "Item-" + i;
                    System.out.println("Trying to put: " + item);
                    queue.put(item); // Blocks when queue is full
                    System.out.println("Put: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        Thread.sleep(2000); // Let producer fill the queue
        
        // Consumer
        while (!queue.isEmpty()) {
            System.out.println("Took: " + queue.take());
            Thread.sleep(500);
        }
        
        producer.join();
    }
    
    public static void demonstrateLinkedBlockingQueue() throws InterruptedException {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(); // Unbounded
        
        // Producer won't block (unless memory runs out)
        for (int i = 0; i < 1000; i++) {
            queue.put("Item-" + i);
        }
        
        System.out.println("LinkedBlockingQueue size: " + queue.size());
        
        // Consume some items
        for (int i = 0; i < 5; i++) {
            System.out.println("Consumed: " + queue.take());
        }
    }
    
    public static void demonstratePriorityBlockingQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = new PriorityBlockingQueue<>();
        
        // Add items in random order
        queue.put(5);
        queue.put(1);
        queue.put(3);
        queue.put(2);
        queue.put(4);
        
        // Items come out in priority order (natural ordering)
        while (!queue.isEmpty()) {
            System.out.println("Priority item: " + queue.take());
        }
    }
    
    public static void demonstrateSynchronousQueue() throws InterruptedException {
        BlockingQueue<String> queue = new SynchronousQueue<>();
        
        Thread producer = new Thread(() -> {
            try {
                System.out.println("Producer putting item...");
                queue.put("Direct handoff"); // Blocks until consumer takes
                System.out.println("Producer finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(2000); // Make producer wait
                System.out.println("Consumer taking item...");
                String item = queue.take();
                System.out.println("Consumer got: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ArrayBlockingQueue ===");
        demonstrateArrayBlockingQueue();
        
        System.out.println("\n=== LinkedBlockingQueue ===");
        demonstrateLinkedBlockingQueue();
        
        System.out.println("\n=== PriorityBlockingQueue ===");
        demonstratePriorityBlockingQueue();
        
        System.out.println("\n=== SynchronousQueue ===");
        demonstrateSynchronousQueue();
    }
}
```

## CountDownLatch

CountDownLatch allows one or more threads to wait until a set of operations being performed in other threads completes.

```java
import java.util.concurrent.CountDownLatch;

public class CountDownLatchExample {
    
    public static void raceExample() throws InterruptedException {
        final int numRunners = 5;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numRunners);
        
        // Create runner threads
        for (int i = 0; i < numRunners; i++) {
            Thread runner = new Thread(new Runner(startSignal, doneSignal, "Runner-" + i));
            runner.start();
        }
        
        System.out.println("Preparing race...");
        Thread.sleep(2000);
        
        System.out.println("Starting race!");
        startSignal.countDown(); // Release all runners
        
        doneSignal.await(); // Wait for all runners to finish
        System.out.println("Race finished!");
    }
    
    static class Runner implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;
        private final String name;
        
        Runner(CountDownLatch startSignal, CountDownLatch doneSignal, String name) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.name = name;
        }
        
        @Override
        public void run() {
            try {
                System.out.println(name + " ready");
                startSignal.await(); // Wait for start signal
                
                // Simulate running
                int runTime = (int) (Math.random() * 3000) + 1000;
                Thread.sleep(runTime);
                
                System.out.println(name + " finished in " + runTime + "ms");
                doneSignal.countDown(); // Signal completion
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void serviceStartupExample() throws InterruptedException {
        final int numServices = 3;
        CountDownLatch servicesReady = new CountDownLatch(numServices);
        
        // Start services
        Thread dbService = new Thread(new Service("Database", 2000, servicesReady));
        Thread cacheService = new Thread(new Service("Cache", 1500, servicesReady));
        Thread webService = new Thread(new Service("WebServer", 1000, servicesReady));
        
        dbService.start();
        cacheService.start();
        webService.start();
        
        System.out.println("Waiting for all services to start...");
        servicesReady.await(); // Wait for all services
        
        System.out.println("All services started! Application ready.");
    }
    
    static class Service implements Runnable {
        private final String name;
        private final int startupTime;
        private final CountDownLatch latch;
        
        Service(String name, int startupTime, CountDownLatch latch) {
            this.name = name;
            this.startupTime = startupTime;
            this.latch = latch;
        }
        
        @Override
        public void run() {
            try {
                System.out.println(name + " starting...");
                Thread.sleep(startupTime);
                System.out.println(name + " started");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Race Example ===");
        raceExample();
        
        System.out.println("\n=== Service Startup Example ===");
        serviceStartupExample();
    }
}
```

## CyclicBarrier

CyclicBarrier allows a set of threads to all wait for each other to reach a common barrier point.

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {
    
    public static void matrixMultiplication() throws InterruptedException {
        final int numWorkers = 4;
        final int[][] matrixA = {{1, 2}, {3, 4}};
        final int[][] matrixB = {{5, 6}, {7, 8}};
        final int[][] result = new int[2][2];
        
        CyclicBarrier barrier = new CyclicBarrier(numWorkers, () -> {
            System.out.println("All workers completed their part. Result:");
            for (int[] row : result) {
                for (int val : row) {
                    System.out.print(val + " ");
                }
                System.out.println();
            }
        });
        
        // Create worker threads for each cell
        Thread worker1 = new Thread(new MatrixWorker(matrixA, matrixB, result, 0, 0, barrier));
        Thread worker2 = new Thread(new MatrixWorker(matrixA, matrixB, result, 0, 1, barrier));
        Thread worker3 = new Thread(new MatrixWorker(matrixA, matrixB, result, 1, 0, barrier));
        Thread worker4 = new Thread(new MatrixWorker(matrixA, matrixB, result, 1, 1, barrier));
        
        worker1.start();
        worker2.start();
        worker3.start();
        worker4.start();
        
        worker1.join();
        worker2.join();
        worker3.join();
        worker4.join();
    }
    
    static class MatrixWorker implements Runnable {
        private final int[][] matrixA;
        private final int[][] matrixB;
        private final int[][] result;
        private final int row;
        private final int col;
        private final CyclicBarrier barrier;
        
        MatrixWorker(int[][] matrixA, int[][] matrixB, int[][] result, 
                    int row, int col, CyclicBarrier barrier) {
            this.matrixA = matrixA;
            this.matrixB = matrixB;
            this.result = result;
            this.row = row;
            this.col = col;
            this.barrier = barrier;
        }
        
        @Override
        public void run() {
            try {
                // Calculate one cell of result matrix
                int sum = 0;
                for (int k = 0; k < matrixA[0].length; k++) {
                    sum += matrixA[row][k] * matrixB[k][col];
                }
                result[row][col] = sum;
                
                System.out.println("Worker calculated result[" + row + "][" + col + "] = " + sum);
                
                // Wait for all workers to complete
                barrier.await();
                
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void phaseProcessing() throws InterruptedException {
        final int numThreads = 3;
        final int numPhases = 3;
        
        CyclicBarrier barrier = new CyclicBarrier(numThreads, () -> {
            System.out.println("Phase completed by all threads\n");
        });
        
        for (int i = 0; i < numThreads; i++) {
            Thread worker = new Thread(new PhaseWorker(i, numPhases, barrier));
            worker.start();
        }
        
        Thread.sleep(10000); // Let workers complete
    }
    
    static class PhaseWorker implements Runnable {
        private final int workerId;
        private final int numPhases;
        private final CyclicBarrier barrier;
        
        PhaseWorker(int workerId, int numPhases, CyclicBarrier barrier) {
            this.workerId = workerId;
            this.numPhases = numPhases;
            this.barrier = barrier;
        }
        
        @Override
        public void run() {
            try {
                for (int phase = 1; phase <= numPhases; phase++) {
                    // Simulate work for this phase
                    int workTime = (int) (Math.random() * 2000) + 500;
                    Thread.sleep(workTime);
                    
                    System.out.println("Worker " + workerId + " completed phase " + phase);
                    
                    // Wait for all workers to complete this phase
                    barrier.await();
                }
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Matrix Multiplication Example ===");
        matrixMultiplication();
        
        System.out.println("\n=== Phase Processing Example ===");
        phaseProcessing();
    }
}
```

## Semaphore

Semaphore maintains a set of permits and is used to control access to a resource pool.

```java
import java.util.concurrent.Semaphore;

public class SemaphoreExample {
    
    public static void connectionPoolExample() throws InterruptedException {
        final int poolSize = 3;
        Semaphore connectionPool = new Semaphore(poolSize);
        
        // Create multiple clients trying to get connections
        for (int i = 1; i <= 6; i++) {
            Thread client = new Thread(new DatabaseClient(i, connectionPool));
            client.start();
        }
        
        Thread.sleep(10000); // Let clients finish
    }
    
    static class DatabaseClient implements Runnable {
        private final int clientId;
        private final Semaphore connectionPool;
        
        DatabaseClient(int clientId, Semaphore connectionPool) {
            this.clientId = clientId;
            this.connectionPool = connectionPool;
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Client " + clientId + " requesting connection...");
                connectionPool.acquire(); // Get a permit
                
                System.out.println("Client " + clientId + " got connection. Available: " + 
                    connectionPool.availablePermits());
                
                // Simulate database work
                Thread.sleep(2000);
                
                System.out.println("Client " + clientId + " releasing connection");
                connectionPool.release(); // Return the permit
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void printQueueExample() throws InterruptedException {
        final int maxPrintJobs = 2;
        Semaphore printQueue = new Semaphore(maxPrintJobs);
        
        for (int i = 1; i <= 5; i++) {
            Thread printJob = new Thread(new PrintJob(i, printQueue));
            printJob.start();
        }
        
        Thread.sleep(15000); // Let print jobs finish
    }
    
    static class PrintJob implements Runnable {
        private final int jobId;
        private final Semaphore printQueue;
        
        PrintJob(int jobId, Semaphore printQueue) {
            this.jobId = jobId;
            this.printQueue = printQueue;
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Print job " + jobId + " waiting in queue...");
                printQueue.acquire();
                
                System.out.println("Print job " + jobId + " started printing");
                Thread.sleep(3000); // Simulate printing time
                
                System.out.println("Print job " + jobId + " finished printing");
                printQueue.release();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Connection Pool Example ===");
        connectionPoolExample();
        
        System.out.println("\n=== Print Queue Example ===");
        printQueueExample();
    }
}
```

## Exchanger

Exchanger allows two threads to exchange objects at a synchronization point.

```java
import java.util.concurrent.Exchanger;

public class ExchangerExample {
    
    public static void dataExchangeExample() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        
        Thread producer = new Thread(new DataProducer(exchanger));
        Thread consumer = new Thread(new DataConsumer(exchanger));
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    static class DataProducer implements Runnable {
        private final Exchanger<String> exchanger;
        
        DataProducer(Exchanger<String> exchanger) {
            this.exchanger = exchanger;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 3; i++) {
                    String data = "Data-" + i;
                    System.out.println("Producer created: " + data);
                    
                    // Exchange data with consumer
                    String received = exchanger.exchange(data);
                    System.out.println("Producer received acknowledgment: " + received);
                    
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    static class DataConsumer implements Runnable {
        private final Exchanger<String> exchanger;
        
        DataConsumer(Exchanger<String> exchanger) {
            this.exchanger = exchanger;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 3; i++) {
                    // Exchange acknowledgment with producer
                    String received = exchanger.exchange("ACK-" + i);
                    System.out.println("Consumer received: " + received);
                    
                    // Process the data
                    Thread.sleep(500);
                    System.out.println("Consumer processed: " + received);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void bufferSwapExample() throws InterruptedException {
        Exchanger<StringBuilder> exchanger = new Exchanger<>();
        
        Thread writer = new Thread(new BufferWriter(exchanger));
        Thread reader = new Thread(new BufferReader(exchanger));
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
    }
    
    static class BufferWriter implements Runnable {
        private final Exchanger<StringBuilder> exchanger;
        private StringBuilder buffer = new StringBuilder();
        
        BufferWriter(Exchanger<StringBuilder> exchanger) {
            this.exchanger = exchanger;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 3; i++) {
                    // Fill buffer
                    buffer.append("Message ").append(i).append("\n");
                    System.out.println("Writer filled buffer: " + buffer.toString().trim());
                    
                    // Exchange full buffer for empty buffer
                    buffer = exchanger.exchange(buffer);
                    System.out.println("Writer got empty buffer back");
                    
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    static class BufferReader implements Runnable {
        private final Exchanger<StringBuilder> exchanger;
        private StringBuilder buffer = new StringBuilder();
        
        BufferReader(Exchanger<StringBuilder> exchanger) {
            this.exchanger = exchanger;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 1; i <= 3; i++) {
                    // Exchange empty buffer for full buffer
                    buffer = exchanger.exchange(buffer);
                    System.out.println("Reader received buffer: " + buffer.toString().trim());
                    
                    // Process and clear buffer
                    buffer.setLength(0); // Clear buffer
                    System.out.println("Reader processed and cleared buffer");
                    
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Data Exchange Example ===");
        dataExchangeExample();
        
        System.out.println("\n=== Buffer Swap Example ===");
        bufferSwapExample();
    }
}
```

## Phaser

Phaser is a more flexible alternative to CountDownLatch and CyclicBarrier, supporting dynamic registration and multiple phases.

```java
import java.util.concurrent.Phaser;

public class PhaserExample {
    
    public static void basicPhaserExample() throws InterruptedException {
        Phaser phaser = new Phaser(1); // Register main thread
        
        // Create worker threads
        for (int i = 1; i <= 3; i++) {
            Thread worker = new Thread(new PhaserWorker(i, phaser));
            worker.start();
        }
        
        // Wait for all workers to complete phase 0
        phaser.arriveAndAwaitAdvance();
        System.out.println("Main: All workers completed phase 0");
        
        // Wait for all workers to complete phase 1
        phaser.arriveAndAwaitAdvance();
        System.out.println("Main: All workers completed phase 1");
        
        // Deregister main thread
        phaser.arriveAndDeregister();
        
        Thread.sleep(2000); // Let workers finish
    }
    
    static class PhaserWorker implements Runnable {
        private final int workerId;
        private final Phaser phaser;
        
        PhaserWorker(int workerId, Phaser phaser) {
            this.workerId = workerId;
            this.phaser = phaser;
            phaser.register(); // Register this worker
        }
        
        @Override
        public void run() {
            try {
                // Phase 0
                System.out.println("Worker " + workerId + " working on phase 0");
                Thread.sleep(1000 + workerId * 500);
                System.out.println("Worker " + workerId + " completed phase 0");
                phaser.arriveAndAwaitAdvance();
                
                // Phase 1
                System.out.println("Worker " + workerId + " working on phase 1");
                Thread.sleep(800 + workerId * 300);
                System.out.println("Worker " + workerId + " completed phase 1");
                phaser.arriveAndAwaitAdvance();
                
                // Deregister when done
                phaser.arriveAndDeregister();
                System.out.println("Worker " + workerId + " finished all phases");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void dynamicPhaserExample() throws InterruptedException {
        Phaser phaser = new Phaser() {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println("Phase " + phase + " completed with " + 
                    registeredParties + " parties");
                return phase >= 2 || registeredParties == 0; // Terminate after phase 2
            }
        };
        
        // Start with 2 workers
        for (int i = 1; i <= 2; i++) {
            Thread worker = new Thread(new DynamicWorker(i, phaser, false));
            worker.start();
        }
        
        Thread.sleep(1000);
        
        // Add more workers dynamically
        for (int i = 3; i <= 4; i++) {
            Thread worker = new Thread(new DynamicWorker(i, phaser, true));
            worker.start();
        }
        
        Thread.sleep(10000); // Let workers finish
    }
    
    static class DynamicWorker implements Runnable {
        private final int workerId;
        private final Phaser phaser;
        private final boolean lateJoiner;
        
        DynamicWorker(int workerId, Phaser phaser, boolean lateJoiner) {
            this.workerId = workerId;
            this.phaser = phaser;
            this.lateJoiner = lateJoiner;
            phaser.register();
        }
        
        @Override
        public void run() {
            try {
                if (lateJoiner) {
                    Thread.sleep(1500); // Join late
                }
                
                while (!phaser.isTerminated()) {
                    int phase = phaser.getPhase();
                    System.out.println("Worker " + workerId + " working on phase " + phase);
                    
                    Thread.sleep(1000);
                    
                    if (workerId == 2 && phase == 1) {
                        // Worker 2 leaves after phase 1
                        System.out.println("Worker " + workerId + " leaving after phase " + phase);
                        phaser.arriveAndDeregister();
                        break;
                    } else {
                        phaser.arriveAndAwaitAdvance();
                    }
                }
                
                if (!phaser.isTerminated()) {
                    phaser.arriveAndDeregister();
                }
                
                System.out.println("Worker " + workerId + " finished");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Basic Phaser Example ===");
        basicPhaserExample();
        
        System.out.println("\n=== Dynamic Phaser Example ===");
        dynamicPhaserExample();
    }
}
```

## Summary

- **wait/notify/notifyAll**: Basic thread communication using intrinsic locks
- **Producer-Consumer**: Classic pattern for coordinating producers and consumers
- **BlockingQueue**: Thread-safe queues with blocking operations
- **CountDownLatch**: Wait for multiple threads to complete
- **CyclicBarrier**: Synchronize threads at a common barrier point
- **Semaphore**: Control access to a resource pool
- **Exchanger**: Exchange objects between two threads
- **Phaser**: Flexible multi-phase synchronization

## Next Steps

Continue to [Concurrent Collections](05_Concurrent_Collections.md) to learn about thread-safe data structures.