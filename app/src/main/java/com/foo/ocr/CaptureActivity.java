package com.foo.ocr;


import static com.foo.ocr.MainActivity.MRZ_RESULT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Bundle;

import com.foo.ocr.databinding.ActivityCaptureBinding;
import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.text.TextRecognitionProcessor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.internal.ViewPorts;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.foo.ocr.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.jmrtd.lds.icao.MRZInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity implements TextRecognitionProcessor.ResultListener {

    private ActivityCaptureBinding binding;

    private Executor executor = Executors.newSingleThreadExecutor();
    PreviewView mPreviewView;
    ImageCapture imageCapture;
    private TextRecognitionProcessor frameProcessor;
    Rect rect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        mPreviewView = binding.camera;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCamera();
    }


    private Rect getViewRect(View view) {
        int[] l = new int[2];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.getLocationInWindow(l);
        }
        int x = l[0];
        int y = l[1];
        int w = view.getWidth();
        int h = view.getHeight();
        return new Rect(x, y, x + w, y + h);
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));

        frameProcessor = new TextRecognitionProcessor(this, DocType.PASSPORT, this);
    }


    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
//                        .setOutputImageFormat(ImageAnalysis.O)
//                        .setTargetResolution(cameraSelector.)
                        .setTargetResolution(new Size(1280, 720))
//                        .setTargetRotation(Surface.ROTATION_90)

//                        .setImageQueueDepth(200)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
//                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                frameProcessor.process(imageProxy);
            }
        });

        ImageCapture.Builder builder = new ImageCapture.Builder();

//        Vendor-Extensions (The CameraX extensions dependency in build.gradle)
//        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

//         Query if extension is available (optional).
//        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
//            // Enable the extension if available.
//            hdrImageCaptureExtender.enableExtension(cameraSelector);
//        }

        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        ;
//        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
//                .addUseCase(preview)
//                .addUseCase(imageAnalysis)
//                .addUseCase(imageCapture)
//                .setViewPort(viewPort)
//                .build();

        cameraProvider.unbindAll();
//           cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, imageAnalysis);
        if (camera.getCameraInfo().hasFlashUnit())
            camera.getCameraControl().enableTorch(true);
    }


    void captureImage() {
        if (imageCapture != null) {
            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    ImageInfo imageInfo = image.getImageInfo();
                    rect = getViewRect(binding.view);

                    // Do crop for overlay frame
                    Bitmap bitmap = cropBitmapToAFrame(cropPreviewBitmapWidth(rotateBitmapIfNeeded(imageProxyToBitmap(image), imageInfo)), binding.frame, binding.view);

                    // Do crop for object detected  frame
//                    objectDetector(image);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.captured.setImageBitmap(bitmap);
                        }
                    });
                    super.onCaptureSuccess(image);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    super.onError(exception);
                    // reload text detection proccess
                }
            });
        }
    }

    Bitmap cropPreviewBitmapWidth(Bitmap previewBitmap) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int deviceWidthPx = dm.widthPixels;
        int deviceHeightPx = dm.heightPixels;
        float cropHeightPx = 0f;
        float cropWidthPx = 0f;
        if (deviceHeightPx > deviceWidthPx) {
            cropHeightPx = 1.0f * previewBitmap.getHeight();
            cropWidthPx = 1.0f * deviceWidthPx / deviceHeightPx * cropHeightPx;
        } else {
            cropWidthPx = 1.0f * previewBitmap.getWidth();
            cropHeightPx = 1.0f * deviceHeightPx / deviceWidthPx * cropWidthPx;
        }
        float cx = previewBitmap.getWidth() / 2;
        float cy = previewBitmap.getHeight();
        float minimusPx = Math.min(cropHeightPx, cropWidthPx);
        float left2 = cx - minimusPx / 2;
        float top2 = cy - minimusPx / 2;
        Bitmap croppedBitmap = Bitmap.createBitmap(previewBitmap, (int) left2, (int) 0, (int) minimusPx, (int) previewBitmap.getHeight());
        return croppedBitmap;
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap source, ImageInfo info) {
        int angle = info.getRotationDegrees();
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mat, true);
    }

    void detectObjectInImage(ImageProxy imageProxy) {
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
//                        .enableClassification()  // Optional
                        .build();

        ObjectDetector objectDetector = ObjectDetection.getClient(options);
        @SuppressLint("UnsafeOptInUsageError") android.media.Image mediaImage = imageProxy.getImage();
//            InputImage image =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        InputImage inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
        objectDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
            @Override
            public void onSuccess(List<DetectedObject> detectedObjects) {
//                for (DetectedObject detectedObject : detectedObjects) {
                Rect boundingBox = detectedObjects.get(0).getBoundingBox();
                Bitmap bitmap = inputImage.getBitmapInternal();
                Bitmap m = Image.cropImage(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()), boundingBox);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.captured.setImageBitmap(m);
//                           Glide.with(binding.captured).load(bitmap).into(binding.captured
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

    }

    Bitmap cropBitmapToAFrame(Bitmap source, View frame, View cardPlaceHolder) {
        float scaleX = source.getWidth() / (float) frame.getWidth();
        float scaleY = source.getHeight() / (float) frame.getHeight();
        int x = (int) ((cardPlaceHolder.getLeft()) * scaleX);
        int y = (int) ((cardPlaceHolder.getTop()) * scaleY);
        int width = (int) (cardPlaceHolder.getWidth() * scaleX);
        int height = (int) (cardPlaceHolder.getHeight() * scaleY);
        return Bitmap.createBitmap(source, x, y, width, height);
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }


    @Override
    public void onSuccess(MrzRecord mrzInfo) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MRZ_RESULT, mrzInfo);
        setResult(Activity.RESULT_OK, returnIntent);
        captureImage();
//        finish();
    }


    @Override
    public void onError(Exception exp) {
        Log.d("MRZ", "onError");
    }

}