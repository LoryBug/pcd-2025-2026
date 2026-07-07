package pcd.ass04.cs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DistributedLockIntegrationTest {

    @Test
    void coordinatesTwoProcessesThroughRabbitMq() throws Exception {
        assumeTrue(rabbitMqAvailable(), "RabbitMQ is not running on localhost:5672");
        String lockName = "it-" + System.nanoTime();
        AtomicInteger inside = new AtomicInteger();

        try (DistributedLockCoordinator coordinator = new DistributedLockCoordinator(
                RabbitConfig.localhost(), lockName, true);
             DistributedLock p1 = new DistributedLock(RabbitConfig.localhost(), lockName, "p1");
             DistributedLock p2 = new DistributedLock(RabbitConfig.localhost(), lockName, "p2")) {
            coordinator.awaitStarted();
            assertTrue(p1.acquire(Duration.ofSeconds(3)));
            assertTrue(inside.compareAndSet(0, 1));
            Thread contender = Thread.startVirtualThread(() -> {
                try {
                    assertTrue(p2.acquire(Duration.ofSeconds(3)));
                    assertTrue(inside.compareAndSet(0, 1));
                    inside.set(0);
                    p2.release();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(200);
            inside.set(0);
            p1.release();
            contender.join();
        }
    }

    private static boolean rabbitMqAvailable() {
        try (var connection = RabbitMq.connect(RabbitConfig.localhost())) {
            return connection.isOpen();
        } catch (Exception e) {
            return false;
        }
    }
}
