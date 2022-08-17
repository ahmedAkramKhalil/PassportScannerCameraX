package com.foo.ocr;

import static com.foo.ocr.util.ImageUtil.cropBitmapToAFrame;
import static com.foo.ocr.util.ImageUtil.cropPreviewBitmapWidth;
import static com.foo.ocr.util.ImageUtil.imageProxyToBitmap;
import static com.foo.ocr.util.ImageUtil.rotateBitmapIfNeeded;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
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
    //    PassportDetails details;
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
            /* Camera previewView */ PreviewView previewView) {
        // initializing CameraX and its UseCases
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,screenFrameView,overlayView);
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this);
        camera = CameraX.createInstance(activity, previewView, frameProcessor, overlayView);
        this.activity = activity;
        this.previewView = previewView;
        mrzLiveData = new StateLiveData<>();
        passportDetailsLiveData = new StateLiveData<>();
    }

    public PassportScanner(/* LifeCycle Owner */ Activity activity,
            /* Camera previewView */ PreviewView previewView , View overly,View mrzView ) {
        // initializing CameraX and its UseCases
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this);
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

//    public PassportScanner(/* LifeCycle Owner */ Activity activity,
//            /* Camera previewView */ PreviewView previewView,
//    Context context
//    ) {
//        // initializing CameraX and its UseCases
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this);
//        camera = CameraX.createInstance(activity, previewView, frameProcessor, previewView);
//        this.activity = activity;
//        this.previewView = previewView;
//    }

    public static PassportScanner createInstance(/* LifeCycle Owner */ Activity activity,
            /* Camera previewView */ PreviewView previewView){
//        passportScanner = new PassportScanner(activity,previewView);
        return passportScanner;

    }
    public static PassportScanner getInstance() {
        return passportScanner;
    }

    public void start() {
//        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this);
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this,screenFrameView,overlayView,activity);

        camera.start();
        mrzLiveData.postLoading();
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
        faceDetectorHelper.detectFaces(inputImage).observe((LifecycleOwner) activity, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                switch (passportDetailsStateData.getStatus()){
                    case ERROR:
                        passportDetailsLiveData.postError(passportDetailsStateData.getError());
                        Log.d("LOOG", "startFaceDetection  ERROR");
                        break;
                    case SUCCESS:
                        PassportDetails p = passportDetailsStateData.getData();
                        p.setMrzRecord(mrzRecord);
                        passportDetailsLiveData.postSuccess(p);
//                        passportDetailsLiveData.postComplete();
                        Log.d("LOOG", "startFaceDetection  SUCCESS");

                        break;
                }
            }
        });
    }


    @Override
    public void onMRZDetectionSuccess(MrzRecord mrzRecord) {
        Log.d("LOOG","MrzRecord mrzRecord  " );

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                camera.captureImage().observe((LifecycleOwner) activity, new Observer<Bitmap>() {
                    @Override
                    public void onChanged(Bitmap image) {
//                        if (image == null) {
//                            // handle failed image capture
//                        } else {

//                            ImageInfo imageInfo = image.getImageInfo();
//                            Bitmap resultBitmap = rotateBitmapIfNeeded(
//                                    imageProxyToBitmap(image),
//                                    imageInfo);
                            if (overlayView != null) {
                                image = cropBitmapToAFrame(
                                        cropPreviewBitmapWidth(
                                                image,
                                                activity.getResources().getDisplayMetrics())
                                        , (screenFrameView != null ? screenFrameView : previewView)
                                        , overlayView);
                            }
                            passportDetailsLiveData.postLoading();
                            startFaceDetection(image);
                        Log.d("LOOG","Camera Captured");
//                        }
                    }
                });
            }

        });


        this.mrzRecord = mrzRecord;
        mrzLiveData.postSuccess(mrzRecord);
//        mrzLiveData.postComplete();
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

    public View getMrzView() {
        return mrzView;
    }


    //
//    @Override
//    public void onCaptureSuccess(ImageProxy image) {
////        if (!MainActivity.isObjectDetection) {
//        ImageInfo imageInfo = image.getImageInfo();
//
//        Bitmap resultBitmap = rotateBitmapIfNeeded(
//                imageProxyToBitmap(image),
//                imageInfo);
//
//
//        if (overlayView != null) {
//            resultBitmap = cropBitmapToAFrame(
//                    cropPreviewBitmapWidth(
//                            resultBitmap,
//                            activity.getResources().getDisplayMetrics())
//                    , (screenFrameView != null ? screenFrameView : previewView)
//                    , overlayView);
//        }
//        // detect person pic in input image
//
//
////            details.setPassportPhoto(resultBitmap);
//
//        startFaceDetection(resultBitmap);
//
////            Bitmap finalResultBitmap1 = resultBitmap;
////            imageCaptureCompleteListeners.forEach(l -> l.onImageCaptureSuccess(finalResultBitmap1));
//
////        }
////        else {
////            detectObjectInImage(image);
////
////        }
//    }

//    @Override
//    public void onImageCapturingError(ImageCaptureException exception) {
//
////        imageCaptureCompleteListeners.forEach(l -> l.onImageCaptureFailed());
//    }



//    public interface ImageCaptureCompleteListener {
//        void onImageCaptureSuccess(Bitmap capturedImage);
//
//        void onImageCaptureFailed();
//    }
//
//    public interface MRZDetectionListener {
//        void onMRZDetectionSuccess(MrzRecord mrzRecord);
//
//        void onMRzDetectionFailed(Exception exp);
//    }
//
//    public interface FaceDetectionListener {
//        void onFaceDetectionFailed(Exception e);
//
//        void onFaceDetectionSuccess(PassportDetails passportDetails);
//    }
//
//    public interface PassportDetailsDetectionListener {
//        void onPassportDetailsDetectionFailed();
//
//        void onPassportDetailsDetectionSuccess(PassportDetails passportDetails);
//    }


//    public void detectObjectInImage(ImageProxy imageProxy) {
//        ObjectDetectorOptions options =
//                new ObjectDetectorOptions.Builder()
//                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
//                        .build();
//
//        com.google.mlkit.vision.objects.ObjectDetector objectDetector = ObjectDetection.getClient(options);
//        @SuppressLint("UnsafeOptInUsageError") android.media.Image mediaImage = imageProxy.getImage();
//        InputImage inputImage =
//                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//        objectDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
//            @Override
//            public void onSuccess(List<DetectedObject> detectedObjects) {
//                /**
//                 * #TODO:
//                 * To Optimize selecting Passport object in image, You Should loop on detected objects and get area of
//                 * each one, then compare it with camera overlay and get the nearest area as Passport object.
//                 */
//                Log.d("TAG", "detectedObjects = " + detectedObjects.size());
//                MlBitmap mlBitmap;
//                if (detectedObjects.size() > 0) {
//                    Rect boundingBox = detectedObjects.get(0).getBoundingBox();
//                    Bitmap bitmap = inputImage.getBitmapInternal();
//                    Bitmap m = ImageUtil.cropImage(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()), boundingBox);
//                    if (m == null){
//
//                    }
//
////                    mlBitmap = new MlBitmap(m, imageProxy.getImageInfo().getRotationDegrees());
////                    mlBitmap.setBitmapDetectedByObjectDetector(true);
//                    startFaceDetection(m);
//                    frameProcessor.stop();
//                    imageCaptureCompleteListeners.forEach(l -> l.onImageCaptureFailed());
//
//                    imageProxy.close();
//
//                } else {
//                    Toast.makeText(activity.getApplicationContext(), " MRZ Scanned but Object not detected", Toast.LENGTH_LONG).show();
////                    frameProcessor.stop();
////                    start();
//
//                }
////
////                else {
////                mlBitmap = new MlBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
////                mlBitmap.setBitmap2(bit);
////                mlBitmap.setBitmapDetectedByObjectDetector(false);
////                }
//
////                FaceDetectorHelper.getObjectObservable(mlBitmap)
////                        .observeOn(Schedulers.newThread())
////                        .subscribe();
//
//
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d("TAG", "onFailure detectedObjects ");
//                Toast.makeText(activity.getApplicationContext(), " MRZ Scanned but Object not detected", Toast.LENGTH_LONG).show();
//
////                e.printStackTrace();
//            }
//        });
//    }


}
