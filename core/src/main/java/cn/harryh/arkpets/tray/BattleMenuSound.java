/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Preloads PCM and opens reusable Java Sound clips off the Swing and game threads. */
final class BattleMenuSound implements AutoCloseable {
    private static final int CLIPS_PER_CUE = 2;
    private static final float MASTER_GAIN_DB = -15.5f;
    private static final float WHIP_GAIN_OFFSET_DB = -3;
    private static final long WHIP_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(45);
    enum Cue {
        OPEN(75),
        WHIP(28),
        LOCK(65),
        DAWN(110);

        final int durationMs;

        Cue(int durationMs) {
            this.durationMs = durationMs;
        }

        float gainDb() {
            return MASTER_GAIN_DB + (this == WHIP ? WHIP_GAIN_OFFSET_DB : 0);
        }
    }

    private static final class ClipPool {
        private final byte[] pcm;
        private final List<Clip> clips;
        private int next;

        ClipPool(byte[] pcm, List<Clip> clips) {
            this.pcm = pcm;
            this.clips = List.copyOf(clips);
        }

        void play() {
            // Advance even if a device disappears, allowing the other clip to remain usable.
            Clip clip = clips.get(next);
            next = (next + 1) % clips.size();
            try {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            } catch (RuntimeException | LinkageError | ServiceConfigurationError ignored) {
                // Audio device changes must stay off the menu input path.
            }
        }
    }

    private final Map<Cue, ClipPool> pools = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean muted;
    private volatile int muteGeneration;
    private final ExecutorService audio = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Vivime-menu-audio");
        thread.setDaemon(true);
        return thread;
    });
    // Accessed by the Swing menu event thread.
    private boolean whipTriggered;
    private long lastWhip;

    BattleMenuSound(boolean muted) {
        this.muted = muted;
        audio.execute(this::preload);
    }

    private void preload() {
        for (Cue cue : Cue.values()) {
            if (closed.get()) return;
            try {
                byte[] pcm = BattleMenuFoley.synthesize(cue);
                List<Clip> clips = new ArrayList<>(CLIPS_PER_CUE);
                for (int i = 0; i < CLIPS_PER_CUE && !closed.get(); i++) {
                    Clip clip = openClip(pcm, cue.gainDb());
                    if (clip != null) clips.add(clip);
                }
                pools.put(cue, new ClipPool(pcm, clips));
            } catch (RuntimeException | LinkageError | ServiceConfigurationError ignored) {
                // Other cues and all UI actions remain available if a provider fails to initialize.
            }
        }
    }

    private static Clip openClip(byte[] pcm, float gainDb) {
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(BattleMenuFoley.FORMAT, pcm, 0, pcm.length);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                var gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), gainDb)));
            } else {
                // Precompute the same level on providers that lack a master gain control.
                closeClip(clip);
                clip = null;
                byte[] quieter = pcm.clone();
                double gain = Math.pow(10, gainDb / 20.0);
                for (int i = 0; i < quieter.length; i += 2) scaleSample(quieter, i, gain);
                clip = AudioSystem.getClip();
                clip.open(BattleMenuFoley.FORMAT, quieter, 0, quieter.length);
            }
            return clip;
        } catch (LineUnavailableException | RuntimeException | LinkageError | ServiceConfigurationError ignored) {
            closeClip(clip);
            return null;
        }
    }

    private static void scaleSample(byte[] samples, int offset, double gain) {
        short value = (short) ((samples[offset] & 0xff) | samples[offset + 1] << 8);
        int scaled = (int) Math.round(value * gain);
        samples[offset] = (byte) scaled;
        samples[offset + 1] = (byte) (scaled >> 8);
    }

    void playWhip(long now) {
        if (muted) return;
        if (whipTriggered && now - lastWhip < WHIP_INTERVAL_NS) return;
        whipTriggered = true;
        lastWhip = now;
        play(Cue.WHIP);
    }

    void play(Cue cue) {
        int generation = muteGeneration;
        ClipPool pool = pools.get(cue);
        // Drop early requests instead of replaying stale input after device warmup.
        if (pool == null || pool.clips.isEmpty() || muted || closed.get()) return;
        submit(() -> {
            if (!closed.get() && !muted && generation == muteGeneration) pool.play();
        });
    }

    boolean isMuted() {
        return muted;
    }

    void setMuted(boolean value) {
        if (muted == value) return;
        muted = value;
        muteGeneration++;
        if (value) submit(() -> {
            for (ClipPool pool : pools.values()) {
                for (Clip clip : pool.clips) {
                    try {
                        clip.stop();
                    } catch (RuntimeException | LinkageError | ServiceConfigurationError ignored) {
                        // A removed output device may have already stopped playback.
                    }
                }
            }
        });
    }

    private void submit(Runnable task) {
        try {
            audio.execute(task);
        } catch (RejectedExecutionException ignored) {
            // The menu may close on a different thread while the game dispatches a cue.
        }
    }

    private static void closeClip(Clip clip) {
        if (clip == null) return;
        try {
            clip.close();
        } catch (RuntimeException | LinkageError | ServiceConfigurationError ignored) {
            // A removed device can already have torn down the line.
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        audio.execute(() -> {
            for (ClipPool pool : pools.values()) pool.clips.forEach(BattleMenuSound::closeClip);
            pools.clear();
        });
        audio.shutdown();
    }
}
