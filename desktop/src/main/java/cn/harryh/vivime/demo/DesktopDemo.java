/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.vivime.demo;

import cn.harryh.arkpets.entertainment.EffectPreview;
import cn.harryh.arkpets.office.OfficeMotion;
import cn.harryh.arkpets.office.PlacementStore;
import cn.harryh.arkpets.office.ReleaseVelocity;
import cn.harryh.arkpets.utils.Plane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/** Asset-free Windows example that runs the extracted office physics. */
public final class DesktopDemo {
    private static final int PET_WIDTH = 94;
    private static final int PET_HEIGHT = 99;
    private final Rectangle work = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    private final JFrame support = new JFrame("维维美桌宠 · 开源示例");
    private final JWindow pet = new JWindow(support);
    private final Plane plane = new Plane();
    private final ReleaseVelocity velocity = new ReleaseVelocity();
    private final EffectPreview effect = new EffectPreview();
    private final JLabel status = new JLabel();
    private final JButton walk = new JButton("沿窗口走两步");
    private final JButton visibility = new JButton("隐藏角色");
    private final boolean smokeMode;
    private final Plane.RectArea screen;
    private final PlacementStore placement;
    private final OfficeMotion motion;
    private final Timer ticker = new Timer(16, event -> tick());
    private long lastFrame;
    private boolean hidden;
    private Point dragStart;
    private float startingX;
    private float startingY;

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].startsWith("--smoke-dir="))) {
            System.getLogger("ViviMeDemo").log(System.Logger.Level.ERROR,
                    "此构建提供程序绘制的角色。加载专有模型或效果需要另外获准的运行时和素材。可用参数为 --smoke-dir=<目录>。");
            System.exit(2);
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.getLogger("ViviMeDemo").log(System.Logger.Level.ERROR, "请从 Windows 图形桌面运行示例。");
            System.exit(2);
        }
        Path smoke = args.length == 0 ? null : Path.of(args[0].substring("--smoke-dir=".length()));
        EventQueue.invokeLater(() -> new DesktopDemo(smoke).show(smoke));
    }

    private DesktopDemo(Path smoke) {
        smokeMode = smoke != null;
        String local = System.getenv("LOCALAPPDATA");
        Path profile = local == null || local.isBlank()
                ? Path.of(System.getProperty("user.home"), ".vivime-demo") : Path.of(local, "ViviMeDemo");
        placement = new PlacementStore((smoke == null ? profile : smoke).resolve("placement.json"));
        screen = new Plane.RectArea(work.x, work.x+work.width, -work.y, -work.y-work.height);
        plane.world.add(screen);
        plane.setObjSize(PET_WIDTH, PET_HEIGHT);
        plane.setGravity(700);
        plane.setFrict(100, 500);
        plane.setSpeedLimit(850, 850);
        motion = new OfficeMotion(plane, PET_WIDTH, PET_HEIGHT, placement);
        motion.areas(screen, screen);
        configureSupport();
        configurePet();
    }

    private void configureSupport() {
        support.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        support.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { close(); }
        });
        var buttons = new JPanel();
        JButton drop = new JButton("落到窗口顶沿");
        JButton pulse = new JButton("播放索敌示意");
        drop.addActionListener(event -> drop());
        walk.addActionListener(event -> motion.startManualWalk(.1, .5));
        pulse.addActionListener(event -> effect.start());
        visibility.addActionListener(event -> setHidden(!hidden));
        buttons.add(drop);
        buttons.add(walk);
        buttons.add(pulse);
        buttons.add(visibility);
        if (smokeMode) {
            for (var button : buttons.getComponents()) button.setEnabled(false);
            support.setAlwaysOnTop(true);
        }
        status.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        support.add(status, BorderLayout.NORTH);
        support.add(effect, BorderLayout.CENTER);
        support.add(buttons, BorderLayout.SOUTH);
        int width = Math.min(660, work.width-16);
        int height = Math.min(300, work.height/2);
        support.setBounds(work.x+(work.width-width)/2, work.y+work.height/2, width, height);
    }

    private void configurePet() {
        pet.setAlwaysOnTop(true);
        pet.setFocusableWindowState(false);
        pet.setSize(PET_WIDTH, PET_HEIGHT);
        var device = pet.getGraphicsConfiguration().getDevice();
        if (device.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            pet.setBackground(new Color(0, 0, 0, 0));
        }
        if (device.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            pet.setShape(new RoundRectangle2D.Float(9, 4, 76, 94, 22, 22));
        }
        var drawing = new PlaceholderPet();
        var drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) return;
                dragStart = event.getLocationOnScreen();
                startingX = plane.getX();
                startingY = plane.getY();
                velocity.clear();
                velocity.sample(System.nanoTime(), dragStart.x, dragStart.y);
                motion.beginDrag();
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (dragStart == null) return;
                Point at = event.getLocationOnScreen();
                velocity.sample(System.nanoTime(), at.x, at.y);
                motion.drag(startingX + at.x - dragStart.x, startingY - at.y + dragStart.y);
            }

            @Override public void mouseReleased(MouseEvent event) {
                if (dragStart == null || event.getButton() != MouseEvent.BUTTON1) return;
                Point at = event.getLocationOnScreen();
                long now = System.nanoTime();
                velocity.sample(now, at.x, at.y);
                var release = velocity.release(now);
                motion.release(release.x(), -release.y());
                dragStart = null;
            }
        };
        if (!smokeMode) {
            drawing.addMouseListener(drag);
            drawing.addMouseMotionListener(drag);
        }
        pet.add(drawing);
    }

    private void show(Path smoke) {
        support.setVisible(true);
        drop();
        if (!smokeMode) motion.restore();
        setHidden(false);
        lastFrame = System.nanoTime();
        ticker.start();
        if (smoke != null) verifyDesktop(smoke);
    }

    private void updateSupport() {
        plane.clearBarriers();
        if (support.isShowing() && (support.getExtendedState() & JFrame.ICONIFIED) == 0) {
            plane.setBarrier(-support.getY(), support.getX(), support.getWidth(), false);
        }
    }

    private void drop() {
        updateSupport();
        motion.beginDrag();
        motion.drag(support.getX()+(support.getWidth()-PET_WIDTH)/2f, -support.getY()+72f);
        motion.release(0, 0);
    }

    private void setHidden(boolean value) {
        hidden = value;
        if (!hidden) placeWindow();
        pet.setVisible(!hidden);
        visibility.setText(hidden ? "显示角色" : "隐藏角色");
    }

    private void tick() {
        updateSupport();
        long now = System.nanoTime();
        motion.tick((now-lastFrame)/1e9, 0, hidden);
        lastFrame = now;
        if (!hidden) placeWindow();
        effect.update();
        if (!smokeMode) walk.setEnabled(!hidden && motion.isGrounded() && !motion.isBusy());
        String label = motion.isGrounded() && Math.abs(plane.getY()+support.getY()) < 1
                ? "角色站在示例窗口顶沿。可拖动角色或让它沿顶沿移动。"
                : "拖动角色，把它放到示例窗口上方。";
        if (!label.equals(status.getText())) status.setText(label);
    }

    private void placeWindow() {
        int x = Math.round(plane.getX());
        int y = Math.round(-plane.getY())-PET_HEIGHT;
        if (x != pet.getX() || y != pet.getY()) pet.setLocation(x, y);
    }

    private void close() {
        ticker.stop();
        if (!smokeMode && motion.isGrounded()) {
            placement.save(true, plane.getX(), plane.getY(), screen);
        }
        placement.close();
        pet.dispose();
        support.dispose();
    }

    private void verifyDesktop(Path output) {
        long deadline = System.nanoTime() + 12_000_000_000L;
        final class Smoke extends Timer {
            private float initial = Float.NaN;

            Smoke() {
                super(60, null);
                addActionListener(event -> sample());
            }

            private void sample() {
                try {
                    if (System.nanoTime() > deadline) throw new IOException("Native desktop smoke timed out");
                    if (!support.isShowing() || !pet.isShowing() || !motion.isGrounded() || motion.isBusy()
                            || Math.abs(plane.getY()+support.getY()) > 1) return;
                    if (Float.isNaN(initial)) {
                        initial = plane.getX();
                        setHidden(true);
                        if (pet.isShowing()) throw new IOException("Pet window did not hide");
                        setHidden(false);
                        if (!motion.startManualWalk(.1, .5)) throw new IOException("Platform walk did not start");
                        effect.start();
                        return;
                    }
                    float moved = Math.abs(plane.getX()-initial);
                    if (moved < 24 || effect.impacts() != 6) return;
                    Files.createDirectories(output);
                    Rectangle evidence = support.getBounds().union(pet.getBounds());
                    ImageIO.write(new Robot().createScreenCapture(evidence), "PNG", output.resolve("desktop.png").toFile());
                    Files.writeString(output.resolve("desktop-smoke.json"), String.format(Locale.ROOT,
                            "{\"visibleWindows\":2,\"platformY\":%d,\"walkedPixels\":%.2f,\"impacts\":%d}%n",
                            support.getY(), moved, effect.impacts()));
                    stop();
                    close();
                } catch (Exception failure) {
                    System.getLogger("ViviMeDemo").log(System.Logger.Level.ERROR, "Desktop smoke failed", failure);
                    stop();
                    close();
                    System.exit(1);
                }
            }
        }
        new Smoke().start();
    }
}
