/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DawnTuningTest {
    @Test void doublesTravelSpeedWithoutChangingAttackTimeline(){
        for(double distance:new double[]{300,500,1000,2000})assertEquals(distance/DawnTuning.ORIGINAL_SPEED/2,DawnTuning.flight(distance),1e-9);
        assertEquals(.09,DawnTuning.flight(0));assertEquals(.09,DawnTuning.flight(20));
    }
    @Test void trajectoryEndsAtTheFrozenTargetAtEveryRefreshRate(){
        double duration=DawnTuning.flight(1000);
        for(int fps:new int[]{30,60,120}){
            double prior=0;
            for(int i=0;i<=fps;i++){double t=i/(double)fps;double p=DawnTuning.progress(t,duration);assertTrue(p>=prior&&p<=1);prior=p;}
            assertEquals(1,DawnTuning.progress(duration,duration));assertEquals(0,DawnTuning.progress(-.1,duration));
        }
    }
    @Test void lockContractsOnlyInThePreparationWindow(){assertEquals(0,DawnTuning.lock(0));assertEquals(0,DawnTuning.lock(.2));assertTrue(DawnTuning.lock(.4)>.5);assertEquals(1,DawnTuning.lock(.53333336));}
    @Test void markerIsSuppressedForInvalidPointerAndAfterLastFx(){assertFalse(DawnTuning.markerVisible(false,0,0));assertTrue(DawnTuning.markerVisible(true,0,0));assertTrue(DawnTuning.markerVisible(true,6,10));assertFalse(DawnTuning.markerVisible(true,6,0));}
}
