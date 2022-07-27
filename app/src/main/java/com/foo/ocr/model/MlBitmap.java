package com.foo.ocr.model;

import android.graphics.Bitmap;

public class MlBitmap {
    private Bitmap bitmap;
    private  int rotationDegree;
    // Assign isBitmapDetectedByObjectDetector to  true when object detector detect this bitmap otherwise it should be false
    private boolean isBitmapDetectedByObjectDetector = false;



    public MlBitmap(Bitmap bitmap, int rotationDegree) {
        this.bitmap = bitmap;
        this.rotationDegree = rotationDegree;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }


    public int getRotationDegree() {
        return rotationDegree;
    }

    public boolean isBitmapDetectedByObjectDetector() {
        return isBitmapDetectedByObjectDetector;
    }

    public void setBitmapDetectedByObjectDetector(boolean bitmapDetectedByObjectDetector) {
        isBitmapDetectedByObjectDetector = bitmapDetectedByObjectDetector;
    }
}
