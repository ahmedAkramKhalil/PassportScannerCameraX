package com.foo.ocr.util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;


public class BitmapUtil {
    /**
     * Get Possible Aspect Ratio of size
     * This method not used because we matches the Aspect Ratio on CameraX preview and ImageCapture
     * If the Aspect Ratio on CameraX preview and ImageCapture this method should be used before cropping captured image
     */
    public static int aspectRatio(int width, int height) {
        int previewRatio = Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - AspectRatio.RATIO_4_3) <= Math.abs(previewRatio - AspectRatio.RATIO_16_9)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    public static Bitmap createBitmap(Bitmap bitmap, int x, int y, int width, int height) {
        if (width == 0 || height == 0 || (x + width) > bitmap.getWidth() || (y + height) > bitmap.getHeight())
            return null;
        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }


    public static Rect scaleAndCenterWithin(Size containingSize, int width, int height) {
        float aspectRatio = 0;
        if (height != 0)
            aspectRatio = (float) width / (float) height;
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
    public static Size maxAspectRatioInSize(Size area, Float aspectRatio) {
        int width = area.getWidth();
        int height = 0;
        if (aspectRatio != 0)
            height = Math.round(width / aspectRatio);
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
        //TODO:  try to get actual x,y to get accurate rotation
        if (source.getWidth() == 0 || source.getHeight() == 0)
            return null;
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mat, true);
//        return source;
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
        float minimumsPx = Math.min(cropHeightPx, cropWidthPx);
        float left2 = cx - minimumsPx / 2;
        float top2 = cy - minimumsPx / 2;
//        Bitmap.createBitmap(previewBitmap, (int) left2, (int) 0, (int) minimumsPx, (int) previewBitmap.getHeight());
        Bitmap croppedBitmap = createBitmap(previewBitmap, (int) left2, (int) 0, (int) minimumsPx, (int) previewBitmap.getHeight());
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

        if (frame.getWidth() == 0 || frame.getHeight() == 0) {
            return null;
        }
        float scaleX = source.getWidth() / (float) frame.getWidth();
        float scaleY = source.getHeight() / (float) frame.getHeight();
        int x = (int) ((cardPlaceHolder.getLeft()) * scaleX);
        int y = (int) ((cardPlaceHolder.getTop()) * scaleY);
        int width = (int) (cardPlaceHolder.getWidth() * scaleX);
        int height = (int) (cardPlaceHolder.getHeight() * scaleY);
        return createBitmap(source, x, y, width, height);
    }


    /**
     * Convert Image Proxy to Bitmap
     */
    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        if (bytes.length <= 0)
            return null;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Bitmap compressBitmap(Bitmap org) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.compress(Bitmap.CompressFormat.PNG, 100, out);
        return BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
    }


    public static Bitmap getBitmap(ImageProxy image) {
        FrameMetadata frameMetadata =
                new FrameMetadata.Builder()
                        .setWidth(image.getWidth())
                        .setHeight(image.getHeight())
                        .setRotation(image.getImageInfo().getRotationDegrees())
                        .build();
        @SuppressLint("UnsafeOptInUsageError") ByteBuffer nv21Buffer =
                yuv420ThreePlanesToNV21(image.getImage().getPlanes(), image.getWidth(), image.getHeight());
        return getBitmap(nv21Buffer, frameMetadata, image.getCropRect());
    }

    public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata, Rect cropRect) {
        Log.d("LOOG", "CROP RECT top=" + cropRect.top + " left=" + cropRect.left + " bottom=" + cropRect.bottom + " right=" + cropRect.right + " width=" + cropRect.width() + " height=" + cropRect.height() + " ");
        Rect rect = new Rect(cropRect);
//        rect.top = rect.top +60 ;
//        rect.bottom = rect.bottom - 60;
        rect.top = rect.top + (rect.height() / 7);
        rect.bottom = rect.bottom - (rect.height() / 7);
        Log.d("LOOG", "rect RECT top=" + rect.top + " left=" + rect.left + " bottom=" + rect.bottom + " right=" + rect.right + " width=" + rect.width() + " height=" + rect.height() + " ");
        data.rewind();
        byte[] imageInBuffer = new byte[data.limit()];
        data.get(imageInBuffer, 0, imageInBuffer.length);
        try {
            YuvImage image =
                    new YuvImage(
                            imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(rect, 100, stream);
//            image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);
            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
            return rotateBitmap(bmp, metadata.getRotation(), false, false);
        } catch (Exception e) {
            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
        }
        return null;
    }

    private static ByteBuffer yuv420ThreePlanesToNV21(
            Image.Plane[] yuv420888planes, int width, int height) {
        int imageSize = width * height;
        byte[] out = new byte[imageSize + 2 * (imageSize / 4)];
        if (areUVPlanesNV21(yuv420888planes, width, height)) {

            // Copy the Y values.
            yuv420888planes[0].getBuffer().get(out, 0, imageSize);
            ByteBuffer uBuffer = yuv420888planes[1].getBuffer();
            ByteBuffer vBuffer = yuv420888planes[2].getBuffer();
            // Get the first V value from the V buffer, since the U buffer does not contain it.
            vBuffer.get(out, imageSize, 1);
            // Copy the first U value and the remaining VU values from the U buffer.
            uBuffer.get(out, imageSize + 1, 2 * imageSize / 4 - 1);
        } else {
            // Fallback to copying the UV values one by one, which is slower but also works.
            // Unpack Y.
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
            // Unpack U.
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            // Unpack V.
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
        }
        return ByteBuffer.wrap(out);
    }

    private static boolean areUVPlanesNV21(Image.Plane[] planes, int width, int height) {

     try {
         int imageSize = width * height;

         ByteBuffer uBuffer = planes[1].getBuffer();
         ByteBuffer vBuffer = planes[2].getBuffer();
         // Backup buffer properties.
         int vBufferPosition = vBuffer.position();
         int uBufferLimit = uBuffer.limit();
         // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
         vBuffer.position(vBufferPosition + 1);
         // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
         uBuffer.limit(uBufferLimit - 1);
         // Check that the buffers are equal and have the expected number of elements.
         boolean areNV21 = (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);
         // Restore buffers to their initial state.
         vBuffer.position(vBufferPosition);
         uBuffer.limit(uBufferLimit);
         return areNV21;
     }catch (Exception e ){
         return false;
     }
    }

    private static Bitmap rotateBitmap(
            Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();
        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);
        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private static void unpackPlane(
            Image.Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();
        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
        if (numRow == 0) {
            return;
        }
        int scaleFactor = height / numRow;
        int numCol = width / scaleFactor;
        // Extract the data in the output buffer.
        int outputPos = offset;
        int rowStart = 0;
        for (int row = 0; row < numRow; row++) {
            int inputPos = rowStart;
            for (int col = 0; col < numCol; col++) {
                out[outputPos] = buffer.get(inputPos);
                outputPos += pixelStride;
                inputPos += plane.getPixelStride();
            }
            rowStart += plane.getRowStride();
        }
    }


}
