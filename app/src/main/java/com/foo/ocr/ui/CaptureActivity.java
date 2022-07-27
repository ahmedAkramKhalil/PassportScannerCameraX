package com.foo.ocr.ui;


import static com.foo.ocr.util.ImageUtil.cropBitmapToAFrame;
import static com.foo.ocr.util.ImageUtil.cropPreviewBitmapWidth;
import static com.foo.ocr.util.ImageUtil.imageProxyToBitmap;
import static com.foo.ocr.util.ImageUtil.rotateBitmapIfNeeded;
import static com.foo.ocr.ui.MainActivity.MRZ_RESULT;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.foo.ocr.RxBus;
import com.foo.ocr.camera.CameraX;
import com.foo.ocr.R;
import com.foo.ocr.databinding.ActivityCaptureBinding;
import com.foo.ocr.ml.ObjectDetectorHelper;
import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.ml.TextRecognitionProcessor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.databinding.DataBindingUtil;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CaptureActivity extends AppCompatActivity implements TextRecognitionProcessor.IMRZDetectorResultListener, CameraX.ICaptureImageListener {

    private ActivityCaptureBinding binding;
    private PreviewView mPreviewView;
    private TextRecognitionProcessor frameProcessor;
    private CameraX camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        mPreviewView = binding.camera;
        frameProcessor = new TextRecognitionProcessor(DocType.PASSPORT, this);
        camera = CameraX.createInstance(this, mPreviewView, frameProcessor);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        camera.start();
    }

    @Override
    protected void onDestroy() {
        camera.stopCamera();
        super.onDestroy();
    }




    @Override
    public void onCaptureSuccess(ImageProxy image) {
        /**
         * The following two options are to Crop the captured image,
         * the first: is to crop it based on the overlay borders on the screen,
         * and the second is to locate the passport based on Machine leaning kit to identify objects,
         * and it was suggested to combine the two mechanisms to improve performance.
         */

        // Cropping the captured image on the borders of the Overlay on the screen
        ImageInfo imageInfo = image.getImageInfo();
        Bitmap bitmap = cropBitmapToAFrame(
                cropPreviewBitmapWidth(
                        rotateBitmapIfNeeded(
                                imageProxyToBitmap(image),
                                imageInfo),
                        getResources().getDisplayMetrics())
                , binding.frame, binding.view);
        // detect face image
        ObjectDetectorHelper.getObjectObservable(image,bitmap)
                .observeOn(Schedulers.computation())
                        .subscribe();

        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }


    @Override
    public void onMRZDetectionSuccess(MrzRecord mrzInfo) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MRZ_RESULT, mrzInfo);
        setResult(Activity.RESULT_OK, returnIntent);
        RxBus.getRecord().onNext(mrzInfo);
        camera.captureImage(this);
    }

    @Override
    public void onMRZDetectionError(Exception exp) {
      // Handle Method with application strategy
    }
    @Override
    public void onImageCapturingError(ImageCaptureException exception) {
    }

}