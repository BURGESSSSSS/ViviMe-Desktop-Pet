/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DawnTargetLockTest {
    @Test void freezesVisibleTargetThroughoutFlightAndImpact(){
        var lock=new DawnTargetLock();lock.lock(300,200);
        for(int i=0;i<100;i++){
            lock.advance(i>20,false);
            assertEquals(300,lock.x(1000+i));assertEquals(200,lock.y(-1000-i));
        }
        assertEquals(DawnTargetLock.State.IMPACT,lock.state());
        assertTrue(lock.advance(true,true));assertFalse(lock.frozen());assertEquals(123,lock.x(123));
    }
    @Test void doesNotUseElapsedTimeOrZeroLiveCountAsCompletion(){
        var lock=new DawnTargetLock();lock.lock(400,300);
        for(int frame=0;frame<600;frame++)lock.advance(true,false);
        assertTrue(lock.frozen());
        lock.advance(true,true);assertEquals(DawnTargetLock.State.TRACKING,lock.state());
    }
    @Test void repeatsSixIndependentLocksAtEveryRefreshRate(){
        for(int fps:new int[]{30,60,120}){
            var lock=new DawnTargetLock();
            for(int shot=1;shot<=6;shot++){
                lock.lock(shot*100,shot*25);
                for(int frame=0;frame<fps*3;frame++){
                    lock.advance(frame>=fps/2,false);
                    assertEquals(shot*100,lock.x(frame));assertEquals(shot*25,lock.y(-frame));
                }
                lock.advance(true,true);assertFalse(lock.frozen());
            }
        }
    }
    @Test void refusesToOverwriteAnUnfinishedShot(){
        var lock=new DawnTargetLock();lock.lock(1,2);
        assertThrows(IllegalStateException.class,()->lock.lock(3,4));
        assertEquals(1,lock.x(3));assertEquals(2,lock.y(4));
    }
    @Test void clearsFrozenStateOnExitOrResourceFailure(){
        var lock=new DawnTargetLock();lock.lock(1,2);lock.advance(true,false);lock.reset();
        assertEquals(DawnTargetLock.State.TRACKING,lock.state());assertEquals(50,lock.x(50));
        lock.lock(5,6);assertEquals(5,lock.x(0));
    }
    @Test void rejectsInvalidTargetsWithoutStartingALock(){
        var lock=new DawnTargetLock();
        assertThrows(IllegalArgumentException.class,()->lock.lock(Double.NaN,0));
        assertFalse(lock.frozen());
    }
}
