package com.foo.ocr;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication INSTANCE;

   public DataRepository dataRepository; // this is YOUR class

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        dataRepository = new DataRepository();
    }

    public static MyApplication get() {
        return INSTANCE;
    }
}
