package com.perabru.vanishcam;

import org.junit.Test;
import static org.junit.Assert.*;

public class EffectTimelineTest {
    @Test public void transitionKeepsHiddenUntilReturn() {
        EffectTimeline t=new EffectTimeline();assertTrue(t.trigger(100));
        assertFalse(t.trigger(200));t.advance(3699);assertEquals(EffectTimeline.Phase.OUT,t.phase);
        t.advance(3700);assertEquals(EffectTimeline.Phase.HIDDEN,t.phase);
        assertTrue(t.trigger(4000));t.advance(7600);assertEquals(EffectTimeline.Phase.READY,t.phase);
    }
    @Test public void gokuRequiresContinuousHoldAndReleaseBeforeReturn() {
        EffectTimeline t=new EffectTimeline();t.mode=EffectTimeline.Mode.GOKU;
        assertFalse(t.gesture(true,0));assertFalse(t.gesture(true,999));
        assertTrue(t.gesture(true,1000));t.advance(1950);
        assertFalse(t.gesture(true,2500));assertFalse(t.gesture(false,2600));
        assertFalse(t.gesture(true,2700));assertTrue(t.gesture(true,3700));
        t.advance(4650);assertEquals(EffectTimeline.Phase.READY,t.phase);
    }
    @Test public void interruptedPoseRestartsCountdown() {
        EffectTimeline t=new EffectTimeline();t.mode=EffectTimeline.Mode.GOKU;
        t.gesture(true,0);t.gesture(false,999);assertFalse(t.gesture(true,1000));
        assertFalse(t.gesture(true,1999));assertTrue(t.gesture(true,2000));
    }
    @Test public void calibrationResetDisarmsHeldPose() {
        EffectTimeline t=new EffectTimeline();t.reset();
        assertFalse(t.gesture(true,100));assertFalse(t.gesture(true,10000));
        t.gesture(false,10100);t.gesture(true,10200);assertTrue(t.gesture(true,10450));
    }
    @Test public void onlyGokuReturnsByGesture() {
        for(EffectTimeline.Mode m:EffectTimeline.Mode.values()) {
            EffectTimeline t=new EffectTimeline();t.mode=m;t.trigger(0);t.advance(t.duration());
            t.gesture(false,5000);t.gesture(true,5100);
            assertEquals(m==EffectTimeline.Mode.GOKU,t.gesture(true,6200));
        }
    }
    @Test public void progressIsClamped() {
        EffectTimeline t=new EffectTimeline();t.trigger(1000);
        assertEquals(0,t.progress(0),0);assertEquals(1,t.progress(10000),0);
    }
}
