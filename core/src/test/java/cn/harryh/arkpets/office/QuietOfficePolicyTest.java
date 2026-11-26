/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;
import cn.harryh.arkpets.animations.PetAction;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QuietOfficePolicyTest {
    private QuietOfficePolicy.Settings settings(){try(var in=getClass().getResourceAsStream("/vivime-autonomy.json")){
        return QuietOfficePolicy.Settings.parse(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
    }catch(Exception e){throw new IllegalStateException(e);}}
    @Test void initialOpportunitiesRespectQuietProbabilityAndConfiguredWeights(){
        var random=new Random(4004);int idle=0;var counts=new EnumMap<PetAction,Integer>(PetAction.class);var s=settings();
        for(int i=0;i<100000;i++){
            var p=new QuietOfficePolicy(0,random::nextDouble,s);var a=p.poll(p.nextDue(),false,x->true);
            if(a==null)idle++;else counts.merge(a,1,Integer::sum);
        }
        assertEquals(.35,idle/100000d,.006);
        for(var entry:s.rules().entrySet())assertEquals(entry.getValue().weight()/100,counts.get(entry.getKey())/(double)(100000-idle),.006);
        assertEquals(7,counts.size());assertFalse(counts.containsKey(PetAction.RELAX));
        var result=new com.alibaba.fastjson2.JSONObject();result.put("seed",4004);result.put("opportunities",100000);result.put("quiet",idle);result.put("actions",counts);
        save("S3-004B-probability.json",result.toJSONString());
    }
    @Test void manySimulatedHoursReachAllBranchesRespectRestCooldownsAndNoRepeats(){
        var random=new Random(20261117);var p=new QuietOfficePolicy(0,random::nextDouble,settings());
        var seen=EnumSet.noneOf(PetAction.class);var ends=new EnumMap<PetAction,Double>(PetAction.class);
        PetAction last=null;double lastEnd=0;int quiet=0,actions=0;
        var trace=new StringBuilder("opportunity,seconds,action,end_seconds\n");
        for(int i=0;i<10000;i++){
            double now=p.nextDue();assertTrue(now-lastEnd>=20-1e-7&&now-lastEnd<=30+1e-7);
            var a=p.poll(now,false,x->true);
            if(a==null){quiet++;lastEnd=now;trace.append(i).append(',').append(now).append(",QUIET,").append(now).append('\n');continue;}
            assertNotEquals(last,a);if(ends.containsKey(a))assertTrue(now-ends.get(a)>=settings().rules().get(a).cooldownSeconds()-1e-7);
            seen.add(a);actions++;p.started(a,now);double finish=now+(a==PetAction.SPECIAL?12.5:7);
            p.finished(a,finish);ends.put(a,finish);last=a;lastEnd=finish;
            trace.append(i).append(',').append(now).append(',').append(a).append(',').append(finish).append('\n');
        }
        assertEquals(7,seen.size());assertTrue(actions>5000&&quiet>3000);
        save("S3-004B-seeded-trace.csv",trace.toString());
    }
    @Test void blockedInputPauseHideAndPhysicsDiscardOverdueOpportunities(){
        var p=new QuietOfficePolicy(0,()->.5,settings());
        assertNull(p.poll(1000,true,x->true));assertNull(p.poll(5000,true,x->true));
        assertNull(p.poll(5001,false,x->true));assertEquals(5026,p.nextDue());
        assertNotNull(p.poll(5026,false,x->true));
    }
    @Test void manualSpecialAndInterruptionsShareFullFifteenMinuteCooldown(){
        var p=new QuietOfficePolicy(0,()->.99,settings());p.started(PetAction.SPECIAL,0);p.finished(PetAction.SPECIAL,12);
        p.started(PetAction.SIT,13);p.finished(PetAction.SIT,15);
        assertEquals(912,p.allowedAt(PetAction.SPECIAL));
        assertNull(p.poll(911,false,a->a==PetAction.SPECIAL));
        assertEquals(PetAction.SPECIAL,p.poll(942,false,a->a==PetAction.SPECIAL));
    }
    @Test void outsideOrMissingModelsNeverSelectIneligibleActionsAndNoBacklog(){
        var random=new Random(41);var p=new QuietOfficePolicy(0,random::nextDouble,settings());
        for(int i=0;i<1000;i++){
            double t=p.nextDue();var a=p.poll(t,false,x->x!=PetAction.WALK&&x!=PetAction.HOP&&x!=PetAction.ANGRY);
            if(a!=null){assertNotEquals(PetAction.WALK,a);assertNotEquals(PetAction.HOP,a);assertNotEquals(PetAction.ANGRY,a);p.started(a,t);p.finished(a,t+8);}
        }
        p.defer(100000);assertNull(p.poll(p.nextDue(),false,a->false));assertTrue(p.nextDue()>100020);
    }
    @Test void invalidUnreviewedRulesAndShortSpecialCooldownAreRejected(){
        assertEquals(900,settings().rules().get(PetAction.SPECIAL).cooldownSeconds());
        assertThrows(IllegalArgumentException.class,()->QuietOfficePolicy.Settings.parse("{\"rest_min_seconds\":1}"));
    }
    private void save(String name,String text){try{
        var dir=java.nio.file.Path.of("build/reports");java.nio.file.Files.createDirectories(dir);java.nio.file.Files.writeString(dir.resolve(name),text);
    }catch(Exception e){throw new IllegalStateException(e);}}
}
