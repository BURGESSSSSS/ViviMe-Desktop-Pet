/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class DawnPlanTest {
    @Test void layoutsKeepBothSidesAndWholeVisibleRectsSafe(){
        for(int seed=0;seed<1000;seed++)for(double scale:new double[]{.4,.7,1}){
            double x=800,w=1600,tw=104.2166667*scale,aw=131.083333*scale;
            var layout=DawnPlan.layout(seed,x,w,tw,aw,scale);assertEquals(3,layout.size());assertTrue(layout.stream().anyMatch(p->p.x()<x));assertTrue(layout.stream().anyMatch(p->p.x()>x));
            for(int i=0;i<3;i++){var p=layout.get(i);assertTrue(p.x()-tw/2>=40*scale);assertTrue(p.x()+tw/2<=w-40*scale);assertTrue(Math.abs(p.x()-x)>=(tw+aw)/2+20*scale);for(int j=0;j<i;j++)assertTrue(Math.abs(p.x()-layout.get(j).x())>=tw+20*scale);}
        }
    }
    @Test void matchesApprovedJavascriptSeedAndBag(){
        var layout=DawnPlan.layout(104729,500,1600,104.2166667,131.083333,1);
        assertEquals(142.397718466,layout.get(0).x(),1e-6);assertEquals(653.579429192,layout.get(1).x(),1e-6);assertEquals(273.624266971,layout.get(2).x(),1e-6);
        assertEquals(List.of("R3C1","R1C2","R1C1","R1C3","R2C1","R2C2"),DawnPlan.faces(104729));
    }
    @Test void sixFacesAreWhitelistedAndNeverRepeat(){for(int seed=0;seed<1000;seed++)assertEquals(Set.of(DawnPlan.FACES),new HashSet<>(DawnPlan.faces(seed)));}
    @Test void feetLandAndStayOnTheSameSupport(){for(var p:DawnPlan.layout(104729,500,1600,104.2166667,131.083333,1)){
        var middle=DawnPlan.pose(p,p.start()+p.duration()/2,500,660);assertTrue(middle.visible());assertTrue(middle.y()<660-90);
        for(int fps:new int[]{30,60,120})for(int i=0;i<fps*5;i++){var pose=DawnPlan.pose(p,i/(double)fps,500,660);assertTrue(pose.y()<=660+1e-6);}
        var done=DawnPlan.pose(p,2.7,500,660);assertTrue(done.landed());assertEquals(660,done.y(),1e-6);assertEquals(0,done.rotation(),1e-6);
    }}
    @Test void insufficientSpaceFailsInsteadOfHidingUnitsOffScreen(){assertThrows(IllegalArgumentException.class,()->DawnPlan.layout(3,100,300,104,132,1));}
    @Test void summonsGrowUniformlyAndRotateAroundTheirLandingSupport(){
        for(var p:DawnPlan.layout(104729,500,1600,104,132,1)){
            var start=DawnPlan.pose(p,p.start(),500,660);
            assertEquals(500,start.x());assertEquals(.25,start.sx());
            double previous=.25;
            for(double age=0;age<1.5;age+=1./120){
                var pose=DawnPlan.pose(p,p.start()+age,500,660);
                assertEquals(pose.sx(),pose.sy());
                assertTrue(pose.sx()>=previous&&pose.sx()<=1);
                assertTrue(Math.abs(pose.rotation())<=Math.toRadians(15));
                assertTrue(pose.y()<=660);
                previous=pose.sx();
            }
            var end=DawnPlan.pose(p,p.start()+1.5,500,660);
            assertEquals(1,end.sx());assertEquals(0,end.rotation());assertEquals(660,end.y());
        }
    }
    @Test void physicalPixelMappingRoundTripsAcrossDpiSizesAndNegativeOrigins(){for(int[] r:new int[][]{{0,0,2560,1528},{-1920,100,1920,1040},{1920,-200,3840,2080},{0,0,1366,728}}){var c=new DawnCoordinates(r[0],r[1],r[2],r[3]);double x=r[0]+r[2]*.37,y=r[1]+r[3]*.61;assertTrue(c.contains(c.localX(x),c.localY(y)));assertEquals(x,c.screenX(c.localX(x)),1e-7);assertEquals(y,c.screenY(c.localY(y)),1e-7);assertFalse(c.contains(c.localX(r[0]-1),c.localY(y)));assertFalse(c.contains(Double.NaN,0));}}
}
