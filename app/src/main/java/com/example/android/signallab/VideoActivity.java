package com.example.android.signallab;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Collections;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import java.util.List;
import android.graphics.PointF;
import android.graphics.Rect;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VideoActivity extends AppCompatActivity {
    private CustomImageView mcustomSurfaceView;
    private ImageView processedImageView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Handler mainHandler;
    private PointF leftEyePos;
    private PointF rightEyePos;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable stopCameraRunnable;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mcustomSurfaceView = findViewById(R.id.customImageView);

        mainHandler = new Handler(Looper.getMainLooper());
        processedImageView = findViewById(R.id.processedImageView); // ensure that setContentView() is implemented before it
//        openCamera();
        // 绑定按钮

        openCamera();
        // 设置点击监听器

    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[1];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Size[] supportedSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
            Size previewSize = supportedSizes[supportedSizes.length - 1];

            Size targetSize = null;
            for (int i = 0; i<supportedSizes.length; i++){
            }

            for (Size size : supportedSizes) {
                Log.d("MyTag","size: " + size.getWidth() + "+" + size.getHeight());
                if (size.getWidth() == 640 && size.getHeight() == 480) {
                    targetSize = size;
                    break;
                }
            }
            if (targetSize != null) {
                previewSize = targetSize;
            }
//            Size previewSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                    .getOutputSizes(SurfaceTexture.class)[0];
//            Log.d("MyTag","previewSize: " + supportedSizes.length);
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(this::onImageAvailable, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraCaptureSession() {
        try {
            Surface imageSurface = imageReader.getSurface();

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(imageSurface);

            // Set the desired frame rate range
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

            // Select a suitable FPS range (e.g., [30, 30] for 30 FPS)
            Range<Integer> targetFpsRange = null;
            for (Range<Integer> range : fpsRanges) {
                if (range.getLower() == 30 && range.getUpper() == 30) { // Example: 30 FPS
                    targetFpsRange = range;
                    break;
                }
            }

            if (targetFpsRange != null) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFpsRange);
            } else {
                Log.w("VideoActivity", "Target FPS range not found, using default.");
            }
            cameraDevice.createCaptureSession(Collections.singletonList(imageSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void onImageAvailable(ImageReader reader) {
        backgroundHandler.post(() -> {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    processImage(image); // Process the image on the background thread
                }
            } catch (Exception e) {
                Log.e("VideoActivity", "Error processing image", e);
            }
        });
    }

    private void processImage(Image image) {

        // Process the image data by first converting into an RGB Bitmap
        Bitmap bitmap = yuvToRgbBitmap(image);
        InputImage input_image = InputImage.fromBitmap(bitmap, 0);
        // Update the ImageView on the UI thread
        if (bitmap != null) {
            mainHandler.post(() -> processedImageView.setImageBitmap(bitmap));
            detectFaces(input_image);
            drawGreenPoint(bitmap,leftEyePos);
            drawGreenPoint(bitmap,rightEyePos);
        }
    }
    private Bitmap yuvToRgbBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer(); // Y plane
        ByteBuffer uBuffer = planes[1].getBuffer(); // U plane
        ByteBuffer vBuffer = planes[2].getBuffer(); // V plane

        // Extract row strides and pixel strides
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] rgbArray = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Y plane index
                int yIndex = y * yRowStride + x;

                // UV plane indices
                int uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride;

                // YUV to RGB conversion
                int yValue = (yBuffer.get(yIndex) & 0xFF);
                int uValue = (uBuffer.get(uvIndex) & 0xFF) - 128;
                int vValue = (vBuffer.get(uvIndex) & 0xFF) - 128;

                int r = (int) (yValue + 1.370705 * vValue);
                int g = (int) (yValue - 0.337633 * uValue - 0.698001 * vValue);
                int b = (int) (yValue + 1.732446 * uValue);

                // Clamp RGB values to 0-255
                r = Math.min(Math.max(r, 0), 255);
                g = Math.min(Math.max(g, 0), 255);
                b = Math.min(Math.max(b, 0), 255);

                //Here you can insert code to modify the pixel values
                
                // Set RGB pixel in array
                rgbArray[y * width + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // Create a Bitmap from the RGB array
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(rgbArray, 0, width, 0, 0, width, height);

        //Rotate the bitmap 90 degrees (required for correct display on Xperia XA1)
        int totalRotation=270;
        Bitmap rotatedBitmap = rotateBitmap(bitmap, totalRotation);

        return rotatedBitmap;
    }
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectFaces(InputImage image) {
        // [START set_detector_options]
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        FaceDetector detector = FaceDetection.getClient(options);
        // Or use the default options:
        // FaceDetector detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                            if (leftEye != null) {
                                                leftEyePos = leftEye.getPosition();
                                                rightEyePos = rightEye.getPosition();
                                                Log.d("tag","Left eye: " + leftEyePos);
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }
                                        // [END get_face_info]
                                        // [END_EXCLUDE]
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        // [END run_detector]
    }
    public static Bitmap drawGreenPoint(Bitmap bitmap, PointF point) {
        if (bitmap == null || point == null) {
            return null;
        }
        // 确保 Bitmap 是可变的，如果不是，则创建一个副本
        Bitmap mutableBitmap = bitmap.isMutable()
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 使用 Canvas 在 Bitmap 上绘制
        Canvas canvas = new Canvas(mutableBitmap);

        // 创建一个画笔，并设置为绿色实心样式
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);

        // 绘制一个圆点，半径为 5 像素（根据需要调整）
        canvas.drawCircle(point.x, point.y, 5f, paint);

        // 返回修改后的 Bitmap
        return mutableBitmap;
    }
}
