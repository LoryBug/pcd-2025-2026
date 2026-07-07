package pcd.ass01.view;

public final class RenderSynch {

    private long nextFrameToRender;
    private long lastFrameRendered;

    public RenderSynch() {
        nextFrameToRender = 0;
        lastFrameRendered = -1;
    }

    public synchronized long nextFrameToRender() {
        long frame = nextFrameToRender;
        nextFrameToRender++;
        return frame;
    }

    public synchronized void notifyFrameRendered() {
        lastFrameRendered++;
        notifyAll();
    }

    public synchronized void waitForFrameRendered(long frame) throws InterruptedException {
        while (lastFrameRendered < frame) {
            wait();
        }
    }
}
