package pcd.ass01.view;

/**
 * Monitor used as a rendezvous between the game loop and the Swing EDT.
 *
 * <p>The game loop requests a repaint and then waits until the EDT completes
 * the corresponding paint operation. This prevents the simulation from running
 * far ahead of rendering.
 */
public final class RenderSynch {

    private long nextFrameToRender;
    private long lastFrameRendered;

    /** Creates an empty rendering synchronizer. */
    public RenderSynch() {
        nextFrameToRender = 0;
        lastFrameRendered = -1;
    }

    /**
     * Reserves the next frame identifier for the game loop.
     *
     * @return frame number that must later be reported as rendered
     */
    public synchronized long nextFrameToRender() {
        long frame = nextFrameToRender;
        nextFrameToRender++;
        return frame;
    }

    /** Notifies waiting threads that the EDT has completed one more frame. */
    public synchronized void notifyFrameRendered() {
        lastFrameRendered++;
        notifyAll();
    }

    /**
     * Waits until the requested frame has been rendered by the EDT.
     *
     * @param frame frame number returned by {@link #nextFrameToRender()}
     * @throws InterruptedException if the game loop is interrupted while waiting
     */
    public synchronized void waitForFrameRendered(long frame) throws InterruptedException {
        while (lastFrameRendered < frame) {
            wait();
        }
    }
}
