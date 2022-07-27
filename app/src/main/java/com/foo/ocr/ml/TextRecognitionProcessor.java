package com.foo.ocr.ml;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzParser;
import com.foo.ocr.mrzdecoder.MrzRecord;
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

    public TextRecognitionProcessor(DocType docType, IMRZDetectorResultListener resultListener) {
        this.docType = docType;
        this.resultListener = resultListener;
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());
    }

    /**
     *  Process Method to process bitmap from still image
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
     *  Process image from cameraX source
     */
    //
    @Override
    public void processImageProxy(ImageProxy imageProxy) throws MlKitException {
        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detectInImage(image)
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
        if (docType == DocType.PASSPORT) {

            // Compile and matching MRZ Text using REGEX
            Pattern patternPassportTD3Line1 = Pattern.compile(PASSPORT_REGEX_LINE_ONE);
            Matcher matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer);
            Pattern patternPassportTD3Line2 = Pattern.compile(PASSPORT_REGEX_LINE_TWO);
            Matcher matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer);

            if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
                String line2 = matcherPassportTD3Line2.group(0);
                String line1 = matcherPassportTD3Line1.group(0);

                try {
                    // MRZ first line detection not detect remain '<' character so we should fill it with '<' to match  the correctMRZ length
                    if (line1.length() < 44) {
                        int l1 = line1.length();
                        for (int i = 0; i < (44 - l1); i++) {
                            line1 = line1 + "<";
                        }
                    }
                    // Combine two lines and check MRZ validation and finish scanning
                    finishScanning(MrzParser.parse(line1 + '\n' + line2));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
        resultListener.onMRZDetectionError(e);
    }

    private void finishScanning(final MrzRecord mrzInfo) {
        try {
            resultListener.onMRZDetectionSuccess(mrzInfo);
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

