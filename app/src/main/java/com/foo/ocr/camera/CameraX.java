package com.foo.ocr.camera;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.foo.ocr.ml.VisionImageProcessor;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraX {
    private Activity context;
    private VisionImageProcessor processor;
    PreviewView previewView;

    private static CameraX camera;

    private Executor executor;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;
    private Preview cameraPreview;

    private CameraX(Activity activity, PreviewView previewView, VisionImageProcessor processor) {
        this.context = activity;
        this.previewView = previewView;
        this.processor = processor;
        // TODO: should be tested to select the best for performance
//        executor = Executors.newSingleThreadExecutor();
        executor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public static CameraX createInstance(Activity activity, PreviewView previewView, VisionImageProcessor processor) {
        if (camera == null)
            camera = new CameraX(activity, previewView, processor);
        return camera;
    }


    public void start() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindUseCases(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }


    public void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        /** creating Camera Preview  with AspectRatio matches image Capture  AspectRatio */
        cameraPreview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        /**  Create Camera selection  use case view using back Camera */
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

         /**  Image Analyzer use case  to analyze image stream from camera and pass to text processor */
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
         /**  assign ImageAnalyzer class to Image Analyzer use case */
        imageAnalysis.setAnalyzer(executor, new ImageAnalyzer(processor));

         /**  Image capture use case with AspectRatio matches Camera Preview use case to process returned image correctly */
        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder
                .setTargetRotation(context.getWindowManager().getDefaultDisplay().getRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        cameraPreview.setSurfaceProvider(previewView.createSurfaceProvider());

         /**  Unbind all recent use cases */
        cameraProvider.unbindAll();
        androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, cameraPreview, imageCapture, imageAnalysis);
         /**  check availability of flash light and enable it  to avoid brightness related issues */
        if (camera.getCameraInfo().hasFlashUnit())
            camera.getCameraControl().enableTorch(true);

    }


    public void captureImage(ICaptureImageListener captureImageListener) {
        if (imageCapture != null) {
            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    captureImageListener.onCaptureSuccess(image);
                    super.onCaptureSuccess(image);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    captureImageListener.onImageCapturingError(exception);
                    super.onError(exception);
                    // reload text detection process
                }
            });
        }
    }

    /**
     *  When LifecycleOwner is destroyed the CameraX Object should be assigned to null
     *  to allow re-creation when new binding
     */
    public void stopCamera() {
        camera = null;
    }

    public interface ICaptureImageListener {
        void onCaptureSuccess(ImageProxy image);
        void onImageCapturingError(ImageCaptureException exception);
    }

}
