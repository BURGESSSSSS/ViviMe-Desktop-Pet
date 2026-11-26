/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import java.util.*;

/** Ports the approved E layout and expression bag; never moves the daily pet. */
public final class DawnPlan {
    public static final String[] FACES={"R1C1","R1C2","R1C3","R2C1","R2C2","R3C1"};
    public static final class Random {
        private int state;
        public Random(long seed){state=(int)seed;}
        public double next(){state=state*1664525+1013904223;return Integer.toUnsignedLong(state)/4294967296.0;}
    }
    public record Landing(double x,double start,double duration,double height,double twist,double bounce){}
    public record Pose(boolean visible,double x,double y,double rotation,double sx,double sy,boolean landed){}
    private static <T> void shuffle(List<T> a,Random r){for(int i=a.size()-1;i>0;i--)Collections.swap(a,i,(int)(r.next()*(i+1)));}
    public static List<String> faces(long seed){var a=new ArrayList<>(List.of(FACES));shuffle(a,new Random(seed^0x5f3759df));return a;}
    public static List<Landing> layout(long seed,double actorX,double width,double tokenWidth,double actorWidth,double scale){
        Random r=new Random(seed);double gap=tokenWidth+20*scale,clear=(actorWidth+tokenWidth)/2+20*scale;
        for(int attempt=0;attempt<128;attempt++){
            var slots=new ArrayList<>(List.of(-360.,-225.,-155.,155.,285.,420.));shuffle(slots,r);
            double[] x=new double[3];boolean left=false,right=false,valid=true;
            for(int i=0;i<3;i++){double d=(slots.get(i)+(r.next()-.5)*18)*scale;x[i]=actorX+d;left|=d<0;right|=d>0;
                if(Math.abs(d)<clear||x[i]-tokenWidth/2<40*scale||x[i]+tokenWidth/2>width-40*scale)valid=false;}
            for(int i=0;i<3;i++)for(int j=i+1;j<3;j++)if(Math.abs(x[i]-x[j])<gap)valid=false;
            if(!valid||!left||!right)continue;
            var result=new ArrayList<Landing>();for(int i=0;i<3;i++)result.add(new Landing(x[i],.25+i*.36,.85+r.next()*.15,(100+r.next()*45)*scale,(r.next()-.5)*.48,7*scale));return result;
        }
        throw new IllegalArgumentException("Selected monitor has no safe two-sided summon layout");
    }
    public static Pose pose(Landing l,double time,double actorX,double ground){
        double u=(time-l.start)/l.duration;
        if(u<0)return new Pose(false,actorX,ground,0,.25,.25,false);
        if(u<1){
            double grow=Math.min(1,(time-l.start)/.62);
            double size=.25+.75*grow*grow*(3-2*grow);
            double travel=u*u*(3-2*u);
            return new Pose(true,actorX+(l.x-actorX)*travel,ground-4*l.height*u*(1-u),
                    Math.sin(Math.PI*u)*l.twist,size,size,false);
        }
        double q=Math.min(1,(time-l.start-l.duration)/.26);
        return new Pose(true,l.x,ground-4*l.bounce*q*(1-q),0,1,1,q>=1);
    }
    private DawnPlan(){}
}
