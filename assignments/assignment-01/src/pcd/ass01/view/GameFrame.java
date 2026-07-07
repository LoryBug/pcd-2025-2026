package pcd.ass01.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import pcd.ass01.common.BallSnapshot;
import pcd.ass01.common.CommandBuffer;
import pcd.ass01.common.GameSnapshot;
import pcd.ass01.common.GameStatus;
import pcd.ass01.common.Hole;
import pcd.ass01.common.KickHumanCommand;
import pcd.ass01.common.Player;
import pcd.ass01.common.Vec2;

public final class GameFrame extends JFrame implements KeyListener {

    private static final double HUMAN_IMPULSE = 0.45;

    private final VisualiserPanel panel;
    private final ViewModel model;
    private final RenderSynch sync;
    private final CommandBuffer commands;

    public GameFrame(ViewModel model, CommandBuffer commands, int width, int height, String title) {
        this.model = model;
        this.commands = commands;
        this.sync = new RenderSynch();
        setTitle(title);
        setSize(width, height + 35);
        setResizable(false);
        this.panel = new VisualiserPanel(width, height);
        getContentPane().add(panel);
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                System.exit(0);
            }
        });
    }

    public void render() throws InterruptedException {
        long frame = sync.nextFrameToRender();
        panel.repaint();
        sync.waitForFrameRendered(frame);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Vec2 impulse = switch (e.getExtendedKeyCode()) {
            case KeyEvent.VK_UP -> new Vec2(0, HUMAN_IMPULSE);
            case KeyEvent.VK_DOWN -> new Vec2(0, -HUMAN_IMPULSE);
            case KeyEvent.VK_LEFT -> new Vec2(-HUMAN_IMPULSE, 0);
            case KeyEvent.VK_RIGHT -> new Vec2(HUMAN_IMPULSE, 0);
            default -> null;
        };
        if (impulse != null) {
            commands.offer(new KickHumanCommand(impulse));
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private final class VisualiserPanel extends JPanel {
        private final int ox;
        private final int oy;
        private final int delta;

        VisualiserPanel(int width, int height) {
            setSize(width, height + 35);
            ox = width / 2;
            oy = height / 2;
            delta = Math.min(ox, oy);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GameSnapshot snapshot = model.snapshot();
            if (snapshot == null) {
                sync.notifyFrameRendered();
                return;
            }

            drawBoard(g2, snapshot);
            sync.notifyFrameRendered();
        }

        private void drawBoard(Graphics2D g2, GameSnapshot snapshot) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(ox, 0, ox, oy * 2);
            g2.drawLine(0, oy, ox * 2, oy);

            g2.setColor(new Color(55, 55, 55));
            g2.drawRect(0, 0, ox * 2 - 1, oy * 2 - 1);

            for (Hole hole : snapshot.holes()) {
                drawHole(g2, hole);
            }
            for (BallSnapshot ball : snapshot.smallBalls()) {
                drawBall(g2, ball, colorFor(ball.owner()), 1);
            }
            drawBall(g2, snapshot.humanBall(), new Color(30, 90, 220), 3);
            drawBall(g2, snapshot.botBall(), new Color(210, 80, 40), 3);
            drawHud(g2, snapshot);
        }

        private void drawHole(Graphics2D g2, Hole hole) {
            int radius = scale(hole.radius());
            int x = sx(hole.center().x());
            int y = sy(hole.center().y());
            g2.setColor(Color.BLACK);
            g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        }

        private void drawBall(Graphics2D g2, BallSnapshot ball, Color color, int stroke) {
            int radius = scale(ball.radius());
            int x = sx(ball.pos().x());
            int y = sy(ball.pos().y());
            g2.setColor(color);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }

        private void drawHud(Graphics2D g2, GameSnapshot snapshot) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
            g2.drawString("Human: " + snapshot.humanScore(), 20, 25);
            g2.drawString("Bot: " + snapshot.botScore(), ox * 2 - 90, 25);
            g2.drawString("Small balls: " + snapshot.smallBalls().size(), 20, 45);
            g2.drawString("FPS: " + snapshot.framePerSec(), 20, 65);
            if (snapshot.status() != GameStatus.RUNNING) {
                g2.drawString("Game over: " + snapshot.status(), ox - 60, 25);
            }
        }

        private Color colorFor(Player owner) {
            return switch (owner) {
                case HUMAN -> new Color(80, 130, 240);
                case BOT -> new Color(230, 120, 80);
                case NONE -> Color.DARK_GRAY;
            };
        }

        private int sx(double x) {
            return (int) (ox + x * delta);
        }

        private int sy(double y) {
            return (int) (oy - y * delta);
        }

        private int scale(double value) {
            return Math.max(2, (int) (value * delta));
        }
    }
}
