package pcd.ass04.cs;

import java.time.Duration;

public final class CriticalSectionProcessMain {

    private CriticalSectionProcessMain() {
    }

    public static void main(String[] args) throws Exception {
        String processId = args.length > 0 ? args[0] : "p1";
        String lockName = args.length > 1 ? args[1] : "home-lock";
        long holdMillis = args.length > 2 ? Long.parseLong(args[2]) : 1000L;
        try (DistributedLock lock = new DistributedLock(RabbitConfig.localhost(), lockName, processId)) {
            if (!lock.acquire(Duration.ofSeconds(10))) {
                throw new IllegalStateException("Timeout while waiting for lock");
            }
            System.out.println(processId + " entered critical section");
            Thread.sleep(holdMillis);
            System.out.println(processId + " leaving critical section");
            lock.release();
        }
    }
}
