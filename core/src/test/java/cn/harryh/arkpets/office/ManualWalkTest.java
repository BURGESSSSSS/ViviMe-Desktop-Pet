/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManualWalkTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary;
    private final Plane.RectArea work=new Plane.RectArea(0,1000,0,-800),office=new Plane.RectArea(500,1000,-550,-800);
    private OfficeMotion setup(Plane p){p.world.add(work);p.setObjSize(100,150);p.setGravity(800);p.setFrict(100,500);p.changePosition(0,650,-800);var m=new OfficeMotion(p,100,150,null);m.areas(office,work);m.setGaitSpeed(45);return m;}
    @Test void aManualWalkOutsideNeverBecomesAnOfficeReturn(){
        Plane p=new Plane();var m=setup(p);m.beginDrag();m.drag(100,-800);m.release(0,0);
        for(int i=0;i<60;i++)m.tick(1.0/60,0,false);
        assertEquals(OfficeMotion.State.USER_PLACED,m.state());
        assertTrue(m.startManualWalk(.8,.5));for(int i=0;i<240;i++)m.tick(1.0/60,0,false);
        assertFalse(m.isWalking());assertEquals(OfficeMotion.State.USER_PLACED,m.state());assertTrue(m.isManual());assertTrue(p.getX()>150&&p.getX()<220);
        float x=p.getX();for(int i=0;i<120*60;i++)m.tick(1.0/60,0,false);assertEquals(x,p.getX());
    }
    @Test void aWalkAtAnEdgeChoosesTheOtherDirection(){
        Plane p=new Plane();var m=setup(p);m.beginDrag();m.drag(0,-800);m.release(0,0);for(int i=0;i<60;i++)m.tick(1.0/60,0,false);
        assertTrue(m.startManualWalk(0,.5));for(int i=0;i<240;i++)m.tick(1.0/60,0,false);assertTrue(p.getX()>20&&p.getX()<180);
    }
    @Test void flightCannotStartAManualGroundWalk(){
        Plane p=new Plane();var m=setup(p);m.beginDrag();m.drag(200,-500);m.release(600,400);assertFalse(m.startManualWalk(1,1));
    }
    @Test void noSpaceCancelsInsteadOfWalkingOnTheSpot(){
        Plane p=new Plane();var m=setup(p);m.areas(new Plane.RectArea(650,752,-550,-800),work);assertFalse(m.startManualWalk(.5,.5));assertFalse(m.isWalking());
    }
    @Test void manualWalkPersistsItsNewOutsideLanding() throws Exception {
        var file=temporary.resolve("walk-placement.json");float savedX;
        try(var store=new PlacementStore(file)){
            Plane p=new Plane();p.world.add(work);p.setObjSize(100,150);p.setGravity(800);p.setFrict(100,500);p.changePosition(0,650,-800);
            var m=new OfficeMotion(p,100,150,store);m.areas(office,work);m.setGaitSpeed(45);m.beginDrag();m.drag(100,-800);m.release(0,0);
            for(int i=0;i<60;i++)m.tick(1.0/60,0,false);
            assertTrue(m.startManualWalk(.8,.5));for(int i=0;i<240;i++)m.tick(1.0/60,0,false);savedX=p.getX();
        }
        try(var store=new PlacementStore(file)){var restored=store.load(work,100,150);assertNotNull(restored);assertEquals(savedX,restored.x(),.01);}
    }
}
