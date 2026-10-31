package com.perabru.vanishcam;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class GestureRulesTest {
    private float[][] victory() {
        float[][] h=new float[21][2];
        h[0]=new float[]{100,200};
        h[5]=new float[]{75,140};h[6]=new float[]{65,110};h[8]=new float[]{50,60};
        h[9]=new float[]{105,140};h[10]=new float[]{110,105};h[12]=new float[]{120,50};
        h[13]=new float[]{125,150};h[14]=new float[]{130,125};h[16]=new float[]{125,175};
        h[17]=new float[]{145,160};h[18]=new float[]{155,140};h[20]=new float[]{145,180};
        return h;
    }
    @Test public void victorySurvivesRotationAndMirroring() {
        float[][] h=victory();assertTrue(GestureRules.victory(h));
        for(float[] p:h) {float x=p[0];p[0]=-p[1];p[1]=x;}
        assertTrue(GestureRules.victory(h));
        for(float[] p:h)p[0]=-p[0];assertTrue(GestureRules.victory(h));
    }
    @Test public void invalidAndTinyHandsDoNotTrigger() {
        assertFalse(GestureRules.victory(null));assertFalse(GestureRules.victory(new float[0][]));
        float[][] h=victory();h[8][0]=Float.NaN;assertFalse(GestureRules.victory(h));
        h=victory();for(float[] p:h){p[0]*=.01f;p[1]*=.01f;}assertFalse(GestureRules.victory(h));
    }
    @Test public void joinedHandsMustBeNearEachOther() {
        float[][] a=victory(),b=victory();for(float[] p:b)p[0]+=50;
        assertTrue(GestureRules.joined(List.of(a,b)));
        for(float[] p:b)p[0]+=400;assertFalse(GestureRules.joined(List.of(a,b)));
        assertFalse(GestureRules.joined(java.util.Collections.singletonList(a)));
    }
    @Test public void foreheadRequiresFaceAndCloseFingers() {
        float[][] h=victory();assertFalse(GestureRules.forehead(h,null,100));
        assertFalse(GestureRules.forehead(h,new float[]{85,55},150));
        h[12]=new float[]{65,55};h[10]=new float[]{85,105};
        assertTrue(GestureRules.forehead(h,new float[]{57,52},150));
        assertFalse(GestureRules.forehead(h,new float[]{400,200},150));
    }
}
