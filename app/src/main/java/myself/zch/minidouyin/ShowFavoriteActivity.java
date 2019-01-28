package myself.zch.minidouyin;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import myself.zch.minidouyin.Database.FavoriteContract;
import myself.zch.minidouyin.Database.FavoriteDbHelper;
import myself.zch.minidouyin.JavaBeans.Entry;
import myself.zch.minidouyin.JavaBeans.FeedResponse;
import myself.zch.minidouyin.JavaBeans.PostVideoResponse;
import myself.zch.minidouyin.Utils.ResourceUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ShowFavoriteActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG_FAVORITE_ACTIVITY";
    private static final int VIDEO_REQUESTS = 1;
    private static final int UPLOAD_REQUEST = 100;
    private static final int PICK_IMAGE = 2;
    private static final int PICK_VIDEO = 3;

    private FavoriteDbHelper mDbHelper;
    private SQLiteDatabase db;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter entryAdapter;
    private List<Entry> mEntry;
    private Uri mSelectedImage;
    private Uri mSelectedVideo;
    private String studentId = null;
    private String userName = null;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_favorite);
        mDbHelper = new FavoriteDbHelper(this);
        db = mDbHelper.getWritableDatabase();
        initRecyclerView();
        if (entryAdapter.getItemCount() == 0) {
            findViewById(R.id.no_content).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.no_content).setVisibility(View.INVISIBLE);
        }
        initBtns();
    }

    private void initBtns() {
        //首页
        findViewById(R.id.btn_homepage_fav).setOnClickListener(v -> {
            Intent intent = new Intent(ShowFavoriteActivity.this, MainActivity.class);
            startActivity(intent);
        });

        //录制
        findViewById(R.id.btn_record_fav).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(ShowFavoriteActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ShowFavoriteActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ShowFavoriteActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO},
                        VIDEO_REQUESTS);
            } else {
                Intent intent = new Intent(ShowFavoriteActivity.this, RecordActivity.class);
                startActivity(intent);
            }
        });

        //发布
        findViewById(R.id.btn_post_fav).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(ShowFavoriteActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        UPLOAD_REQUEST);
            } else {
                upLoad();
            }
        });
    }

    private void initRecyclerView() {
        entryAdapter = new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                ImageView imageView = new ImageView(viewGroup.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setAdjustViewBounds(true);
                return new ShowFavoriteActivity.MyViewHolder(imageView);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                ImageView iv = (ImageView) viewHolder.itemView;

                //Glide加载图片
                String url = mEntry.get(i).getImageUrl();
                Glide.with(iv.getContext()).load(url).into(iv);

                //单击进入详情页面播放
                viewHolder.itemView.setOnClickListener((v -> {
                    Intent intent = new Intent(ShowFavoriteActivity.this, PlayActivity.class);
                    intent.putExtra("VIDEO_URL", mEntry.get(i).getVideoUrl());
                    intent.putExtra("IMAGE_URL", mEntry.get(i).getImageUrl());
                    intent.putExtra("STUDENT_ID", mEntry.get(i).getStudentId());
                    intent.putExtra("USER_NAME", mEntry.get(i).getUserName());

                    startActivity(intent);
                }));

                //长按取消收藏
                viewHolder.itemView.setOnLongClickListener(v -> {
                    Log.d(TAG, "onBindViewHolder: LONG CLICK");
                    if (db == null) {
                        Log.d(TAG, "onBindViewHolder: db=null");
                        return true;
                    }

                    int rows = db.delete(FavoriteContract.FavEntry.TABLE_NAME,
                            FavoriteContract.FavEntry._ID + " =?",
                            new String[]{String.valueOf(mEntry.get(i).id)});
                    if (rows > 0) {
                        Toast.makeText(ShowFavoriteActivity.this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                        //setContentView(R.layout.activity_show_favorite);
                        initRecyclerView();
                        if (entryAdapter.getItemCount() == 0) {
                            findViewById(R.id.no_content).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.no_content).setVisibility(View.INVISIBLE);
                        }
                        return true;
                    }
                    return true;
                });
            }

            @Override
            public int getItemCount() {
                return mEntry.size();
            }
        };
        mEntry = loadEntiresFromDatabase();
        recyclerView = findViewById(R.id.rv_favorite);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(entryAdapter);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    //从数据库中查询数据，并转换成 JavaBeans
    private List<Entry> loadEntiresFromDatabase() {
        if (db == null) {
            return Collections.emptyList();
        }

        List<Entry> result = new LinkedList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(FavoriteContract.FavEntry.TABLE_NAME,
                    new String[]{FavoriteContract.FavEntry._ID,
                            FavoriteContract.FavEntry.COLUMN_DATE,
                            FavoriteContract.FavEntry.COLUMN_STUDENT_ID,
                            FavoriteContract.FavEntry.COLUMN_USER_NAME,
                            FavoriteContract.FavEntry.COLUMN_IMAGE_URL,
                            FavoriteContract.FavEntry.COLUMN__VIDEO_URL},
                    null, null, null, null,
                    FavoriteContract.FavEntry.COLUMN_DATE + " ASC");

            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(FavoriteContract.FavEntry._ID));
                long dateInMs = cursor.getLong(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_DATE));
                String stuId = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_STUDENT_ID));
                String userName = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_USER_NAME));
                String vidUrl = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN__VIDEO_URL));
                String imgUrl = cursor.getString(cursor.getColumnIndex(FavoriteContract.FavEntry.COLUMN_IMAGE_URL));

                Entry entry = new Entry(id);
                entry.setDate(new Date(dateInMs));
                entry.setImageUrl(imgUrl);
                entry.setStudentId(stuId);
                entry.setUserName(userName);
                entry.setVideoUrl(vidUrl);

                result.add(entry);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case VIDEO_REQUESTS: {
                int num = grantResults.length;
                boolean gotPermissionVideo = true;
                for (int i = 0; i < num; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    } else {
                        gotPermissionVideo = false;
                        Toast.makeText(this, "视频权限获取失败，请重试", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (gotPermissionVideo) {
                    Intent intent = new Intent(ShowFavoriteActivity.this, RecordActivity.class);
                    startActivity(intent);
                }
                break;
            }
            case UPLOAD_REQUEST:{
                int num = grantResults.length;
                boolean gotPermissionUpload = true;
                for (int i = 0; i < num; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    } else {
                        gotPermissionUpload = false;
                        Toast.makeText(this, "文件权限获取失败，请重试", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (gotPermissionUpload) {
                    upLoad();
                }
                break;
            }
        }
    }

    int MODE = 1;

    private void upLoad() {
        Log.d(TAG, "upLoad: " + String.valueOf(MODE));
        if (MODE == 1) {
            chooseImg();
        } else if (MODE == 2) {
            chooseVid();
        } else if (MODE == 3) {
            if (studentId != null) {//避免重复输入
                MODE++;
                upLoad();
            } else {
                View view = getLayoutInflater().inflate(R.layout.half_dialog_view, null);
                final EditText editText = (EditText) view.findViewById(R.id.dialog_edit);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("输入学号")//设置对话框的标题
                        .setView(view)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                studentId = editText.getText().toString();
                                dialog.dismiss();
                                MODE++;
                                upLoad();
                            }
                        }).create();
                dialog.show();
            }
        } else if (MODE == 4) {
            if (userName != null) {//避免重复输入
                MODE++;
                upLoad();
            } else {
                View view = getLayoutInflater().inflate(R.layout.half_dialog_view, null);
                final EditText editText = (EditText) view.findViewById(R.id.dialog_edit);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("输入用户名")//设置对话框的标题
                        .setView(view)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                userName = editText.getText().toString();
                                dialog.dismiss();
                                MODE++;
                                upLoad();
                            }
                        }).create();
                dialog.show();
            }
        } else {
            postVideo();
        }
    }

    private void chooseImg() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE);
    }

    private void chooseVid() {
        Intent intent2 = new Intent();
        intent2.setType("video/*");
        intent2.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent2, "Select Video"),
                PICK_VIDEO);
    }

    private void getResponseFromMiniDouyin(Callback<FeedResponse> callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.108.10.39:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofit.create(IMiniDouyinService.class).getFeed().
                enqueue(callback);
    }

    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        // if NullPointerException thrown, try to allow storage permission in system settings
        File f = new File(ResourceUtils.getRealPath(this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }

    private void postToMiniDouyin(Callback<PostVideoResponse> callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.108.10.39:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofit.create(IMiniDouyinService.class).createVideo(studentId,
                userName,
                getMultipartFromUri("cover_image", mSelectedImage),
                getMultipartFromUri("video", mSelectedVideo)).
                enqueue(callback);
    }

    private void postVideo() {
        postToMiniDouyin(new Callback<PostVideoResponse>() {
            @Override
            public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                Toast.makeText(ShowFavoriteActivity.this.getApplicationContext(), "上传成功！", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                Toast.makeText(ShowFavoriteActivity.this.getApplicationContext(), "上传失败", Toast.LENGTH_LONG).show();
            }
        });

        MODE = 1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                MODE++;
                upLoad();
            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
                MODE++;
                upLoad();
            }
        }
    }
}
