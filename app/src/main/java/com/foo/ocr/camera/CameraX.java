package com.foo.ocr.camera;

import static com.foo.ocr.util.ImageUtil.imageProxyToBitmap;
import static com.foo.ocr.util.ImageUtil.rotateBitmapIfNeeded;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.foo.ocr.ml.VisionImageProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;

import java.util.BitSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraX {
    private Activity context;
    private VisionImageProcessor processor;
    private PreviewView previewView;

    private static CameraX camera;

    private Executor executor;
    View frame ;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;
    private Preview cameraPreview;

    private CameraX(Activity activity, PreviewView previewView, VisionImageProcessor processor , View frame) {
        this.context = activity;
        this.previewView = previewView;
        this.processor = processor;
        this.frame = frame ;
        // TODO: should be tested to select the best for performance
//        executor = Executors.newSingleThreadExecutor();
        executor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public static CameraX createInstance(Activity activity, PreviewView previewView, VisionImageProcessor processor, View frame) {
        if (camera == null)
            camera = new CameraX(activity, previewView, processor, frame);
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


    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    public void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        /** creating Camera Preview  with AspectRatio matches image Capture  AspectRatio */
        cameraPreview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_0)
                .build();



        /**  Create Camera selection  use case view using back Camera */
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        /**  Image Analyzer use case  to analyze image stream from camera and pass to text processor */
        imageAnalysis =
                new ImageAnalysis.Builder()
//                        .setTargetResolution(new Size(1280, 720))
//                        .setTargetResolution(new Size(640, 480))
                        .setTargetRotation(Surface.ROTATION_0)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        /**  assign ImageAnalyzer class to Image Analyzer use case */

//        imageAnalysis.setAnalyzer(executor, new ImageAnalyzer(processor));
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                try {
                    processor.processImageProxy(image);
                } catch (MlKitException e) {
                    e.printStackTrace();
                }
            }
        });

        // imageProxy the output of an ImageAnalysis.
//        OutputTransform source = new ImageProxyTransformFactory().getOutputTransform();
//        @SuppressLint("UnsafeOptInUsageError") OutputTransform target = previewView.getOutputTransform();
//
//        // Build the transform from ImageAnalysis to PreviewView
//        @SuppressLint("UnsafeOptInUsageError") CoordinateTransform coordinateTransform = new CoordinateTransform(source, target);
//
//        // Detect face in ImageProxy and transform the coordinates to PreviewView.
//        // The value of faceBox can be used to highlight the face in PreviewView.
//        RectF faceBox = detectFaceInImageProxy(imageProxy);
//        coordinateTransform.mapRect(faceBox);
        /**  Image capture use case with AspectRatio matches Camera Preview use case to process returned image correctly */
        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder
//                .setTargetRotation(context.getWindowManager().getDefaultDisplay().getRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//                .setTargetRotation(Surface.ROTATION_0)
                .build();
        int width = 300;
        int height = 300 ;
        @SuppressLint("RestrictedApi") ViewPort viewPort = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            viewPort = new ViewPort.Builder(
                    new Rational(frame.getWidth(), frame.getHeight()),
//                    new Rational(480, 640),
//                    new Rational(frame.getWidth(), frame.getHeight()),
                    context.getDisplay().getRotation()
            ).build();

        }else {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            viewPort = new ViewPort.Builder(
                    new Rational(frame.getWidth(), frame.getHeight()),
//                    new Rational(480, 320),
//                    new Rational(frame.getWidth(), frame.getHeight()),
                    display.getRotation()
            ).build();
//            new ViewPort.Builder()
        }

//        OrientationEventListener orientationEventListener = new OrientationEventListener((context)) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                int rotation;
//
//                // Monitors orientation values to determine the target rotation value
//                if (orientation >= 45 && orientation < 135) {
//                    rotation = Surface.ROTATION_270;
//                } else if (orientation >= 135 && orientation < 225) {
//                    rotation = Surface.ROTATION_180;
//                } else if (orientation >= 225 && orientation < 315) {
//                    rotation = Surface.ROTATION_90;
//                } else {
//                    rotation = Surface.ROTATION_0;
//                }
//
//                imageCapture.setTargetRotation(rotation);
//
//            }
//        };


        @SuppressLint("RestrictedApi") UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(cameraPreview)
                .addUseCase(imageAnalysis)
                .addUseCase(imageCapture)
                .setViewPort(viewPort)
                .build();
        cameraProvider.unbindAll();
        androidx.camera.core.Camera camera =cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, useCaseGroup);
//        cameraPreview.setSurfaceProvider( previewView.getSurfaceProvider());
        cameraPreview.setSurfaceProvider(previewView.createSurfaceProvider());


        /**  Unbind all recent use cases */
//        cameraProvider.unbindAll();
//        androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, cameraPreview, imageCapture, imageAnalysis);
        /**  check availability of flash light and enable it  to avoid brightness related issues */
//        if (camera.getCameraInfo().hasFlashUnit())
//            camera.getCameraControl().enableTorch(true);

//        camera.getCameraControl().setLinearZoom(0);


    }



    public MutableLiveData<Bitmap> captureImage() {
        if (capturedImageLifeData == null)
            capturedImageLifeData = new MutableLiveData<>();

        if (imageCapture != null) {
            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    ImageInfo imageInfo = image.getImageInfo();
                    Bitmap resultBitmap = rotateBitmapIfNeeded(
                            imageProxyToBitmap(image),
                            imageInfo);
                    image.close();
                    capturedImageLifeData.postValue(resultBitmap);
                    super.onCaptureSuccess(image);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    capturedImageLifeData.postValue(null);
                    super.onError(exception);
                    // reload text detection process
                }
            });
        }

        return capturedImageLifeData;
    }

    /**
     * When LifecycleOwner is destroyed the CameraX Object should be assigned to null
     * to allow re-creation when new binding
     */
    public void stopCamera() {
        camera = null;
    }

    MutableLiveData<Bitmap> capturedImageLifeData ;

//    public MutableLiveData<ImageProxy> getCapturedImageLifeData() {
//        if (capturedImageLifeData == null)
//            capturedImageLifeData = new MutableLiveData<>();
//    }

    //    public interface ICaptureImageListener {
//        void onCaptureSuccess(ImageProxy image);
//
//        void onImageCapturingError(ImageCaptureException exception);
//    }

}
