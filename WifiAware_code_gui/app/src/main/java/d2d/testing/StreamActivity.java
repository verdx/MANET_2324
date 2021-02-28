package d2d.testing;

import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;


import d2d.testing.wifip2p.WifiAwareViewModel;
import d2d.testing.streaming.sessions.Session;
import d2d.testing.streaming.sessions.SessionBuilder;
import d2d.testing.streaming.gl.SurfaceView;
import d2d.testing.streaming.rtsp.RtspClient;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class StreamActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final static String TAG = "StreamActivity";

    private SurfaceView mSurfaceView;

    private RtspClient rtspClient;

    public Session mSesion;

    private Button recordButton;
    public boolean mRecording = false;

    private WifiAwareViewModel mAwareModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_stream);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSurfaceView = findViewById(R.id.surface);

        // Configures the SessionBuilder
        mSesion = SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .build();

        mSurfaceView.getHolder().addCallback(this);

        recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mRecording) {
                    startStreaming();
                } else {
                    stopStreaming();
                }
            }
        });

        mAwareModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(WifiAwareViewModel.class);

    }

    public WifiAwareViewModel getWifiAwareModel(){
        return mAwareModel;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSesion.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    public void startStreaming() {
        rtspClient = new RtspClient(mAwareModel, this);
        rtspClient.setSession(mSesion);
        rtspClient.setStreamPath("/Cliente1");
        //rtspClient.setServerAddress("192.168.49.1", 12345);
        rtspClient.startStream();
        Toast.makeText(this,"Retransmitting streaming to server for multihopping", Toast.LENGTH_SHORT).show();

        mRecording = true;
    }

    private void stopStreaming() {
        rtspClient.stopStream();

        mRecording = false;
        Toast.makeText(this,"Stopped retransmitting the streaming", Toast.LENGTH_SHORT).show();
    }

    public void onDestroy(){
        if(mRecording) {
            stopStreaming();
        }

        super.onDestroy();
        //mSesion.stop();
        //this.stopService(mIntent);
        mSesion.stopPreview();
    }
}
