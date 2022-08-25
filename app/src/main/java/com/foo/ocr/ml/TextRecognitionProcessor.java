package com.foo.ocr.ml;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzParser;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.util.BitmapUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextRecognitionProcessor implements VisionImageProcessor {

    private static final String TAG = TextRecognitionProcessor.class.getName();
    private final TextRecognizer textRecognizer;
    private IMRZDetectorResultListener resultListener;
    private String scannedTextBuffer;
    private DocType docType;

//    public static final String PASSPORT_REGEX_LINE_ONE = "P<(?<country>\\w{3})(?<lname>[A-Z]+)(<(?<lname2>[A-Z]+))?<<(?<fname>[A-Z]+)<(?<mname1>[A-Z]+)?<(?<mname2>[A-Z]+)?<(?<mname3>[A-Z]+)?";
    public static final String PASSPORT_REGEX_LINE_ONE = "P<(?<country>\\w{3})(?<lname>[A-Z]+)(<(?<lname2>[A-Z]+))?<<(?<fname>[A-Z]+)<(?<mname1>[A-Z]+)?<(?<mname2>[A-Z]+)?(?<mname3>[A-Z]+)?";

    public static final String PASSPORT_REGEX_LINE_TWO = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})";

    // boolean lock for locking text processing during mrz verification
    boolean lock = false;
    List<String> scanningText ;


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

    View frame;
    View overlay;
    Activity context;

    public TextRecognitionProcessor(DocType docType, IMRZDetectorResultListener resultListener, View frame, View overlay, Activity context) {
        this.docType = docType;
        this.resultListener = resultListener;
        this.overlay = overlay;
        this.frame = frame;
        this.context = context;
        scanningText = new ArrayList<>();
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
     *
     */
    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void processImageProxy(ImageProxy imageProxy) throws MlKitException {

        Bitmap image = BitmapUtil.getBitmap(imageProxy);
        if (image == null) {
            imageProxy.close();
            return;
        }
        InputImage inputImage = InputImage.fromBitmap(image, imageProxy.getImageInfo().getRotationDegrees());

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

    public void stop() {
        textRecognizer.close();
    }


    protected Task<Text> detectInImage(InputImage image) {
        return textRecognizer.process(image);
    }


    protected void onSuccess(@NonNull Text results) {
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

    private void filterScannedText(Text.Element element) {
        scannedTextBuffer += element.getText();
        Log.d("TAG", "filterScannedText " + scannedTextBuffer);

//        if (!lock)
            if (docType == DocType.PASSPORT) {
                // Compile and matching MRZ Text using REGEX
                Pattern patternPassportTD3Line1 = Pattern.compile(PASSPORT_REGEX_LINE_ONE);
                Matcher matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer);
                Pattern patternPassportTD3Line2 = Pattern.compile(PASSPORT_REGEX_LINE_TWO);
                Matcher matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer);
                if (matcherPassportTD3Line1.find()){
                    String line1 = matcherPassportTD3Line1.group(0);
                    scanningText.add(line1);
                    Log.d("TAG", "filterScannedText line1 " + findMostDuplicates(scanningText).toString());

                }
                if ( matcherPassportTD3Line2.find()) {
//                if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {

                    String line2 = matcherPassportTD3Line2.group(0);
//                    String line1 = matcherPassportTD3Line1.group(0);
                     String line1 = getLine1(findMostDuplicates(scanningText));
                     if (line1 == null)
                         return;
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

    Map<String, Long> findMostDuplicates(List<String> scanningText){
        return scanningText.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    String getLine1( Map<String, Long> map){
        final Long[] maxNum = {0L};
        final String[] maxString = {""};

        map.keySet().forEach(s -> {
            Long i = map.get(s) ;
            if (i > maxNum[0]){
                maxNum[0] = i ;
                maxString[0] = s ;
            }
        });
        Log.d("TAG", "filterScannedText line1  " +  maxString[0]);

        if (maxNum[0] < 5){
            return null;
        }
        return maxString[0];
    }

    protected void onFailure(@NonNull Exception e) {
        Log.d("FAIL", "Text detection failed." + e);
        resultListener.onMRZDetectionError(e);
    }

    private void finishScanning(final MrzRecord mrzInfo) {
        try {
            Log.d("LOOG", "finishScanning");
            resultListener.onMRZDetectionSuccess(mrzInfo);
//            stop();
        } catch (Exception exp) {
            exp.printStackTrace();
            Log.d("FAIL", "MRZ DATA is not valid");
        }
    }

    public interface IMRZDetectorResultListener {
        void onMRZDetectionSuccess(MrzRecord mrzInfo);

        void onMRZDetectionError(Exception exp);
    }
}

