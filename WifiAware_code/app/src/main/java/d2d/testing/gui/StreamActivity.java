package d2d.testing.gui;


import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.verdx.libstreaming.SaveStream;
import net.verdx.libstreaming.StreamingRecord;
import net.verdx.libstreaming.gui.AutoFitTextureView;
import net.verdx.libstreaming.sessions.SessionBuilder;
import net.verdx.libstreaming.video.CameraController;
import net.verdx.libstreaming.video.VideoPacketizerDispatcher;
import net.verdx.libstreaming.video.VideoQuality;

import java.util.UUID;

import d2d.testing.R;
import d2d.testing.gui.main.dialogName.CustomDialogFragment;
import d2d.testing.gui.main.dialogName.CustomDialogListener;


public class StreamActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, CameraController.Callback, CustomDialogListener, View.OnClickListener {

    private final static String TAG = "StreamActivity";

    private AutoFitTextureView mTextureView;

    private SessionBuilder mSessionBuilder;

    private FloatingActionButton recordButton;
    private FloatingActionButton switchButton;
    public boolean mRecording = false;

    private String mNameStreaming = "defaultName";
    private final VideoQuality mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;

    private boolean isDownload;

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
            putAuthorInDefaultName();
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
        switchButton = findViewById(R.id.button_switch_camera);
        recordButton.setOnClickListener(this);
        switchButton.setOnClickListener(this);

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
        Toast.makeText(this,getString(R.string.stream_stop_str), Toast.LENGTH_SHORT).show();
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
        CameraController.getInstance().configureCamera(mTextureView, this);
        CameraController.getInstance().startCamera();
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
        putAuthorInDefaultName();

    }

    private void putAuthorInDefaultName(){
        String author = getIntent().getStringExtra("author");
        author = author.replaceAll("\\s+$", "");
        author = author.replaceAll("\\s+", "_");
        mNameStreaming = mNameStreaming +  "__" + author;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_capture){
            if(!mRecording) {
                startStreaming();
            } else {
                stopStreaming();
            }
        } else if (view.getId() == R.id.button_switch_camera) {
            CameraController.getInstance().switchCamera();
        }
    }
}