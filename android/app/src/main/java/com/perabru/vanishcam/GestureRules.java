package com.perabru.vanishcam;

import java.util.List;

/** Geometry uses pixel coordinates so portrait/landscape share the same thresholds. */
final class GestureRules {
    static float distance(float[] a, float[] b) { return (float)Math.hypot(a[0]-b[0], a[1]-b[1]); }
    static boolean valid(float[][] h) {
        if (h == null || h.length != 21) return false;
        for (float[] p : h) if (p == null || p.length < 2 || !Float.isFinite(p[0]) || !Float.isFinite(p[1])) return false;
        return distance(h[0], h[9]) > 8;
    }
    static float scale(float[][] h) { return Math.max(distance(h[0], h[9]), distance(h[5], h[17])); }
    static boolean straight(float[][] h, int base, int joint, int tip) {
        float ax=h[base][0]-h[joint][0], ay=h[base][1]-h[joint][1];
        float bx=h[tip][0]-h[joint][0], by=h[tip][1]-h[joint][1];
        float cosine=(ax*bx+ay*by)/Math.max(0.001f,(float)Math.hypot(ax,ay)*(float)Math.hypot(bx,by));
        return cosine < -0.72f && distance(h[tip],h[0]) > distance(h[joint],h[0])*1.10f;
    }
    static boolean twoFingers(float[][] h) {
        return valid(h) && straight(h,5,6,8) && straight(h,9,10,12)
            && !straight(h,13,14,16) && !straight(h,17,18,20);
    }
    static boolean victory(float[][] h) { return twoFingers(h) && distance(h[8],h[12]) > scale(h)*0.23f; }
    static float[] palm(float[][] h) {
        float x=0,y=0;
        for(int i:new int[]{0,5,9,13,17}) { x+=h[i][0]; y+=h[i][1]; }
        return new float[]{x/5,y/5};
    }
    static boolean joined(List<float[][]> hands) {
        if(hands.size()<2 || !valid(hands.get(0)) || !valid(hands.get(1))) return false;
        float[][] a=hands.get(0),b=hands.get(1);
        float sa=scale(a),sb=scale(b),size=(sa+sb)/2;
        float ax=a[9][0]-a[0][0],ay=a[9][1]-a[0][1],bx=b[9][0]-b[0][0],by=b[9][1]-b[0][1];
        float alignment=(ax*bx+ay*by)/Math.max(1,(float)Math.hypot(ax,ay)*(float)Math.hypot(bx,by));
        return Math.max(sa,sb)<Math.min(sa,sb)*2.2f && alignment>0.25f
            && distance(palm(a),palm(b))<size*1.55f && distance(a[12],b[12])<size*1.9f
            && straight(a,9,10,12) && straight(b,9,10,12);
    }
    static boolean forehead(float[][] hand,float[] forehead,float faceWidth) {
        if(forehead==null || faceWidth<10 || !twoFingers(hand)) return false;
        float[] midpoint={(hand[8][0]+hand[12][0])/2,(hand[8][1]+hand[12][1])/2};
        return distance(hand[8],hand[12])<scale(hand)*0.6f && distance(midpoint,forehead)<faceWidth*0.38f;
    }
}
