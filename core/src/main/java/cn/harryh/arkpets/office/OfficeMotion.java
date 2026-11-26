/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Plane;
import cn.harryh.arkpets.utils.Logger;

/** Owns the office pet's position updates through Ark-Pets Plane and its original terrain. */
public final class OfficeMotion {
    public enum State { NORMAL, DRAGGING, THROWN, FALLING, LANDED, USER_PLACED, RELOCATING }
    public enum HomeState { NONE, GRACE, DUE, RETURNING, RETRY }
    private final Plane plane;
    private final float width, height;
    private final PlacementStore store;
    private Plane.RectArea office, work;
    private State state = State.NORMAL;
    private double accumulator, landedTime;
    private boolean manual;
    private boolean walking;
    private boolean manualWalking;
    private float destinationX, gaitSpeed = 45;
    private double relocationTime;
    private final java.util.function.DoubleSupplier homeRandom;
    private HomeState homeState = HomeState.NONE;
    private double homeRemaining, outsideStableTime, stuckTime;
    private boolean automaticReturn;
    private String homeReason = "";
    public static final double STEP = 1.0 / 120;

    public OfficeMotion(Plane plane, float width, float height, PlacementStore store) {
        this(plane,width,height,store,Math::random);
    }
    public OfficeMotion(Plane plane, float width, float height, PlacementStore store,
                        java.util.function.DoubleSupplier homeRandom) {
        this.plane = plane;
        this.width = width;
        this.height = height;
        this.store = store;
        this.homeRandom = homeRandom;
        plane.setStableImpacts(true);
        plane.setResilience(.09f);
    }
    public State state(){return state;}
    public boolean isBusy(){return state!=State.NORMAL && state!=State.USER_PLACED;}
    public boolean isManual(){return manual;}
    public boolean isWalking(){return walking;}
    public HomeState homeState(){return homeState;}
    public double homeRemaining(){return homeRemaining;}
    public String homeReason(){return homeReason;}
    public boolean isAutomaticReturn(){return automaticReturn;}
    public float destinationX(){return destinationX;}
    private void clearHome(){homeState=HomeState.NONE;homeRemaining=0;outsideStableTime=0;automaticReturn=false;homeReason="";}
    private void waitHome(boolean retry,String reason){
        automaticReturn=false;homeState=retry?HomeState.RETRY:HomeState.GRACE;
        homeRemaining=(retry?30:90)+(retry?30:60)*Math.max(0,Math.min(1,homeRandom.getAsDouble()));
        homeReason=reason;
        Logger.info("Home",homeState+", remaining="+homeRemaining+", reason="+reason+", position="+plane.getX()+","+plane.getY());
    }
    /** Advances the existing controller's grace clock before ordinary behavior selection. */
    public void tickHome(double dt,boolean suspended,boolean occupied){
        if(suspended||!Double.isFinite(dt)||dt<=0||dt>.25)return;
        if(state!=State.USER_PLACED||!isGrounded()||walking)return;
        if(inOffice()){settle();return;}
        if(homeState==HomeState.NONE){
            outsideStableTime+=dt;
            if(outsideStableTime>=.12)waitHome(false,"restored outside placement is stable");
            return;
        }
        homeRemaining=Math.max(0,homeRemaining-dt);
        if(homeRemaining>0)return;
        homeState=HomeState.DUE;
        if(occupied)return;
        // Uses Plane's actual support without inventing an elevated or teleporting route.
        float target=Math.max(office.left()+1,Math.min(plane.getX(),office.right()-width-1));
        if(office.getWidth()<width+2 || plane.getY()<office.bottom()-.1f || plane.getY()+height>office.top()+.1f){
            waitHome(true,"office cannot be reached on the current support height");return;
        }
        if(!supportedPath(target)){waitHome(true,"no continuous supported walking path");return;}
        relocate();automaticReturn=true;homeState=HomeState.RETURNING;homeReason="";
        Logger.info("Home","RETURNING, target="+destinationX+", from="+plane.getX()+", feet="+plane.getY());
    }
    /** Gives explicit user input priority without changing the saved landing position. */
    public void deferHomeForInteraction(){
        if(automaticReturn){settle();}
        if(homeState!=HomeState.NONE){homeRemaining=Math.max(20,homeRemaining);homeState=HomeState.GRACE;}
    }
    public boolean isGrounded(){return Math.abs(plane.getY()-plane.borderBottom())<.1f && Math.abs(plane.getVelocityY())<.1f;}
    /** Reports actual supported motion, rather than an animation's requested mobility. */
    public int walkingDirection(){
        return (walking || state==State.RELOCATING) && isGrounded() && Math.abs(plane.getVelocityX())>1
                ? (plane.getVelocityX()>0?1:-1) : 0;
    }
    public void setGaitSpeed(float speed){gaitSpeed=Math.max(10,Math.abs(speed));}
    public void cancelWalk(){
        if(walking){walking=false;plane.setHorizontalDrive(0);plane.stopMotion();
            if(manualWalking && store!=null)store.save(manual,plane.getX(),plane.getY(),work);
        }
        manualWalking=false;
    }
    /** Plans a short supported walk without changing manual placement or selecting the office as a return target. */
    public boolean startManualWalk(double directionRandom,double distanceRandom){
        if(isBusy()||!isGrounded())return false;
        var area=state==State.USER_PLACED?work:office;
        float distance=gaitSpeed*(1.5f+(float)Math.max(0,Math.min(1,distanceRandom)));
        int first=directionRandom<.5?-1:1;
        for(int direction:new int[]{first,-first}){
            float available=direction<0?plane.getX()-area.left()-4:area.right()-width-plane.getX()-4;
            float length=Math.min(distance,available);
            while(length>=12&&!supportedPath(plane.getX()+direction*length))length-=8;
            if(length>=12){destinationX=plane.getX()+direction*length;walking=true;manualWalking=true;return true;}
        }
        return false;
    }
    /** Checks the same continuous support used by Plane; avoids planning walks across ledges. */
    private boolean supportedPath(float target){
        float dx=target-plane.getX(), y=plane.getY();
        int steps=Math.max(1,(int)Math.ceil(Math.abs(dx)/8));
        for(int i=1;i<=steps;i++)
            if(Math.abs(plane.supportHeightAt(plane.getX()+dx*i/steps,y)-y)>.1f)return false;
        return true;
    }
    public boolean canRoam(){
        if(state!=State.NORMAL || !inOffice() || !isGrounded())return false;
        float distance=gaitSpeed;
        return (plane.getX()-distance>=office.left() && supportedPath(plane.getX()-distance))
                || (plane.getX()+distance<=office.right()-width && supportedPath(plane.getX()+distance));
    }
    /** Selects a one-to-four-second walk, shortening only when the support or office edge requires it. */
    public boolean startWalk(double directionRandom,double distanceRandom){
        if(!canRoam())return false;
        float distance=gaitSpeed*(1+3*(float)Math.max(0,Math.min(1,distanceRandom)));
        int first=directionRandom<.5?-1:1;
        for(int direction:new int[]{first,-first}){
            float available=direction<0?plane.getX()-office.left():office.right()-width-plane.getX();
            float length=Math.min(distance,available);
            while(length>=gaitSpeed && !supportedPath(plane.getX()+direction*length))length-=8;
            if(length>=gaitSpeed){
                destinationX=plane.getX()+direction*length;walking=true;
                Logger.info("Office","Walk target="+destinationX+", from="+plane.getX());return true;
            }
        }
        return false;
    }
    private void state(State next) {
        if (state != next) {
            Logger.info("Motion", state + " -> " + next + ", position=" + plane.getX() + "," + plane.getY());
            state = next;
        }
    }
    public void areas(Plane.RectArea office,Plane.RectArea work){
        boolean changed=this.work!=null&&!this.work.equals(work);this.office=office;this.work=work;
        if(changed){plane.setMovementArea(work);plane.setIgnoreBarriers(true);plane.changePosition(0,plane.getX(),plane.getY());plane.setIgnoreBarriers(false);}
        plane.setMovementArea(state==State.NORMAL?office:work);
    }
    public void restore(){
        if(store==null)return;var saved=store.load(work,width,height);
        if(saved!=null){
            plane.setMovementArea(work);plane.setIgnoreBarriers(true);plane.changePosition(0,saved.x(),saved.y());plane.setIgnoreBarriers(false);
            manual=!inOffice();state(manual?State.USER_PLACED:State.NORMAL);plane.setMovementArea(manual?work:office);
        }
    }
    public void beginDrag() {
        cancelWalk();
        clearHome();
        manual = true;
        accumulator = 0;
        plane.setMovementArea(work);
        plane.setIgnoreBarriers(true);
        plane.stopMotion();
        state(State.DRAGGING);
    }
    public void drag(float x,float y){plane.changePosition(0,x,y);}
    public void release(float vx,float vy){
        plane.setIgnoreBarriers(false);plane.setVelocity(vx,vy);accumulator=0;
        state(Math.hypot(vx,vy)>=55?State.THROWN:State.FALLING);
    }
    public void suspend() {
        cancelWalk();
        accumulator = 0;
        plane.stopMotion();
        if (state == State.THROWN || state == State.DRAGGING) {
            plane.setIgnoreBarriers(false);
            state(State.FALLING);
        }
    }
    public void relocate() {
        cancelWalk();
        clearHome();
        manual = false;
        accumulator = 0;
        if(isGrounded())plane.stopMotion();
        plane.setMovementArea(work);
        plane.setIgnoreBarriers(false);
        if(inOffice() && isGrounded()){settle();return;}
        // Preserves a valid position and chooses the nearest office edge rather than its center.
        destinationX=Math.max(office.left()+1,Math.min(plane.getX(),office.right()-width-1));
        relocationTime=0;stuckTime=0;
        state(State.RELOCATING);
    }
    private boolean inOffice(){return plane.getX()>=office.left()-.1f&&plane.getX()+width<=office.right()+.1f&&plane.getY()>=office.bottom()-.1f&&plane.getY()+height<=office.top()+.1f;}
    private void settle(){
        settle(true);
    }
    private void settle(boolean armGrace){
        plane.stopMotion();manual=!inOffice();state(manual?State.USER_PLACED:State.NORMAL);
        plane.setMovementArea(manual?work:office);
        clearHome();
        if(manual && armGrace)waitHome(false,"stable outside landing");
        if(store!=null)store.save(manual,plane.getX(),plane.getY(),work);
    }
    private void failAutomaticReturn(String reason){
        settle(false);waitHome(true,reason);
    }
    public void tick(double dt,float walkSpeed,boolean suspended){
        // Discards resumed/stalled time rather than applying an unbounded catch-up impulse.
        if(suspended||state==State.DRAGGING){accumulator=0;return;}
        if(!Double.isFinite(dt)||dt<0||dt>.25){accumulator=0;return;}
        accumulator+=Math.min(dt,.1);
        while(accumulator+1e-9>=STEP){
            accumulator-=STEP;
            if(state==State.RELOCATING){
                relocationTime+=STEP;
                float dx=destinationX-plane.getX();
                if(Math.abs(dx)<.5f && isGrounded()){
                    if(automaticReturn && !inOffice()){
                        failAutomaticReturn("support height changed at the destination");continue;
                    }
                    if(!inOffice())Logger.warn("Office","Relocation support cannot reach the office vertically; retaining a safe position");
                    settle();continue;
                }
                if(automaticReturn && isGrounded() && !supportedPath(plane.getX()+Math.signum(dx)*Math.min(8,Math.abs(dx)))){
                    failAutomaticReturn("support changed during return");continue;
                }
                // Falls and lands using the existing integrator before any supported walking.
                plane.setHorizontalDrive(isGrounded() && Math.abs(dx)>=.5f ? approachSpeed(dx) : 0);
                float previousX=plane.getX();
                plane.updatePosition((float)STEP);
                if(automaticReturn){
                    stuckTime=Math.abs(plane.getX()-previousX)<.001f?stuckTime+STEP:0;
                    if(stuckTime>1 || relocationTime>120){failAutomaticReturn("return stalled or timed out");continue;}
                }
                if(relocationTime>120){Logger.warn("Office","Relocation timed out safely");settle();}
                continue;
            }
            float drive=state==State.NORMAL?walkSpeed:0;
            if(walking){
                float dx=destinationX-plane.getX();
                if(Math.abs(dx)<.5f || !isGrounded() || (!manualWalking&&!inOffice()) || !supportedPath(destinationX)){cancelWalk();drive=0;}
                else drive=approachSpeed(dx);
            }
            // Autonomous root motion also goes through the single Plane integrator.
            plane.setHorizontalDrive(drive);
            plane.updatePosition((float)STEP);
            if(state==State.THROWN && plane.getVelocityY()<=0)state(State.FALLING);
            if((state==State.FALLING||state==State.THROWN||state==State.USER_PLACED) && !(manualWalking&&walking&&isGrounded())){
                if(Math.abs(plane.getVelocityX())<.01 && Math.abs(plane.getVelocityY())<.01 && Math.abs(plane.getY()-plane.borderBottom())<.01){
                    if(state!=State.USER_PLACED){landedTime=0;state(State.LANDED);}
                }else if(state==State.USER_PLACED)state(State.FALLING);
            }
            if(state==State.LANDED){landedTime+=STEP;if(landedTime>=.12)settle();}
        }
    }
    private float approachSpeed(float dx){
        return Math.signum(dx)*Math.min(gaitSpeed,Math.max(8.1f,Math.abs(dx)*8));
    }
}
