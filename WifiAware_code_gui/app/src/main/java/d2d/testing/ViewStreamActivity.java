package d2d.testing;

import android.content.pm.ActivityInfo;

import android.os.Build;
import android.os.Bundle;

import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class ViewStreamActivity extends AppCompatActivity implements IVLCVout.Callback,MediaPlayer.EventListener {
    public final static String TAG = "VideoActivity";

    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;

    private ProgressBar bufferSpinner;

    private String rtspUrl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.setContentView(R.layout.activity_view_stream);

        String ip = getIntent().getExtras().getString("IP");
        //String ip2 = "["+ ip.substring(0, ip.lastIndexOf("%")) + "%25" + ip.substring(ip.lastIndexOf("%") + 1, ip.lastIndexOf(":")) + "]" + ip.substring(ip.lastIndexOf(":"));
        //String path= "rtsp://" + ip2;

        // Get URL
        rtspUrl = "rtsp://127.0.0.1:1234/" + ip;
        Log.d(TAG, "Playing back " + rtspUrl);

        mSurface = (SurfaceView) findViewById(R.id.surface);
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
        if (Build.VERSION.SDK_INT >= 19) {
            // include navigation bar
            getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        } else {
            // exclude navigation bar
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        }
        int mHeight = displayMetrics.heightPixels;
        int mWidth = displayMetrics.widthPixels;

        vout.setWindowSize(mWidth,mHeight);
        vout.addCallback(this);
        vout.attachViews();

        /*
        Uri.Builder b = new Uri.Builder();
        b.scheme("rtsp");
        b.authority(ip.substring(0, ip.lastIndexOf("/")));
        b.appendPath(ip.substring(ip.lastIndexOf("/")));
        Uri ur2 = b.build();
        String host2 = ur2.getHost();
        int port2 = ur2.getPort();
        */

        Media m = new Media(libvlc, Uri.parse(rtspUrl));
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
        mVideoWidth = 0;
        mVideoHeight = 0;
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

    private String createVideoFilePath(){
        String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String filename = sdf.format(cal.getTime());
        filename = filename.replaceAll(" ", "_");
        filename = filename.replaceAll(":", "-");
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + filename + ".mp4";
    }
}
