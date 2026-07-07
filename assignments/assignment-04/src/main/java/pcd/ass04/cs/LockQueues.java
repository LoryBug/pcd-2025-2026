package pcd.ass04.cs;

public record LockQueues(String requestQueue, String releaseQueue) {

    public static LockQueues of(String lockName) {
        String safeName = lockName.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return new LockQueues("pcd.ass04.cs." + safeName + ".request",
            "pcd.ass04.cs." + safeName + ".release");
    }
}
