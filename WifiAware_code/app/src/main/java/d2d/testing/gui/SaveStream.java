package d2d.testing.gui;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class SaveStream implements MediaPlayer.EventListener {
    public final static String TAG = "SaveStream";

    Context context;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer;

    private String rtspUrl;

    public SaveStream(Context context, String uuid){
        this.context = context;

        String pathSave = createVideoFilePath();
        File file = new File(pathSave);

        rtspUrl = "rtsp://127.0.0.1:1234/" + uuid;
        Log.d(TAG, "Save video " + rtspUrl);

        ArrayList<String> options = new ArrayList<String>();
        //options.add("--aout=opensles");
        //options.add("--audio-time-stretch"); // time stretching
        //options.add("-vvv"); // verbosity
        //options.add("--aout=opensles");
        //options.add("--avcodec-codec=h264");
        //options.add("--file-logging");
        //options.add("--logfile=vlc-log.txt");
        //options.add("--sout=#file{dst=" + file.getAbsolutePath() + "}");

        libvlc = new LibVLC(context, options);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(this);

        Media m = new Media(libvlc, Uri.parse(rtspUrl));
        //m.addOption(":sout=#file{dst=" + file.getAbsolutePath() + "}");
        //m.addOption(":sout-keep");

        m.addOption(":sout=#transcode{vcodec=h264,acodec=mp4a,ab=160}"
                  + ":file{dst="+ file.getPath() +"}");

        mMediaPlayer.setMedia(m);
        mMediaPlayer.play();
    }

    private String createVideoFilePath(){
        String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String filename = sdf.format(cal.getTime());
        filename = filename.replaceAll(" ", "_");
        filename = filename.replaceAll(":", "");
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + filename + ".mp4";
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {

    }
}



