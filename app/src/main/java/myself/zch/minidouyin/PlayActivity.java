package myself.zch.minidouyin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import myself.zch.minidouyin.R;
import retrofit2.http.Url;

public class PlayActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private boolean isPlayingVideo = false;
    private TextView textView;
    private TextView currentTime;
    private TextView totalTime;
    private SurfaceHolder holder;
    private MediaPlayer mediaPlayer = null;

    private final int NORMAL = 0;
    private final int PLAYING = 1;
    private final int PAUSING = 2;
    private final int STOPING = 3;

    private int state = NORMAL;

    Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        textView = findViewById(R.id.tv_play);

        SurfaceView surfaceView = findViewById(R.id.sv_play);
        holder = surfaceView.getHolder();
        intent = getIntent();

//        //详情
//        String str = intent.getStringExtra("USER_ID") + "\n"
//                + intent.getStringExtra("USER_NAME") + "\n";
//        String text = new String(new char[20]).replace("\0", str);
//        textView.setText(text);
//        textView.setMovementMethod(ScrollingMovementMethod.getInstance());

        //封面
        String url = intent.getStringExtra("IMAGE_URL");
        //debug
        //url = "http://10.108.10.39:8080/minidouyin/storage/image?path=1548486044060/91d3614ae38d158da044b1029be475b5.png";
    }

    public void start(View view) {
        if (mediaPlayer != null) {
            if (state != PAUSING) {
                mediaPlayer.start();
                state = PLAYING;

                isPlayingVideo = false;
                return;
            } else if (state == STOPING) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        }

        play();
    }

    public void stop(View v) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            state = STOPING;
        }
    }

    public void play() {
        String url = intent.getStringExtra("VIDEO_URL");
        //debug
        //url = "http://10.108.10.39:8080/minidouyin/storage/video?path=1548486044060/7332511a51549683f5585e59321de6c7.mov";
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDisplay(holder);

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(this);

            state = PLAYING;

            int duration = mediaPlayer.getDuration();

            int m = duration / 1000 / 60;
            int s = duration / 1000 % 60;

            totalTime.setText("/" + m + ":" + s);
            currentTime.setText("00:00");

            isPlayingVideo = false;
            new Thread(new UpdateProgressRunnable()).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void pause(View view) {
        if (mediaPlayer != null && state == PLAYING) {
            mediaPlayer.pause();
            state = PAUSING;

            isPlayingVideo = true;
        }
    }

    public void restart(View view) {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            play();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Toast.makeText(this, "Finished,play again", Toast.LENGTH_SHORT).show();
        mediaPlayer.start();
    }

    private class UpdateProgressRunnable implements Runnable {
        @Override
        public void run() {
            while (!isPlayingVideo) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                final int m = currentPosition / 1000 / 60;
                final int s = currentPosition / 1000 % 60;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentTime.setText(m + ":" + s);
                    }
                });
                SystemClock.sleep(1000);
            }
        }
    }
}