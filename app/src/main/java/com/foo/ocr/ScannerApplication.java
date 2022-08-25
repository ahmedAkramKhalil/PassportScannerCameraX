package com.foo.ocr;

import android.app.Application;

public class ScannerApplication extends Application {
    private static ScannerApplication INSTANCE;

   public  DataRepository dataRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        dataRepository = new DataRepository();
    }

    public  void reset(){
        dataRepository = new DataRepository();
    }
    public static ScannerApplication get() {
        return INSTANCE;
    }
}
