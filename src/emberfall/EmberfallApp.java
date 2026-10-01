package emberfall;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class EmberfallApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(EmberfallApp::open);
    }

    private static void open() {
        World world = new World(360, 200, new Random());
        world.loadVolcano();
        Stage stage = new Stage(world);
        JFrame frame = new JFrame("Emberfall");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(stage);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        stage.start();
    }

    private static final class Stage extends JPanel {
        private static final int BAR = 64;
        private final World world;
        private final BufferedImage image;
        private final int[] pixels;
        private Material selected = Material.SAND;
        private int brush = 3;
        private boolean paused;
        private boolean painting;
        private boolean erasing;
        private int frames;
        private int fps;
        private long fpsStamp = System.nanoTime();
        private volatile boolean running = true;

        Stage(World world) {
            this.world = world;
            this.image = new BufferedImage(world.width, world.height, BufferedImage.TYPE_INT_RGB);
            this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            setPreferredSize(new Dimension(1180, 760));
            setBackground(new Color(0x100E0C));
            setFocusable(true);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    if (event.getY() < BAR) {
                        pickSwatch(event.getX());
                        return;
                    }
                    painting = event.getButton() == MouseEvent.BUTTON1;
                    erasing = event.getButton() == MouseEvent.BUTTON3;
                    applyBrush(event.getX(), event.getY());
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    applyBrush(event.getX(), event.getY());
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    painting = false;
                    erasing = false;
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    onKey(event);
                }
            });
        }

        void start() {
            Thread loop = new Thread(() -> {
                long next = System.nanoTime();
                while (running) {
                    long frame = 1_000_000_000L / 60L;
                    if (!paused) {
                        synchronized (world) {
                            world.step();
                        }
                    }
                    repaint();
                    next += frame;
                    long sleep = next - System.nanoTime();
                    if (sleep > 0) {
                        try {
                            Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else {
                        next = System.nanoTime();
                    }
                }
            }, "emberfall");
            loop.setDaemon(true);
            loop.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            synchronized (world) {
                world.render(pixels);
            }
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int scale = Math.max(1, Math.min(getWidth() / world.width, (getHeight() - BAR) / world.height));
            int originX = (getWidth() - world.width * scale) / 2;
            int originY = BAR + (getHeight() - BAR - world.height * scale) / 2;
            g.drawImage(image, originX, originY, world.width * scale, world.height * scale, null);
            paintBar(g);
            frames++;
            long now = System.nanoTime();
            if (now - fpsStamp >= 1_000_000_000L) {
                fps = frames;
                frames = 0;
                fpsStamp = now;
            }
        }

        private void paintBar(Graphics2D g) {
            g.setColor(new Color(0x1A1613));
            g.fillRect(0, 0, getWidth(), BAR);
            g.setColor(new Color(0xFF6A2A));
            g.drawString("EMBERFALL", 16, 26);
            g.setColor(new Color(0xC8BBA8));
            g.drawString(selected.label + "   brush " + brush + "   " + fps + " fps   V volcano   space pause   C clear", 16, 48);
            int x = 430;
            for (Material material : Material.values()) {
                if (material == Material.EMPTY) {
                    continue;
                }
                g.setColor(new Color(material.color));
                g.fillRect(x, 16, 22, 32);
                if (material == selected) {
                    g.setColor(Color.WHITE);
                    g.drawRect(x - 2, 14, 25, 35);
                }
                x += 28;
            }
        }

        private void pickSwatch(int mouseX) {
            int x = 430;
            for (Material material : Material.values()) {
                if (material == Material.EMPTY) {
                    continue;
                }
                if (mouseX >= x && mouseX < x + 22) {
                    selected = material;
                    return;
                }
                x += 28;
            }
        }

        private void applyBrush(int mouseX, int mouseY) {
            if (!painting && !erasing) {
                return;
            }
            int scale = Math.max(1, Math.min(getWidth() / world.width, (getHeight() - BAR) / world.height));
            int originX = (getWidth() - world.width * scale) / 2;
            int originY = BAR + (getHeight() - BAR - world.height * scale) / 2;
            int gx = (mouseX - originX) / scale;
            int gy = (mouseY - originY) / scale;
            Material material = erasing ? Material.EMPTY : selected;
            synchronized (world) {
                world.paint(gx, gy, brush, material);
            }
        }

        private void onKey(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_1 -> selected = Material.SAND;
                case KeyEvent.VK_2 -> selected = Material.WATER;
                case KeyEvent.VK_3 -> selected = Material.SALT;
                case KeyEvent.VK_4 -> selected = Material.OIL;
                case KeyEvent.VK_5 -> selected = Material.LAVA;
                case KeyEvent.VK_6 -> selected = Material.FIRE;
                case KeyEvent.VK_7 -> selected = Material.WOOD;
                case KeyEvent.VK_8 -> selected = Material.PLANT;
                case KeyEvent.VK_9 -> selected = Material.ACID;
                case KeyEvent.VK_0 -> selected = Material.STONE;
                case KeyEvent.VK_Q -> selected = Material.GLASS;
                case KeyEvent.VK_W -> selected = Material.GUNPOWDER;
                case KeyEvent.VK_E -> selected = Material.ICE;
                case KeyEvent.VK_R -> selected = Material.SMOKE;
                case KeyEvent.VK_OPEN_BRACKET -> brush = Math.max(0, brush - 1);
                case KeyEvent.VK_CLOSE_BRACKET -> brush = Math.min(18, brush + 1);
                case KeyEvent.VK_SPACE -> paused = !paused;
                case KeyEvent.VK_C -> {
                    synchronized (world) {
                        world.clear();
                    }
                }
                case KeyEvent.VK_V -> {
                    synchronized (world) {
                        world.loadVolcano();
                    }
                }
                default -> {
                }
            }
            repaint();
        }
    }
}
