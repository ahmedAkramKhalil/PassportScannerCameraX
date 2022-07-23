package com.foo.ocr;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.AspectRatio;

public class Image {


    int aspectRatio(int width, int height) {
        int previewRatio = Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - AspectRatio.RATIO_4_3) <= Math.abs(previewRatio - AspectRatio.RATIO_16_9)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public  static Bitmap cropImage(Bitmap fullImage, Size previewSize, Rect cardFinder) {
        Rect scaledPreviewImage =  scaleAndCenterWithin(previewSize,fullImage.getWidth(),fullImage.getHeight()) ;
        float previewScale = (float)scaledPreviewImage.width() /previewSize.getWidth();

        // Scale the cardFinder to match the scaledPreviewImage
        Log.d("LOOG","scaledPreviewImage.width() -- " + scaledPreviewImage.width());
        Log.d("LOOG","previewSize.getWidth() -- " + previewSize.getWidth());
        Log.d("LOOG","previewScale -- " + previewScale);
        Log.d("LOOG","previewScale>>  -- " + previewSize.toString() + " cardFinder  " + cardFinder.toString() + " fullImage.getWidth " + fullImage.getWidth() + "  fullImage.getHeight "+ fullImage.getHeight());

                Rect scaledCardFinder = new Rect(
                Math.round(cardFinder.left * previewScale),
                Math.round(cardFinder.top * previewScale),
                Math.round(cardFinder.right * previewScale),
                Math.round(cardFinder.bottom * previewScale)
        );
        // Position the scaledCardFinder on the fullImage
        Rect cropRect = new Rect(
                Math.max(0, scaledCardFinder.left + scaledPreviewImage.left),
                Math.max(0, scaledCardFinder.top + scaledPreviewImage.top),
                Math.min(fullImage.getWidth(), scaledCardFinder.right + scaledPreviewImage.left),
                Math.min(fullImage.getHeight(), scaledCardFinder.bottom + scaledPreviewImage.top)
        );
        Log.d("LOOG"," -- " + cropRect.left+" -- " + cropRect.top+" -- " + cropRect.width()+" -- " + cropRect.height() ) ;
        return Bitmap.createBitmap(fullImage, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());

    }

    static Rect scaleAndCenterWithin(Size containingSize, int width, int height) {
        Log.d("LOOG","scaleAndCenterWithin width -- " + width + " height " + height);
        float aspectRatio = (float) width / (float) height;
        // Since the preview image may be at a different resolution than the full image, scale the
        // preview image to be circumscribed by the fullImage.

        Size scaledSize = maxAspectRatioInSize(containingSize, aspectRatio);
        int left = (containingSize.getWidth() - scaledSize.getWidth()) / 2;
        int top = (containingSize.getHeight() - scaledSize.getHeight()) / 2;
        Log.d("LOOG","scaleAndCenterWithin left -- " + left + " top " + top);

        return new Rect(
                /* left */ left,
                /* top */ top,
                /* right */ left + scaledSize.getWidth(),
                /* bottom */ top + scaledSize.getHeight()
        ) ;
    }


    static Size maxAspectRatioInSize(Size area, Float aspectRatio) {
        Log.d("LOOG","maxAspectRatioInSize aspectRatio -- " + aspectRatio );

        int width = area.getWidth();
        int height = Math.round(width / aspectRatio);
//         height = Math.round(width / aspectRatio);
        Size s = null;
        if (height <= area.getHeight()) {

            s = new Size(area.getWidth(), height);
            Log.d("LOOG","height <= area.getHeight()) maxAspectRatioInSize -- " + s.toString());
            Log.d("LOOG","width <= area.width()) maxAspectRatioInSize -- " + width + " heghit  " + area.getHeight());

        } else {
            height = area.getHeight();
            width = Math.round(height * aspectRatio);
//            width = (int) (height * aspectRatio);
            s = new Size(Math.min(width, area.getWidth()), height);
            Log.d("LOOG","else maxAspectRatioInSize -- " + s.toString());

        }


        return s;
    }



//    public synchronized Rect getFramingRectInPreview() {
//        if (framingRectInPreview == null) {
//            Rect framingRect = getFramingRect();
//            if (framingRect == null) {
//                return null;
//            }
//            Rect rect = new Rect(framingRect);
//            Point cameraResolution = configManager.getCameraResolution();
//            Point screenResolution = configManager.getScreenResolution();
//            if (cameraResolution == null || screenResolution == null) {
//                // Called early, before init even finished
//                return null;
//            }
//            rect.left = rect.left * cameraResolution.x / screenResolution.x;
//            rect.right = rect.right * cameraResolution.x / screenResolution.x;
//            rect.top = rect.top * cameraResolution.y / screenResolution.y;
//            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
//            framingRectInPreview = rect;
//        }
//        return framingRectInPreview;
//    }

}
