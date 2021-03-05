package d2d.testing;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.sessions.Session;
import d2d.testing.streaming.sessions.SessionBuilder;
import d2d.testing.streaming.gl.SurfaceView;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.UUID;


public class StreamActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final static String TAG = "StreamActivity";

    private SurfaceView mSurfaceView;

    public Session mSesion;
    public SessionBuilder mSessionBuilder;

    private FloatingActionButton recordButton;
    public boolean mRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_stream);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSurfaceView = findViewById(R.id.surface);

        // Configures the SessionBuilder
        mSessionBuilder = SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264);
        mSesion = mSessionBuilder.build();

        mSurfaceView.getHolder().addCallback(this);

        recordButton = findViewById(R.id.button_capture);
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
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSesion.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void startStreaming() {
        UUID localStreamUUID = UUID.randomUUID();
        StreamingRecord.getInstance().addLocalStreaming(localStreamUUID, mSessionBuilder);
        /*
        //rtspClient.setSession(mSesion);
        rtspClient.setmSessionBuilder(mSessionBuilder);
        rtspClient.setStreamPath("/Cliente1");
        //rtspClient.setServerAddress("192.168.49.1", 12345);
        rtspClient.startStream();
        Toast.makeText(this,"Retransmitting streaming to server for multihopping", Toast.LENGTH_SHORT).show();
        */
        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop));
        mRecording = true;
    }

    private void stopStreaming() {
        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.videocam));
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