package myself.zch.minidouyin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static myself.zch.minidouyin.Utils.Utils.MEDIA_TYPE_IMAGE;
import static myself.zch.minidouyin.Utils.Utils.MEDIA_TYPE_VIDEO;
import static myself.zch.minidouyin.Utils.Utils.getOutputMediaFile;

public class RecordActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "DEBUG_RECORD_ACTIVITY";

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private Camera.AutoFocusCallback myAutoCallBack = null;
    private int CAMERA_TYPE = CAMERA_FACING_BACK;
    private boolean isRecording = false;
    private int rotationDegree = 0;
    private MediaRecorder mMediaRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_record);

        mSurfaceView = findViewById(R.id.img);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //给SurfaceHolder添加Callback
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCamera = getCamera(CAMERA_TYPE);
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        });

        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            myAutoCallBack = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                }
            };
            if (mCamera == null)
                mCamera = getCamera(CAMERA_TYPE);
            mCamera.autoFocus(myAutoCallBack);
            mCamera.takePicture(null, null, mPicture);
            Toast.makeText(this, "拍照成功", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_record).setOnClickListener(v -> {
            //录制，第一次点击是start，第二次点击是stop
            if (isRecording) {
                //停止录制
                stopRecord();
            } else {
                //录制
                prepareVideoRecorder();
            }
        });

        findViewById(R.id.btn_facing).setOnClickListener(v -> {
            //切换前后摄像头
            CAMERA_TYPE = 1 - CAMERA_TYPE;
            mCamera = getCamera(CAMERA_TYPE);
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "切换失败", Toast.LENGTH_LONG).show();
            }


        });

        findViewById(R.id.btn_zoom).setOnClickListener(v -> {
            //调焦
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.setParameters(parameters);
            } else
                Toast.makeText(this, "您的设备不支持自动调焦", Toast.LENGTH_SHORT).show();
        });
    }

    private void stopRecord() {
        releaseMediaRecorder();
        isRecording = false;
        upLoad();//上传
    }

    public Camera getCamera(int position) {
        CAMERA_TYPE = position;
        if (mCamera != null) {
            releaseCameraAndPreview();
        }
        Camera cam = Camera.open(position);
        cam.setDisplayOrientation(getCameraDisplayOrientation(CAMERA_TYPE));//旋转方向
        return cam;
    }

    private static final int DEGREE_90 = 90;
    private static final int DEGREE_180 = 180;
    private static final int DEGREE_270 = 270;
    private static final int DEGREE_360 = 360;

    private int getCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = DEGREE_90;
                break;
            case Surface.ROTATION_180:
                degrees = DEGREE_180;
                break;
            case Surface.ROTATION_270:
                degrees = DEGREE_270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % DEGREE_360;
            result = (DEGREE_360 - result) % DEGREE_360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + DEGREE_360) % DEGREE_360;
        }
        return result;
    }


    private void releaseCameraAndPreview() {
        //释放camera资源
        mCamera.stopPreview();
        mCamera.release();
    }

    Camera.Size size;

    private void startPreview(SurfaceHolder holder) {
        //开始预览
        mCamera.startPreview();
    }


    private boolean prepareVideoRecorder() {
        //准备MediaRecorder
        mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setMaxDuration(3 * 1000);//单位：毫秒

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        //设置清晰度
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        Log.d(TAG, "prepareVideoRecorder: " + getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);
        mMediaRecorder.setOnInfoListener(this::onInfo);
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
        } catch (Exception e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


    private void releaseMediaRecorder() {
        //释放MediaRecorder
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCamera.lock();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //释放Camera和MediaRecorder资源
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCamera.lock();
    }


    private Camera.PictureCallback mPicture = (data, camera) -> {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();

            //照片实时更新至系统
            MediaStore.Images.Media.insertImage(getContentResolver(), BitmapFactory.decodeFile(pictureFile.getAbsolutePath()), pictureFile.getName(), null);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(pictureFile);
            intent.setData(uri);
            sendBroadcast(intent);
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }

        mCamera.startPreview();
    };


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(w, h);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private static final int PICK_IMAGE = 2;
    private static final int PICK_VIDEO = 3;
    private static final int UPLOAD_REQUEST = 100;
    private Uri mSelectedImage;
    private Uri mSelectedVideo;
    private String studentId = null;
    private String userName = null;

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
                Toast.makeText(RecordActivity.this.getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                Toast.makeText(RecordActivity.this.getApplicationContext(), "上传失败", Toast.LENGTH_LONG).show();
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

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.v("AUDIOCAPTURE", "Maximum Duration Reached");
            stopRecord();
        }
    }
}
