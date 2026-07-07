package pcd.ass04.cs;

public final class CriticalSectionCoordinatorMain {

    private CriticalSectionCoordinatorMain() {
    }

    public static void main(String[] args) throws Exception {
        String lockName = args.length > 0 ? args[0] : "home-lock";
        try (DistributedLockCoordinator coordinator = new DistributedLockCoordinator(
                RabbitConfig.localhost(), lockName, true)) {
            coordinator.awaitStarted();
            System.out.println("Distributed lock coordinator ready for lock " + lockName);
            Thread.currentThread().join();
        }
    }
}
