package com.foo.ocr.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.View;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;


public class ImageUtil {
    /**
     * Get Possible Aspect Ratio of size
     * This method not used because we matches the Aspect Ratio on CameraX preview and ImageCapture
     * If the Aspect Ratio on CameraX preview and ImageCapture this method should be used before cropping captured image
     */
    int aspectRatio(int width, int height) {
        int previewRatio = Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - AspectRatio.RATIO_4_3) <= Math.abs(previewRatio - AspectRatio.RATIO_16_9)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public static Bitmap cropImage(Bitmap fullImage, Size previewSize, Rect cardFinder) {
        Rect scaledPreviewImage = scaleAndCenterWithin(previewSize, fullImage.getWidth(), fullImage.getHeight());
        float previewScale = (float) scaledPreviewImage.width() / previewSize.getWidth();
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


    /**
     * cropImageWithBounder used to crop face image in passport document
     */
    public static Bitmap cropImageWithBoundary(Bitmap fullImage, Size previewSize, Rect cardFinder, boolean higherBoundary) {
        Rect rect = new Rect((int) (cardFinder.left / 1.55), (int) (cardFinder.top / (higherBoundary ? 1.09 : 1.25)), (int) (cardFinder.right * 1.25), (int) (cardFinder.bottom * (higherBoundary ? 1.06 : 1.1)));

        cardFinder = rect;
        Rect scaledPreviewImage = scaleAndCenterWithin(previewSize, fullImage.getWidth(), fullImage.getHeight());
        float previewScale = (float) scaledPreviewImage.width() / previewSize.getWidth();

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
        /**
         * Since the preview image may be at a different resolution than the full image, scale the
         * preview image to be circumscribed by the fullImage.
         */
        Size scaledSize = maxAspectRatioInSize(containingSize, aspectRatio);
        int left = (containingSize.getWidth() - scaledSize.getWidth()) / 2;
        int top = (containingSize.getHeight() - scaledSize.getHeight()) / 2;
        return new Rect(
                /** left */left,
                /** top */top,
                /** right */left + scaledSize.getWidth(),
                /** bottom */top + scaledSize.getHeight()
        );
    }

    /**
     * Get the maximum aspectRation of containing area
     */
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


    /**
     * Rotating Bitmap depending on Camera Rotation degree
     */
    public static Bitmap rotateBitmapIfNeeded(Bitmap source, ImageInfo info) {
        int angle = info.getRotationDegrees();
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mat, true);
    }

    public static Bitmap cropPreviewBitmapWidth(Bitmap previewBitmap, DisplayMetrics dm) {
        int deviceWidthPx = dm.widthPixels;
        int deviceHeightPx = dm.heightPixels;
        float cropHeightPx = 0f;
        float cropWidthPx = 0f;
        if (deviceHeightPx > deviceWidthPx) {
            cropHeightPx = 1.0f * previewBitmap.getHeight();
            cropWidthPx = 1.0f * deviceWidthPx / deviceHeightPx * cropHeightPx;
        } else {
            cropWidthPx = 1.0f * previewBitmap.getWidth();
            cropHeightPx = 1.0f * deviceHeightPx / deviceWidthPx * cropWidthPx;
        }
        float cx = previewBitmap.getWidth() / 2;
        float cy = previewBitmap.getHeight();
        float minimusPx = Math.min(cropHeightPx, cropWidthPx);
        float left2 = cx - minimusPx / 2;
        float top2 = cy - minimusPx / 2;
        Bitmap croppedBitmap = Bitmap.createBitmap(previewBitmap, (int) left2, (int) 0, (int) minimusPx, (int) previewBitmap.getHeight());
        return croppedBitmap;
    }

    /**
     * Crop Bitmap to a Overlay Frame
     */
    public static Bitmap cropBitmapToAFrame(
            // Original Bitmap to be cropped
            Bitmap source,
            // The Frame the source bitmap should be cropped to
            View frame,
            // The root frame of camera and cropping frame
            View cardPlaceHolder) {

        float scaleX = source.getWidth() / (float) frame.getWidth();
        float scaleY = source.getHeight() / (float) frame.getHeight();
        int x = (int) ((cardPlaceHolder.getLeft()) * scaleX);
        int y = (int) ((cardPlaceHolder.getTop()) * scaleY);
        int width = (int) (cardPlaceHolder.getWidth() * scaleX);
        int height = (int) (cardPlaceHolder.getHeight() * scaleY);
        return Bitmap.createBitmap(source, x, y, width, height);
    }

    /**
     * Convert Image Proxy to Bitmap
     */
    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }

    /**
     * Getting Rect object and positioning the View
     */
    public static Rect getViewRect(View view) {
        int[] l = new int[2];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.getLocationInWindow(l);
        }
        int x = l[0];
        int y = l[1];
        int w = view.getWidth();
        int h = view.getHeight();
        return new Rect(x, y, x + w, y + h);
    }

}
