/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClickInteractionTest {
    private long t(double seconds){return (long)(seconds*1e9);}
    @Test void threeClicksWithinTheWholeWindowAreRequired(){
        var c=new ClickInteraction();assertFalse(c.click(t(1)));assertFalse(c.click(t(2)));
        assertTrue(c.click(t(3.5)));assertEquals(0,c.count());
    }
    @Test void staleFirstClickSlidesOutInsteadOfMakingASlowTriple(){
        var c=new ClickInteraction();assertFalse(c.click(t(1)));assertFalse(c.click(t(3)));
        assertFalse(c.click(t(4)));assertTrue(c.click(t(5)));
    }
    @Test void cooldownStartsOnlyForAnAcceptedReactionAndCannotBeClearedByOtherInput(){
        var c=new ClickInteraction();c.click(t(1));c.click(t(1.1));assertTrue(c.click(t(1.2)));c.triggered(t(1.2));
        c.clear();for(int i=0;i<590;i++)assertFalse(c.click(t(1.3+i*.1)));
        assertEquals(1.2,c.cooldownSeconds(t(60)),.001);
        assertFalse(c.click(t(61.2)));assertFalse(c.click(t(61.3)));assertTrue(c.click(t(61.4)));
    }
    @Test void invalidGestureDropsPartialClicksButDoesNotManufactureAReaction(){
        var c=new ClickInteraction();c.click(t(2));c.click(t(2.1));c.clear();
        assertFalse(c.click(t(2.2)));assertFalse(c.click(t(2.3)));assertTrue(c.click(t(2.4)));
    }
    @Test void rejectedReactionDoesNotConsumeTheCooldown(){
        var c=new ClickInteraction();c.click(t(1));c.click(t(1.1));assertTrue(c.click(t(1.2)));
        assertFalse(c.click(t(1.3)));assertFalse(c.click(t(1.4)));assertTrue(c.click(t(1.5)));
        assertEquals(0,c.cooldownSeconds(t(1.5)));
    }
    @Test void outOfOrderTimesCannotCompleteAPartialGesture(){
        var c=new ClickInteraction();c.click(t(10));c.click(t(11));assertFalse(c.click(t(9)));assertEquals(1,c.count());
    }
}
