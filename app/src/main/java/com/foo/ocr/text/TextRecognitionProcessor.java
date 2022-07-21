package com.foo.ocr.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.foo.ocr.model.DocType;
import com.foo.ocr.mrzdecoder.MrzParseException;
import com.foo.ocr.mrzdecoder.MrzParser;
import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.model.DocType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import net.sf.scuba.data.Gender;

import org.jmrtd.lds.icao.MRZInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRecognitionProcessor  {

    private static final String TAG = TextRecognitionProcessor.class.getName();

    private final TextRecognizer textRecognizer;

    private ResultListener resultListener;

    private String scannedTextBuffer;

    private DocType docType;

    public static final String TYPE_PASSPORT = "P<";

    public static final String TYPE_ID_CARD = "I<";

    public static final String ID_CARD_TD_1_LINE_1_REGEX = "([A|C|I][A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{31})";

    public static final String ID_CARD_TD_1_LINE_2_REGEX = "([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9]{1})";
    public static final String ID_CARD_TD_1_LINE_2_REGEX_2 = "([0-9]{6})([0-9]{1})([M|F|X|>]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9]{1})";

    public static final String ID_CARD_TD_1_LINE_3_REGEX = "([A-Z0-9<]{30})";

    public static final String PASSPORT_TD_3_LINE_1_REGEX = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})";
    public static final String PASSPORT_TD_3_LINE_1_REGEX_1 = "P<(?<country>\\w{3})(?<lname>[A-Z]+)(<(?<lname2>[A-Z]+))?<<(?<fname>[A-Z]+)<(?<mname1>[A-Z]+)?<(?<mname2>[A-Z]+)?<(?<mname3>[A-Z]+)?";
    public static final String PASSPORT_TD_3_LINE_1_REGEX_5 = "P<(w{3})([A-Z]+)([A-Z]+))?<<([A-Z]+)<([A-Z]+)?<(?<mname2>[A-Z]+)?<(?<mname3>[A-Z]+)?";
    public static final String PASSPORT_TD_3_LINE_1_REGEX_2 =  "P<(?<country>\\w{3})(?<lname>[A-Z]+)(<(?<lname2>[A-Z]+))?<<(?<fname>[A-Z]+)<(?<mname1>[A-Z]+)?<(?<mname2>[A-Z]+)?<(?<mname3>[A-Z]+)?";
    public static final String PASSPORT_TD_3_LINE_2_REGEX = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})";
    public static final String PASSPORT_TD_3_LINE_2_REGEX_1 = "(?<pnum>[\\dA-Z]{9}).(?<nat>\\w{3})(?<yob>\\d{2})(?<mob>\\d{2})(?<dob>\\d{2}).(?<sex>M|F|<{1})(?<yoe>\\d{2})(?<moe>\\d{2})(?<doe>\\d{2}).(?<prnum>[]\\d|<A-Z]{16})";

    // Whether we should ignore process(). This is usually caused by feeding input data faster than
    // the model can handle.
    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);
//       StringResponse lineOneResponse;
//       StringResponse lineTwoResponse;
    public TextRecognitionProcessor(Context context, DocType docType, ResultListener resultListener) {
//         super(context);
        this.docType = docType;
        this.resultListener = resultListener;
//        lineOneResponse = new StringResponse();
//        lineTwoResponse = new StringResponse();


//        textRecognizer = TextRecognition.getClient();
//        TextRecognizerOptions options = new TextRecognizerOptions();

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    //region ----- Exposed Methods -----



    public void stop() {
        textRecognizer.close();
    }



    //endregion

    //region ----- Helper Methods -----
    // Hello, I hope you're well.
    // this is the result of the test that I did on the application using both orientation modes  after adding the camera improvements, the result is based on the time estimation of  capturing  MRZ codes ( from the start of the camera until the success of the capture (without errors) using the same device and passport situation), and this is the demo app, I added the both orientation modes to allow testing the cases.
    // *time in chart is in Millis
    // the result is, capturing in portrait mode is more bit better than landscape , maybe equale but the portrait mode is more better in user experiance in usage because its not requiring rotation form the user and there an estimated time in changing app orintation (not long time but noticed)



    // Hello, I hope you are doing well.
    // This is the result of the test I did on the app using both modes of orientation after adding camera improvements and fixing auto focusing, the result depends on the time taken to capture MRZ codes (from start of camera to successful capture (without errors) using the same device and passport status), this is the demo app, I added both routing modes to allow testing of states.
    // * Time in graph in millis

    // The result is that the capture in portrait mode is a little better than landscape, maybe even but portrait mode is better in user experience of use because it does not require rotating the phone from the user and there is time taken to change the orientation of the app (not long but noticeable)

    protected Task<Text> detectInImage(InputImage image) {
        return textRecognizer.process(image);
    }
     public void process(ImageProxy imageProxy){
        detectInVisionImage(imageProxy);
     }


    protected void onSuccess(@NonNull Text results) {
//        graphicOverlay.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
        scannedTextBuffer = "";
        List<Text.TextBlock> blocks = results.getTextBlocks();
//        Log.d("LOOG", "TIME! 1 " + System.currentTimeMillis());
        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    filterScannedText( elements.get(k));
                }
            }
        }
            }
        }).start();
//        Log.d("LOOG", "TIME! 2 " + System.currentTimeMillis());

    }

    private void filterScannedText( Text.Element element ) {
//        GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, element, Color.GREEN);
        scannedTextBuffer += element.getText();

//        Log.d("LOOG " ,element.getText());

        if(docType == DocType.ID_CARD) {

//            Pattern patternIDCardTD1Line1 = Pattern.compile(ID_CARD_TD_1_LINE_1_REGEX);
//            Matcher matcherIDCardTD1Line1 = patternIDCardTD1Line1.matcher(scannedTextBuffer);
//
//            Pattern patternIDCardTD1Line2 = Pattern.compile(ID_CARD_TD_1_LINE_2_REGEX);
//            Matcher matcherIDCardTD1Line2 = patternIDCardTD1Line2.matcher(scannedTextBuffer);
//
//            if(matcherIDCardTD1Line1.find() && matcherIDCardTD1Line2.find()) {
//                graphicOverlay.add(textGraphic);
//                String line1 = matcherIDCardTD1Line1.group(0);
//                String line2 = matcherIDCardTD1Line2.group(0);
//                if (line1.indexOf(TYPE_ID_CARD) > 0) {
//                    line1 = line1.substring(line1.indexOf(TYPE_ID_CARD));
//                    String documentNumber = line1.substring(5, 14);
//                    documentNumber = documentNumber.replace("O", "0");
//                    String dateOfBirthDay = line2.substring(0, 6);
//                    String expiryDate = line2.substring(8, 14);
//
//                    Log.d(TAG, "Scanned Text Buffer ID Card ->>>> " + "Doc Number: " + documentNumber + " DateOfBirth: " + dateOfBirthDay + " ExpiryDate: " + expiryDate);
//
//                    MRZInfo mrzInfo = buildTempMrz(documentNumber, dateOfBirthDay, expiryDate);
//                    if (mrzInfo != null)
//                        finishScanning(mrzInfo,frameMetadata);
//                }
//            }
        } else if (docType == DocType.PASSPORT) {

            Pattern patternPassportTD3Line1 = Pattern.compile(PASSPORT_TD_3_LINE_1_REGEX_1);
            Matcher matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer);



            Pattern patternPassportTD3Line2 = Pattern.compile(PASSPORT_TD_3_LINE_2_REGEX);
            Matcher matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer);
              Log.d("LOOG" , "LOGS" + matcherPassportTD3Line1.toString());
            if(matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
                String line2 = matcherPassportTD3Line2.group(0);
                String line1 = matcherPassportTD3Line1.group(0);
                //#TODO: repair these statements
                    try {
                        if (line1.length()< 44){
                            int l1 = line1.length();
                            int l2 = 44;
                            for (int i = 0 ; i < (44 - l1); i++){
                                line1 = line1 + "<";
                            }

                        }
//                        Log.d("LOOG",line1 + '\n' + line2);

                        final MrzRecord record = MrzParser.parse(line1 + '\n' + line2);
                        finishScanning(record);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (ex instanceof MrzParseException) {
//                            final MrzParseException mpe = (MrzParseException) ex;
//                            final MrzRange r = mpe.range;
                        }
                    }
//                }



//                if (lineOneResponse.addString( line1) != null && lineTwoResponse.addString( line2) !=null){
//                    Log.d("Scanned", " success  line 1 :" + lineOneResponse.getResult());
//                    Log.d("Scanned", " success line 2 :" + lineTwoResponse.getResult());
//
//                }

            }
        }
    }

    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
        resultListener.onError(e);
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal) {

        if (bmpOriginal != null) {

            int width, height;
            height = bmpOriginal.getHeight();
            width = bmpOriginal.getWidth();

            Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmpGrayscale);
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            paint.setColorFilter(f);
            c.drawBitmap(bmpOriginal, 0, 0, paint);
            return bmpGrayscale;
        }
        return null;
    }



    Rect middleArea(int areaPer1000){
       return new Rect(-areaPer1000, -areaPer1000, areaPer1000, areaPer1000);
    }
    private void detectInVisionImage(ImageProxy imageProxy) {

//        imageProxy.setCropRect(middleArea(400));
//        imageProxy.getPlanes()[0].getBuffer();
        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());



                        detectInImage(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<Text>() {
                                            @Override
                                            public void onSuccess(Text results) {
                                                shouldThrottle.set(false);
                                                Log.d("LOOG", "LOOG>>__>>");
                                                TextRecognitionProcessor.this.onSuccess(results);
                                                imageProxy.close();

                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                shouldThrottle.set(false);
                                                TextRecognitionProcessor.this.onFailure(e);
                                                imageProxy.close();

                                            }
                                        });



                // Begin throttling until this frame of input has been processed, either in onSuccess or
                // onFailure.
//                shouldThrottle.set(true);

        }
    }


    private void finishScanning(final MrzRecord mrzInfo) {
        try {
                Log.d("Scanned","isMrzValid");
                // Delay returning result 1 sec. in order to make mrz text become visible on graphicOverlay by user
                // You want to call 'resultListener.onSuccess(mrzInfo)' without no delay
//                new Handler().postDelayed(() -> resultListener.onSuccess(mrzInfo), 10);
            resultListener.onSuccess(mrzInfo);


        } catch(Exception exp) {
            exp.printStackTrace();
            Log.d("Scanned","MRZ DATA is not valid");

            Log.d(TAG, "MRZ DATA is not valid");
        }
    }

    private MRZInfo buildTempMrz(String documentNumber, String dateOfBirth, String expiryDate) {
        MRZInfo mrzInfo = null;
        try {
            mrzInfo = new MRZInfo("P","NNN", "", "", documentNumber, "NNN", dateOfBirth, Gender.UNSPECIFIED, expiryDate, "");
        } catch (Exception e) {
            Log.d(TAG, "MRZInfo error : " + e.getLocalizedMessage());
        }

        return mrzInfo;
    }

    private boolean isMrzValid(MRZInfo mrzInfo) {
        return mrzInfo.getDocumentNumber() != null && mrzInfo.getDocumentNumber().length() >= 7 &&
                mrzInfo.getDateOfBirth() != null && mrzInfo.getDateOfBirth().length() == 6 &&
                mrzInfo.getDateOfExpiry() != null && mrzInfo.getDateOfExpiry().length() == 6;


    }

    public interface ResultListener {
        void onSuccess(MRZInfo mrzInfo);
        void onSuccess(MRZInfo mrzInfo,InputImage frameMetadata);
        void onSuccess(MrzRecord mrzInfo);
        void onError(Exception exp);
    }
}

