/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.utils;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WindowEdgeIndexTest {
    @Test void matchesPixelReferenceForOverlappingAndNegativeWindows(){
        Random r=new Random(27);WindowEdgeIndex index=new WindowEdgeIndex();
        for(int run=0;run<500;run++){
            index.clear();Set<Integer> pixels=new HashSet<>();
            for(int w=0;w<80;w++){
                int top=r.nextInt(4000)-2000,bottom=top+1+r.nextInt(2000);
                boolean expected=!pixels.contains(top);
                assertEquals(expected,index.add(top,bottom));
                for(int y=top;y<bottom;y++)pixels.add(y);
            }
        }
    }
}
