package com.foo.ocr.mrzscanner;

import static com.foo.ocr.mrzscanner.util.BitmapUtil.cropBitmapToAFrame;
import static com.foo.ocr.mrzscanner.util.BitmapUtil.cropPreviewBitmapWidth;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.foo.ocr.mrzscanner.camera.CameraX;
import com.foo.ocr.mrzscanner.ml.FaceDetectorHelper;
import com.foo.ocr.mrzscanner.ml.TextRecognitionProcessor;
import com.foo.ocr.mrzscanner.model.DocType;
import com.foo.ocr.mrzscanner.model.PassportDetails;
import com.foo.ocr.mrzscanner.mrzdecoder.MrzRecord;
import com.google.mlkit.vision.common.InputImage;


public class MRZScanner implements TextRecognitionProcessor.IMRZDetectorResultListener {

    private TextRecognitionProcessor frameProcessor;
    private CameraX camera;
    private Activity activity;
    private View overlayView;
    private View screenFrameView;
    private PreviewView previewView;
    private static StateLiveData<PassportDetails> passportDetailsLiveData;
    private static StateLiveData<MrzRecord> mrzLiveData;
    private MrzRecord mrzRecord;
    private boolean imageCaptureLock = false;
    private View mrzAreaView;
    private String TAG = MRZScanner.class.getName();;


    public MRZScanner(/** LifeCycle Owner */ Activity activity,
            /** Camera previewView */ PreviewView previewView , View overlyView, View mrzAreaView ) {

        /** initializing CameraX and its UseCases  */
        this.mrzAreaView = mrzAreaView;
        this.overlayView = overlyView;
        this.screenFrameView = previewView;
        this.activity = activity;
        this.previewView = previewView;
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this, previewView, overlyView, activity);
        camera = CameraX.createInstance(activity, previewView, frameProcessor, this.mrzAreaView);
        mrzLiveData = new StateLiveData<>();
        passportDetailsLiveData = new StateLiveData<>();
    }


    public void startScanning() {
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this, screenFrameView, overlayView, activity);
        camera.start();
        mrzLiveData.postLoading();
    }

    public void stopScanning() {
        frameProcessor.stop();
        camera.stopCamera();
    }


    private void startFaceDetection(Bitmap bitmap) {
        passportDetailsLiveData.postLoading();
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorHelper faceDetectorHelper = new FaceDetectorHelper();
        faceDetectorHelper.detectFaces(inputImage).observe((LifecycleOwner) activity, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                switch (passportDetailsStateData.getStatus()) {
                    case LOADING:
                        Toast.makeText(activity.getApplicationContext(), "Face detection failed..", Toast.LENGTH_LONG).show();
                        imageCaptureLock = false;
                    case ERROR:
                        Log.d(TAG,  passportDetailsStateData.getError().toString());
                        break;
                    case SUCCESS:
                        PassportDetails p = passportDetailsStateData.getData();
                        Bitmap image = p.getPassportPhoto();
                        if (overlayView != null) {
                            image = cropBitmapToAFrame(
                                    cropPreviewBitmapWidth(
                                            image,
                                            activity.getResources().getDisplayMetrics())
                                    , (screenFrameView != null ? screenFrameView : previewView)
                                    , overlayView);
                        }
                        p.setPassportPhoto(image);
                        p.setMrzRecord(mrzRecord);
                        passportDetailsLiveData.postSuccess(p);
                        imageCaptureLock = false;
                        break;
                }
            }
        });
    }


    @Override
    public void onMRZDetectionSuccess(MrzRecord mrzRecord) {
        this.mrzRecord = mrzRecord;
        captureImage();
        mrzLiveData.postSuccess(mrzRecord);
    }


    private void captureImage() {
        Handler handler = new Handler(Looper.getMainLooper());
        /**
         *  the @lock preventing image capturing to capture image until last image processed
         */
        if (!imageCaptureLock)
            handler.post(new Runnable() {
                public void run() {
                    camera.captureImage().observe((LifecycleOwner) activity, new Observer<Bitmap>() {
                        @Override
                        public void onChanged(Bitmap image) {
                            imageCaptureLock = true;
                            passportDetailsLiveData.postLoading();
                            startFaceDetection(image);
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

    public void toggleFlash(Switch flashSwitch) {
        camera.toggleFlash(flashSwitch);
    }

}
