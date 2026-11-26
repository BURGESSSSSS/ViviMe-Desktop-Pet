/** Copyright (c) 2022-2026, Harry Huang; 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.animations.PetAction;
import com.alibaba.fastjson2.JSONObject;
import java.util.EnumMap;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/** Selects quiet opportunities; the existing action slot owns playback and completion. */
public final class QuietOfficePolicy {
    public record Rule(double weight,double cooldownSeconds) {}
    public record Settings(double restMin,double restMax,double idleProbability,EnumMap<PetAction,Rule> rules) {
        public static Settings parse(String json){
            var o=JSONObject.parseObject(json);
            double min=o.getDoubleValue("rest_min_seconds"),max=o.getDoubleValue("rest_max_seconds"),idle=o.getDoubleValue("idle_probability");
            if(!Double.isFinite(min)||!Double.isFinite(max)||!Double.isFinite(idle)||min<20||max<min||max>30||idle<0||idle>1)
                throw new IllegalArgumentException("Invalid quiet opportunity configuration");
            var rules=new EnumMap<PetAction,Rule>(PetAction.class);
            for(var entry:o.getJSONObject("actions").entrySet()){
                var action=PetAction.valueOf(entry.getKey());var r=(JSONObject)entry.getValue();
                double weight=r.getDoubleValue("weight"),cooldown=r.getDoubleValue("cooldown_seconds");
                if(action==PetAction.RELAX||!Double.isFinite(weight)||!Double.isFinite(cooldown)||weight<=0||cooldown<0
                        ||action==PetAction.SPECIAL&&cooldown<900)throw new IllegalArgumentException("Invalid action rule: "+action);
                rules.put(action,new Rule(weight,cooldown));
            }
            if(rules.size()!=7)throw new IllegalArgumentException("Exactly seven reviewed actions required");
            return new Settings(min,max,idle,rules);
        }
    }
    public enum Decision { WAIT, BLOCKED, QUIET, NO_ELIGIBLE_ACTION, SELECTED }
    private final DoubleSupplier random;
    private final Settings settings;
    private final double[] allowedAt=new double[PetAction.values().length];
    private final PetAction[] actions;
    private PetAction lastAction;
    private double nextDue;
    private boolean wasBlocked;
    private Decision decision=Decision.WAIT;
    public QuietOfficePolicy(double now,DoubleSupplier random,Settings settings){
        this.random=random;this.settings=settings;actions=settings.rules.keySet().toArray(PetAction[]::new);defer(now);
    }
    private double roll(){double r=random.getAsDouble();if(!Double.isFinite(r))throw new IllegalStateException("Invalid random value");return Math.max(0,Math.min(Math.nextDown(1d),r));}
    public void defer(double now){nextDue=now+settings.restMin+roll()*(settings.restMax-settings.restMin);}
    /** Never queues an opportunity lost to input, physics, menus, pause or invisibility. */
    public PetAction poll(double now,boolean blocked,Predicate<PetAction> eligible){
        decision=Decision.WAIT;
        if(blocked){wasBlocked=true;decision=Decision.BLOCKED;return null;}
        if(wasBlocked){wasBlocked=false;defer(now);return null;}
        if(now<nextDue)return null;
        if(roll()<settings.idleProbability){decision=Decision.QUIET;defer(now);return null;}
        double total=0;
        for(var a:actions)if(a!=lastAction&&now>=allowedAt[a.ordinal()]&&eligible.test(a))total+=settings.rules.get(a).weight;
        if(total==0){decision=Decision.NO_ELIGIBLE_ACTION;defer(now);return null;}
        double target=roll()*total;
        for(var a:actions)if(a!=lastAction&&now>=allowedAt[a.ordinal()]&&eligible.test(a)){
            target-=settings.rules.get(a).weight;
            if(target<0){decision=Decision.SELECTED;defer(now);return a;}
        }
        throw new IllegalStateException("Changing eligibility during selection");
    }
    /** Shares cooldowns with menu and context playback, including interrupted performances. */
    public void started(PetAction action,double now){lastAction=action;var rule=settings.rules.get(action);if(rule!=null)allowedAt[action.ordinal()]=now+rule.cooldownSeconds;}
    public void finished(PetAction action,double now){var rule=settings.rules.get(action);if(rule!=null)allowedAt[action.ordinal()]=now+rule.cooldownSeconds;defer(now);}
    public double nextDue(){return nextDue;}
    public double allowedAt(PetAction action){return allowedAt[action.ordinal()];}
    public Decision decision(){return decision;}
}
