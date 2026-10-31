package com.perabru.vanishcam;

import android.content.Context;
import android.graphics.Bitmap;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/** All calls and close must run on the same analysis executor. No network access. */
final class VisionProcessor implements AutoCloseable {
    private HandLandmarker hands;
    private FaceLandmarker faces;
    private ImageSegmenter segmenter;
    VisionProcessor(Context context) {
        try {
            hands=HandLandmarker.createFromOptions(context, HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base("hand_landmarker.task")).setRunningMode(RunningMode.VIDEO).setNumHands(2).build());
            faces=FaceLandmarker.createFromOptions(context, FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(base("face_landmarker.task")).setRunningMode(RunningMode.VIDEO).setNumFaces(1).build());
            segmenter=ImageSegmenter.createFromOptions(context, ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(base("selfie_segmenter.tflite")).setRunningMode(RunningMode.IMAGE)
                .setOutputConfidenceMasks(true).setOutputCategoryMask(false).build());
        } catch(RuntimeException e) { close(); throw e; }
    }
    private static BaseOptions base(String asset) { return BaseOptions.builder().setModelAssetPath(asset).build(); }
    boolean gesture(Bitmap bitmap,EffectTimeline.Mode mode,long now) {
        MPImage image=new BitmapImageBuilder(bitmap).build();
        try {
            var result=hands.detectForVideo(image,now);
            List<float[][]> detected=new ArrayList<>();
            for(List<NormalizedLandmark> list:result.landmarks()) {
                float[][] points=new float[list.size()][2];
                for(int i=0;i<list.size();i++) { points[i][0]=list.get(i).x()*bitmap.getWidth(); points[i][1]=list.get(i).y()*bitmap.getHeight(); }
                detected.add(points);
            }
            if(mode==EffectTimeline.Mode.NARUTO) return GestureRules.joined(detected);
            if(mode!=EffectTimeline.Mode.GOKU) {
                for(float[][] hand:detected) if(GestureRules.victory(hand)) return true;
                return false;
            }
            if(detected.isEmpty()) return false;
            var faceResult=faces.detectForVideo(image,now);
            if(faceResult.faceLandmarks().isEmpty()) return false;
            var f=faceResult.faceLandmarks().get(0);
            float[] forehead={f.get(10).x()*bitmap.getWidth(), f.get(10).y()*bitmap.getHeight()};
            float width=Math.abs(f.get(454).x()-f.get(234).x())*bitmap.getWidth();
            for(float[][] hand:detected) if(GestureRules.forehead(hand,forehead,width)) return true;
            return false;
        } finally { image.close(); }
    }
    float[] mask(Bitmap bitmap) {
        MPImage image=new BitmapImageBuilder(bitmap).build();
        List<MPImage> masks=null;
        try {
            var result=segmenter.segment(image);
            masks=result.confidenceMasks().orElseThrow(()->new IllegalStateException("Máscara indisponível"));
            // Selfie Segmenter has one output: the foreground (person) confidence.
            MPImage mask=masks.get(0);
            FloatBuffer buffer=ByteBufferExtractor.extract(mask).order(ByteOrder.nativeOrder()).asFloatBuffer();
            int mw=mask.getWidth(),mh=mask.getHeight(),w=bitmap.getWidth(),h=bitmap.getHeight();
            float[] small=new float[mw*mh]; buffer.get(small);
            float[] full=new float[w*h];
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                float mx=x*(mw-1f)/Math.max(1,w-1),my=y*(mh-1f)/Math.max(1,h-1);
                int x0=(int)mx,y0=(int)my,x1=Math.min(x0+1,mw-1),y1=Math.min(y0+1,mh-1);
                float tx=mx-x0,ty=my-y0;
                float v=(small[y0*mw+x0]*(1-tx)+small[y0*mw+x1]*tx)*(1-ty)
                    +(small[y1*mw+x0]*(1-tx)+small[y1*mw+x1]*tx)*ty;
                full[y*w+x]=Math.max(0,Math.min(1,(v-0.15f)/0.7f));
            }
            return full;
        } finally { if(masks!=null) for(MPImage m:masks)m.close(); image.close(); }
    }
    @Override public void close() {
        if(hands!=null) { hands.close(); hands=null; }
        if(faces!=null) { faces.close(); faces=null; }
        if(segmenter!=null) { segmenter.close(); segmenter=null; }
    }
}
