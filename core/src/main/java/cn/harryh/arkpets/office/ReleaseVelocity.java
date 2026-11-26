/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import java.util.Arrays;

/** Estimates screen-space pointer velocity using a bounded 140ms history and robust segment statistics. */
public final class ReleaseVelocity {
    private final long[] time=new long[64];
    private final float[] x=new float[64],y=new float[64],vx=new float[64],vy=new float[64],scratch=new float[64];
    private int count;
    public record Velocity(float x,float y){}
    public void clear(){count=0;}
    public void sample(long nanos,float px,float py){
        if(!Float.isFinite(px)||!Float.isFinite(py))return;
        // Keeps about 128ms even with high-poll-rate mice; never allocates per event.
        if(count>0 && nanos-time[count-1]<2_000_000L)return;
        if(count==time.length){System.arraycopy(time,1,time,0,--count);System.arraycopy(x,1,x,0,count);System.arraycopy(y,1,y,0,count);}
        time[count]=nanos;x[count]=px;y[count++]=py;
    }
    public Velocity release(long now){
        int start=0;while(start<count && now-time[start]>140_000_000L)start++;
        // No fabricated throw from batched/two-point messages or stale motion.
        if(count-start<3 || time[count-1]-time[start]<25_000_000L || now-time[count-1]>80_000_000L)return new Velocity(0,0);
        if(count>1 && time[count-1]-time[count-2]>=50_000_000L
                && Math.hypot(x[count-1]-x[count-2],y[count-1]-y[count-2])<2)return new Velocity(0,0);
        int n=0;
        for(int i=start+1;i<count;i++){
            double dt=(time[i]-time[i-1])/1e9;
            if(dt<.002 || dt>.1)continue;
            float sx=(float)((x[i]-x[i-1])/dt),sy=(float)((y[i]-y[i-1])/dt);
            if(Math.hypot(sx,sy)>12000)continue;
            vx[n]=sx;vy[n++]=sy;
        }
        if(n<2)return new Velocity(0,0);
        System.arraycopy(vx,0,scratch,0,n);Arrays.sort(scratch,0,n);float mx=scratch[n/2];
        System.arraycopy(vy,0,scratch,0,n);Arrays.sort(scratch,0,n);float my=scratch[n/2];
        double sx=0,sy=0,weight=0;
        for(int i=0;i<n;i++)if(Math.hypot(vx[i]-mx,vy[i]-my)<=Math.max(350,Math.hypot(mx,my)*.8)){
            double w=i+1;sx+=w*vx[i];sy+=w*vy[i];weight+=w;
        }
        if(weight==0)return new Velocity(0,0);
        sx/=weight;sy/=weight;double speed=Math.hypot(sx,sy),scale=speed>1400?1400/speed:1;
        return speed<55?new Velocity(0,0):new Velocity((float)(sx*scale),(float)(sy*scale));
    }
}
