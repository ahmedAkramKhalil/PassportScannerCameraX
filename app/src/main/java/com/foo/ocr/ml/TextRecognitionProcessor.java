package com.foo.ocr.ml;

import static com.foo.ocr.util.ImageUtil.cropBitmapToAFrame;
import static com.foo.ocr.util.ImageUtil.cropPreviewBitmapWidth;
import static com.foo.ocr.util.ImageUtil.imageProxyToBitmap;
import static com.foo.ocr.util.ImageUtil.rotateBitmapIfNeeded;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzParser;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.util.BitmapUtil;
import com.foo.ocr.util.ImageUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRecognitionProcessor implements VisionImageProcessor {

    private static final String TAG = TextRecognitionProcessor.class.getName();
    private final TextRecognizer textRecognizer;
    private IMRZDetectorResultListener resultListener;
    private String scannedTextBuffer;
    private DocType docType;

    public static final String PASSPORT_REGEX_LINE_ONE = "P<(?<country>\\w{3})(?<lname>[A-Z]+)(<(?<lname2>[A-Z]+))?<<(?<fname>[A-Z]+)<(?<mname1>[A-Z]+)?<(?<mname2>[A-Z]+)?<(?<mname3>[A-Z]+)?";
    public static final String PASSPORT_REGEX_LINE_TWO = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})";

    // boolean lock for locking text processing during mrz verification
    boolean lock = false;


    /**
     TODO#:
     Detect first image with success MRZ
     Re-check face and image after getting MRZ and do Name correction processes
     In parallel detect image > then get face > insure of the image resolution
     re-correct detected name with line search methods
     */


    /**
     * Whether we should ignore process(). This is usually caused by feeding input data faster than
     * the model can handle.
     */
    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

//    public TextRecognitionProcessor(DocType docType, IMRZDetectorResultListener resultListener) {
//        this.docType = docType;
//        this.resultListener = resultListener;
//        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
//    }
    View frame ;
    View overlay;
    Activity context ;
    public TextRecognitionProcessor(DocType docType, IMRZDetectorResultListener resultListener,View frame,View overlay,Activity context) {
        this.docType = docType;
        this.resultListener = resultListener;
        this.overlay = overlay ;
        this.frame = frame ;
        this.context =context ;
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
    }

    /**
     * Process Method to process bitmap from still image
     */
    @Override
    public void processBitmap(Bitmap bitmap, int rotation) {
        if (bitmap != null) {
            InputImage image = InputImage.fromBitmap(bitmap, rotation);
            detectInImage(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text results) {
                                    Log.d("LOOG", "LOOG>>__>>");
                                    TextRecognitionProcessor.this.onSuccess(results);
                                }
                            })

                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    TextRecognitionProcessor.this.onFailure(e);
                                }
                            });
        }
    }

    /**
     * Process image from cameraX source
     */

     long time1 = 0 ;
     long time2 = 0 ;
    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void processImageProxy(ImageProxy imageProxy) throws MlKitException {
//       byte[] a =  ImageUtil.cropImageProxyToCropRect(imageProxy);
//        InputImage image = InputImage.fromByteArray(
//                a,
//                /* image width */imageProxy.getWidth(),
//                /* image height */imageProxy.getHeight(),
//                imageProxy.getImageInfo().getRotationDegrees(),
//                InputImage.IMAGE_FORMAT_YV12 // or IMAGE_FORMAT_YV12
//        );
//        time1 = System.nanoTime();

//       Thread thread =  new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("LOOG","getCropRect  width = " + imageProxy.getCropRect().width() + " H= " + imageProxy.getCropRect().height() );
//                Log.d("LOOG","imageProxy  width = " + imageProxy.getWidth() + " H= " + imageProxy.getHeight() );

//        if (mediaImage != null && mediaImage.getFormat() == ImageFormat.YUV_420_888) {

                // *********
//        int w = imageProxy.getCropRect().width() ;
//        int h = (w * 16)/ 9 ;
//                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
//              byte[] array =   ImageUtil.croppedNV21(mediaImage, imageProxy.getCropRect());
//
//              if (array == null){
//                  imageProxy.close();
//                  return;
//              }
//                InputImage inputImage = InputImage.fromByteArray(array, imageProxy.getCropRect().width(),w,
//                        imageProxy.getImageInfo().getRotationDegrees(), InputImage.IMAGE_FORMAT_YV12);
//                imageProxy.close();
        //*********


//              Bitmap bitmap =   ImageUtil.cropImage(imageProxy,imageProxy.getImageInfo().getRotationDegrees(),imageProxy.getCropRect().left,imageProxy.getCropRect().top,imageProxy.getCropRect().width(),imageProxy.getCropRect().height());
//            Bitmap bitmap =  ImageUtil.cropBitmapToAFrame(ImageUtil.toBitmap(imageProxy),frame,overlay);
//        ImageInfo imageInfo = imageProxy.getImageInfo();
//        Bitmap resultBitmap = rotateBitmapIfNeeded(
//                ImageUtil.toBitmap(imageProxy),
//                imageInfo);
//
//
//
//          Bitmap image = cropBitmapToAFrame(
//                cropPreviewBitmapWidth(
//                        resultBitmap,
//                        context.getResources().getDisplayMetrics())
//                , frame
//                , overlay);
//          imageProxy.close();


                Bitmap image = BitmapUtil.getBitmap(imageProxy);
             if (image == null){
                 imageProxy.close();
                 return;
             }
//             Log.d("BIT","bitmap  ?? " + resultBitmap.getWidth() +" + " + resultBitmap.getHeight());
//             Log.d("BIT","image  ?? " + image.getWidth() +" + " + image.getHeight());
//             Log.d("BIT","imageProxy ?? " + imageProxy.getWidth() +" + " + imageProxy.getHeight());
//             Log.d("BIT","imageProxy ?? " + imageProxy.getCropRect().width() +" + " + imageProxy.getCropRect().height());
              InputImage inputImage = InputImage.fromBitmap(image, imageProxy.getImageInfo().getRotationDegrees());



//                Log.d("LOOG"," TIME = " + (System.nanoTime() - time1));
//            Bitmap b = ImageUtil.getBitmap(imageProxy);
//            Log.d("LOOG", "BITMap SIZe " + inputImage.getWidth() + " " + inputImage.getHeight());

//            InputImage inputImage = InputImage.fromBitmap(b, imageProxy.getImageInfo().getRotationDegrees());

        //***********
//        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
//        if (mediaImage != null) {
//            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            //***********

                detectInImage(inputImage)
                        .addOnSuccessListener(
                                new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text results) {

                                        shouldThrottle.set(false);
                                        TextRecognitionProcessor.this.onSuccess(results);
                                        // imageProxy should be closed after processing to allow next frame
                                        imageProxy.close();
                                    }
                                })

                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        shouldThrottle.set(false);
                                        TextRecognitionProcessor.this.onFailure(e);
                                        // imageProxy should be closed after processing to allow next frame
                                        imageProxy.close();
                                    }
                                });
        }
//
//        else {
//            imageProxy.close();
//            Log.d("LOOG", ">>>>>>>>>>>>>>>>");
//        }

//            }
//        });
//       thread.start();
////        Log.d("LOOG","processImageProxy");

//        }


        public void stop () {
            textRecognizer.close();

        }


        protected Task<Text> detectInImage (InputImage image){
            return textRecognizer.process(image);
        }


        protected void onSuccess (@NonNull Text results){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    scannedTextBuffer = "";
                    List<Text.TextBlock> blocks = results.getTextBlocks();
                    for (int i = 0; i < blocks.size(); i++) {
                        List<Text.Line> lines = blocks.get(i).getLines();
                        for (int j = 0; j < lines.size(); j++) {
                            List<Text.Element> elements = lines.get(j).getElements();
                            for (int k = 0; k < elements.size(); k++) {
                                filterScannedText(elements.get(k));
                            }
                        }
                    }
                }
            }).start();
        }

        private void filterScannedText (Text.Element element){
            scannedTextBuffer += element.getText();
            Log.d("TAG", "filterScannedText " + scannedTextBuffer);

            if (!lock)
                if (docType == DocType.PASSPORT) {
                    // Compile and matching MRZ Text using REGEX
                    Pattern patternPassportTD3Line1 = Pattern.compile(PASSPORT_REGEX_LINE_ONE);
                    Matcher matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer);
                    Pattern patternPassportTD3Line2 = Pattern.compile(PASSPORT_REGEX_LINE_TWO);
                    Matcher matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer);
                    if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
                        String line2 = matcherPassportTD3Line2.group(0);
                        String line1 = matcherPassportTD3Line1.group(0);
                        // P<PSEELSHEIKHKHALIL<<ANAS<A<I<<4667050<<6PSE8710042M2309154801131327<<<<<84
                        try {
                            // MRZ first line detection does not detect remain '<' character so we should fill it with '<' to match  the correctMRZ length
                            if (line1.length() < 44) {
                                int l1 = line1.length();
                                for (int i = 0; i < (44 - l1); i++) {
                                    line1 = line1 + "<";
                                }
                            }
                            lock = true;
                            // Combine two lines and check MRZ validation and finish scanning
                            finishScanning(MrzParser.parse(line1 + '\n' + line2));
                        } catch (Exception ex) {
                            lock = false;
                            ex.printStackTrace();
                        }
                    }
                }
        }

        protected void onFailure (@NonNull Exception e){
            Log.w(TAG, "Text detection failed." + e);
            resultListener.onMRZDetectionError(e);
        }

        private void finishScanning ( final MrzRecord mrzInfo){
            try {
                Log.d("LOOG","finishScanning");
                resultListener.onMRZDetectionSuccess(mrzInfo);
//            stop();
            } catch (Exception exp) {
                exp.printStackTrace();
                Log.d(TAG, "MRZ DATA is not valid");
            }
        }


        public interface IMRZDetectorResultListener {
            void onMRZDetectionSuccess(MrzRecord mrzInfo);

            void onMRZDetectionError(Exception exp);
        }
    }

