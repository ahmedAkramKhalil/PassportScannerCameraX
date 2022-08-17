//package com.foo.ocr.ml;
//
//import android.annotation.SuppressLint;
//import android.graphics.Bitmap;
//import android.graphics.Rect;
//import android.util.Log;
//import android.util.Size;
//
//import androidx.annotation.NonNull;
//import androidx.camera.core.ImageProxy;
//
//import com.foo.ocr.model.MlBitmap;
//import com.foo.ocr.util.ImageUtil;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.objects.DetectedObject;
//import com.google.mlkit.vision.objects.ObjectDetection;
//import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
//
//import java.util.List;
//
//
//public class ObjectDetectorHelper {
//    /**
//     * Detecting object from captured image and pass the detected object to Face Processor to crop face image.
//     * @param imageProxy returned from captured image
//     */
//    public static void detectObjectInImage(ImageProxy imageProxy,Bitmap bitmap, Bitmap bit) {
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
//                Log.d("TAG","detectedObjects = "+ detectedObjects.size() );
//                MlBitmap mlBitmap ;
//                if (detectedObjects.size()>0) {
//                    Rect boundingBox = detectedObjects.get(0).getBoundingBox();
//                    Bitmap bitmap = inputImage.getBitmapInternal();
//                    Bitmap m = ImageUtil.cropImage(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()), boundingBox);
//                    mlBitmap = new MlBitmap(m, imageProxy.getImageInfo().getRotationDegrees());
//                    mlBitmap.setBitmapDetectedByObjectDetector(true);
//
////                    FaceDetectorHelper.detectFaces(inputImage);
//
////                    FaceDetectorHelper.getObjectObservable(mlBitmap)
////                            .observeOn(Schedulers.newThread())
////                            .subscribe();
//
//
//                }
//
////                else {
////                    mlBitmap = new MlBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
////                    mlBitmap.setBitmap2(bit);
////                    mlBitmap.setBitmapDetectedByObjectDetector(false);
////
////                }
//
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d("TAG","onFailure detectedObjects ");
//                e.printStackTrace();
//            }
//        });
//    }
//    /**
//     *
//     * @param imageProxy image from cameraX directly to object processor
//     * @param processor Text recognition processor to detect MRZ text in the detected object
//     * @Note: This method is unused and written for second approach which is detecting object before
//     * detecting text to minimize detecting area for text processor and this approach needs optimization and test again.
//     */
////    public static void process(ImageProxy imageProxy,TextRecognitionProcessor processor) {
////        ObjectDetectorOptions options =
////                new ObjectDetectorOptions.Builder()
////                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
////                        .enableClassification()  // Optional
////                        .build();
////        com.google.mlkit.vision.objects.ObjectDetector objectDetector = ObjectDetection.getClient(options);
////        @SuppressLint("UnsafeOptInUsageError") InputImage inputImage =
////                InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
////        objectDetector.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
////            @Override
////            public void onSuccess(List<DetectedObject> detectedObjects) {
////                if (detectedObjects.size() > 0) {
////                    Rect boundingBox = detectedObjects.get(0).getBoundingBox();
////                    Bitmap bitmap = ImageUtil.imageProxyToBitmap(imageProxy);
////                    Bitmap m = ImageUtil.cropImage(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()), boundingBox);
////                    processor.processBitmap(m, imageProxy.getImageInfo().getRotationDegrees());
////                    MlBitmap mlBitmap = new MlBitmap(m,imageProxy.getImageInfo().getRotationDegrees());
////
////                    FaceDetectorHelper.getObjectObservable(mlBitmap)
////                            .observeOn(Schedulers.newThread())
////                            .subscribe();
////                } else {
////                    imageProxy.close();
////                }
////            }
////        }).addOnFailureListener(new OnFailureListener() {
////            @Override
////            public void onFailure(@NonNull Exception e) {
////                e.printStackTrace();
////            }
////        });
////    }
//}
