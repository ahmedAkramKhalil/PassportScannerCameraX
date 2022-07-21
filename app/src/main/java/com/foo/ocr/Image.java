package com.foo.ocr;

import android.graphics.Bitmap;
import android.graphics.Rect;
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
        Rect scaledPreviewImage =  scaleAndCenterWithin(previewSize,fullImage.getHeight(),fullImage.getWidth()) ;
        int previewScale = scaledPreviewImage.width() / previewSize.getWidth();

        // Scale the cardFinder to match the scaledPreviewImage
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

        return Bitmap.createBitmap(fullImage, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());

    }

    static Rect scaleAndCenterWithin(Size containingSize, int width, int height) {
        float aspectRatio = (float) width / (float) height;

        // Since the preview image may be at a different resolution than the full image, scale the
        // preview image to be circumscribed by the fullImage.
        Size scaledSize = maxAspectRatioInSize(containingSize, aspectRatio);
        int left = (containingSize.getWidth() - scaledSize.getWidth()) / 2;
        int top = (containingSize.getHeight() - scaledSize.getHeight()) / 2;
        return new Rect(
                /* left */ left,
                /* top */ top,
                /* right */ left + scaledSize.getWidth(),
                /* bottom */ top + scaledSize.getHeight()
        ) ;
    }


    static Size maxAspectRatioInSize(Size area, Float aspectRatio) {
        int width = area.getWidth();
        int height = Math.round(width / aspectRatio);
        Size s = null;
        if (height <= area.getHeight()) {
            s = new Size(area.getWidth(), height);
        } else {
            height = area.getHeight();
            width = Math.round(height * aspectRatio);
            s = new Size(Math.min(width, area.getWidth()), height);
        }
        return s;
    }


}
