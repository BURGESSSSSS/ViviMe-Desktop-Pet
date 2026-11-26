/** Copyright (c) 2022-2026, Harry Huang; 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ActivityRegionTest {
    @Test void quadrantsRespectOddSizesNegativeOriginsAndTaskbarOffsets() {
        Random r = new Random(12015001);
        for (int i = 0; i < 10000; i++) {
            int x = r.nextInt(8000) - 4000, y = r.nextInt(5000) - 2500;
            int w = 640 + r.nextInt(5000), h = 480 + r.nextInt(3000);
            for (ActivityRegion.Corner c : ActivityRegion.Corner.values()) {
                ActivityRegion a = ActivityRegion.of(x,y,w,h,ActivityRegion.Mode.OFFICE,c);
                assertTrue(a.width()*a.height() <= w*(double)h/8);
                assertTrue(a.x()>=x && a.y()>=y && a.x()+a.width()<=x+w && a.y()+a.height()<=y+h);
                assertEquals(w/2,a.width());assertEquals(h/4,a.height());
                assertEquals(c==ActivityRegion.Corner.TOP_RIGHT||c==ActivityRegion.Corner.BOTTOM_RIGHT?x+w-w/2:x,a.x());
                assertEquals(c==ActivityRegion.Corner.BOTTOM_LEFT||c==ActivityRegion.Corner.BOTTOM_RIGHT?y+h-h/4:y,a.y());
            }
        }
    }

    @Test void physicsContainsWholeCanvasUnderGravityThrowingAndWindowBarriers() {
        Random r=new Random(7331);
        for(float dpi:new float[]{1,1.25f,1.5f,2}) for(ActivityRegion.Corner c:ActivityRegion.Corner.values()) {
            ActivityRegion region=ActivityRegion.of(-1920,40,1920,1040,ActivityRegion.Mode.OFFICE,c);
            float margin=8*dpi,offset=50*dpi,w=100*dpi,h=60*dpi;
            Plane p=new Plane();p.world.add(new Plane.RectArea(-1920,0,-40,-1080));
            p.setGravity(800);p.setSpeedLimit(1000,1000);p.setObjSize(w,h);
            Plane.RectArea bounds=region.physicsArea(margin,offset);p.setMovementArea(bounds);
            p.changePosition(0,bounds.left(),bounds.bottom());
            for(int i=0;i<2000;i++) {
                if(i%70==0){p.clearBarriers();p.setBarrier(bounds.bottom()-100,bounds.left(),bounds.getWidth(),false);}
                p.changePosition(.016f,p.getX()+(r.nextFloat()-.5f)*200,p.getY()+(r.nextFloat()-.5f)*200);
                p.updatePosition(.016f);
                for(float animOffset:new float[]{0,5*dpi,25*dpi,50*dpi})
                    assertTrue(region.contains(p.getX(),-p.getY()-h+animOffset,w,h,.01f),c+" escaped");
            }
        }
    }

    @Test void draggingAndModeChangesReturnSmoothlyWithoutTeleportOrOscillation() {
        Plane p=new Plane();p.world.add(new Plane.RectArea(-1920,1920,1080,-2160));p.setObjSize(130,230);
        ActivityBounds controller=new ActivityBounds();
        p.changePosition(0,-1000,500);
        for(ActivityRegion.Corner c:ActivityRegion.Corner.values()) {
            ActivityRegion region=ActivityRegion.of(0,0,1920,1440,ActivityRegion.Mode.OFFICE,c);
            controller.setArea(region.physicsArea(10,50));
            controller.prepare(p,130,230,true,.016f);
            p.changePosition(0,-1000,500); // Explicit user drag exception.
            assertEquals(-1000,p.getX());
            boolean entered=false;
            for(int i=0;i<1000;i++) {
                float x=p.getX(),y=p.getY();
                controller.prepare(p,130,230,false,.016f);
                assertTrue(Math.hypot(p.getX()-x,p.getY()-y)<=480*.016+.01);
                if(!controller.isReturning()){entered=true;break;}
            }
            assertTrue(entered);
            assertTrue(region.contains(p.getX(),-p.getY()-230+50,130,230,.01f));
            for(int i=0;i<120;i++)assertFalse(controller.prepare(p,130,230,false,.016f));
        }
    }

    @Test void entertainmentRestoresWholeSelectedWorkAreaAndLegacyPlaneRemainsUnchanged() {
        ActivityRegion region=ActivityRegion.of(1920,-600,1600,860,ActivityRegion.Mode.ENTERTAINMENT,ActivityRegion.Corner.BOTTOM_RIGHT);
        assertEquals(new ActivityRegion(1920,-600,1600,860),region);
        Plane p=new Plane();p.world.add(new Plane.RectArea(0,1000,1000,0));p.setObjSize(100,100);
        p.changePosition(0,900,0);assertEquals(900,p.getX());
        p.setMovementArea(new Plane.RectArea(500,1000,500,0));p.changePosition(0,0,0);assertEquals(500,p.getX());
        p.setMovementArea(null);p.changePosition(0,0,0);assertEquals(0,p.getX());
    }

    @Test void currentModelFitsOneEighthWithoutScalingAndRatiosCanBeConfigured() {
        ActivityRegion region=ActivityRegion.of(0,0,2560,1528,ActivityRegion.Mode.OFFICE,ActivityRegion.Corner.BOTTOM_RIGHT);
        assertEquals(new ActivityRegion(1280,1146,1280,382),region);
        Plane.RectArea physics=region.physicsArea(12,75);
        assertTrue(physics.getWidth()>=536 && physics.getHeight()>=232);
        ActivityRegion larger=ActivityRegion.of(0,0,1920,1040,ActivityRegion.Mode.OFFICE,ActivityRegion.Corner.BOTTOM_RIGHT,.5f,.5f);
        assertEquals(520,larger.height());
    }

    @Test void rejectsOversizedCanvasInsteadOfSquashingPetOrSilentlyExpandingRegion() {
        Plane p=new Plane();ActivityBounds c=new ActivityBounds();c.setArea(new Plane.RectArea(0,100,100,0));
        assertThrows(IllegalStateException.class,()->c.prepare(p,200,100,false,.016f));
    }

    @Test void realWindowSupportsPetInsideRegionButCannotLowerTheLocalFloor() {
        Plane p=new Plane();p.world.add(new Plane.RectArea(0,2000,1200,0));
        p.setObjSize(100,160);p.setGravity(800);
        Plane.RectArea region=new Plane.RectArea(1000,2000,600,100);p.setMovementArea(region);
        p.changePosition(0,1200,400);p.setBarrier(250,1000,1000,false);
        for(int i=0;i<240;i++)p.updatePosition(1/60f);
        assertEquals(250,p.getY(),.01);
        p.clearBarriers();p.setBarrier(-100,1000,1000,false);
        for(int i=0;i<240;i++)p.updatePosition(1/60f);
        assertEquals(100,p.getY(),.01);
    }
}
