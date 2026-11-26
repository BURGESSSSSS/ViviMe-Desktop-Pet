/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.tray;

import org.junit.jupiter.api.Test;

import static cn.harryh.arkpets.tray.BattleMenuSound.Cue.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleMenuFoleyTest {
    @Test
    void generatesShortDeterministicMonoCuesWithTaperedEdges() {
        var format = BattleMenuFoley.FORMAT;
        assertEquals(44_100f, format.getSampleRate());
        assertEquals(1, format.getChannels());
        assertEquals(16, format.getSampleSizeInBits());
        assertFalse(format.isBigEndian());
        assertCue(OPEN, 3308, -15.5f);
        assertCue(WHIP, 1235, -18.5f);
        assertCue(LOCK, 2867, -15.5f);
        assertCue(DAWN, 4851, -15.5f);
    }

    @Test
    void lockHasTwoImpactsSeparatedByAQuietGap() {
        byte[] pcm = BattleMenuFoley.synthesize(LOCK);
        double gap = rms(pcm, 20, 24);
        assertTrue(rms(pcm, 1, 6) > gap * 5);
        assertTrue(rms(pcm, 25, 30) > gap * 5);
        assertTrue(rms(pcm, 57, 65) < rms(pcm, 25, 30) * .05);
    }

    private static void assertCue(BattleMenuSound.Cue cue, int frames, float gain) {
        byte[] pcm = BattleMenuFoley.synthesize(cue);
        assertEquals(frames * 2, pcm.length, cue.name());
        assertEquals(gain, cue.gainDb());
        assertArrayEquals(pcm, BattleMenuFoley.synthesize(cue));
        assertEquals(0, sample(pcm, 0));
        assertEquals(0, sample(pcm, frames - 1));
        int peak = 0;
        for (int i = 0; i < frames; i++) peak = Math.max(peak, Math.abs(sample(pcm, i)));
        assertEquals(Math.round(.72 * Short.MAX_VALUE), peak);
    }

    private static double rms(byte[] pcm, int firstMs, int lastMs) {
        int start = (int) Math.round(firstMs * (BattleMenuFoley.SAMPLE_RATE / 1000.0));
        int end = (int) Math.round(lastMs * (BattleMenuFoley.SAMPLE_RATE / 1000.0));
        double sum = 0;
        for (int i = start; i < end; i++) {
            double value = sample(pcm, i);
            sum += value * value;
        }
        return Math.sqrt(sum / (end - start));
    }

    private static short sample(byte[] pcm, int frame) {
        int i = frame * 2;
        return (short)((pcm[i] & 0xff) | pcm[i + 1] << 8);
    }
}
