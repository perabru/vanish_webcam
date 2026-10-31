package com.perabru.vanishcam;

/** State machine independent of camera and UI, timestamps are monotonic milliseconds. */
final class EffectTimeline {
    enum Mode { NORMAL, NARUTO, PARTICLES, GOKU }
    enum Phase { READY, OUT, HIDDEN, IN }
    Mode mode = Mode.NORMAL;
    Phase phase = Phase.READY;
    long started;
    private long heldSince = -1;
    private boolean armed = true;

    void reset() { phase = Phase.READY; heldSince = -1; armed = false; }
    boolean animating() { return phase == Phase.OUT || phase == Phase.IN; }
    long duration() {
        return switch (mode) { case NORMAL -> 3600; case NARUTO -> 1450; case PARTICLES -> 2100; case GOKU -> 950; };
    }
    float progress(long now) { return Math.max(0f, Math.min(1f, (now - started) / (float) duration())); }
    void advance(long now) {
        if (animating() && progress(now) >= 1f) phase = phase == Phase.OUT ? Phase.HIDDEN : Phase.READY;
    }
    boolean trigger(long now) {
        if (animating()) return false;
        phase = phase == Phase.HIDDEN ? Phase.IN : Phase.OUT;
        started = now;
        armed = false;
        heldSince = -1;
        return true;
    }
    boolean gesture(boolean matches, long now) {
        if (!matches) { heldSince = -1; armed = true; return false; }
        if (!armed || animating() || (phase == Phase.HIDDEN && mode != Mode.GOKU)) return false;
        if (heldSince < 0) heldSince = now;
        return now - heldSince >= (mode == Mode.GOKU ? 1000 : 250) && trigger(now);
    }
}
