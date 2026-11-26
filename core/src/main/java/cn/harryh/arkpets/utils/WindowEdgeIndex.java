/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.utils;

import java.util.Arrays;

/** Tests top-edge visibility against preceding Z-ordered vertical intervals without per-pixel boxing. */
public final class WindowEdgeIndex {
    private int[] tops=new int[32], bottoms=new int[32];
    private int size;
    public void clear(){size=0;}
    public boolean add(int top,int bottom){
        boolean visible=true;
        for(int i=0;i<size;i++)if(tops[i]<=top && top<bottoms[i]){visible=false;break;}
        if(size==tops.length){tops=Arrays.copyOf(tops,size*2);bottoms=Arrays.copyOf(bottoms,size*2);}
        tops[size]=top;bottoms[size++]=bottom;
        return visible;
    }
}
