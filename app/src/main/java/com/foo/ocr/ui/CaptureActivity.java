package com.foo.ocr.ui;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.foo.ocr.MyApplication;
import com.foo.ocr.PassportScanner;
import com.foo.ocr.R;
import com.foo.ocr.StateData;
import com.foo.ocr.databinding.ActivityCaptureBinding;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
//import androidx.lifecycle.ViewModelProviders;

public class CaptureActivity extends AppCompatActivity {
    private static final String PASSPORT_RESULT = "PASSPORT_RESULT";
//    PassportDetailedViewModel viewModel;

    private ActivityCaptureBinding binding;
    private PreviewView mPreviewView;
    PassportScanner passportScanner;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        mPreviewView = binding.camera;
//        viewModel = ViewModelProviders.of(this).get(PassportDetailedViewModel.class);
        mPreviewView = binding.camera;
//        passportScanner = PassportScanner.createInstance(this, mPreviewView);
        passportScanner = new PassportScanner(this, mPreviewView,binding.view,binding.overlayView);
        passportScanner.setOverlayView(binding.view);
        passportScanner.setScreenFrameView(binding.frame);
        passportScanner.setMrzView(binding.overlayView);
//            viewModel.getMrzDateStateLiveData().postSuccess(passportScanner.getMrzLiveData().getValue().getData());


//        passportScanner.getMrzLiveData().observe(this, new Observer<StateData<MrzRecord>>() {
//            @Override
//            public void onChanged(StateData<MrzRecord> mrzRecordStateData) {
//                switch (mrzRecordStateData.getStatus()){
//                    case SUCCESS:
//                        finish();
//                }
//
//            }
//        });

//        passportScanner.getPassportDetailsLiveData().observe(this, new Observer<StateData<PassportDetails>>() {
//            @Override
//            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
//                switch (passportDetailsStateData.getStatus()){
//                    case LOADING:
//                        Log.d("LOOG","LOOGLOOGLOOGLOOGLOOGLOOGLOOGLOOGLOOG");
//                        finish();
//                }
//
//            }
//        });

        passportScanner.getMrzLiveData().observe(this, new Observer<StateData<MrzRecord>>() {
            @Override
            public void onChanged(StateData<MrzRecord> mrzRecordStateData) {
//                viewModel.setPassportDetailsMutableLiveData(passportScanner.getPassportDetailsLiveData());
                Log.d("LOOG", "" + mrzRecordStateData.getStatus().toString());

                MyApplication.get().dataRepository.updateData(mrzRecordStateData);

//                switch (mrzRecordStateData.getStatus()) {
//                    case SUCCESS:
//                        Log.d("LOOG", mrzRecordStateData.getData().toString());
////                        viewModel.getMrzDateStateLiveData().postSuccess(mrzRecordStateData.getData());
//
//                        break;
//                }
            }
        });
        passportScanner.getPassportDetailsLiveData().observe(this, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                Log.d("LOOG", ">>>>>>" + passportDetailsStateData.getStatus());
                MyApplication.get().dataRepository.updateText(passportDetailsStateData);

                switch (passportDetailsStateData.getStatus()) {
                    case SUCCESS:
                        Intent returnIntent = new Intent();
//                        returnIntent.putExtra(MRZ_RESULT, mrzRecord);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
//                        viewModel.getPassportDetailsMutableLiveData().postSuccess(passportDetailsStateData.getData());
                        break;
                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        passportScanner.start();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            passportScanner.stop();
        }
        super.onDestroy();
    }


//
//    @SuppressLint("UnsafeOptInUsageError")
//    @Override
//    public void onCaptureSuccess(ImageProxy image) {
//        /**
//         * The following two options are to Crop the captured image,
//         * the first: is to crop it based on the overlay borders on the screen,
//         * and the second is to locate the passport based on Machine leaning kit to identify objects,
//         * and it was suggested to combine the two mechanisms to improve performance.
//         */
////        Log.d("TAG","onCaptureSuccess");
////        camera.stopCamera();
////        // Cropping the captured image on the borders of the Overlay on the screen
//        ImageInfo imageInfo = image.getImageInfo();
//        // Rotate captured image to device rotation then crop it to the frame
//        Bitmap bitmap = cropBitmapToAFrame(
//                cropPreviewBitmapWidth(
//                        rotateBitmapIfNeeded(
//                                imageProxyToBitmap(image),
//                                imageInfo),
//                        getResources().getDisplayMetrics())
//                , binding.frame, binding.view);
//
//
////        Bitmap bitmap = rotateBitmapIfNeeded(
////                imageProxyToBitmap(image),
////                imageInfo) ;
//        // detect person pic in input image
//        InputImage inputImage = InputImage.fromBitmap(bitmap , 0);
//        FaceDetectorHelper.detectFaces(inputImage);
//
//        // finish the activity
//        passportDetails.setPassportPhoto(bitmap);
//
////        Intent returnIntent = new Intent();
////        returnIntent.putExtra(PASSPORT_RESULT,passportDetails);
////        setResult(Activity.RESULT_OK, returnIntent);
//
//
//// TODO: object detection approach
////        ObjectDetectorHelper.getObjectObservable(image,  bitmap ,bitmap)
////                .observeOn(Schedulers.computation())
////                        .subscribe();
//        finish();
////        Image mediaImage = image.getImage();
////            InputImage image1 =
////                    InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
//    }


//    @Override
//    public void onMRZDetectionSuccess(MrzRecord mrzRecord) {
//        Intent returnIntent = new Intent();
//        returnIntent.putExtra(MRZ_RESULT, mrzRecord);
//        setResult(Activity.RESULT_OK, returnIntent);
//
//        RxBus.getRecord().onNext(mrzRecord);
//        passportDetails.setMrzRecord(mrzRecord);
//        camera.captureImage(this);
//    }
//
//

//
//
//
//    @Override
//    public void onMRZDetectionError(Exception exp) {
//      // Handle Method with application strategy
//    }
//    @Override
//    public void onImageCapturingError(ImageCaptureException exception) {
//    }

}