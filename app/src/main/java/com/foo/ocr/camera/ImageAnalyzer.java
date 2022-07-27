package com.foo.ocr.camera;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.foo.ocr.ml.VisionImageProcessor;
import com.google.mlkit.common.MlKitException;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

   private VisionImageProcessor visionImageProcessor;

    public ImageAnalyzer(VisionImageProcessor visionImageProcessor) {
        this.visionImageProcessor = visionImageProcessor;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        try {
            visionImageProcessor.processImageProxy(image);
        } catch (MlKitException e) {
            e.printStackTrace();
        }
    }
}
