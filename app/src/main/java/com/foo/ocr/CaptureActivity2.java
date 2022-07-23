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

import com.foo.ocr.databinding.ActivityCapture2Binding;
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
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import android.widget.ImageView;

import org.jmrtd.lds.icao.MRZInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CaptureActivity2 extends AppCompatActivity implements TextRecognitionProcessor.ResultListener {
    private ActivityCapture2Binding binding;
    private Executor executor = Executors.newSingleThreadExecutor();
    PreviewView mPreviewView;
    ImageCapture imageCapture;
    private TextRecognitionProcessor frameProcessor;
    View overlay;
    ImageView captureImage;
    static List<Scan> scans = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture2);
        mPreviewView = binding.camera;
//        overlay = binding.view;
//        captureImage = binding.captured;
    }
    private Rect getRectOfView(View view) {
        int[] l = new int[2];
        view.getLocationOnScreen(l);
        return new Rect(l[0], l[1], l[0] + view.getWidth(), l[1] + view.getHeight());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
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
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
//                        .setOutputImageFormat(ImageAnalysis.O)
//                        .setTargetResolution(new Size(640, 480))
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
                .build();
        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
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
//        Scan scan = new Scan();
//        scan.startTime = System.currentTimeMillis();
//        scans.add(scan);
    }


    void takePicture() {
        if (imageCapture != null) {
            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
//                Bitmap detectedImage =  imageProxyToBitmap(image);
                    ImageInfo imageInfo = image.getImageInfo();
                    Bitmap bitmap = rotateBitmapIfNeeded(imageProxyToBitmap(image), imageInfo);
                    Size size = new Size(binding.frame.getWidth(), binding.frame.getHeight());
                    Rect rect = new Rect(binding.view.getLeft(), binding.view.getTop(), binding.view.getRight(), binding.view.getBottom());
                    Bitmap bitmap1 = Image.cropImage(bitmap, size, rect);
//                    Matrix m = getMappingMatrix(image,mPreviewView);
//                    Log.d("LOOG", "MainActivity-Crop " + m.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                           binding.captured.setImageBitmap(cropBitmapTAFrame(bitmap,binding.camera,binding.view));
//                           binding.captured.setImageBitmap(cropView(bitmap,binding.frame,binding.view));
                            binding.captured.setImageBitmap(bitmap1);
//                           Glide.with(binding.captured).load(bitmap).into(binding.captured);
                        }
                    });


                    super.onCaptureSuccess(image);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    super.onError(exception);
                }
            });
        }
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap source, ImageInfo info) {
        int angle = info.getRotationDegrees();
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mat, true);
    }

    Bitmap cropBitmapTAFrame(Bitmap source, View frame, View cardPlaceHolder) {
        float scaleX = source.getWidth() / (float) frame.getWidth();
        float scaleY = source.getHeight() / (float) frame.getHeight();

        int x = (int) ((cardPlaceHolder.getLeft()) * scaleX);
        int y = (int) ((cardPlaceHolder.getTop()) * scaleY);

        Log.v("MainActivity-Crop", "leftPos: " + cardPlaceHolder.getLeft() + " width: " + cardPlaceHolder.getWidth());
        Log.d("LOOG", "MainActivity-Crop source " + source.getHeight() + " " + source.getWidth());
        Log.d("LOOG", "MainActivity-Crop frame " + frame.getHeight() + " " + frame.getWidth());
        Log.d("LOOG", "MainActivity-Crop cardPlaceHolder " + cardPlaceHolder.getHeight() + " " + cardPlaceHolder.getWidth());


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


    private void getCroppedImage(ImageProxy imageProxy, Rect rect) {
        ByteBuffer yByteBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer yuByteBuffer = imageProxy.getPlanes()[2].getBuffer();


    }


    @Override
    public void onSuccess(MrzRecord mrzInfo) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MRZ_RESULT, mrzInfo);
        setResult(Activity.RESULT_OK, returnIntent);
//        scans.get(scans.size()-1).endTime = System.currentTimeMillis();
//        Log.d("Scanns","Scan + " + scans.toString()) ;
//        Log.d("String", mrzInfo.toString());
//        takePicture();
        finish();

    }


    @Override
    public void onError(Exception exp) {
        Log.d("MRZ", "onError");

    }


    void setProcess() {
//        Task<Text> results =
//                recognizer.process(image)
//                        .addOnSuccessListener(new OnSuccessListener<Text>() {
//                            @Override
//                            public void onSuccess(Text result) {
//                                for (Text.TextBlock block : result.getTextBlocks()) {
//                                    String blockText = block.getText();
//                                    Log.d("LOOG"," " + blockText);
//
//                                    Point[] blockCornerPoints = block.getCornerPoints();
//                                    Rect blockFrame = block.getBoundingBox();
//                                    for (Text.Line line : block.getLines()) {
//                                        String lineText = line.getText();
//                                        Point[] lineCornerPoints = line.getCornerPoints();
//                                        Rect lineFrame = line.getBoundingBox();
//                                        for (Text.Element element : line.getElements()) {
//                                            String elementText = element.getText();
//                                            Point[] elementCornerPoints = element.getCornerPoints();
//                                            Rect elementFrame = element.getBoundingBox();
//
//                                        }
//                                    }
//                                }
//                                imageProxy.close();
//
//
//
//                            }
//                        })
//                        .addOnFailureListener(
//                                new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        // Task failed with an exception
//                                        // ...
//                                        e.printStackTrace();
//                                    }
//                                });

    }

    Matrix getMappingMatrix(ImageProxy imageProxy, PreviewView previewView) {
        Rect cropRect = imageProxy.getCropRect();
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        Matrix matrix = new Matrix();

        // A float array of the source vertices (crop rect) in clockwise order.
        float[] source = {
                cropRect.left,
                cropRect.top,
                cropRect.right,
                cropRect.top,
                cropRect.right,
                cropRect.bottom,
                cropRect.left,
                cropRect.bottom
        };

        // A float array of the destination vertices in clockwise order.
        float[] destination = {
                0f,
                0f,
                previewView.getWidth(),
                0f,
                previewView.getWidth(),
                previewView.getHeight(),
                0f,
                previewView.getHeight()
        };

        // The destination vertexes need to be shifted based on rotation degrees.
        // The rotation degree represents the clockwise rotation needed to correct
        // the image.
        // Each vertex is represented by 2 float numbers in the vertices array.
        int vertexSize = 2;
        // The destination needs to be shifted 1 vertex for every 90° rotation.
        int shiftOffset = rotationDegrees / 90 * vertexSize;
        float[] tempArray = destination.clone();
        for (int toIndex = 0; toIndex < source.length; toIndex++) {
            int fromIndex = (toIndex + shiftOffset) % source.length;
            destination[toIndex] = tempArray[fromIndex];
        }
        matrix.setPolyToPoly(source, 0, destination, 0, 4);
        return matrix;
    }


    private Bitmap cropView(Bitmap bitmap, View frame, View reference) {
        int heightOriginal = frame.getHeight();
        int widthOriginal = frame.getWidth();
        int heightFrame = reference.getHeight();
        int widthFrame = reference.getWidth();
        int leftFrame = reference.getLeft();
        int topFrame = reference.getTop();
        int heightReal = bitmap.getHeight();
        int widthReal = bitmap.getWidth();
        int widthFinal = widthFrame * widthReal / widthOriginal;
        int heightFinal = heightFrame * heightReal / heightOriginal;
        int leftFinal = leftFrame * widthReal / widthOriginal;
        int topFinal = topFrame * heightReal / heightOriginal;
        return Bitmap.createBitmap(bitmap, leftFinal, topFinal, widthFinal, heightFinal);


    }
}