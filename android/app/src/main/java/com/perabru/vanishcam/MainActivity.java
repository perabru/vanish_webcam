package com.perabru.vanishcam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends ComponentActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean pendingFrame=new AtomicBoolean();
    private VisionProcessor vision;
    private EffectRenderer effects;
    private ProcessCameraProvider provider;
    private ImageView preview;
    private TextView status;
    private Button trigger,capture,calibrate,camera,permission;
    private volatile boolean active,modelsReady,gestures=true;
    private volatile int generation;
    private boolean front=true,sound=true;
    private Bitmap displayed;
    private boolean preparingPhoto;
    private ToneGenerator tone;
    private long lastFrame;
    private int failedFrames;
    private final int bg=Color.rgb(11,11,19),panel=Color.rgb(25,23,37),accent=Color.rgb(167,139,250);

    private final ActivityResultLauncher<String> permissionRequest=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),granted->{
            permission.setVisibility(granted?View.GONE:View.VISIBLE);
            if(granted) startCamera();
            else status.setText("Permita o acesso à câmera para usar os efeitos. Toque em Permitir câmera para tentar novamente ou abrir as configurações.");
        });
    private final ActivityResultLauncher<String> saveImage=registerForActivityResult(
        new ActivityResultContracts.CreateDocument("image/png"),uri->{
            java.io.File photo=new java.io.File(getCacheDir(),"pending-photo.png");
            if(uri==null) { photo.delete(); return; }
            worker.execute(()->{
                boolean ok=false;
                try(var in=new java.io.FileInputStream(photo);var out=getContentResolver().openOutputStream(uri)) {
                    if(out==null)throw new java.io.IOException("Destino indisponível");
                    byte[] bytes=new byte[16384];int count;
                    while((count=in.read(bytes))!=-1)out.write(bytes,0,count);
                    ok=true;
                } catch(Exception e) { android.util.Log.e("VanishCam","Salvar foto",e); }
                finally { photo.delete(); }
                boolean success=ok;
                runOnUiThread(()->Toast.makeText(this,success?"Foto salva!":"Não foi possível salvar a foto. Tente outro destino.",Toast.LENGTH_LONG).show());
            });
        });

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        front=getPreferences(0).getBoolean("front",true);
        gestures=getPreferences(0).getBoolean("gestures",true);
        sound=getPreferences(0).getBoolean("sound",true);
        try { tone=new ToneGenerator(AudioManager.STREAM_MUSIC,45); } catch(RuntimeException ignored) { sound=false; }
        buildUi();
        status.setText("Preparando os modelos de visão…");
        worker.execute(()->{
            try {
                vision=new VisionProcessor(getApplicationContext());
                effects=new EffectRenderer(getApplicationContext());
                effects.timeline.mode=EffectTimeline.Mode.values()[Math.max(0,Math.min(3,getPreferences(0).getInt("mode",0)))];
                modelsReady=true;
                runOnUiThread(()->{
                    if(isDestroyed()) return;
                    status.setText("Apoie o celular. Capture o fundo vazio antes de entrar na imagem.");
                    startCamera();
                });
            } catch(Exception e) {
                android.util.Log.e("VanishCam","Inicialização",e);
                runOnUiThread(()->status.setText("Não foi possível carregar os modelos. Feche e abra o app; se persistir, reinstale o APK completo."));
            }
        });
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            permissionRequest.launch(Manifest.permission.CAMERA);
    }
    private int dp(float v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private GradientDrawable rounded(int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private Button button(String label,boolean primary) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(primary?bg:Color.WHITE);b.setBackground(rounded(primary?accent:panel,14));
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(12),dp(6),dp(12),dp(6));
        return b;
    }
    private void addFull(LinearLayout parent,View view,int height) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));lp.topMargin=dp(8);parent.addView(view,lp);
    }
    private void buildUi() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        root.setPadding(dp(16),dp(10),dp(16),dp(10));setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            int l,t,r,b;
            if(android.os.Build.VERSION.SDK_INT>=30) { var i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=i.left;t=i.top;r=i.right;b=i.bottom; }
            else { l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom(); }
            root.setPadding(dp(16)+l,dp(8)+t,dp(16)+r,dp(8)+b);return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("VANISH / CAM",24,Color.WHITE);title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        Button help=button("Como usar",false);header.addView(help);help.setOnClickListener(v->help());
        root.addView(header);
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout content=new LinearLayout(this);content.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setBackground(rounded(Color.BLACK,18));
        preview.setContentDescription("Imagem da câmera com o efeito selecionado");preview.setClipToOutline(true);
        content.addView(preview,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        LinearLayout.LayoutParams sp=landscape?new LinearLayout.LayoutParams(dp(310),-1):new LinearLayout.LayoutParams(-1,-2);
        if(landscape)sp.leftMargin=dp(12);content.addView(scroll,sp);
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);scroll.addView(controls);
        status=text("Preparando…",13,Color.rgb(206,198,225));status.setMinHeight(dp(42));status.setGravity(Gravity.CENTER_VERTICAL);addFull(controls,status,-2);
        permission=button("Permitir câmera",true);addFull(controls,permission,48);
        permission.setVisibility(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED?View.GONE:View.VISIBLE);
        permission.setOnClickListener(v->{
            if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();
            else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))permissionRequest.launch(Manifest.permission.CAMERA);
            else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));
        });
        HorizontalScrollView modesScroll=new HorizontalScrollView(this);modesScroll.setHorizontalScrollBarEnabled(false);
        RadioGroup modes=new RadioGroup(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={"Dissolver","Naruto","Partículas","Goku"};
        int selected=getPreferences(0).getInt("mode",0);
        for(int i=0;i<labels.length;i++) {
            RadioButton b=new RadioButton(this);b.setId(100+i);b.setText(labels[i]);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setMinHeight(dp(48));modes.addView(b);b.setChecked(i==selected);
        }
        modesScroll.addView(modes);addFull(controls,modesScroll,52);
        modes.setOnCheckedChangeListener((group,id)->{
            int index=id-100;getPreferences(0).edit().putInt("mode",index).apply();
            worker.execute(()->{if(effects!=null)effects.mode(EffectTimeline.Mode.values()[index]);});
        });
        calibrate=button("1  Capturar fundo • 4 s",false);addFull(controls,calibrate,48);calibrate.setEnabled(false);
        calibrate.setOnClickListener(v->worker.execute(()->{if(effects!=null)effects.calibrate(SystemClock.uptimeMillis());}));
        trigger=button("2  Desaparecer",true);addFull(controls,trigger,52);trigger.setEnabled(false);
        trigger.setOnClickListener(v->worker.execute(()->{if(effects!=null)effects.requestTrigger();}));
        LinearLayout actions=new LinearLayout(this);
        camera=button("Trocar câmera",false);capture=button("Salvar foto",false);capture.setEnabled(false);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(48),1);left.rightMargin=dp(6);
        actions.addView(camera,left);actions.addView(capture,new LinearLayout.LayoutParams(0,dp(48),1));addFull(controls,actions,48);
        camera.setOnClickListener(v->{front=!front;getPreferences(0).edit().putBoolean("front",front).apply();startCamera();});
        capture.setOnClickListener(v->{
            if(displayed==null || preparingPhoto)return;
            preparingPhoto=true;
            Bitmap snapshot=displayed.copy(Bitmap.Config.ARGB_8888,false);
            worker.execute(()->{
                boolean ok=false;
                try(var out=new java.io.FileOutputStream(new java.io.File(getCacheDir(),"pending-photo.png"))) {
                    ok=snapshot.compress(Bitmap.CompressFormat.PNG,100,out);
                } catch(Exception e) { android.util.Log.e("VanishCam","Preparar foto",e); }
                finally { snapshot.recycle(); }
                boolean success=ok;
                runOnUiThread(()->{
                    preparingPhoto=false;
                    if(isDestroyed())return;
                    if(success)saveImage.launch("VanishCam-"+System.currentTimeMillis()+".png");
                    else Toast.makeText(this,"Não foi possível preparar a foto.",Toast.LENGTH_LONG).show();
                });
            });
        });
        LinearLayout toggles=new LinearLayout(this);
        Switch gesturesSwitch=new Switch(this);gesturesSwitch.setText("Gestos  ");gesturesSwitch.setTextColor(Color.WHITE);gesturesSwitch.setChecked(gestures);
        Switch soundSwitch=new Switch(this);soundSwitch.setText("Som  ");soundSwitch.setTextColor(Color.WHITE);soundSwitch.setChecked(sound);
        toggles.addView(gesturesSwitch,new LinearLayout.LayoutParams(0,dp(48),1));toggles.addView(soundSwitch,new LinearLayout.LayoutParams(0,dp(48),1));addFull(controls,toggles,48);
        gesturesSwitch.setOnCheckedChangeListener((b,on)->{gestures=on;getPreferences(0).edit().putBoolean("gestures",on).apply();});
        soundSwitch.setOnCheckedChangeListener((b,on)->{sound=on;getPreferences(0).edit().putBoolean("sound",on).apply();});
        TextView privacy=text("Processamento local • nenhuma imagem enviada",11,Color.rgb(150,143,168));privacy.setGravity(Gravity.CENTER);addFull(controls,privacy,22);
    }
    private void help() {
        new android.app.AlertDialog.Builder(this).setTitle("Seu efeito, em quatro passos")
            .setMessage("1. Apoie o celular em uma superfície firme, com boa iluminação.\n\n2. Toque em Capturar fundo e saia da imagem durante os 4 segundos. Depois volte à mesma posição.\n\n3. Escolha Dissolver, Naruto, Partículas ou Goku. Use o botão Desaparecer ou os gestos indicados. No Goku, abaixe a mão e repita o gesto para reaparecer.\n\n4. Toque em Salvar foto para escolher onde guardar a imagem com o efeito.\n\nNão mova o celular após capturar o fundo. Ao trocar de câmera, girar o aparelho ou voltar ao app, capture novamente.\n\nA animação utiliza um recorte da pessoa no momento do acionamento. Objetos e sombras podem deixar resíduos. A precisão dos gestos depende da luz e do enquadramento.\n\nEsta versão gera efeitos dentro do app e salva fotos. Não substitui a câmera de WhatsApp, Meet ou outros apps, nem grava vídeos.")
            .setPositiveButton("Entendi",null).show();
    }
    private void startCamera() {
        if(!active || !modelsReady || checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        permission.setVisibility(View.GONE);trigger.setEnabled(false);capture.setEnabled(false);calibrate.setEnabled(false);
        int token=++generation;
        worker.execute(()->{if(effects!=null)effects.clear();});
        var future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            if(!active || token!=generation)return;
            try {
                provider=future.get();provider.unbindAll();
                CameraSelector selector=front?CameraSelector.DEFAULT_FRONT_CAMERA:CameraSelector.DEFAULT_BACK_CAMERA;
                if(!provider.hasCamera(selector)) {
                    front=!front;selector=front?CameraSelector.DEFAULT_FRONT_CAMERA:CameraSelector.DEFAULT_BACK_CAMERA;
                    if(!provider.hasCamera(selector))throw new IllegalStateException("Nenhuma câmera disponível");
                }
                final boolean mirrored=front;
                ImageAnalysis analysis=new ImageAnalysis.Builder().setTargetResolution(new Size(640,480))
                    .setTargetRotation(preview.getDisplay().getRotation())
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(worker,image->analyze(image,mirrored,token));
                provider.bindToLifecycle(this,selector,analysis);
                status.setText("Apoie o celular e capture o fundo vazio.");
            } catch(Exception e) {
                android.util.Log.e("VanishCam","Câmera",e);
                status.setText("Não foi possível abrir a câmera. Feche outros apps que a utilizam e toque em Trocar câmera para tentar novamente.");
            }
        },ContextCompat.getMainExecutor(this));
    }
    private void analyze(ImageProxy image,boolean mirrored,int token) {
        long now=SystemClock.uptimeMillis();
        if(!active || token!=generation || now-lastFrame<55 || !pendingFrame.compareAndSet(false,true)) { image.close();return; }
        lastFrame=now;
        Bitmap frame=null;
        try {
            Bitmap raw=image.toBitmap();
            Matrix matrix=new Matrix();matrix.postRotate(image.getImageInfo().getRotationDegrees());
            if(mirrored)matrix.postScale(-1,1);
            Bitmap oriented=Bitmap.createBitmap(raw,0,0,raw.getWidth(),raw.getHeight(),matrix,true);
            float scale=Math.min(1f,640f/Math.max(oriented.getWidth(),oriented.getHeight()));
            frame=Bitmap.createScaledBitmap(oriented,Math.max(1,Math.round(oriented.getWidth()*scale)),Math.max(1,Math.round(oriented.getHeight()*scale)),true);
            if(raw!=oriented && raw!=frame)raw.recycle();if(oriented!=frame)oriented.recycle();
            Bitmap result=effects.render(frame,vision,now,gestures);
            String message=!gestures && effects.ready() && effects.timeline.phase==EffectTimeline.Phase.READY
                ?"Pronto • toque em Desaparecer":effects.status;
            boolean canTrigger=effects.ready()&&!effects.timeline.animating();
            boolean hidden=effects.timeline.phase==EffectTimeline.Phase.HIDDEN;
            boolean play=effects.soundRequested;failedFrames=0;
            runOnUiThread(()->{
                try {
                    if(!active || token!=generation || isDestroyed()) {result.recycle();return;}
                    displayed=result;preview.setImageBitmap(result);
                    status.setText(message);trigger.setEnabled(canTrigger);trigger.setAlpha(canTrigger?1f:.45f);trigger.setText(hidden?"2  Reaparecer":"2  Desaparecer");
                    capture.setEnabled(true);calibrate.setEnabled(true);
                    if(play && sound && tone!=null)tone.startTone(ToneGenerator.TONE_PROP_ACK,150);
                } finally {pendingFrame.set(false);}
            });
        } catch(Exception e) {
            android.util.Log.e("VanishCam","Processamento",e);pendingFrame.set(false);failedFrames++;
            if(failedFrames>=3) {
                failedFrames=0;effects.clear();
                runOnUiThread(()->{if(token==generation){status.setText("Falha no processamento. Capture o fundo novamente; tente desativar Gestos se persistir.");trigger.setEnabled(false);}});
            }
        } finally {if(frame!=null)frame.recycle();image.close();}
    }
    @Override protected void onResume() {super.onResume();active=true;startCamera();}
    @Override protected void onPause() {
        active=false;generation++;
        if(provider!=null)provider.unbindAll();
        if(effects!=null)worker.execute(()->effects.clear());
        super.onPause();
    }
    @Override protected void onDestroy() {
        worker.execute(()->{if(vision!=null)vision.close();});worker.shutdown();
        if(tone!=null)tone.release();
        super.onDestroy();
    }
}
