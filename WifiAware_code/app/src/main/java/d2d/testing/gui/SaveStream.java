package d2d.testing.gui;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import d2d.testing.utils.IOUtils;

public class SaveStream implements MediaPlayer.EventListener {
    public final static String TAG = "SaveStream";

    Context context;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer;

    private String rtspUrl;
    private String uuid;

    public SaveStream(Context context, String uuid){
        this.context = context;
        this.uuid = uuid;
    }

    public void startDownload(){

        String pathSave = IOUtils.createVideoFilePath(context);
        File file = new File(pathSave);

        rtspUrl = "rtsp://127.0.0.1:1234/" + uuid;
        Log.d(TAG, "Save video " + rtspUrl);

        ArrayList<String> options = new ArrayList<String>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch"); // time stretching
        options.add("-vvv"); // verbosity
        options.add("--aout=opensles");
        options.add("--avcodec-codec=h264");
        options.add("--file-logging");
        options.add("--logfile=vlc-log.txt");

        libvlc = new LibVLC(context, options);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(this);

        Media m = new Media(libvlc, Uri.parse(rtspUrl));

        m.addOption(":sout=#transcode{vcodec=h264,acodec=mp4v,ab=128}");
        m.addOption(":sout=#file{dst=" + file.getPath() + "}");
        m.addOption(":sout-keep");

        mMediaPlayer.setMedia(m);
        mMediaPlayer.play();

    }

    public void stopDownload(){
        mMediaPlayer.stop();
        libvlc.release();
        libvlc = null;
    }



    @Override
    public void onEvent(MediaPlayer.Event event) {

        switch(event.type) {
            case MediaPlayer.Event.EndReached:
                Log.e(TAG, "EL STREAMING EndReached");
                stopDownload();
                break;
            case MediaPlayer.Event.Buffering:
                Log.e(TAG, "EL STREAMING Buffering"); break;
            case MediaPlayer.Event.Playing:
                Log.e(TAG, "EL STREAMING Playing"); break;
            case MediaPlayer.Event.Paused:
                Log.e(TAG, "EL STREAMING Paused"); break;
            case MediaPlayer.Event.Stopped:
                Log.e(TAG, "EL STREAMING Stopped"); break;
            default:
                break;
        }

    }
}



