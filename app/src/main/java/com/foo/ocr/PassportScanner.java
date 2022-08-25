package com.foo.ocr;

import static com.foo.ocr.util.BitmapUtil.cropBitmapToAFrame;
import static com.foo.ocr.util.BitmapUtil.cropPreviewBitmapWidth;
import static com.foo.ocr.util.BitmapUtil.imageProxyToBitmap;
import static com.foo.ocr.util.BitmapUtil.rotateBitmapIfNeeded;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.camera.core.ImageInfo;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import com.foo.ocr.camera.CameraX;
import com.foo.ocr.ml.FaceDetectorHelper;
import com.foo.ocr.ml.TextRecognitionProcessor;
import com.foo.ocr.model.DocType;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.google.mlkit.vision.common.InputImage;


public class PassportScanner implements TextRecognitionProcessor.IMRZDetectorResultListener {

    TextRecognitionProcessor frameProcessor;
    CameraX camera;
    Activity activity;
    View overlayView;
    View screenFrameView;
    PreviewView previewView;
    private static StateLiveData<PassportDetails> passportDetailsLiveData ;
    private static StateLiveData<MrzRecord> mrzLiveData ;
    MrzRecord mrzRecord;
    static PassportScanner passportScanner ;
    private View mrzView;


    public PassportScanner(/* LifeCycle Owner */ Activity activity,
            /* Camera previewView */ PreviewView previewView , View overly,View mrzView ) {
        // initializing CameraX and its UseCases
        this.mrzView = mrzView;
        this.overlayView = overly ;
        this.screenFrameView = previewView ;
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,previewView,overly,activity);
        camera = CameraX.createInstance(activity, previewView, frameProcessor, mrzView);
        this.activity = activity;
        this.previewView = previewView;
        mrzLiveData = new StateLiveData<>();
        passportDetailsLiveData = new StateLiveData<>();
    }

    public static PassportScanner createInstance(/* LifeCycle Owner */ Activity activity,
            /* Camera previewView */ PreviewView previewView){
        return passportScanner;
    }

    public static PassportScanner getInstance() {
        return passportScanner;
    }

    public void start() {
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,screenFrameView,overlayView,activity);
        camera.start();
        mrzLiveData.postLoading();
    }

    private void restart() {
//        mrzLiveData = new StateLiveData<>();
//        passportDetailsLiveData = new StateLiveData<>();        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,screenFrameView,overlayView,activity);
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,screenFrameView,overlayView,activity);
//        camera.resetImageAnalyzer(frameProcessor);
//        camera = CameraX.createInstance(activity, previewView, frameProcessor, mrzView);
//        camera./start();
//        mrzLiveData.postLoading();


    }

    public void stop() {
        frameProcessor.stop();
        camera.stopCamera();
    }

    public void setScreenFrameView(View frame) {
        screenFrameView = frame;
    }

    public void setOverlayView(View overlayView) {
        this.overlayView = overlayView;
    }

    private  void startFaceDetection(Bitmap bitmap) {
        Log.d("LOOG", "startFaceDetection");
        passportDetailsLiveData.postLoading();
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorHelper faceDetectorHelper = new FaceDetectorHelper();
        faceDetectorHelper.detectFaces(inputImage, activity.getResources().getDisplayMetrics()).observe((LifecycleOwner) activity, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                switch (passportDetailsStateData.getStatus()){
                    case LOADING:
                        Log.d("FAIL", "startFaceDetection  LOADING");
//                        passportDetailsLiveData.postError(passportDetailsStateData.getError());
                        Toast.makeText(activity.getApplicationContext(), "Face detection failed..", Toast.LENGTH_LONG).show();
//                      Toast.makeText(getContext(), "Failed to detect passport picture.. please try again", Toast.LENGTH_LONG).show();
                        // recapture
//                        captureImage();
                        lock = false;

                    case ERROR:

//                        passportDetailsLiveData.postError(passportDetailsStateData.getError());
//                        Log.d("FAIL", "startFaceDetection  ERROR");
////                        passportDetailsLiveData.postError(passportDetailsStateData.getError());
////
//                        // recapture
//                        captureImage();
//                        ScannerApplication.get().reset();

//                        restart();
                        break;
                    case SUCCESS:
                        PassportDetails p = passportDetailsStateData.getData();
                      Bitmap image = p.getPassportPhoto();
                        if (overlayView != null) {
                            image = cropBitmapToAFrame(
                                        cropPreviewBitmapWidth(
                                                image,
                                                activity.getResources().getDisplayMetrics())
//                                    image
                                    , (screenFrameView != null ? screenFrameView : previewView)
                                    , overlayView);
                        }
                        p.setPassportPhoto(image);
                        p.setMrzRecord(mrzRecord);
                        passportDetailsLiveData.postSuccess(p);
//                        passportDetailsLiveData.postComplete();
                        Log.d("LOOG", "startFaceDetection  SUCCESS");
                        lock = false;
                        break;
                }
            }
        });
    }



    @Override
    public void onMRZDetectionSuccess(MrzRecord mrzRecord) {
        Log.d("LOOG","MrzRecord mrzRecord  " );
        captureImage();
        this.mrzRecord = mrzRecord;

        mrzLiveData.postSuccess(mrzRecord);
    }

    boolean lock = false ;
    private void captureImage(){
        Log.d("LOOG","captureImage ===>>>  " );

        Handler handler = new Handler(Looper.getMainLooper());
        if (!lock)
            handler.post(new Runnable() {
            public void run() {
                camera.captureImage().observe((LifecycleOwner) activity, new Observer<Bitmap>() {
                    @Override
                    public void onChanged(Bitmap image) {
//                            ImageInfo imageInfo = image.getImageInfo();
//                            Bitmap resultBitmap = rotateBitmapIfNeeded(
//                                    imageProxyToBitmap(image),
//                                    imageInfo);
                        // ----------------------------
//                            if (overlayView != null) {
//                                image = cropBitmapToAFrame(
////                                        cropPreviewBitmapWidth(
////                                                image,
////                                                activity.getResources().getDisplayMetrics())
//
//                                        image
//                                        , (screenFrameView != null ? screenFrameView : previewView)
//                                        , overlayView);
//                            }

                        lock = true ;
                        passportDetailsLiveData.postLoading();
                        startFaceDetection(image);
                        Log.d("LOOG","Camera Captured");
                    }
                });
            }
        });


    }
    @Override
    public void onMRZDetectionError(Exception exp) {
        mrzLiveData.postError(exp);
    }

    public static StateLiveData<PassportDetails> getPassportDetailsLiveData() {
        return passportDetailsLiveData;
    }

    public static StateLiveData<MrzRecord> getMrzLiveData() {
        return mrzLiveData;
    }

    public void setMrzView(View mrzView) {
        this.mrzView = mrzView;
    }

    public void toggleFlash(Switch flashSwitch){
        camera.toggleFlash(flashSwitch);
    }

}
