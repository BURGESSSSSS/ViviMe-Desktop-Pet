/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OfficeMotionTest {
    @TempDir Path temp;
    private static final Plane.RectArea WORK=new Plane.RectArea(0,1000,0,-800), OFFICE=new Plane.RectArea(500,1000,-550,-800);
    private record Rig(Plane p,OfficeMotion motion){}
    private Rig rig(PlacementStore store){
        Plane p=new Plane();p.world.add(WORK);p.setObjSize(100,150);p.setGravity(800);p.setFrict(100,500);p.setSpeedLimit(1000,1000);
        p.changePosition(0,650,-800);var motion=new OfficeMotion(p,100,150,store);motion.areas(OFFICE,WORK);return new Rig(p,motion);
    }
    private void run(Rig r,double seconds,int fps){for(int i=0;i<seconds*fps;i++)r.motion.tick(1.0/fps,0,false);}
    @Test void physicsAloneDoesNotBypassHomeArbitrationOrExplicitRelocation(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(120,-400);r.motion.release(0,0);run(r,5,60);
        assertEquals(OfficeMotion.State.USER_PLACED,r.motion.state());assertEquals(120,r.p.getX(),.1);assertEquals(-800,r.p.getY(),.1);
        run(r,180,60);assertEquals(120,r.p.getX(),.1);assertEquals(OfficeMotion.State.USER_PLACED,r.motion.state());
        r.motion.relocate();run(r,12,60);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());assertEquals(501,r.p.getX(),.5);
    }
    @Test void diagonalThrowHasApexAndFiniteBounceAcrossFrameRates() throws Exception {
        List<String> csv=new ArrayList<>();csv.add("fps,seconds,x,y,vx,vy,state");
        float endpoint=0;double referenceApex=0;
        for(int fps:new int[]{15,30,60,120,144}){
            Rig r=rig(null);r.motion.beginDrag();r.motion.drag(150,-550);r.motion.release(600,650);
            float apex=-550;double apexTime=0;
            for(int i=0;i<8*fps;i++){
                r.motion.tick(1.0/fps,0,false);float x=r.p.getX(),y=r.p.getY();
                assertTrue(x>=0&&x<=900&&y>=-800&&y<=-150,"canvas bounds");
                if(y>apex){apex=y;apexTime=(i+1.0)/fps;}
                csv.add(fps+","+(i+1.0)/fps+","+x+","+y+","+r.p.getVelocityX()+","+r.p.getVelocityY()+","+r.motion.state());
            }
            assertTrue(apex>-400);assertTrue(apexTime>.3&&apexTime<1.2);assertEquals(0,r.p.getVelocityY(),.001);
            assertEquals(-800,r.p.getY(),.001);assertFalse(r.motion.isBusy());
            if(fps==15){endpoint=r.p.getX();referenceApex=apexTime;}else{assertEquals(endpoint,r.p.getX(),.2);assertEquals(referenceApex,apexTime,.04);}
        }
        Path out=Path.of("build/reports/BASE-002B-trajectories.csv");Files.createDirectories(out.getParent());Files.write(out,csv);
    }
    @Test void horizontalFlingAndHighSpeedRemainRecoverable(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(100,-500);r.motion.release(800,0);run(r,.3,60);assertTrue(r.p.getX()>250);
        r.motion.beginDrag();r.motion.drag(400,-500);r.motion.release(100000,100000);run(r,12,60);
        assertTrue(r.p.getX()>=0&&r.p.getX()<=900);assertEquals(-800,r.p.getY(),.01);assertFalse(r.motion.isBusy());
    }
    @Test void hitchAndPauseDoNotAdvanceHugeSteps(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(150,-500);r.motion.release(700,650);float x=r.p.getX(),y=r.p.getY();
        r.motion.tick(5,0,false);assertEquals(x,r.p.getX());assertEquals(y,r.p.getY());
        r.motion.tick(1,0,true);assertEquals(x,r.p.getX());r.motion.tick(Double.NaN,0,false);assertEquals(y,r.p.getY());
        r.motion.tick(1.0/60,0,false);assertTrue(r.p.getX()>x&&r.p.getX()-x<15);
    }
    @Test void inOfficeLandingResumesNormalAndWindowBarrierSupports(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(650,-700);r.motion.release(0,0);run(r,5,60);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
        r.motion.beginDrag();r.motion.drag(120,-300);r.p.setBarrier(-500,0,400,false);r.motion.release(0,0);run(r,5,60);
        assertEquals(-500,r.p.getY(),.01);assertEquals(OfficeMotion.State.USER_PLACED,r.motion.state());
    }
    @Test void persistenceAndInvalidWorkAreaRecovery() throws Exception {
        Path file=temp.resolve("placement.json");
        try(var store=new PlacementStore(file)){Rig r=rig(store);r.motion.beginDrag();r.motion.drag(120,-400);r.motion.release(0,0);run(r,5,60);}
        try(var store=new PlacementStore(file)){
            Rig restart=rig(store);restart.motion.restore();assertEquals(120,restart.p.getX(),.01);assertEquals(OfficeMotion.State.USER_PLACED,restart.motion.state());
            var smaller=store.load(new Plane.RectArea(500,1000,0,-600),100,150);assertEquals(500,smaller.x());assertEquals(-600,smaller.y());
        }
        Files.writeString(file,"{bad json");try(var store=new PlacementStore(file)){assertNull(store.load(WORK,100,150));}
        Files.writeString(file,"{\"version\":1,\"manual\":true,\"x\":1e20,\"y\":0}");try(var store=new PlacementStore(file)){assertNull(store.load(WORK,100,150));}
    }
    @Test void aNewWindowAboveTheFeetDoesNotTeleportThePetUpward(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(120,-600);r.p.setBarrier(-500,0,400,false);
        r.motion.release(0,0);r.motion.tick(1.0/60,0,false);assertTrue(r.p.getY()<=-600);
        run(r,5,60);assertEquals(-800,r.p.getY(),.01);
    }
    @Test void aChangedMonitorClampsTheWholeCanvasAndRestoresNormalInsideOffice() throws Exception {
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(120,-600);r.motion.release(0,0);run(r,5,60);
        var movedWork=new Plane.RectArea(1200,2200,0,-900);
        r.motion.areas(new Plane.RectArea(1700,2200,-650,-900),movedWork);
        assertEquals(1200,r.p.getX(),.01);assertTrue(r.p.getX()+100<=movedWork.right());
        Path file=temp.resolve("inside.json");Files.writeString(file,"{\"version\":1,\"manual\":true,\"x\":650,\"y\":-800}");
        try(var store=new PlacementStore(file)){Rig restored=rig(store);restored.motion.restore();assertEquals(OfficeMotion.State.NORMAL,restored.motion.state());}
    }
    @Test void autonomousRootMotionPreservesConfiguredSpeedWithoutWeakeningThrowFriction(){
        Rig r=rig(null);float start=r.p.getX();
        for(int i=0;i<60;i++)r.motion.tick(1.0/60,45,false);
        assertEquals(45,r.p.getX()-start,.05);
        r.motion.beginDrag();r.motion.drag(120,-800);r.motion.release(45,0);run(r,1,60);
        assertEquals(0,r.p.getVelocityX(),.01);
    }
    @Test void walksToMeaningfulTargetsOnBothSidesThenStopsWithoutDrift(){
        for(double side:new double[]{0,1}){
            Rig r=rig(null);float start=r.p.getX();
            assertTrue(r.motion.startWalk(side,.5));r.motion.tick(.1,0,false);
            assertEquals(side==0?-1:1,r.motion.walkingDirection());
            run(r,5,60);assertFalse(r.motion.isWalking());assertEquals(0,r.motion.walkingDirection());
            assertEquals(112.5,Math.abs(r.p.getX()-start),.5);
            float stopped=r.p.getX();run(r,30,60);assertEquals(stopped,r.p.getX(),.001);
        }
    }
    @Test void autonomousWalkingDoesNotCrossASupportLedgeAndCanBeInterrupted(){
        Rig r=rig(null);r.p.setBarrier(-780,600,120,false);r.p.changePosition(0,650,-780);
        assertTrue(r.motion.startWalk(1,1));run(r,5,60);
        assertEquals(-780,r.p.getY(),.01);assertTrue(r.p.getX()<720);assertFalse(r.motion.isWalking());
        r.p.clearBarriers();r.p.changePosition(0,650,-800);
        assertTrue(r.motion.startWalk(0,1));r.motion.tick(.1,0,false);r.motion.cancelWalk();
        float x=r.p.getX();run(r,2,60);assertEquals(x,r.p.getX(),.001);
        assertTrue(r.motion.startWalk(1,1));r.motion.beginDrag();assertFalse(r.motion.isWalking());
    }
    @Test void relocationFallsThroughExistingPhysicsBeforeWalkingAndDoesNotRecenterValidPositions(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(120,-400);r.motion.release(0,0);r.motion.relocate();
        r.motion.tick(.1,0,false);assertTrue(r.p.getY()<-400);assertEquals(120,r.p.getX(),.001);
        assertEquals(0,r.motion.walkingDirection());
        boolean walked=false;
        for(int i=0;i<15*60;i++){r.motion.tick(1.0/60,0,false);if(r.motion.walkingDirection()!=0){walked=true;assertTrue(r.motion.isGrounded());}}
        assertTrue(walked);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
        float x=r.p.getX();r.motion.relocate();run(r,2,60);assertEquals(x,r.p.getX(),.001);
        assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
    }
    @Test void smallOfficeAndOutsidePlacementDoNotStartRoaming(){
        Rig r=rig(null);r.motion.areas(new Plane.RectArea(640,750,-550,-800),WORK);
        assertFalse(r.motion.canRoam());assertFalse(r.motion.startWalk(0,1));
        r.motion.beginDrag();r.motion.drag(100,-700);r.motion.release(0,0);run(r,4,60);
        assertEquals(OfficeMotion.State.USER_PLACED,r.motion.state());assertFalse(r.motion.startWalk(1,1));
        r.motion.beginDrag();r.motion.drag(650,-700);r.motion.release(0,0);run(r,4,60);
        assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
    }
    @Test void relocationCanWalkLeftAndKeepsAnAirborneThrowVelocity(){
        Rig r=rig(null);r.motion.areas(new Plane.RectArea(0,500,-550,-800),WORK);
        r.motion.beginDrag();r.motion.drag(800,-800);r.motion.release(0,0);run(r,1,60);
        r.motion.relocate();r.motion.tick(.1,0,false);assertEquals(-1,r.motion.walkingDirection());
        run(r,12,60);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());assertEquals(399,r.p.getX(),.5);
        r.motion.beginDrag();r.motion.drag(800,-500);r.motion.release(-150,250);
        r.motion.relocate();assertEquals(-150,r.p.getVelocityX(),.001);assertEquals(250,r.p.getVelocityY(),.001);
        r.motion.tick(.1,0,false);assertEquals(0,r.motion.walkingDirection());assertTrue(r.p.getY()>-500);
    }
    @Test void guidedWalkingAndRelocationAgreeAcrossRenderFrameRates(){
        float walkEnd=0,returnEnd=0;
        for(int fps:new int[]{15,30,60,120,144}){
            Rig r=rig(null);assertTrue(r.motion.startWalk(0,.5));run(r,5,fps);
            assertFalse(r.motion.isWalking());
            if(fps==15)walkEnd=r.p.getX();else assertEquals(walkEnd,r.p.getX(),.01);
            r.motion.beginDrag();r.motion.drag(120,-400);r.motion.release(0,0);r.motion.relocate();run(r,15,fps);
            assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
            if(fps==15)returnEnd=r.p.getX();else assertEquals(returnEnd,r.p.getX(),.01);
        }
    }
    private Rig outside(double random){
        Rig base=rig(null);OfficeMotion m=new OfficeMotion(base.p,100,150,null,()->random);m.areas(OFFICE,WORK);
        Rig r=new Rig(base.p,m);m.beginDrag();m.drag(120,-400);m.release(0,0);run(r,5,60);return r;
    }
    private void home(Rig r,double seconds,boolean suspended,boolean occupied){
        for(int i=0;i<Math.round(seconds*60);i++){r.motion.tickHome(1d/60,suspended,occupied);r.motion.tick(1d/60,0,suspended);}
    }
    @Test void homeGraceStartsAfterStableLandingAndUsesBothRandomEndpoints(){
        for(double random:new double[]{0,.5,1}){
            Rig r=outside(random);assertEquals(90+60*random,r.motion.homeRemaining(),.001);
            home(r,89,false,false);assertEquals(OfficeMotion.HomeState.GRACE,r.motion.homeState());assertEquals(120,r.p.getX(),.001);
            home(r,2+60*random,false,false);assertEquals(OfficeMotion.State.RELOCATING,r.motion.state());
            home(r,12,false,false);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());assertEquals(OfficeMotion.HomeState.NONE,r.motion.homeState());
        }
    }
    @Test void airborneReleaseDoesNotStartHomeClock(){
        Rig r=rig(null);r.motion.beginDrag();r.motion.drag(120,-300);r.motion.release(100,200);
        home(r,.2,false,false);assertEquals(OfficeMotion.HomeState.NONE,r.motion.homeState());
        home(r,5,false,false);assertEquals(OfficeMotion.HomeState.GRACE,r.motion.homeState());
        assertTrue(r.motion.homeRemaining()>85);
    }
    @Test void pauseAndHideFreezeGraceAndSupportedReturn(){
        Rig r=outside(0);home(r,80,false,false);double remaining=r.motion.homeRemaining();
        home(r,200,true,false);assertEquals(remaining,r.motion.homeRemaining(),.0001);assertEquals(120,r.p.getX(),.001);
        home(r,11,false,false);assertTrue(r.motion.isAutomaticReturn());float x=r.p.getX();r.motion.suspend();
        home(r,200,true,false);assertEquals(x,r.p.getX(),.001);assertTrue(r.motion.isAutomaticReturn());
        home(r,12,false,false);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
    }
    @Test void fullSpecialAndManualActionBlockHomeWithoutQueuingARandomWalk(){
        Rig r=outside(0);home(r,95,false,true);
        assertEquals(OfficeMotion.HomeState.DUE,r.motion.homeState());assertEquals(120,r.p.getX(),.001);assertFalse(r.motion.canRoam());
        home(r,1,false,false);assertTrue(r.motion.isAutomaticReturn());
    }
    @Test void userInputDefersHomeAndDraggingCancelsReturnUntilNewLanding(){
        Rig r=outside(0);home(r,89,false,false);r.motion.deferHomeForInteraction();
        assertEquals(20,r.motion.homeRemaining(),.001);home(r,21,false,false);assertTrue(r.motion.isAutomaticReturn());
        r.motion.beginDrag();assertEquals(OfficeMotion.HomeState.NONE,r.motion.homeState());assertFalse(r.motion.isAutomaticReturn());
        r.motion.drag(200,-400);r.motion.release(0,0);home(r,5,false,false);
        assertEquals(OfficeMotion.HomeState.GRACE,r.motion.homeState());assertTrue(r.motion.homeRemaining()>85);assertEquals(200,r.p.getX(),.001);
        r.motion.relocate();assertEquals(OfficeMotion.State.RELOCATING,r.motion.state());assertFalse(r.motion.isAutomaticReturn());
        home(r,12,false,false);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
    }
    @Test void unsupportedAndVerticallyUnreachablePathsWaitAndRetryWithoutWalking(){
        Rig r=outside(0);r.p.setBarrier(-780,0,300,false);r.p.changePosition(0,120,-780);
        home(r,91,false,false);assertEquals(OfficeMotion.HomeState.RETRY,r.motion.homeState());assertEquals(120,r.p.getX(),.001);
        assertEquals(0,r.motion.walkingDirection());assertTrue(r.motion.homeReason().contains("support"));
        r.p.clearBarriers();home(r,140,false,false);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
        Rig high=outside(0);high.p.setBarrier(-400,0,1000,false);high.p.changePosition(0,120,-400);
        home(high,91,false,false);assertEquals(OfficeMotion.HomeState.RETRY,high.motion.homeState());assertEquals(120,high.p.getX(),.001);
    }
    @Test void changingSupportDuringReturnStopsAndRetries(){
        Rig r=outside(0);home(r,91,false,false);assertTrue(r.motion.isAutomaticReturn());
        float x=r.p.getX();r.p.setBarrier(-790,x-100,100,false);r.p.changePosition(0,x,-790);
        home(r,.1,false,false);assertEquals(OfficeMotion.HomeState.RETRY,r.motion.homeState());assertEquals(0,r.motion.walkingDirection());
    }
    @Test void outsideSavedPositionReceivesFreshGraceWithoutRestartTeleport() throws Exception {
        Path file=temp.resolve("home-placement.json");Files.writeString(file,"{\"version\":1,\"manual\":true,\"x\":120,\"y\":-800}");
        try(var store=new PlacementStore(file)){Rig r=rig(store);r.motion.restore();assertEquals(120,r.p.getX(),.001);
            assertEquals(OfficeMotion.HomeState.NONE,r.motion.homeState());home(r,1,false,false);
            assertEquals(OfficeMotion.HomeState.GRACE,r.motion.homeState());assertTrue(r.motion.homeRemaining()>89&&r.motion.homeRemaining()<=150);
        }
    }

    @Test void homeWalkingRemainsFrameRateIndependent(){
        float endpoint=0;
        for(int fps:new int[]{15,30,60,120,144}){
            Rig r=outside(0);boolean moved=false;
            for(int n=0;n<110*fps;n++){
                r.motion.tickHome(1d/fps,false,false);r.motion.tick(1d/fps,0,false);
                if(r.motion.walkingDirection()!=0){moved=true;assertTrue(r.motion.isGrounded());}
            }
            assertTrue(moved);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
            if(fps==15)endpoint=r.p.getX();else assertEquals(endpoint,r.p.getX(),.01);
        }
    }
    @Test void unreachableRouteRetriesWithoutPositionChangesAndNewSupportCanUnblockIt(){
        Rig r=outside(0);r.p.setBarrier(-780,0,300,false);r.p.changePosition(0,120,-780);
        home(r,91,false,false);assertEquals(OfficeMotion.HomeState.RETRY,r.motion.homeState());
        home(r,29,false,false);assertEquals(120,r.p.getX(),.001);assertEquals(OfficeMotion.HomeState.RETRY,r.motion.homeState());
        r.p.clearBarriers();r.p.setBarrier(-780,0,1000,false);
        home(r,15,false,false);assertEquals(OfficeMotion.State.NORMAL,r.motion.state());
    }

}
