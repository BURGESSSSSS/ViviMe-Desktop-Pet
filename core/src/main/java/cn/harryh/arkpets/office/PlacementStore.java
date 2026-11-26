/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.office;

import cn.harryh.arkpets.utils.Logger;
import cn.harryh.arkpets.utils.Plane;
import com.alibaba.fastjson2.JSONObject;
import java.nio.file.*;
import java.util.concurrent.*;

/** Persists only settled placement on a serial daemon writer, never on the rendering thread. */
public final class PlacementStore implements AutoCloseable {
    private final Path path;
    private final ExecutorService writer=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"VivimePlacement");t.setDaemon(true);return t;});
    public PlacementStore(Path path){this.path=path;}
    public record Position(float x,float y){}
    public Position load(Plane.RectArea work,float width,float height){
        try{
            if(!Files.isRegularFile(path)||Files.size(path)>16384)return null;
            JSONObject j=JSONObject.parseObject(Files.readString(path));
            if(j.getIntValue("version")!=1||!j.getBooleanValue("manual"))return null;
            if(!j.containsKey("x")||!j.containsKey("y"))return null;
            float x=j.getFloatValue("x"),y=j.getFloatValue("y");
            if(!Float.isFinite(x)||!Float.isFinite(y)||Math.abs(x)>1000000||Math.abs(y)>1000000)return null;
            // Absolute coordinates remain useful after DPI/work-area changes; clamp the entire canvas.
            return new Position(Math.max(work.left(),Math.min(x,work.right()-width)),
                    Math.max(work.bottom(),Math.min(y,work.top()-height)));
        }catch(Exception e){Logger.warn("Placement","Ignoring invalid saved placement: "+e.getClass().getSimpleName());return null;}
    }
    public void save(boolean manual,float x,float y,Plane.RectArea work){
        JSONObject j=new JSONObject();j.put("version",1);j.put("manual",manual);j.put("x",x);j.put("y",y);j.put("work",work);
        String text=j.toJSONString();
        writer.submit(()->{try{
            Path absolute=path.toAbsolutePath();Files.createDirectories(absolute.getParent());Path temp=absolute.resolveSibling(absolute.getFileName()+".tmp");
            Files.writeString(temp,text);
            try{Files.move(temp,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,absolute,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){Logger.warn("Placement","Cannot save placement: "+e);}});
    }
    @Override public void close(){writer.shutdown();try{writer.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
