package d2d.testing.gui;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


import d2d.testing.BuildConfig;
import d2d.testing.gui.main.ProofManager;
import d2d.testing.gui.main.dialogName.CustomDialogFragment;
import d2d.testing.gui.main.dialogName.CustomDialogListener;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.sessions.SessionBuilder;
import d2d.testing.streaming.video.CameraController;
import d2d.testing.streaming.video.VideoPacketizerDispatcher;
import d2d.testing.streaming.video.VideoQuality;

import d2d.testing.R;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.witness.proofmode.ProofMode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class StreamActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, CameraController.Callback, CustomDialogListener {

    private final static String TAG = "StreamActivity";

    private AutoFitTextureView mTextureView;

    private SessionBuilder mSessionBuilder;

    private FloatingActionButton recordButton;
    public boolean mRecording = false;

    private String mNameStreaming = "defaultName";
    private VideoQuality mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;

    private boolean isDownload;
    CameraController ctrl;

    private SaveStream saveStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(MainActivity.mode.equals(getString(R.string.mode_humanitarian))){
            CustomDialogFragment dialog = new CustomDialogFragment();
            dialog.show(getSupportFragmentManager(), "CustomDialogFragment");
        }
        else {
            putAutorInDefaultName();
        }

        mTextureView = findViewById(R.id.textureView);

        // Configures the SessionBuilder
        mSessionBuilder = SessionBuilder.getInstance()
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(mVideoQuality);

        mTextureView.setSurfaceTextureListener(this);
        //mSurfaceView.getHolder().addCallback(this);

        recordButton = findViewById(R.id.button_capture);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mRecording) {
                    setProofMode();

                    startStreaming();
                } else {
                    stopStreaming();
                }
            }
        });
    }

    private void setProofMode(){
        boolean proofDeviceIds = true;
        boolean proofLocation = true;
        boolean proofNetwork = true;
        boolean proofNotary = false;

//        ProofMode.setProofPoints(this, proofDeviceIds, proofLocation, proofNetwork, proofNotary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("trackDeviceId", proofDeviceIds);
        editor.putBoolean("trackLocation", proofLocation);
        editor.putBoolean("autoNotarize", proofNotary);
        editor.putBoolean("trackMobileNetwork", proofNetwork);
        editor.apply();

        int resourceId = R.raw.test;
        String uriString = "android.resource://" + getPackageName() + "/" + resourceId;
        Uri uri = Uri.parse(uriString);

        //generate proof for a URI
        String proofHash = ProofMode.generateProof(StreamActivity.this,uri);

        //get the folder that proof is stored
        File proofDir = ProofMode.getProofDir(StreamActivity.this, proofHash);

//        shareProof(proofDir);

        try {
            File res = makeProofZip(proofDir);

            ProofManager.getInstance().setProofZipFile(proofHash, res);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void shareProof(File proofDir){
        try {
            File res = makeProofZip(proofDir);
            try {
                if (res.exists()) {
                    Uri auxUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", res);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_STREAM, auxUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
                    startActivity(Intent.createChooser(intent, "Share via"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "setProofMode: ", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File makeProofZip(File proofDirPath) throws IOException {
        File outputZipFile = new File(getFilesDir(), proofDirPath.getName() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputZipFile)))) {
            Files.walkFileTree(proofDirPath.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String zipFileName = file.toAbsolutePath().toString().substring(proofDirPath.getAbsolutePath().length() + 1);
                    ZipEntry entry = new ZipEntry(zipFileName + (file.toFile().isDirectory() ? "/" : ""));
                    zos.putNextEntry(entry);
                    if (file.toFile().isFile()) {
                        Files.copy(file, zos);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            ZipEntry keyEntry = new ZipEntry("pubkey.asc");
            zos.putNextEntry(keyEntry);
//            String publicKey = ProofMode.getPublicKeyString(this);
//            zos.write(publicKey.getBytes());
        }

        return outputZipFile;
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public void startStreaming() {
        final UUID localStreamUUID = UUID.randomUUID();
        StreamingRecord.getInstance().addLocalStreaming(localStreamUUID, mNameStreaming, mSessionBuilder);

        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop));
        mRecording = true;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        isDownload = preferences.getBoolean("saveMyStreaming", false);
        if(isDownload) {
            saveStream = new SaveStream(getApplicationContext(), localStreamUUID.toString());
            saveStream.startDownload();
        }
    }

    private void stopStreaming() {
        StreamingRecord.getInstance().removeLocalStreaming();
        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.videocam));
        mRecording = false;
        if(isDownload) saveStream.stopDownload();
        Toast.makeText(this,"Stopped retransmitting the streaming", Toast.LENGTH_SHORT).show();
    }

    public void onDestroy(){
        if(mRecording) {
            stopStreaming();
        }
        VideoPacketizerDispatcher.stop();
        CameraController.getInstance().stopCamera();

        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        ctrl = CameraController.getInstance();
        List<Surface> surfaces = new ArrayList<>();
        String cameraId = ctrl.getCameraIdList()[0];
        Size[] resolutions = ctrl.getPrivType_2Target_MaxResolutions(cameraId, SurfaceTexture.class, MediaCodec.class);

        mTextureView.setAspectRatio(resolutions[0].getWidth(), resolutions[0].getHeight());
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(resolutions[0].getWidth(), resolutions[0].getHeight());
        Surface surfaceT = new Surface(surfaceTexture);
        surfaces.add(surfaceT);

        try {
            VideoPacketizerDispatcher.start(PreferenceManager.getDefaultSharedPreferences(this), mVideoQuality);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "No se pudo iniciar la grabacion", Toast.LENGTH_LONG).show();
        }
        surfaces.add(VideoPacketizerDispatcher.getEncoderInputSurface());

        ctrl.startCamera(cameraId, surfaces);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    public void cameraStarted() {

    }

    @Override
    public void cameraError(int error) {
        Toast.makeText(this, "Camera error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void cameraError(Exception ex) {
        Toast.makeText(this, "Camera error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void cameraClosed() {
        //Toast.makeText(this, "Camera closed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAction(Object object) {

    }

    @Override
    public void onDialogPositive(Object object) {
        String name = (String)object;
        String author = getIntent().getStringExtra("author");

        name = name.replaceAll("\\s+$", "");
        name = name.replaceAll("\\s+", "_");

        author = author.replaceAll("\\s+$", "");
        author = author.replaceAll("\\s+", "_");

        mNameStreaming = name + "__" + author;
        //Toast.makeText(getApplicationContext(), mNameStreaming, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onDialogNegative(Object object) {
        putAutorInDefaultName();

    }

    private void putAutorInDefaultName(){
        String author = getIntent().getStringExtra("author");
        author = author.replaceAll("\\s+$", "");
        author = author.replaceAll("\\s+", "_");
        mNameStreaming = mNameStreaming +  "__" + author;
    }
}