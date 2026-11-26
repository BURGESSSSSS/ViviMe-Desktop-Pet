/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.animations.ActionSequence;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ActionSequenceTest {
    @Test void queueTimeIncludesNativeMixOverlapAndSpeed(){
        var s=new ActionSequence("Building","lowest_leg",List.of(new ActionSequence.Step("Relax",0,.25f,1,0),new ActionSequence.Step("Sit",0,1.1333333f,1,.12f),new ActionSequence.Step("Relax",0,.65f,1,.16f)));
        assertEquals(1.7533333f,s.duration(),.00001f);
    }
    @Test void invalidSegmentsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new ActionSequence.Step("Sit",1,0,1,0));
        assertThrows(IllegalArgumentException.class,()->new ActionSequence.Step("Sit",0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ActionSequence.Step("Sit",0,Float.NaN,1,0));
    }
}
