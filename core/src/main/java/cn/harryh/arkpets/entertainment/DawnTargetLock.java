/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

import java.util.Map;

/** Retains one immutable visual target until the rendering lifecycle completes. */
final class DawnTargetLock {
    enum State { TRACKING, LOCKED, IMPACT }
    private State state=State.TRACKING;
    private double x,y;

    State state(){return state;}
    boolean frozen(){return state!=State.TRACKING;}
    double x(double tracking){return frozen()?x:tracking;}
    double y(double tracking){return frozen()?y:tracking;}

    void lock(double x,double y){
        if(frozen())throw new IllegalStateException("Previous target is still in flight or rendering");
        if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Invalid target");
        this.x=x;this.y=y;state=State.LOCKED;
    }
    boolean advance(boolean hit,boolean renderingComplete){
        State before=state;
        if(state==State.LOCKED&&hit)state=State.IMPACT;
        if(state==State.IMPACT&&renderingComplete)state=State.TRACKING;
        return before!=state;
    }
    void reset(){state=State.TRACKING;x=0;y=0;}
    Map<String,Object> snapshot(){return Map.of("state",state.name(),"frozen",frozen(),"x",x,"y",y);}
}
