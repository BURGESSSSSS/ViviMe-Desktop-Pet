/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import cn.harryh.arkpets.animations.PetAction;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Draws Skeleton A and uses its current frame polygons for mouse input. */
final class BattleMenuPanel extends JComponent {
    private static final Color C_BLACK = new Color(0x0D0D10);
    private static final Color C_RED = new Color(0xE60012);
    private static final Color C_WHITE = new Color(0xF8F8FA);
    private static final Color C_DISABLED_BG = new Color(0x232328);
    private static final Color C_DISABLED_FG = new Color(0x70707A);
    private static final Color C_PAUSED_TAG = new Color(0xFFCC00);
    private static final Color DISABLED_ACCENT = new Color(112, 112, 122, 60);
    private static final Color DAWN_HAZARD = new Color(13, 13, 16, 72);
    private static final TexturePaint HALFTONE_DARK = halftone(1.6, new Color(10, 10, 14, 95));
    private static final TexturePaint HALFTONE_RED = halftone(1.5, new Color(230, 0, 18, 120));

    private static final int HOLD_MS = 260;
    private static final int JITTER_MS = 83;
    private static final double CURSOR_LERP = .36;
    private static final BasicStroke DRAG_TOLERANCE = new BasicStroke(10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private static final Font BOLD = readFont();
    private static final Font BADGE_FONT = BOLD.deriveFont(10f);
    private static final Font NAMEPLATE_FONT = BOLD.deriveFont(10.5f);
    private static final Pattern ACTION_DURATION = Pattern.compile("\\h*[（(]\\h*约?\\h*(\\d+)\\h*秒\\h*[）)]\\h*$");
    private static final Path2D.Float BLACK_SPINE = polygon(18, -118, 52, -126, 34, 132, 8, 122);
    private static final Path2D.Float RED_SPINE = polygon(26, -112, 62, -122, 44, 138, 16, 128);
    private static final Path2D.Float SPINE_POINTER = polygon(-22, 22, 16, 4, 12, 34);
    private static final Path2D.Float POINTER_SHADOW = transformed(SPINE_POINTER, AffineTransform.getTranslateInstance(2, 2));
    private static final Path2D.Float DIVIDER = line(382, -138, 372, 28, 3.5);
    private static final Path2D.Float DIVIDER_NODE = polygon(377, -43, 382, -38, 377, -33, 372, -38);
    private static final Path2D.Float NAMEPLATE = polygon(-76, -12, 80, -10, 74, 11, -78, 12);
    private static final Path2D.Float NAMEPLATE_BORDER = transformed(NAMEPLATE, AffineTransform.getScaleInstance(1.04, 1.04));
    private static final AffineTransform NAMEPLATE_PLACEMENT = placement(64, -128, -8.5, 1, 1);
    private static final Path2D.Float NAMEPLATE_STAR = star(-61, 0, 5, 6, 2.7);
    private static final Path2D.Float DURATION_BADGE = polygon(-13, -7, 15, -7, 12, 7, -15, 7);
    private static final AffineTransform BADGE_PLACEMENT = placement(54, -14, 6, 1, 1);
    private static final Shape DURATION_BADGE_SHAPE = transformed(DURATION_BADGE, BADGE_PLACEMENT);
    private static final Path2D.Float DAWN_STRIPES = hazardStripes();
    private static final Path2D.Float DAWN_STAR = star(78, 10, 4, 8, 2.5);
    private static final Path2D.Float CURSOR_FACE = polygon(-.5, -.28, .5, -.5, .42, .31, -.45, .5);
    private static final Path2D.Float CURSOR_SLIVER = polygon(-.5, -.15, .48, -.5, .38, -.22, -.46, .5);
    private static final double[] FIRST_ANGLES = {-8, 7, -7, 9};
    private static final Backbone SETTLED_SPINE = backbone(1);

    enum Command { ACTIONS, PAUSE, RELOCATE, DAWN, VISIBILITY, SOUND, EXIT }

    record Target(Command command, PetAction action) {}

    record State(boolean paused, boolean hidden, boolean dawn, boolean canStart, Set<PetAction> actions) {
        static final State EMPTY = new State(false, false, false, false, Set.of());
    }

    private record Card(Target target, double x, double y, double angle, Font font, Path2D.Float face,
                        Path2D.Float border, String label, String duration) {
        boolean mainCommand() {
            return target.action() == null && target.command().ordinal() < Command.VISIBILITY.ordinal();
        }
    }

    private static Card command(Command id, double x, double y, double angle, int fontSize, double... vertices) {
        Path2D.Float face = polygon(vertices);
        Path2D.Float border = id.ordinal() < Command.VISIBILITY.ordinal() ? paperBorder(vertices) : smallBorder(face);
        return new Card(new Target(id, null), x, y, angle, BOLD.deriveFont((float) fontSize), face, border, "", "");
    }

    private static Card action(PetAction id, double x, double y, double angle, Path2D.Float face) {
        var duration = ACTION_DURATION.matcher(id.label);
        boolean timed = duration.find();
        return new Card(new Target(Command.ACTIONS, id), x, y, angle, BOLD.deriveFont(14f), face, smallBorder(face),
                timed ? id.label.substring(0, duration.start()) : id.label, timed ? duration.group(1) + "s" : "");
    }

    private static final List<Card> MAIN = List.of(
            command(Command.ACTIONS, 110, -82, -7, 17, -92, -21, 96, -16, 86, 20, -98, 18),
            command(Command.PAUSE, 124, -26, -2.5, 16, -88, -19, 86, -22, 92, 18, -82, 20),
            command(Command.RELOCATE, 120, 28, 2.5, 16, -82, -20, 88, -17, 78, 21, -86, 18),
            command(Command.DAWN, 114, 88, 6, 19, -100, -22, 104, -26, 92, 24, -94, 20),
            command(Command.VISIBILITY, 62, 140, 4, 13, -42, -14, 44, -12, 38, 14, -46, 13),
            command(Command.SOUND, 149, 143, 2, 13, -25, -13, 26, -12, 24, 13, -26, 12),
            command(Command.EXIT, 231, 144, -3.5, 13, -38, -13, 42, -15, 36, 14, -42, 12)
    );
    private static final Path2D.Float LEFT_ACTION = polygon(-60, -15, 62, -13, 56, 15, -62, 14);
    private static final Path2D.Float RIGHT_ACTION = polygon(-62, -15, 64, -14, 58, 16, -64, 14);
    private static final List<Card> ACTIONS = List.of(
            action(PetAction.WALK, 298, -122, -2.8, LEFT_ACTION),
            action(PetAction.SIT, 304, -78, 1.8, LEFT_ACTION),
            action(PetAction.SLEEP, 299, -34, -2.2, LEFT_ACTION),
            action(PetAction.INTERACT, 305, 10, 1.5, LEFT_ACTION),
            action(PetAction.ANGRY, 454, -98, 2.4, RIGHT_ACTION),
            action(PetAction.HOP, 460, -46, -1.8, RIGHT_ACTION),
            action(PetAction.SPECIAL, 452, 6, 2.8, RIGHT_ACTION)
    );
    static final List<PetAction> MENU_ACTIONS = ACTIONS.stream().map(card -> card.target().action()).toList();
    private static final Card ACTIONS_COMMAND = MAIN.get(0);
    private static final Path2D.Float PAUSED_TAG = polygon(80, -22, 86, -22, 92, 18, 86, 18);
    private static final Point2D ACTIONS_LAUNCH = new Point2D.Double(
            ACTIONS_COMMAND.x() + ACTIONS_COMMAND.face().getBounds2D().getMaxX(), ACTIONS_COMMAND.y());
    private static final List<PlacedCard> SETTLED_MAIN = MAIN.stream().map(PlacedCard::new).toList();
    private static final List<PlacedCard> SETTLED_ACTIONS = ACTIONS.stream().map(PlacedCard::new).toList();

    private record PlacedCard(Card card, double x, double y, double angle, double scale,
                              AffineTransform transform, Path2D.Float face,
                              Path2D.Float border, Path2D.Float shadow, Shape badge) {
        PlacedCard(Card card) {
            this(card, card.x(), card.y(), card.angle(), 1);
        }

        PlacedCard(Card card, double x, double y, double angle, double scale) {
            this(card, x, y, angle, scale, placement(x, y, angle, scale, scale));
        }

        private PlacedCard(Card card, double x, double y, double angle, double scale, AffineTransform transform) {
            this(card, x, y, angle, scale, transform, transformed(card.face(), transform),
                    transformed(card.border(), transform),
                    transformed(card.face(), placement(x + 5, y + 5, angle, scale, scale)),
                    card.duration().isEmpty() ? null : transform.createTransformedShape(DURATION_BADGE_SHAPE));
        }

        double rightEdge() {
            double edge = Math.max(border.getBounds2D().getMaxX(), shadow.getBounds2D().getMaxX());
            return badge == null ? edge : Math.max(edge, badge.getBounds2D().getMaxX());
        }
    }

    private record Cursor(double x, double y, double angle, double width, double height) {}
    private record Spark(Shape outline, Shape center) {
        Spark transformedBy(AffineTransform transform) {
            return new Spark(transform.createTransformedShape(outline), transform.createTransformedShape(center));
        }
    }
    private record Backbone(Path2D.Float red, Path2D.Float black, Path2D.Float pointerShadow, Path2D.Float pointer,
                            AffineTransform nameplate, Shape nameplateBorder) {}

    private final Timer timer = new Timer(16, e -> animate());
    private final BattleMenuSound sounds;
    private final Consumer<Target> chosen;
    private final Runnable refresh;
    private final String nameplateLabel;
    private final BattleMenuTypography typography = new BattleMenuTypography();
    private final List<PlacedCard> cards = new ArrayList<>(MAIN.size() + ACTIONS.size());
    private State state = State.EMPTY;
    private Card hovered;
    private Card lastWhipTarget;
    private Card pressed;
    private Point2D pointer;
    private Point2D pressPoint;
    private Cursor cursor;
    private Shape cursorFace, cursorSliver;
    private Spark cursorStart, cursorEnd;
    private Backbone spine = SETTLED_SPINE;
    private int originX, originY, direction;
    private long opened, actionsOpened, pressedAt, lastRefresh, jitterStep;
    private double pulse, jitterX, jitterY;
    private boolean holdCancelled, heldOpen;

    BattleMenuPanel(BattleMenuSound sounds, Consumer<Target> chosen, Runnable refresh, String characterLabel) {
        this.sounds = sounds;
        this.chosen = chosen;
        this.refresh = refresh;
        this.nameplateLabel = nameplateLabel(characterLabel);
        setOpaque(false);
        setFocusable(false);
        MouseAdapter input = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { moveTo(e.getX(), e.getY()); }
            @Override public void mouseMoved(MouseEvent e) { moveTo(e.getX(), e.getY()); }
            @Override public void mouseDragged(MouseEvent e) { moveTo(e.getX(), e.getY()); }
            @Override public void mouseExited(MouseEvent e) {
                if (pressed == null) {
                    pointer = null;
                    setHover(null, System.nanoTime());
                }
            }
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) press(e.getX(), e.getY());
                else if (!containsMenu(e.getX(), e.getY())) chosen.accept(null);
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) releaseAt(e.getX(), e.getY());
            }
        };
        addMouseListener(input);
        addMouseMotionListener(input);
    }

    void begin(int originX, int originY, int direction) {
        this.originX = originX;
        this.originY = originY;
        this.direction = direction;
        opened = System.nanoTime();
        actionsOpened = lastRefresh = 0;
        jitterStep = -1;
        pointer = null;
        pressed = hovered = null;
        lastWhipTarget = null;
        cursor = null;
        cursorFace = cursorSliver = null;
        updateCards(opened);
        timer.start();
        repaint();
    }

    void stop() {
        timer.stop();
        pointer = null;
        pressed = hovered = null;
    }

    void updateState(State state) {
        this.state = state;
        if (pressed != null && !enabled(pressed)) pressed = null;
        if (hovered != null && !enabled(hovered)) setHover(null, System.nanoTime());
        repaint();
    }

    private boolean enabled(Card card) {
        if (card == null) return false;
        Target target = card.target();
        if (target.action() != null) return !state.dawn() && state.actions().contains(target.action());
        return switch (target.command()) {
            case ACTIONS -> !state.dawn() && !state.actions().isEmpty();
            case DAWN -> state.canStart();
            default -> true;
        };
    }

    /** Includes decorative polygons while keeping the transparent gaps outside the menu. */
    boolean containsMenu(double x, double y) {
        Point2D point = local(x, y);
        if (spine.black().contains(point) || spine.red().contains(point)
                || spine.pointer().contains(point) || spine.pointerShadow().contains(point)
                || spine.nameplateBorder().contains(point)
                || actionsOpened != 0 && (DIVIDER.contains(point) || DIVIDER_NODE.contains(point))) return true;
        if (hovered != null && cursorFace != null
                && (cursorFace.contains(point) || cursorSliver.contains(point)
                    || cursorStart.outline().contains(point) || cursorEnd.outline().contains(point))) return true;
        for (PlacedCard card : cards) {
            if (card.face().contains(point) || card.border().contains(point) || card.shadow().contains(point)
                    || card.badge() != null && card.badge().contains(point)) return true;
        }
        return false;
    }

    private void press(double x, double y) {
        if (!containsMenu(x, y)) {
            chosen.accept(null);
            return;
        }
        pointer = local(x, y);
        Card hit = hit(pointer, false);
        pressed = enabled(hit) ? hit : null;
        pressPoint = pointer;
        pressedAt = System.nanoTime();
        holdCancelled = heldOpen = false;
    }

    private void moveTo(double x, double y) {
        pointer = local(x, y);
        cancelHoldForMovement();
        setHover(hit(pointer, heldOpen), System.nanoTime());
    }

    void releaseAt(double x, double y) {
        if (pressed == null) return;
        long now = System.nanoTime();
        moveTo(x, y);
        checkHold(now);
        Card start = pressed;
        Card end = hit(pointer, heldOpen);
        pressed = null;
        if (start == ACTIONS_COMMAND) {
            if (heldOpen && enabled(end) && end.target().action() != null) {
                chosen.accept(end.target());
            } else if (!holdCancelled && !heldOpen && start == end && elapsed(now, pressedAt) < HOLD_MS) {
                actionsOpened = actionsOpened == 0 ? now : 0;
                if (actionsOpened != 0) sounds.play(BattleMenuSound.Cue.LOCK);
                updateCards(now);
                setHover(hit(pointer, false), now);
                repaint();
            }
        } else if (start == end && enabled(end)) {
            if (!isDawn(end) && end.target().command() != Command.SOUND) sounds.play(BattleMenuSound.Cue.LOCK);
            chosen.accept(end.target());
        }
        heldOpen = false;
    }

    private void cancelHoldForMovement() {
        if (pressed == ACTIONS_COMMAND && !heldOpen && pressPoint.distance(pointer) >= 6) holdCancelled = true;
    }

    private void checkHold(long now) {
        if (pressed == ACTIONS_COMMAND && !heldOpen && !holdCancelled && elapsed(now, pressedAt) >= HOLD_MS) {
            heldOpen = true;
            if (actionsOpened == 0) {
                actionsOpened = pressedAt + HOLD_MS * 1_000_000L;
                sounds.play(BattleMenuSound.Cue.LOCK);
            }
        }
    }

    private Card hit(Point2D point, boolean drag) {
        if (point == null) return null;
        for (int i = cards.size() - 1; i >= 0; i--) {
            PlacedCard card = cards.get(i);
            if (card.face().contains(point)) return card.card();
        }
        if (drag) {
            for (int i = cards.size() - 1; i >= MAIN.size(); i--) {
                PlacedCard card = cards.get(i);
                if (enabled(card.card()) && DRAG_TOLERANCE.createStrokedShape(card.face()).contains(point)) {
                    return card.card();
                }
            }
        }
        return null;
    }

    private void setHover(Card target, long now) {
        Card next = enabled(target) ? target : null;
        if (hovered == next) return;
        hovered = next;
        cursorFace = cursorSliver = null;
        if (next != null) {
            pulse = 1.12;
            if (next != lastWhipTarget) {
                lastWhipTarget = next;
                sounds.playWhip(now);
            }
        }
    }

    private void animate() {
        long now = System.nanoTime();
        if (pressed != null) {
            var info = MouseInfo.getPointerInfo();
            if (info != null) {
                Point at = info.getLocation();
                SwingUtilities.convertPointFromScreen(at, this);
                pointer = local(at.x, at.y);
                cancelHoldForMovement();
            }
        }
        checkHold(now);
        updateCards(now);
        setHover(hit(pointer, pressed != null && heldOpen), now);
        updateCursor(now);
        if (elapsed(now, lastRefresh) >= 150) {
            lastRefresh = now;
            refresh.run();
        }
        repaint();
    }

    private void updateCards(long now) {
        cards.clear();
        double time = elapsed(now, opened);
        spine = time < 110 ? backbone(easeOutBack(time / 110)) : SETTLED_SPINE;
        for (int i = 0; i < MAIN.size() && time >= i * 22; i++) {
            double age = time - i * 22;
            Card card = MAIN.get(i);
            double t = easeOutBack(age / 140);
            cards.add(age >= 140 ? SETTLED_MAIN.get(i) : new PlacedCard(card,
                    lerp(-16, card.x(), t), lerp(0, card.y(), t),
                    lerp(card.angle() - 12, card.angle(), t), lerp(.55, 1, t)));
        }
        if (actionsOpened == 0) return;
        time = elapsed(now, actionsOpened);
        double right = forwardEdge() - 1; // Leave an antialiasing fringe at the work-area edge.
        for (int i = 0; i < ACTIONS.size() && time >= i * 16; i++) {
            double age = time - i * 16;
            Card card = ACTIONS.get(i);
            double t = easeOutBack(age / 130);
            PlacedCard settled = SETTLED_ACTIONS.get(i);
            if (age >= 130) {
                cards.add(settled);
            } else {
                double rightOverhang = settled.rightEdge() - card.x();
                double x = Math.min(lerp(ACTIONS_LAUNCH.getX(), card.x(), t), right - rightOverhang);
                cards.add(new PlacedCard(card, x, lerp(ACTIONS_LAUNCH.getY(), card.y(), t), card.angle(), 1));
            }
        }
    }

    private int forwardEdge() {
        return direction == 1 ? getWidth() - originX : originX;
    }

    private void updateCursor(long now) {
        if (hovered == null) return;
        for (PlacedCard placed : cards) {
            if (placed.card() != hovered) continue;
            var bounds = hovered.face().getBounds2D();
            Cursor target = new Cursor(placed.x(), placed.y(), placed.angle(),
                    bounds.getWidth() * placed.scale(), bounds.getHeight() * placed.scale());
            cursor = cursor == null ? target : new Cursor(
                    lerp(cursor.x(), target.x(), CURSOR_LERP), lerp(cursor.y(), target.y(), CURSOR_LERP),
                    lerp(cursor.angle(), target.angle(), CURSOR_LERP),
                    lerp(cursor.width(), target.width(), CURSOR_LERP), lerp(cursor.height(), target.height(), CURSOR_LERP));
            break;
        }

        long step = (now - opened) / (JITTER_MS * 1_000_000L);
        if (step != jitterStep) {
            jitterStep = step;
            var random = ThreadLocalRandom.current();
            jitterX = random.nextDouble(-1.5, 1.5);
            jitterY = random.nextDouble(-1.0, 1.0);
        }
        double w = (cursor.width() + 22) * pulse;
        double h = (cursor.height() + 8) * pulse;
        pulse = lerp(pulse, 1, CURSOR_LERP);
        double angle = cursor.angle() + 3.5;
        var faceTransform = placement(cursor.x(), cursor.y(), angle, w, h);
        cursorFace = transformed(CURSOR_FACE, faceTransform);
        cursorSliver = transformed(CURSOR_SLIVER,
                placement(cursor.x() - 4 + jitterX, cursor.y() - 3 + jitterY,
                        cursor.angle() - 3.5, w, h));
        cursorStart = cursorSpark(faceTransform, -.5, -.28, -3, -3, 7);
        cursorEnd = cursorSpark(faceTransform, .42, .31, 3, 3, 5);
        double edge = Math.max(Math.max(cursorFace.getBounds2D().getMaxX(), cursorSliver.getBounds2D().getMaxX()),
                Math.max(cursorStart.outline().getBounds2D().getMaxX(), cursorEnd.outline().getBounds2D().getMaxX()));
        if (edge > forwardEdge() - 1) {
            var inset = AffineTransform.getTranslateInstance(forwardEdge() - 1 - edge, 0);
            cursorFace = inset.createTransformedShape(cursorFace);
            cursorSliver = inset.createTransformedShape(cursorSliver);
            cursorStart = cursorStart.transformedBy(inset);
            cursorEnd = cursorEnd.transformedBy(inset);
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(originX, originY);
        g.scale(direction, 1);

        paintBackbone(g);
        if (actionsOpened != 0) {
            fill(g, C_RED, DIVIDER);
            fill(g, C_WHITE, DIVIDER_NODE);
        }
        for (PlacedCard card : cards) {
            fill(g, C_BLACK, card.shadow());
            fill(g, C_WHITE, card.border());
            boolean activeDawn = enabled(card.card()) && isDawn(card.card());
            fill(g, activeDawn ? C_RED : enabled(card.card()) ? C_BLACK : C_DISABLED_BG, card.face());
            if (activeDawn) paintDawnPattern(g, card);
        }

        if (hovered != null && cursorFace != null) {
            boolean reverse = isDawn(hovered);
            fill(g, reverse ? C_BLACK : C_WHITE, cursorSliver);
            fill(g, reverse ? C_WHITE : C_RED, cursorFace);
            fill(g, reverse ? HALFTONE_RED : HALFTONE_DARK, cursorFace);
            paintSpark(g, cursorStart, reverse);
            paintSpark(g, cursorEnd, reverse);
        }
        long now = System.nanoTime();
        for (PlacedCard card : cards) paintLabel(g, card, now);
        for (PlacedCard card : cards) {
            if (card.badge() != null) paintBadge(g, card);
        }
        g.dispose();
    }

    private void paintBackbone(Graphics2D g) {
        fill(g, C_RED, spine.red());
        fill(g, HALFTONE_DARK, spine.red());
        fill(g, C_RED, spine.pointerShadow());
        fill(g, C_BLACK, spine.black());
        fill(g, C_BLACK, spine.pointer());
        if (spine.nameplate().getDeterminant() == 0) return;
        Graphics2D plate = (Graphics2D) g.create();
        plate.transform(spine.nameplate());
        fill(plate, C_WHITE, NAMEPLATE_BORDER);
        fill(plate, C_BLACK, NAMEPLATE);
        fill(plate, C_RED, NAMEPLATE_STAR);
        plate.clip(NAMEPLATE);
        plate.scale(direction, 1);
        plate.setFont(NAMEPLATE_FONT);
        plate.setColor(C_WHITE);
        centered(plate, nameplateLabel, direction * 8, 0);
        plate.dispose();
    }

    private static void paintDawnPattern(Graphics2D g, PlacedCard placed) {
        Graphics2D local = (Graphics2D) g.create();
        local.transform(placed.transform());
        local.clip(placed.card().face());
        fill(local, HALFTONE_DARK, placed.card().face());
        fill(local, DAWN_HAZARD, DAWN_STRIPES);
        local.dispose();
    }

    private static void paintSpark(Graphics2D g, Spark spark, boolean reverse) {
        fill(g, reverse ? C_RED : C_WHITE, spark.outline());
        fill(g, reverse ? C_WHITE : C_RED, spark.center());
    }

    private void paintLabel(Graphics2D base, PlacedCard placed, long now) {
        Graphics2D g = (Graphics2D) base.create();
        g.transform(placed.transform());
        Card card = placed.card();
        boolean selected = hovered == card && cursorFace != null;
        boolean dawn = isDawn(card);
        boolean active = enabled(card);
        if (dawn) fill(g, selected ? C_RED : C_WHITE, DAWN_STAR);
        if (card.target().command() == Command.PAUSE && state.paused()) fill(g, C_PAUSED_TAG, PAUSED_TAG);
        if (card == ACTIONS_COMMAND && pressed == card && !holdCancelled && !heldOpen) {
            double amount = Math.min(1, elapsed(now, pressedAt) / HOLD_MS);
            g.clip(card.face());
            fill(g, C_WHITE, charge(amount));
            fill(g, C_RED, charge(Math.max(0, amount - 2 / 184.0)));
        }
        g.scale(direction, 1);
        boolean main = card.mainCommand();
        Color remainder = !active ? C_DISABLED_FG : dawn && selected ? C_BLACK : C_WHITE;
        Color first = main ? (active ? C_WHITE : C_DISABLED_BG) : remainder;
        Color accent = firstAccent(main, active, dawn, selected);
        String label = card.target().action() == null ? commandLabel(card.target().command()) : card.label();
        double firstAngle = main ? FIRST_ANGLES[card.target().command().ordinal()] : -5;
        typography.drawRansomText(g, label, card.font(), main, firstAngle, direction, accent, first, remainder);
        g.dispose();
    }

    private static Color firstAccent(boolean main, boolean active, boolean dawn, boolean selected) {
        if (!main) return !active ? C_DISABLED_FG : selected ? C_WHITE : C_RED;
        if (!active) return DISABLED_ACCENT;
        if (dawn) return selected ? C_RED : C_BLACK;
        return selected ? C_BLACK : C_RED;
    }

    private void paintBadge(Graphics2D base, PlacedCard placed) {
        fill(base, C_RED, placed.badge());
        Graphics2D g = (Graphics2D) base.create();
        g.transform(placed.transform());
        g.transform(BADGE_PLACEMENT);
        g.scale(direction, 1);
        g.setColor(C_WHITE);
        g.setFont(BADGE_FONT);
        centered(g, placed.card().duration(), 0, 0);
        g.dispose();
    }

    private Spark cursorSpark(AffineTransform transform, double x, double y, int dx, int dy, double radius) {
        var at = transform.transform(new Point2D.Double(x, y), null);
        double centerX = at.getX() + dx + jitterX;
        double centerY = at.getY() + dy + jitterY;
        return new Spark(star(centerX, centerY, 4, radius, radius * .32),
                star(centerX, centerY, 4, radius * .38, radius * .15));
    }

    private static Backbone backbone(double progress) {
        var stretch = AffineTransform.getScaleInstance(1, progress);
        var plate = new AffineTransform(stretch);
        plate.concatenate(NAMEPLATE_PLACEMENT);
        return new Backbone(transformed(RED_SPINE, stretch), transformed(BLACK_SPINE, stretch),
                transformed(POINTER_SHADOW, stretch), transformed(SPINE_POINTER, stretch),
                plate, transformed(NAMEPLATE_BORDER, plate));
    }

    private static String nameplateLabel(String characterLabel) {
        if (characterLabel != null) {
            int delimiter = characterLabel.indexOf('·');
            if (delimiter >= 0) {
                String title = characterLabel.substring(delimiter + 1).strip();
                if (!title.isEmpty()) return title + " // WIS'ADEL";
            }
        }
        return "绝对主角 // WIS'ADEL";
    }

    private String commandLabel(Command command) {
        return switch (command) {
            case ACTIONS -> "动作与互动";
            case PAUSE -> state.paused() ? "恢复自主" : "暂停自主";
            case RELOCATE -> "重新定位";
            case DAWN -> "爆裂黎明";
            case VISIBILITY -> state.hidden() ? "显示" : "隐藏";
            case SOUND -> sounds.isMuted() ? "开声" : "静音";
            case EXIT -> "退出";
        };
    }

    private static Path2D.Float charge(double progress) {
        double width = 5 / Math.hypot(184, 2);
        double x = lerp(-98, 86, progress), y = lerp(18, 20, progress);
        return polygon(-98, 18, x, y, x + 2 * width, y - 184 * width,
                -98 + 2 * width, 18 - 184 * width);
    }

    private static void centered(Graphics2D g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x - metrics.stringWidth(text) / 2f, y + (metrics.getAscent() - metrics.getDescent()) / 2f);
    }

    private static boolean isDawn(Card card) { return card.target().command() == Command.DAWN; }

    private Point2D local(double x, double y) {
        return new Point2D.Double(direction * (x - originX), y - originY);
    }

    private static void fill(Graphics2D g, Paint paint, Shape shape) {
        g.setPaint(paint);
        g.fill(shape);
    }

    private static AffineTransform placement(double x, double y, double angle, double sx, double sy) {
        var transform = AffineTransform.getTranslateInstance(x, y);
        transform.rotate(Math.toRadians(angle));
        transform.scale(sx, sy);
        return transform;
    }

    private static Path2D.Float transformed(Shape path, AffineTransform transform) {
        return new Path2D.Float(path, transform);
    }

    private static Path2D.Float smallBorder(Path2D.Float face) {
        return transformed(face, AffineTransform.getScaleInstance(1.055, 1.16));
    }

    private static Path2D.Float paperBorder(double[] v) {
        return polygon(v[0] - 4.5, v[1] - 3.5, v[2] + 4.5 + 2.5, v[3] - 3.5 - 1.5,
                v[4] + 4.5, v[5] + 3.5, v[6] - 4.5 - 2.5, v[7] + 3.5 + 1.5);
    }

    private static TexturePaint halftone(double radius, Color color) {
        var tile = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fill(new Ellipse2D.Double(2 - radius, 2 - radius, 2 * radius, 2 * radius));
        g.fill(new Ellipse2D.Double(6 - radius, 6 - radius, 2 * radius, 2 * radius));
        g.dispose();
        return new TexturePaint(tile, new Rectangle(0, 0, 8, 8));
    }

    private static Path2D.Float hazardStripes() {
        var stripes = new Path2D.Float();
        for (int y = -130; y <= 130; y += 9 + 17) {
            stripes.append(new Rectangle2D.Double(-130, y, 260, 9), false);
        }
        return transformed(stripes, AffineTransform.getRotateInstance(Math.toRadians(-38)));
    }

    private static Path2D.Float star(double x, double y, int points, double outer, double inner) {
        var shape = new Path2D.Float();
        shape.moveTo(x, y - outer);
        for (int i = 1; i < 2 * points; i++) {
            double angle = i * Math.PI / points - Math.PI / 2;
            double radius = i % 2 == 0 ? outer : inner;
            shape.lineTo(x + radius * Math.cos(angle), y + radius * Math.sin(angle));
        }
        shape.closePath();
        return shape;
    }

    private static Path2D.Float polygon(double... vertices) {
        var path = new Path2D.Float();
        path.moveTo(vertices[0], vertices[1]);
        for (int i = 2; i < vertices.length; i += 2) path.lineTo(vertices[i], vertices[i + 1]);
        path.closePath();
        return path;
    }

    private static Path2D.Float line(double x0, double y0, double x1, double y1, double width) {
        double factor = width / (2 * Math.hypot(x1 - x0, y1 - y0));
        double dx = (y1 - y0) * factor;
        double dy = (x0 - x1) * factor;
        return polygon(x0 - dx, y0 - dy, x0 + dx, y0 + dy, x1 + dx, y1 + dy, x1 - dx, y1 - dy);
    }

    private static double elapsed(long now, long then) { return (now - then) / 1_000_000.0; }
    private static double lerp(double from, double to, double t) { return from + (to - from) * t; }

    private static double easeOutBack(double t) {
        double u = Math.max(0, Math.min(1, t)) - 1;
        return 1 + 3.1 * Math.pow(u, 3) + 2.1 * Math.pow(u, 2);
    }

    private static Font readFont() {
        try (var stream = BattleMenuPanel.class.getResourceAsStream("/fonts/SourceHanSansCN-Bold.otf")) {
            if (stream != null) return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException ignored) {}
        return new Font(Font.DIALOG, Font.BOLD, 12);
    }
}
