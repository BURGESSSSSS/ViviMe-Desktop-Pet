/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReleaseVelocityTest {
    @Test void averagesRecentHistoryAndRejectsAnOutlier(){
        ReleaseVelocity v=new ReleaseVelocity();long start=1_000_000_000L;
        for(int i=0;i<=14;i++)v.sample(start+i*10_000_000L,i*5+(i==6?3000:0),-i*6);
        var result=v.release(start+140_000_000L);assertEquals(500,result.x(),.1);assertEquals(-600,result.y(),.1);
    }
    @Test void slowStaleAndBatchedInputsDoNotInventVelocity(){
        ReleaseVelocity v=new ReleaseVelocity();for(int i=0;i<12;i++)v.sample(1_000_000_000L+i*10_000_000L,i*.1f,0);
        assertEquals(0,v.release(1_110_000_000L).x());assertEquals(0,v.release(1_400_000_000L).x());
        v.clear();v.sample(2_000_000_000L,0,0);v.sample(2_001_000_000L,500,0);v.sample(2_002_000_000L,900,0);assertEquals(0,v.release(2_002_000_000L).x());
    }
    @Test void clampsFastReleaseAndHandlesDuplicateTimestamps(){
        ReleaseVelocity v=new ReleaseVelocity();for(int i=0;i<12;i++){v.sample(1_000_000_000L+i*10_000_000L,i*30,-i*30);v.sample(1_000_000_000L+i*10_000_000L,-9999,9999);}
        var result=v.release(1_110_000_000L);assertEquals(1400,Math.hypot(result.x(),result.y()),.1);assertTrue(result.y()<0);
    }
    @Test void supportsHighPollingRatesAndAStationaryHold(){
        ReleaseVelocity v=new ReleaseVelocity();long start=1_000_000_000L;
        for(int i=0;i<=1400;i++)v.sample(start+i*100_000L,i*.05f,-i*.06f);
        var moving=v.release(start+140_000_000L);assertEquals(500,moving.x(),.2);assertEquals(-600,moving.y(),.2);
        v.sample(start+200_000_000L,70,-84);assertEquals(0,v.release(start+200_000_000L).x());
    }
}
