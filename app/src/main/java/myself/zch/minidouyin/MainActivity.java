package myself.zch.minidouyin;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import myself.zch.minidouyin.Database.FavoriteContract;
import myself.zch.minidouyin.Database.FavoriteDbHelper;
import myself.zch.minidouyin.JavaBeans.Feed;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG_MAIN_ACTIVITY";
    private static final int VIDEO_REQUESTS = 1;
    private static final int LOCATION_REQUESTS = 10;
    private static final int NETWORK_REQUESTS = 11;
    private static final int UPLOAD_REQUEST = 100;
    private static final int PICK_IMAGE = 2;
    private static final int PICK_VIDEO = 3;
    private RecyclerView recyclerView;
    private List<Feed> mFeeds = new ArrayList<>();
    TextView textView;
    FavoriteDbHelper mDbHelper;
    SQLiteDatabase db;
    private Uri mSelectedImage;
    private Uri mSelectedVideo;
    private String studentId = null;
    private String userName = null;

    /*************************关于定位*****/
    String latLongString;
    private TextView city;
    private LocationManager locationManager;
    private double latitude = 0;
    private double longitude = 0;
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            double[] data = (double[]) msg.obj;

            List<Address> addList = null;
            Geocoder ge = new Geocoder(getApplicationContext());
            try {
                addList = ge.getFromLocation(data[0], data[1], 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addList != null && addList.size() > 0) {
                for (int i = 0; i < addList.size(); i++) {
                    Address ad = addList.get(i);
                    latLongString = ad.getLocality();
                }
            }
            city.setText(latLongString);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.btn_refresh);
        initRecyclerView();
        initButtons();
        initLocate();
        initDb();
        fetchFeed();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.rv_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                ImageView imageView = new ImageView(viewGroup.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setAdjustViewBounds(true);
                return new MainActivity.MyViewHolder(imageView);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                ImageView iv = (ImageView) viewHolder.itemView;

                //Glide加载图片
                String url = mFeeds.get(i).getImage_url();
                Glide.with(iv.getContext()).load(url).into(iv);

                //单击进入详情页面播放
                viewHolder.itemView.setOnClickListener((v -> {
                    Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                    intent.putExtra("VIDEO_URL", mFeeds.get(i).getVideo_url());
                    intent.putExtra("IMAGE_URL", mFeeds.get(i).getImage_url());
                    intent.putExtra("STUDENT_ID", mFeeds.get(i).getStudent_id());
                    intent.putExtra("USER_NAME", mFeeds.get(i).getUser_name());

                    startActivity(intent);
                }));
            }

            @Override
            public int getItemCount() {
                return mFeeds.size();
            }
        });
    }

    private void initButtons() {
        //收藏
        findViewById(R.id.btn_favorite_main).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShowFavoriteActivity.class);
            startActivity(intent);
        });

        //拍摄
        findViewById(R.id.btn_record_main).setOnClickListener(v -> {
            //申请权限
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO},
                        VIDEO_REQUESTS);
            } else {
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
            }
        });

        //发布
        findViewById(R.id.btn_post_main).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        UPLOAD_REQUEST);
            } else {
                upLoad();
            }
        });

        //刷新
        findViewById(R.id.btn_refresh).setOnClickListener(v -> {
            fetchFeed();
        });

        //主动获取定位
        findViewById(R.id.txt_loc).setOnClickListener(v -> {
            Log.d(TAG, "initButtons: listener");
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                LocationManager.NETWORK_PROVIDER,},
                        LOCATION_REQUESTS);
            } else {
                getLocate();
            }
        });
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
                Toast.makeText(MainActivity.this.getApplicationContext(), "UPLOAD SUCCESS", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "FAILED TO UPLOAD", Toast.LENGTH_LONG).show();
            }
        });

        MODE = 1;
    }

    private void initLocate() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            LocationManager.NETWORK_PROVIDER,},
                    LOCATION_REQUESTS);
        } else {
            getLocate();
        }
    }

    private void getLocate() {
        city = (TextView) findViewById(R.id.txt_loc);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        new Thread() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{
                                    LocationManager.NETWORK_PROVIDER,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            NETWORK_REQUESTS
                    );
                    return;
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude(); // 经度
                    longitude = location.getLongitude(); // 纬度
                    double[] data = {latitude, longitude};
                    Message msg = handler.obtainMessage();
                    msg.obj = data;
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    private void initDb() {
        mDbHelper = new FavoriteDbHelper(MainActivity.this);
        db = mDbHelper.getWritableDatabase();
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
                        Toast.makeText(this, String.valueOf(i) + "权限获取失败，请重试", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (gotPermissionVideo) {
                    Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "VIDEO 权限获取失败，请重试", Toast.LENGTH_LONG);
                }
                break;
            }
            case LOCATION_REQUESTS: {
                int num = grantResults.length;
                boolean gotPermissionLocate = true;
                for (int i = 0; i < num; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    } else {
                        gotPermissionLocate = false;
                        Toast.makeText(this, "定位权限获取失败，请重试", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (gotPermissionLocate) {
                    getLocate();
                }
                break;
            }
            case UPLOAD_REQUEST: {
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

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private void getResponseFromMiniDouyin(Callback<FeedResponse> callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.108.10.39:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofit.create(IMiniDouyinService.class).getFeed().
                enqueue(callback);
    }

    private void fetchFeed() {
        btnRefreshDisable();

        getResponseFromMiniDouyin(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "视频刷新成功", Toast.LENGTH_LONG).show();
                mFeeds = response.body().getFeeds();
                recyclerView.getAdapter().notifyDataSetChanged();
                btnRefreshEnable();
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "刷新失败，原因：\n" + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "onFailure: " + t.getMessage());
                btnRefreshEnable();
            }
        });
    }

    private void btnRefreshDisable() {
        textView.setText("获取中");
        textView.setEnabled(false);
    }

    private void btnRefreshEnable() {
        textView.setText("刷新");
        textView.setEnabled(true);
    }


}