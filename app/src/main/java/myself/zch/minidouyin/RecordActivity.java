package myself.zch.minidouyin;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static myself.zch.minidouyin.Utils.Utils.MEDIA_TYPE_IMAGE;
import static myself.zch.minidouyin.Utils.Utils.MEDIA_TYPE_VIDEO;
import static myself.zch.minidouyin.Utils.Utils.getOutputMediaFile;

public class RecordActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG="DEBUG_RECORD_ACTIVITY";

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
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
            if (mCamera == null) mCamera = getCamera(CAMERA_TYPE);
            mCamera.autoFocus(myAutoCallBack);
            mCamera.takePicture(null, null, mPicture);
        });

        findViewById(R.id.btn_record).setOnClickListener(v -> {
            //录制，第一次点击是start，第二次点击是stop
            if (isRecording) {
                //停止录制
                releaseMediaRecorder();
                isRecording = false;
                // TODO: 2019/1/27 直接发布？
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
                Toast.makeText(this, "ERROR TO CHANGE", Toast.LENGTH_LONG);
            }


        });

        findViewById(R.id.btn_zoom).setOnClickListener(v -> {
            //调焦
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.setParameters(parameters);
            } else
                Toast.makeText(this, "Your device does not support", Toast.LENGTH_SHORT).show();
        });
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
        // TODO: 2019/1/27 10s停止
        //准备MediaRecorder
        mMediaRecorder = new MediaRecorder();
        //Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        //Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        //Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);
        //Step 6: Prepare configured MediaRecorder
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
}
