package com.foo.ocr.ml;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.foo.ocr.StateLiveData;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.util.ImageUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FaceDetectorHelper {

    /**
     * Detecting Face Image in Bitmap  using mlKit detector and cropping face area with expanded boundary to get full image .
     *
     * @param mlBitmap a bitmap with device rotation degree return from camera .
     */


    /**
     * @return PassportDetails to subscriber after face detection process
     */

    /**
     * @param mlBitmap a bitmap with device rotation degree return from camera .
     * @return Observable to run face detection in another thread
     *
     */

    private static Bitmap bitmap;
    private static Canvas canvas;
    private static Paint dotPaint, linePaint;
    private static boolean isRotated = false;

     StateLiveData<PassportDetails> passportDetailsMutableLiveData  ;

     ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    Handler mHandler = new Handler(Looper.getMainLooper());


    public  StateLiveData<PassportDetails> detectFaces(InputImage image) {
        passportDetailsMutableLiveData  = new StateLiveData<>() ;
//      Runnable backgroundThread =   new Runnable(){
//            @Override
//            public void run() {
                initDrawingUtils(image);
                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                .setMinFaceSize(0.01f)
                                .enableTracking()
                                .build();
                // [END set_detector_options]
                // [START get_detector]
                FaceDetector detector = FaceDetection.getClient(options);
                // [END get_detector ]
                //[START run_detector]
        Log.d("LOOG", "StateLiveData<PassportDetails> detectFaces");

        Task<List<Face>> result =
                        detector.process(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<Face>>() {
                                            @RequiresApi(api = Build.VERSION_CODES.Q)
                                            @Override
                                            public void onSuccess(List<Face> faces) {
                                                Log.d("LOOG", "StateLiveData<PassportDetails> onSuccess");
                                                if (!faces.isEmpty()) {
                                                    Log.d("LOOG", "StateLiveData<PassportDetails> isEmpty");

                                                    Bitmap map = draw(faces, image);
                                                    PassportDetails passportDetails = new PassportDetails(map, image.getBitmapInternal());
//                                            iFaceDetectorResultListeners.forEach(l -> l.onFaceDetected(passportDetails));
//                                            passportDetailsMutableLiveData.postValue(passportDetails);
                                                    passportDetailsMutableLiveData.postSuccess(passportDetails);
//                                                    passportDetailsMutableLiveData.postComplete();
                                                } else {
                                                    // Handel detection failure
//                                                    passportDetailsMutableLiveData.postError(new Exception("No Face Detected"));
//                                            iFaceDetectorResultListeners.forEach(l -> l.onFaceDetectionFailed(new Exception("No Face Detected")));
                                                }

                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @RequiresApi(api = Build.VERSION_CODES.N)
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // should handle face detection fail
                                                passportDetailsMutableLiveData.postError(e);
                                            }
                                        });
//            }
//        };
//        mExecutor.execute(backgroundThread);
        return passportDetailsMutableLiveData;
    }




     Bitmap draw(List<Face> faces, InputImage image) {
        Bitmap bitmaps = image.getBitmapInternal();
        Bitmap bitmap1 = bitmaps;
        Bitmap bitmap2 = bitmaps;
        Bitmap resultingImage = Bitmap.createBitmap(image.getBitmapInternal().getWidth(), image.getBitmapInternal().getHeight(), bitmap1.getConfig());
        Canvas canvas = new Canvas(resultingImage);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //     crop the  filled  contour
        Paint polyPaint = new Paint();
        polyPaint.setColor(0xFF5555ee);
        polyPaint.setStyle(Paint.Style.FILL);
        polyPaint.setAntiAlias(true);
        Face face = faces.get(0);
        RectF rect;
//        Rect rect;

        rect = expandFaceRect(face.getBoundingBox(),  0.05f,  0.2f,  0.05f,  0.05f);
//        rect = expandFaceRect(face.getBoundingBox(), isRotated ? 0f : 0.2f, isRotated ? 0.2f : 0.2f, isRotated ? 0f : 0.1f, isRotated ? 0.1f : 0.1f);
//        rect = face.getBoundingBox();
        canvas.drawRect(rect, polyPaint);
        canvas.drawBitmap(bitmap2, 0, 0, paint);
        return ImageUtil.createBitmap(resultingImage, (int) (rect.left < 0 ? 0 : rect.left), (int) (rect.top < 0 ? 0 : rect.top), (int) rect.width(), (int) rect.height());
    }



    public  void initDrawingUtils(InputImage image) {
        bitmap = Bitmap.createBitmap(image.getBitmapInternal().getWidth(),
                image.getBitmapInternal().getHeight(),
                Bitmap.Config.ARGB_8888);
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

    static RectF expandFaceRect(Rect rect, float leftR, float topR, float rightR, float bottomR) {
        return new RectF((rect.left - rect.left * leftR), (rect.top - rect.top * topR), (rect.right + rect.right * rightR), (rect.bottom + rect.bottom * bottomR));
    }

}
