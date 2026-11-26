/** Copyright (c) 2026, Vivime project contributors
 * At GPL-3.0 License
 */
package cn.harryh.arkpets.entertainment;

/** Converts Win32 physical work-area pixels to a normalized effect surface, including negative monitor origins. */
public record DawnCoordinates(int x,int y,int pixelWidth,int pixelHeight) {
    public double scale(){return Math.min(pixelWidth/1600.,pixelHeight/800.);}
    public double width(){return pixelWidth/scale();}
    public double height(){return pixelHeight/scale();}
    public double localX(double screenX){return (screenX-x)/scale();}
    public double localY(double screenY){return (screenY-y)/scale();}
    public double screenX(double localX){return x+localX*scale();}
    public double screenY(double localY){return y+localY*scale();}
    public boolean contains(double localX,double localY){return Double.isFinite(localX)&&Double.isFinite(localY)&&localX>=0&&localX<width()&&localY>=0&&localY<height();}
}
