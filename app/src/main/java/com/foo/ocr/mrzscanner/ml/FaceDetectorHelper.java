package com.foo.ocr.mrzscanner.ml;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.foo.ocr.mrzscanner.StateLiveData;
import com.foo.ocr.mrzscanner.model.PassportDetails;
import com.foo.ocr.mrzscanner.util.BitmapUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;


public class FaceDetectorHelper {

    /**
     * Detecting Face Image in Bitmap  using mlKit detector and cropping face area with expanded boundary to get full image .
     * @param image an InputImage returned from camera stream or converted bitmap.
     */

    private static Bitmap bitmap;
    private static Canvas canvas;
    private static Paint dotPaint, linePaint;
    private StateLiveData<PassportDetails> passportDetailsMutableLiveData;

    public StateLiveData<PassportDetails> detectFaces(InputImage image) {
        passportDetailsMutableLiveData = new StateLiveData<>();
        initFaceDrawingUtils(image);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);

        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @RequiresApi(api = Build.VERSION_CODES.Q)
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        if (!faces.isEmpty()) {
                                            Bitmap map = drawFaceArea(faces, image);
                                            PassportDetails passportDetails = new PassportDetails(map, image.getBitmapInternal());
                                            passportDetailsMutableLiveData.postSuccess(passportDetails);
                                        } else {
                                            /** Handel detection failure -> reloading passport live data */
                                            passportDetailsMutableLiveData.postLoading();
                                        }

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @RequiresApi(api = Build.VERSION_CODES.N)
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        /** should handle face detection fail */
                                        passportDetailsMutableLiveData.postError(e);
                                    }
                                });
        return passportDetailsMutableLiveData;
    }


    private Bitmap drawFaceArea(List<Face> faces, InputImage image) {
        Bitmap bitmaps = image.getBitmapInternal();
        Bitmap bitmap1 = bitmaps;
        Bitmap bitmap2 = bitmaps;
        Bitmap resultingImage = Bitmap.createBitmap(image.getBitmapInternal().getWidth(), image.getBitmapInternal().getHeight(), bitmap1.getConfig());
        Canvas canvas = new Canvas(resultingImage);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Paint polyPaint = new Paint();
        polyPaint.setColor(0xFF5555ee);
        polyPaint.setStyle(Paint.Style.FILL);
        polyPaint.setAntiAlias(true);
        Face face = faces.get(0);
        RectF rect;
        rect = expandFaceRect(face.getBoundingBox(), 0.06f, 0.05f, 0.05f, 0.04f);
        canvas.drawRect(rect, polyPaint);
        canvas.drawBitmap(bitmap2, 0, 0, paint);
        return BitmapUtil.createBitmap(resultingImage, (int) (rect.left < 0 ? 0 : rect.left), (int) (rect.top < 0 ? 0 : rect.top), (int) rect.width(), (int) rect.height());
    }


    private void initFaceDrawingUtils(InputImage image) {
        bitmap = Bitmap.createBitmap(image.getBitmapInternal().getWidth(),image.getBitmapInternal().getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(2f);
        dotPaint.setAntiAlias(true);
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
    }


    private  RectF expandFaceRect(Rect rect, float leftR, float topR, float rightR, float bottomR) {
        return new RectF((rect.left - rect.left * leftR), (rect.top - rect.top * topR), (rect.right + rect.right * rightR), (rect.bottom + rect.bottom * bottomR));
    }

}
