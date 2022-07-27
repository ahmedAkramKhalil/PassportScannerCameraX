package com.foo.ocr.ml;

import android.graphics.Bitmap;
import android.util.Size;

import androidx.annotation.NonNull;

import com.foo.ocr.model.MlBitmap;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.util.ImageUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class FaceDetectorHelper {
    public static final BehaviorSubject<PassportDetails> behaviorSubject = BehaviorSubject.create();

    /**
     * Detecting Face Image in Bitmap  using mlKit detector and cropping face area with expanded boundary to get full image .
     *
     * @param mlBitmap a bitmap with device rotation degree return from camera .
     */
    public static void detectFaceInImage(MlBitmap mlBitmap) {
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        InputImage image = InputImage.fromBitmap(mlBitmap.getBitmap(), mlBitmap.getRotationDegree());
        com.google.mlkit.vision.face.FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        if (faces.size() > 0) {
                                            // The captured image shouldn't contains more than one Face
                                            Face face = faces.get(0);
                                            // Cropping detected face with expanded boundary to get full image
                                            Bitmap m = ImageUtil.cropImageWithBoundary(mlBitmap.getBitmap(),
                                                    new Size(mlBitmap.getBitmap().getWidth(),
                                                            mlBitmap.getBitmap().getHeight()),
                                                                       face.getBoundingBox() , mlBitmap.isBitmapDetectedByObjectDetector());
                                            // publish detected and cropped face with full image of passport it subscriper
                                            PassportDetails passportDetails = new PassportDetails(m, mlBitmap.getBitmap());
                                            behaviorSubject.onNext(passportDetails);
//                                            behaviorSubject.onComplete();

                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                        // Task failed with an exception
                                    }});
    }


    /**
     * @return PassportDetails to subscriber after face detection process
     */
    public static BehaviorSubject<PassportDetails> getFaceDetectorObserver() {
        return behaviorSubject;
    }

    /**
     * @param mlBitmap a bitmap with device rotation degree return from camera .
     * @return Observable to run face detection in another thread
     */
    public static Observable<Object> getObjectObservable(MlBitmap mlBitmap) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<Object> emitter) throws Throwable {
                detectFaceInImage(mlBitmap);
            }
        });
    }

}
