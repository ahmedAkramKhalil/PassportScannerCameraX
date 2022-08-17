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


public class ImageUtil {
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

    public static Bitmap cropImage(Bitmap fullImage, Size previewSize, Rect cardFinder) {
        if (previewSize.getWidth() == 0f)
            return null;
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
//        return Bitmap.createBitmap(fullImage, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
        return createBitmap(fullImage, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }

    public static Bitmap createBitmap(Bitmap bitmap, int x, int y, int width, int height) {
        if (width == 0 || height == 0 || (x + width) > bitmap.getWidth() || (y + height) > bitmap.getHeight())
            return null;
        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }


    /**
     * cropImageWithBounder used to crop face image in passport document
     */
    public static Bitmap cropImageWithBoundary(Bitmap fullImage, Size previewSize, Rect cardFinder, boolean higherBoundary) {
//        Rect rect = new Rect((int) (cardFinder.left / 1.55), (int) (cardFinder.top / (higherBoundary ? 1.09 : 1.45)), (int) (cardFinder.right * 1.25), (int) (cardFinder.bottom * (higherBoundary ? 1.06 : 1.15)));
//        cardFinder = rect;
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

    public static Bitmap cropImageWith(Bitmap fullImage, Rect cardFinder) {
//        Rect rect = new Rect((int) (cardFinder.left / 1.55), (int) (cardFinder.top / (higherBoundary ? 1.09 : 1.45)), (int) (cardFinder.right * 1.25), (int) (cardFinder.bottom * (higherBoundary ? 1.06 : 1.15)));
//        cardFinder = rect;
//        Rect scaledPreviewImage = scaleAndCenterWithin(previewSize, fullImage.getWidth(), fullImage.getHeight());
//        float previewScale = (float) scaledPreviewImage.width() / previewSize.getWidth();

        Rect scaledCardFinder = new Rect(
                Math.round(cardFinder.left),
                Math.round(cardFinder.top),
                Math.round(cardFinder.right),
                Math.round(cardFinder.bottom)
        );
        // Position the scaledCardFinder on the fullImage
        Rect cropRect = new Rect(
                Math.max(0, scaledCardFinder.left),
                Math.max(0, scaledCardFinder.top),
                Math.min(fullImage.getWidth(), scaledCardFinder.right),
                Math.min(fullImage.getHeight(), scaledCardFinder.bottom)
        );
        return Bitmap.createBitmap(fullImage, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());

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

    private static Bitmap rotateBitmap(
            Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
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
//        return Bitmap.createBitmap(source, x, y, width, height);
    }

    public static Bitmap

    cropBitmapToARect(
            // Original Bitmap to be cropped
            Bitmap source,
            // The Frame the source bitmap should be cropped to
            Rect face) {

        //     java.lang.IllegalArgumentException: y must be >= 0
        return Bitmap.createBitmap(source, face.left, face.top, face.width(), face.height());
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


    private static float postScaleWidthOffset;
    private static float postScaleHeightOffset;
    private static final Matrix transformationMatrix = new Matrix();
    private static float scaleFactor = 1.0f;

    public static Rect getRect(Face face) {
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());

        float xOffset = scale(face.getBoundingBox().width() / 2.0f);
        float yOffset = scale(face.getBoundingBox().height() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        return new Rect((int) left, (int) top, (int) right, (int) bottom);

    }

    public static Bitmap draw(Face face, Bitmap bitmap) {
//        Canvas canvas = null;
//        if (face == null) {
//            return;
//        }

        updateTransformationIfNeeded(bitmap, face.getBoundingBox());

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, (int) x, (int) y, face.getBoundingBox().width(), face.getBoundingBox().height());
        return bitmap1;


//        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        // Calculate positions.
//        float left = x - scale(face.getBoundingBox().width() / 2.0f);
//        float top = y - scale(face.getBoundingBox().height() / 2.0f);
//        float right = x + scale(face.getBoundingBox().width() / 2.0f);
//        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
////        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
//        float yLabelOffset = (face.getTrackingId() == null) ? 0 : - 2;
    }

    public static float translateX(float x) {
//        if (overlay.isImageFlipped) {
//            return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
//        } else {
        return scale(x) - postScaleWidthOffset;

//        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    public static float translateY(float y) {

        return scale(y) - postScaleHeightOffset;
    }


    private static void updateTransformationIfNeeded(Bitmap source, Rect dist) {

        float viewAspectRatio = (float) dist.width() / dist.height();
        float imageAspectRatio = (float) source.getWidth() / source.getHeight();
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) dist.width() / source.getWidth();
            postScaleHeightOffset = ((float) dist.width() / imageAspectRatio - dist.height()) / 2;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) dist.height() / source.getHeight();
            postScaleWidthOffset = ((float) dist.height() * imageAspectRatio - dist.width()) / 2;
        }

        transformationMatrix.reset();
        transformationMatrix.setScale(scaleFactor, scaleFactor);
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);

    }


    public static float scale(float imagePixel) {
        return imagePixel * scaleFactor;
    }


    public static byte[] cropImageProxyToCropRect(ImageProxy imageProxy) {

        ImageProxy.PlaneProxy planeProxy = imageProxy.getPlanes()[0];

        ByteBuffer buffer = planeProxy.getBuffer();

        byte[] orgArray = new byte[buffer.remaining()];
        buffer.get(orgArray);
        Rect rect = imageProxy.getCropRect();
        int imageWidth = rect.width();
        byte[] array = new byte[rect.width() * rect.height()];
        int j = 0;
        for (int i = 0; i < orgArray.length; i++) {
            int x = i % imageWidth;
            int y = i / imageWidth;
            if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                array[j] = orgArray[i];
                j++;
            }
        }
        Log.d("TAG", "array.length " + array.length);
        return array;
    }

    public static Bitmap compressBitmap(Bitmap org) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.compress(Bitmap.CompressFormat.PNG, 100, out);
        return BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
    }


//     void analyze( ImageProxy imageProxy ) {
//         Image mediaImage = imageProxy.getImage();
//        if (mediaImage != null && mediaImage.getFormat() == ImageFormat.YUV_420_888) {
//            croppedNV21(mediaImage, imageProxy.getCropRect()) ;
//
//
//            croppedNV21(mediaImage, imageProxy.getCropRect()).let { byteArray ->
//                    requestDetectInImage(
//
//                            InputImage.fromByteArray(
//                                    byteArray,
//                                    imageProxy.cropRect.width(),
//                                    imageProxy.cropRect.height(),
//                                    rotation,
//                                    IMAGE_FORMAT_NV21,
//                                    )
//                    )
//                            .addOnCompleteListener { imageProxy.close() }
//            }
//        } else {
//            imageProxy.close()
//        }
//    }

    public static byte[] croppedNV21(Image mediaImage, Rect cropRect) {
//        Log.d("LOOG", "CROPRECT + W=" + cropRect.width() + " H=" + cropRect.height());
//        Log.d("LOOG", "mediaImage + W=" + mediaImage.getWidth() + " H=" + mediaImage.getHeight());
        try {

            ByteBuffer yBuffer = mediaImage.getPlanes()[0].getBuffer(); // Y
            ByteBuffer vuBuffer = mediaImage.getPlanes()[2].getBuffer(); // VU

            int ySize = yBuffer.remaining();
            int vuSize = vuBuffer.remaining();

            byte[] nv21 = new byte[ySize + vuSize];

            yBuffer.get(nv21, 0, ySize);
            vuBuffer.get(nv21, ySize, vuSize);
//        Log.d("LOOG","cropByteArray " + mediaImage.getWidth() + " "+ cropRect.width());

            return cropByteArray(nv21, mediaImage.getWidth(), cropRect);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] cropByteArray(byte[] array, int imageWidth, Rect cropRect) {

        byte[] orgArray = array;
        byte[] croppedArray = new byte[0];
        try {

//        int imageWidth = cropRect.width();

            croppedArray = new byte[cropRect.width() * cropRect.height()];
            int j = 0;
            for (int i = 0; i < orgArray.length; i++) {
                int x = i % imageWidth;
                int y = i / imageWidth;
                if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
                    croppedArray[j] = orgArray[i];
                    j++;
                }
            }
//        Log.d("TAG", "array.length " + array.length);
//        return  array ;


//        byte [] croppedArray = new byte[cropRect.width() * cropRect.height()];
//        int i = 0 ;
//        for (int j = 0; j < croppedArray.length; j++) {
//            float x = (j % imageWidth)  ;
//            float y = j / imageWidth ;
//
//            if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
//                croppedArray[i] = croppedArray[j];
//                        i++ ;
//            }
//
//        }

        } catch (Exception e) {

        }

        return croppedArray;
    }

    public static Bitmap getBitmap(ImageProxy image) {

        @SuppressLint("UnsafeOptInUsageError") ByteBuffer nv21Buffer =
                yuv420ThreePlanesToNV21(image.getImage().getPlanes(), image.getWidth(), image.getHeight());
        byte[] orgArray = new byte[nv21Buffer.remaining()];
        nv21Buffer.get(orgArray);
        Rect rect = image.getCropRect();
        int imageWidth = rect.width();
        byte[] array = new byte[rect.width() * rect.height()];
        int j = 0;
        for (int i = 0; i < orgArray.length; i++) {
            int x = i % imageWidth;
            int y = i / imageWidth;
            if (rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom) {
                array[j] = orgArray[i];
                j++;
            }
        }


        return getBitmap2(ByteBuffer.wrap(array), image.getWidth(), image.getHeight(), image.getImageInfo().getRotationDegrees());
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

    public static Bitmap getBitmap2(ByteBuffer data, int width, int height, int rotation) {
        data.rewind();
        byte[] imageInBuffer = new byte[data.limit()];
        data.get(imageInBuffer, 0, imageInBuffer.length);
        try {
            YuvImage image =
                    new YuvImage(
                            imageInBuffer, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);

            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

            stream.close();
            return rotateBitmap(bmp, rotation, false, false);
        } catch (Exception e) {
            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
        }
        return null;
    }


    private static boolean areUVPlanesNV21(Image.Plane[] planes, int width, int height) {
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
        boolean areNV21 =
                (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition);
        uBuffer.limit(uBufferLimit);

        return areNV21;
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


    /**
     * Unpack an image plane into a byte array.
     *
     * <p>The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
     * spaced by 'pixelStride'. Note that there is no row padding on the output.
     */
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


    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }
        else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert(rowStride == image.getPlanes()[1].getRowStride());
        assert(pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte)~savePixel);
                if (uBuffer.get(0) == (byte)~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            }
            catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for (int row=0; row<height/2; row++) {
            for (int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    public static Bitmap toBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer()  ;// Y
        ByteBuffer vuBuffer = image.getPlanes()[2].getBuffer() ; // VU

        int ySize = yBuffer.remaining() ;
        int vuSize = vuBuffer.remaining();

        byte[] nv21 = new byte[ySize + vuSize];

        yBuffer.get(nv21, 0, ySize);
        vuBuffer.get(nv21, ySize, vuSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 50, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    public static Bitmap cropImage(ImageProxy image, int rotationDegree, int xOffset, int yOffset, int cropWidth, int cropHeight) {
        // 1 - Convert image to Bitmap
//       Bitmap bitmap =  imageProxyToBitmap(image);
       Bitmap bitmap =  toBitmap(image);

//        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//        byte[] bytes = new byte[buffer.remaining()];
//        buffer.get(bytes);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null)
            return null;

        // 2 - Rotate the Bitmap
        if(rotationDegree != 0) {
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(rotationDegree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);
        }
        // 3 - Crop the Bitmap
        if (bitmap == null){
            return null;
        }


        if ((xOffset + cropWidth) > bitmap.getWidth()){
            xOffset = 0 ;
        }
        bitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, cropWidth, cropHeight);

        return bitmap;
    }


}
