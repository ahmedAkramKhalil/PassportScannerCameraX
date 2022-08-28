package com.foo.ocr.mrzscanner.camera;

import static com.foo.ocr.mrzscanner.util.BitmapUtil.imageProxyToBitmap;
import static com.foo.ocr.mrzscanner.util.BitmapUtil.rotateBitmapIfNeeded;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.Surface;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
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

import com.foo.ocr.mrzscanner.ml.TextRecognitionProcessor;
import com.foo.ocr.mrzscanner.ml.VisionImageProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraX {
    private static final String TAG = CameraX.class.getName();

    private Activity context;
    private VisionImageProcessor processor;
    private PreviewView previewView;

    private static CameraX cameraX;
    private Executor executor;
    private View frame ;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private CameraSelector cameraSelector;
    private Preview cameraPreview;
    private Camera camera ;

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
        if (cameraX == null)
            cameraX = new CameraX(activity, previewView, processor, frame);
        return cameraX;
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
          cameraPreview.setSurfaceProvider(previewView
                  .createSurfaceProvider());



        /**  Create Camera selection  use case view using back Camera */
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();



        /**  Image Analyzer use case  to analyze image stream from camera and pass to text processor */
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                        .build();
        /**  assign ImageAnalyzer class to Image Analyzer use case */
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

        /**  Image capture use case with AspectRatio matches Camera Preview use case to process returned image correctly */
        ImageCapture.Builder builder = new ImageCapture.Builder();
        imageCapture = builder
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        ViewPort cropRectViewPort = new ViewPort.Builder(
                new Rational(frame.getWidth(), frame.getHeight()),
                cameraPreview.getTargetRotation()
        ).setScaleType(ViewPort.FILL_CENTER).build();

        ViewPort fullImageViewPort = new ViewPort.Builder(
                new Rational(previewView.getWidth(), previewView.getHeight()),
                cameraPreview.getTargetRotation()
        ).build();

        @SuppressLint("RestrictedApi") UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(cameraPreview)
                .setViewPort(fullImageViewPort)
                .addUseCase(imageAnalysis)
                .addUseCase(imageCapture)
                .setViewPort(cropRectViewPort)
                .build();
        cameraProvider.unbindAll();
         camera =cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector,useCaseGroup);
        /**  Unbind all recent use cases */
        /**  check availability of flash light and enable it  to avoid brightness related issues */
        if (camera.getCameraInfo().hasFlashUnit())
            camera.getCameraControl().enableTorch(false);
    }

    public void toggleFlash(Switch flashToggle){
        if (camera != null) {
            boolean isFlashAvailable = camera.getCameraInfo().hasFlashUnit();
            flashToggle.setVisibility(isFlashAvailable ? View.VISIBLE : View.INVISIBLE);
            if (isFlashAvailable)
            camera.getCameraControl().enableTorch(flashToggle.isChecked());
        }
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
        cameraX = null;
    }

    MutableLiveData<Bitmap> capturedImageLifeData ;

}
