package pcd.ass04.cs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class CriticalSectionDemo {

    private CriticalSectionDemo() {
    }

    public static void main(String[] args) throws Exception {
        String lockName = args.length > 0 ? args[0] : "demo-lock";
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(3);
        try (DistributedLockCoordinator coordinator = new DistributedLockCoordinator(
                RabbitConfig.localhost(), lockName, true)) {
            coordinator.awaitStarted();
            for (int i = 1; i <= 3; i++) {
                String processId = "p" + i;
                Thread.startVirtualThread(() -> runProcess(lockName, processId, events, done));
            }
            done.await();
        }
        System.out.println(String.join(",", events));
    }

    private static void runProcess(String lockName, String processId, List<String> events, CountDownLatch done) {
        try (DistributedLock lock = new DistributedLock(RabbitConfig.localhost(), lockName, processId)) {
            if (!lock.acquire(Duration.ofSeconds(10))) {
                events.add(processId + ":timeout");
                return;
            }
            events.add(processId + ":enter");
            Thread.sleep(150);
            events.add(processId + ":exit");
            lock.release();
        } catch (Exception e) {
            events.add(processId + ":error");
        } finally {
            done.countDown();
        }
    }
}
