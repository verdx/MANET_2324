package d2d.testing.gui;

import android.content.pm.ActivityInfo;

import android.os.Bundle;

import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

import d2d.testing.R;


public class ViewStreamActivity extends AppCompatActivity implements IVLCVout.Callback,MediaPlayer.EventListener {
    public final static String TAG = "VideoActivity";

    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private MediaController mMediaController = null;
    private final MediaController.MediaPlayerControl mPlayerInterface = new MediaController.MediaPlayerControl() {
        @Override
        public void start() {mMediaPlayer.play();}
        @Override
        public void pause() {mMediaPlayer.pause();}
        @Override
        public int getDuration() {return (int) mMediaPlayer.getLength();}
        @Override
        public int getCurrentPosition() {return (int) (mMediaPlayer.getPosition()*getDuration());}
        @Override
        public void seekTo(int pos) {mMediaPlayer.setPosition((float)pos / getDuration());}
        @Override
        public boolean isPlaying() {return mMediaPlayer.isPlaying();}
        @Override
        public int getBufferPercentage() {return 0;}
        @Override
        public boolean canPause() {return true;}
        @Override
        public boolean canSeekBackward() {return true;}
        @Override
        public boolean canSeekForward() {return true;}
        @Override
        public int getAudioSessionId() {return 0;}
    };

    private ProgressBar bufferSpinner;

    private String rtspUrl;
    private String videoFilePath;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.setContentView(R.layout.activity_view_stream);

        String uuid;
        boolean isFromGallery = getIntent().getExtras().getBoolean("isFromGallery");

        if(isFromGallery){
            videoFilePath = getIntent().getExtras().getString("path");
            mMediaController = new MediaController(this);
            mMediaController.setMediaPlayer(mPlayerInterface);
            mMediaController.setAnchorView(findViewById(R.id.video_layout));
            findViewById(R.id.video_layout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMediaController.show(1500);
                }
            });
        }
        else {
            uuid = getIntent().getExtras().getString("UUID");
            // Get URL
            rtspUrl = "rtsp://127.0.0.1:1234/" + uuid;
            Log.d(TAG, "Playing back " + rtspUrl);
        }

        // display surface
        SurfaceView mSurface = (SurfaceView) findViewById(R.id.textureView);
        holder = mSurface.getHolder();

        ArrayList<String> options = new ArrayList<String>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch"); // time stretching
        options.add("-vvv"); // verbosity
        options.add("--aout=opensles");
        options.add("--avcodec-codec=h264");
        options.add("--file-logging");
        options.add("--logfile=vlc-log.txt");
        //options.add("--video-filter=rotate {angle=270}");

        bufferSpinner = findViewById(R.id.bufferSpinner);

        libvlc = new LibVLC(getApplicationContext(), options);
        holder.setKeepScreenOn(true);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(this);

        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int mHeight = displayMetrics.heightPixels + 70;
        int mWidth = displayMetrics.widthPixels;

        vout.setWindowSize(mWidth,mHeight);
        vout.addCallback(this);
        vout.attachViews();

        Media m;
        if(isFromGallery) m = new Media(libvlc, videoFilePath);
        else m = new Media(libvlc, Uri.parse(rtspUrl));

        mMediaPlayer.setMedia(m);
        mMediaPlayer.play();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch(event.type) {
            case MediaPlayer.Event.EndReached:
                Log.e(TAG, "MediaPlayerEndReached");
                releasePlayer();
                Toast.makeText(getApplicationContext(), "Streaming finished", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case MediaPlayer.Event.Buffering:
                break;
            case MediaPlayer.Event.Playing:
                bufferSpinner.setVisibility(View.INVISIBLE);
                break;
            case MediaPlayer.Event.Paused:
            case MediaPlayer.Event.Stopped:
                Log.e(TAG, "EL STREAMING HA PARADO");
            default:
                break;
        }
    }
}
