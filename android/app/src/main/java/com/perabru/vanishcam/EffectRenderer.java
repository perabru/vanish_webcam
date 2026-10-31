package com.perabru.vanishcam;

import android.content.Context;
import android.graphics.*;
import java.io.IOException;
import java.util.Random;

/** Worker-thread renderer. Frozen person cutout bounds CPU use during animations. */
final class EffectRenderer {
    final EffectTimeline timeline=new EffectTimeline();
    private Bitmap background,cutout,log;
    private int[] personPixels;
    private float[] mask;
    private int width,height;
    private long calibration=-1,lastGesture=-1000;
    private int[] backgroundSum;
    private int samples;
    private boolean manual;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    String status="Apoie o celular e capture o fundo sem ninguém na imagem.";
    boolean soundRequested;

    EffectRenderer(Context context) throws IOException {
        try(var input=context.getAssets().open("substitution_log.png")) { log=BitmapFactory.decodeStream(input); }
        if(log==null) throw new IOException("Imagem do tronco indisponível");
    }
    boolean ready() { return background!=null && calibration<0; }
    void clear() {
        background=null; cutout=null; mask=null; personPixels=null; calibration=-1;
        backgroundSum=null; samples=0; manual=false; timeline.reset();
        status="Apoie o celular e capture o fundo sem ninguém na imagem.";
    }
    void calibrate(long now) {
        clear(); calibration=now; status="Saia da imagem. Capturando em 4 segundos…";
    }
    void mode(EffectTimeline.Mode mode) { timeline.mode=mode; timeline.reset(); manual=false; }
    void requestTrigger() { if(ready()&&!timeline.animating()) manual=true; }

    Bitmap render(Bitmap frame,VisionProcessor vision,long now,boolean gestures) {
        soundRequested=false;
        int w=frame.getWidth(),h=frame.getHeight();
        if(width!=w || height!=h) {
            // A rotation or camera change invalidates spatial calibration.
            if(width!=0) clear();
            width=w; height=h;
        }
        if(calibration>=0) {
            long age=now-calibration;
            status="Saia da imagem • "+Math.max(1,(4000-age+999)/1000)+" s";
            if(age>=3400) {
                int[] pixels=new int[w*h]; frame.getPixels(pixels,0,w,0,0,w,h);
                if(backgroundSum==null) backgroundSum=new int[w*h*3];
                for(int i=0;i<pixels.length;i++) { int p=pixels[i]; backgroundSum[i*3]+=(p>>16)&255; backgroundSum[i*3+1]+=(p>>8)&255; backgroundSum[i*3+2]+=p&255; }
                samples++;
            }
            if(age>=4000 && samples>0) {
                int[] pixels=new int[w*h];
                for(int i=0;i<pixels.length;i++) pixels[i]=Color.rgb(backgroundSum[i*3]/samples,backgroundSum[i*3+1]/samples,backgroundSum[i*3+2]/samples);
                background=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
                backgroundSum=null; calibration=-1; timeline.reset();
                status="Fundo pronto. Entre na imagem e escolha seu efeito.";
            }
            return frame.copy(Bitmap.Config.ARGB_8888,false);
        }
        if(background==null) return frame.copy(Bitmap.Config.ARGB_8888,false);
        timeline.advance(now);
        boolean trigger=manual; manual=false;
        if(gestures && !timeline.animating() && now-lastGesture>=180) {
            lastGesture=now;
            boolean match=vision.gesture(frame,timeline.mode,now);
            if(!trigger) trigger=timeline.gesture(match,now);
        }
        if(trigger) {
            if(!timeline.animating()) timeline.trigger(now);
            capturePerson(frame,vision);
            soundRequested=true;
        }
        status= switch(timeline.phase) {
            case READY -> "Pronto • "+hint(timeline.mode);
            case OUT -> "Desaparecendo…";
            case HIDDEN -> "Invisível • toque em Reaparecer";
            case IN -> "Reaparecendo…";
        };
        if(timeline.phase==EffectTimeline.Phase.READY) return frame.copy(Bitmap.Config.ARGB_8888,false);
        Bitmap output=background.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas=new Canvas(output);
        if(timeline.phase==EffectTimeline.Phase.HIDDEN) {
            if(timeline.mode==EffectTimeline.Mode.NARUTO) drawLog(canvas,1);
            return output;
        }
        float p=timeline.progress(now);
        float vanish=timeline.phase==EffectTimeline.Phase.IN?1-p:p;
        if(cutout==null) return output;
        switch(timeline.mode) {
            case NORMAL -> dissolve(canvas,vanish);
            case PARTICLES -> particles(canvas,vanish);
            case NARUTO -> {
                paint.setAlpha(Math.round(255*Math.max(0,1-vanish*8)));
                canvas.drawBitmap(cutout,0,0,paint); paint.setAlpha(255);
                drawLog(canvas,vanish);
                smoke(canvas,vanish);
            }
            case GOKU -> {
                paint.setAlpha(Math.round(255*Math.max(0,1-vanish*2.7f)));
                canvas.drawBitmap(cutout,0,0,paint); paint.setAlpha(255);
                teleport(canvas,vanish);
            }
        }
        return output;
    }
    static String hint(EffectTimeline.Mode mode) {
        return switch(mode) { case NORMAL,PARTICLES -> "faça um V com os dedos"; case NARUTO -> "junte as mãos, dedos estendidos"; case GOKU -> "dois dedos juntos na testa por 1 segundo"; };
    }
    private void capturePerson(Bitmap frame,VisionProcessor vision) {
        mask=vision.mask(frame); personPixels=new int[width*height];
        frame.getPixels(personPixels,0,width,0,0,width,height);
        int left=width,top=height,right=0,bottom=0;
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int i=y*width+x;
            personPixels[i]=(personPixels[i]&0xFFFFFF)|(Math.round(mask[i]*255)<<24);
            if(mask[i]>.5f) { left=Math.min(left,x); right=Math.max(right,x); top=Math.min(top,y); bottom=Math.max(bottom,y); }
        }
        if(right<=left || bottom<=top) bounds.set(width*.25f,height*.2f,width*.75f,height*.9f);
        else bounds.set(left,top,right,bottom);
        cutout=Bitmap.createBitmap(personPixels,width,height,Bitmap.Config.ARGB_8888);
    }
    private void dissolve(Canvas c,float p) {
        int[] pixels=personPixels.clone();
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int i=y*width+x;
            float field=(hash(x/5,y/5)&65535)/65535f;
            float alpha=Math.min(1,Math.max(0,(field-p)*12+1-p));
            int a=Math.round(mask[i]*alpha*255);
            pixels[i]=(pixels[i]&0xFFFFFF)|(a<<24);
        }
        Bitmap part=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
        c.drawBitmap(part,0,0,paint);
        part.recycle();
    }
    private static int hash(int x,int y) { int n=x*374761393+y*668265263; n=(n^(n>>>13))*1274126177; return n^(n>>>16); }
    private void particles(Canvas c,float p) {
        int step=Math.max(5,width/80);
        for(int y=0;y<height;y+=step) for(int x=0;x<width;x+=step) {
            int i=y*width+x;
            if(mask[i]<.2f) continue;
            float seed=(hash(x,y)&65535)/65535f;
            float local=Math.max(0,Math.min(1,(p-seed*.22f)/.78f));
            float dx=local*local*width*(.5f+seed),dy=-local*height*(.10f+seed*.30f);
            paint.setColor(personPixels[i]); paint.setAlpha(Math.round(mask[i]*(1-local)*255));
            float size=step*(1-local*.65f);
            c.drawRect(x+dx,y+dy,x+dx+size,y+dy+size,paint);
        }
        paint.setAlpha(255);
    }
    private void drawLog(Canvas c,float p) {
        float size=Math.min(height*.60f,Math.max(height*.24f,bounds.height()*.58f));
        float w=size*log.getWidth()/log.getHeight();
        float initial=bounds.centerY(),floor=height-size*.48f;
        float fall=Math.min(1,Math.max(0,(p-.12f)*2.5f));
        float cy=initial+(floor-initial)*fall*fall;
        paint.setAlpha(Math.round(255*Math.min(1,p*6)));
        c.drawBitmap(log,null,new RectF(bounds.centerX()-w/2,cy-size/2,bounds.centerX()+w/2,cy+size/2),paint);
        paint.setAlpha(255);
    }
    private void smoke(Canvas c,float p) {
        Random rng=new Random(9127);
        float size=Math.max(height*.2f,bounds.width()*.8f);
        float opacity=Math.min(1,p*12)*Math.max(0,1-p);
        for(int i=0;i<38;i++) {
            float angle=rng.nextFloat()*6.283f,distance=size*(.15f+p*.8f)*rng.nextFloat();
            float x=bounds.centerX()+(float)Math.cos(angle)*distance;
            float y=bounds.centerY()+(float)Math.sin(angle)*distance*.7f-p*size*.2f;
            float r=size*(.08f+rng.nextFloat()*.14f)*(.8f+p);
            int grey=190+rng.nextInt(60);
            paint.setColor(Color.argb(Math.round(200*opacity),grey,grey,Math.min(255,grey+4)));
            c.drawOval(x-r,y-r*.7f,x+r,y+r*.7f,paint);
        }
        paint.setAlpha(255);
    }
    private void teleport(Canvas c,float p) {
        Random rng=new Random(731);
        float opacity=(float)Math.sin(p*Math.PI);
        for(int i=0;i<65;i++) {
            float x=bounds.left+rng.nextFloat()*bounds.width();
            float y=bounds.top+rng.nextFloat()*bounds.height()-p*height*.15f;
            paint.setColor(i%3==0?Color.WHITE:Color.rgb(175,145,255));
            paint.setAlpha(Math.round(220*opacity)); paint.setStrokeWidth(1+rng.nextFloat()*3);
            c.drawLine(x,y,x,y+height*(.08f+rng.nextFloat()*.25f),paint);
        }
        paint.setAlpha(255);
    }
}
