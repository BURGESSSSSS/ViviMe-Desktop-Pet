/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.animations;

import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Describes native Spine clips and mixes; never edits skeleton timelines. */
public record ActionSequence(String model, String contact, List<Step> steps) {
    public record Step(String animation,float from,float to,float speed,float mix) {
        public Step {
            if(animation==null || !Float.isFinite(from+to+speed+mix) || from<0 || to<=from || speed<=0 || mix<0 || mix>.2f)
                throw new IllegalArgumentException("Invalid reviewed animation segment");
        }
    }
    public ActionSequence { steps=List.copyOf(steps);if(steps.isEmpty())throw new IllegalArgumentException("Empty sequence"); }
    public float duration(){float total=0;for(int i=0;i<steps.size();i++){var s=steps.get(i);total+=(s.to-s.from)/s.speed-(i==0?0:s.mix);}return total;}
    public static ActionSequence parse(JSONObject value){
        List<Step> steps=new ArrayList<>();
        for(var raw:value.getJSONArray("steps")){
            var s=(JSONObject)raw;
            steps.add(new Step(s.getString("animation"),s.getFloatValue("from"),s.getFloatValue("to"),
                    s.containsKey("speed")?s.getFloatValue("speed"):1,s.getFloatValue("mix")));
        }
        return new ActionSequence(value.getString("model"),value.getString("ground_contact"),steps);
    }
}
