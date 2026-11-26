/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import javax.sound.sampled.AudioFormat;
import java.util.SplittableRandom;

/** Synthesizes the menu cues once, with no file or mixer dependency. */
final class BattleMenuFoley {
    static final int SAMPLE_RATE = 44_100;
    static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final double TWO_PI = 2 * Math.PI;
    private static final double PEAK = .72;

    static byte[] synthesize(BattleMenuSound.Cue cue) {
        int frames = Math.round(cue.durationMs * (SAMPLE_RATE / 1000f));
        double[] signal = new double[frames];
        var noise = new SplittableRandom(2803L + cue.ordinal());
        var band = new BandPass();
        double peak = 0;
        for (int i = 0; i < frames; i++) {
            double time = i / (double) SAMPLE_RATE;
            double white = noise.nextDouble(-1, 1);
            double sample = switch (cue) {
                case OPEN -> open(time, band, white);
                case WHIP -> whip(time, band, white);
                case LOCK -> lock(time, band, white);
                case DAWN -> dawn(time, band, white);
            };
            double release = Math.min(1, (frames - 1 - i) / (SAMPLE_RATE * .005));
            signal[i] = sample * release;
            peak = Math.max(peak, Math.abs(signal[i]));
        }

        byte[] pcm = new byte[frames * FORMAT.getFrameSize()];
        double scale = PEAK * Short.MAX_VALUE / peak;
        for (int i = 0; i < frames; i++) {
            int value = (int) Math.round(signal[i] * scale);
            pcm[2 * i] = (byte) value;
            pcm[2 * i + 1] = (byte) (value >> 8);
        }
        return pcm;
    }

    private static double open(double time, BandPass band, double white) {
        double sweep = 1900 + 3600 * Math.pow(Math.min(1, time / .049), .82);
        double paper = band.filter(white, sweep, .85) * envelope(time, .004, .022);
        double edge = tone(time, 4900) * envelope(time, .00065, .0022);
        return paper * .8 + edge * .25;
    }

    private static double whip(double time, BandPass band, double white) {
        double click = band.filter(white, 5200, 1.1) * .74 + tone(time, 6300) * .26;
        return click * envelope(time, .00055, .0052);
    }

    private static double lock(double time, BandPass band, double white) {
        double click = band.filter(white, 4500, 1.3);
        double first = (click * .67 + tone(time, 4100) * .33) * envelope(time, .0008, .0052);
        double secondTime = time - .024;
        double second = (click * .65 + tone(secondTime, 5050) * .35) * envelope(secondTime, .0008, .0067);
        return first + .83 * second;
    }

    private static double dawn(double time, BandPass band, double white) {
        double strike = band.filter(white, 3350, .95) * envelope(time, .001, .010);
        double metal = .31 * tone(time, 1780) * envelope(time, .0011, .020)
                + .24 * tone(time, 2690) * envelope(time, .0011, .015)
                + .16 * tone(time, 4150) * envelope(time, .0008, .009);
        return .72 * strike + metal;
    }

    private static double envelope(double time, double attack, double decay) {
        return time < 0 ? 0 : Math.min(time / attack, 1) * Math.exp(-time / decay);
    }

    private static double tone(double time, double frequency) {
        return Math.sin(TWO_PI * frequency * time);
    }

    private static final class BandPass {
        private double x1, x2, y1, y2;

        double filter(double white, double frequency, double q) {
            double omega = TWO_PI * frequency / SAMPLE_RATE;
            double alpha = Math.sin(omega) / (2 * q);
            double output = (alpha * (white - x2) + 2 * Math.cos(omega) * y1 - (1 - alpha) * y2)
                    / (1 + alpha);
            x2 = x1;
            x1 = white;
            y2 = y1;
            y1 = output;
            return output;
        }
    }
}
