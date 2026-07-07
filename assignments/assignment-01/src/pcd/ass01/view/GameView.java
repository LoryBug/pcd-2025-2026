package pcd.ass01.view;

import pcd.ass01.common.CommandBuffer;

public final class GameView {

    private final GameFrame frame;
    private final ViewModel viewModel;

    public GameView(ViewModel viewModel, CommandBuffer commands, int width, int height) {
        this(viewModel, commands, width, height, "Poool - Multithreaded");
    }

    public GameView(ViewModel viewModel, CommandBuffer commands, int width, int height, String title) {
        this.viewModel = viewModel;
        this.frame = new GameFrame(viewModel, commands, width, height, title);
        this.frame.setVisible(true);
    }

    public void render() throws InterruptedException {
        frame.render();
    }

    public ViewModel viewModel() {
        return viewModel;
    }
}
