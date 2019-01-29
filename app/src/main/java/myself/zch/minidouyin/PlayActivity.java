package myself.zch.minidouyin;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import myself.zch.minidouyin.Database.FavoriteContract;
import myself.zch.minidouyin.Database.FavoriteDbHelper;

public class PlayActivity extends AppCompatActivity implements OnSeekBarChangeListener, OnCompletionListener {
    private static final String TAG = "DEBUG_PLAY_ACTIVITY";

    private boolean isStopUpdatingProgress = false;
    private String etPath;
    private MediaPlayer mMediapPlayer = null;
    private SeekBar mSeekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView author_id;
    private TextView author_name;
    private SurfaceView mSurfaceView;
    private ProgressBar mProcessBar;
    private FavoriteDbHelper mDbHelper;
    private SQLiteDatabase db;
    private LottieAnimationView favorite;
    private TextView process_notice;
    private TextView loading;

    /**
     * 闲置
     */
    private final int NORMAL = 0;
    /**
     * 播放中
     */
    private final int PLAYING = 1;

    /**
     * 播放器当前的状态，默认是空闲状态
     */
    private int currentState = NORMAL;
    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        favorite = findViewById(R.id.favorite);
        favorite.setVisibility(View.INVISIBLE);

        //从另一个活动拿来
        etPath = getIntent().getStringExtra("VIDEO_URL");
        author_id = (TextView) findViewById(R.id.author_id);
        author_name = (TextView) findViewById(R.id.author_name);
        author_name.setText(getIntent().getStringExtra("USER_NAME"));
        author_id.setText(getIntent().getStringExtra("STUDENT_ID"));

        mSeekBar = (SeekBar) findViewById(R.id.sb_progress);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView) findViewById(R.id.tv_total_time);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mSeekBar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }
        mProcessBar = (ProgressBar) findViewById(R.id.progressBar);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        //SurfaceView帮助类对象
        holder = mSurfaceView.getHolder();
        //是采用自己内部的双缓冲区，而是等待别人推送数据
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        initBtns();
    }

    private void initBtns() {
        mSurfaceView.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: CLICK");
            if (mMediapPlayer == null) {
                initPlayer();
            }
            if (mMediapPlayer.isPlaying()) {
                mMediapPlayer.pause();
            } else {
                mMediapPlayer.start();
            }
        });

        mSurfaceView.setOnLongClickListener(v -> {
            initDb();
            Log.d(TAG, "onCreate: LONG CLICK");

            if (db == null) {
                Log.d(TAG, "db=null");
                return true;
            }

            ContentValues values = new ContentValues();
            values.put(FavoriteContract.FavEntry.COLUMN_DATE, System.currentTimeMillis());
            values.put(FavoriteContract.FavEntry.COLUMN_STUDENT_ID, getIntent().getStringExtra("STUDENT_ID"));
            values.put(FavoriteContract.FavEntry.COLUMN_USER_NAME, getIntent().getStringExtra("USER_NAME"));
            values.put(FavoriteContract.FavEntry.COLUMN_IMAGE_URL, getIntent().getStringExtra("IMAGE_URL"));
            values.put(FavoriteContract.FavEntry.COLUMN__VIDEO_URL, getIntent().getStringExtra("VIDEO_URL"));

            long rowId = db.insert(FavoriteContract.FavEntry.TABLE_NAME, null, values);
            if (rowId != -1) {
                favorite.playAnimation();
                Toast.makeText(PlayActivity.this, "收藏成功！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PlayActivity.this, "收藏失败", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void initPlayer() {
        String path = etPath;
        mMediapPlayer = new MediaPlayer();
        try {
            mProcessBar.setVisibility(View.INVISIBLE);
            //设置数据类型
            mMediapPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //设置以下播放器显示的位置
            mMediapPlayer.setDisplay(holder);

            mMediapPlayer.setDataSource(path);
            mMediapPlayer.prepare();
            mMediapPlayer.start();

            mMediapPlayer.setOnCompletionListener(this);
            mMediapPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("MediaPlayer", "what: " + what + " extra: " + extra);
                    return false;
                }
            });
            //把当前播放器的状诚置为：播放中
            currentState = PLAYING;

            //把音乐文件的总长度取出来，设置给seekbar作为最大值
            //总时长
            int duration = mMediapPlayer.getDuration();
            mSeekBar.setMax(duration);
            //把总时间显示textView上
            int m = duration / 1000 / 60;
            int s = duration / 1000 % 60;
            String M = (m < 10) ? "0" + String.valueOf(m) : String.valueOf(m);
            String S = (s < 10) ? "0" + String.valueOf(s) : String.valueOf(s);
            tvTotalTime.setText("/" + M + ":" + S);
            tvCurrentTime.setText("00:00");

            isStopUpdatingProgress = false;
            new Thread(new UpdateProgressRunnable()).start();
            favorite.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initDb() {
        mDbHelper = new FavoriteDbHelper(PlayActivity.this);
        db = mDbHelper.getWritableDatabase();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //当开始拖动时，那么就开始停止刷新线程
        isStopUpdatingProgress = true;
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        //播放器切换到指定的进度位置上
        mMediapPlayer.seekTo(progress);
        isStopUpdatingProgress = false;
        new Thread(new UpdateProgressRunnable()).start();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediapPlayer != null) {
            mMediapPlayer.stop();
            mMediapPlayer.reset();
            try {
                mMediapPlayer.release();
            } catch (Exception e) {
                Log.e("MediaPlayer", e.toString());
            }
            mMediapPlayer = null;
        }
    }

    /**
     * 刷新进度和时间的任务
     */
    class UpdateProgressRunnable implements Runnable {

        @Override
        public void run() {
            //每隔1秒钟取一下当前正在播放的进度，设置给seekbar
            while (!isStopUpdatingProgress && mMediapPlayer != null) {
                //得到当前进度
                int currentPosition = mMediapPlayer.getCurrentPosition();
                mSeekBar.setProgress(currentPosition);
                final int m = currentPosition / 1000 / 60;
                final int s = currentPosition / 1000 % 60;
                String M = (m < 10) ? "0" + String.valueOf(m) : String.valueOf(m);
                String S = (s < 10) ? "0" + String.valueOf(s) : String.valueOf(s);
                //此方法给定的runable对象，会执行主线程（UI线程中）
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCurrentTime.setText(M + ":" + S);
                    }
                });
                SystemClock.sleep(1000);
            }
        }
    }

}