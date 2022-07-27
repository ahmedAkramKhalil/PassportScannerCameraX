package com.foo.ocr;

import com.foo.ocr.mrzdecoder.MrzRecord;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class RxBus {
    private static final BehaviorSubject<MrzRecord> bs = BehaviorSubject.create();
    public static BehaviorSubject<MrzRecord> getRecord() {
        return bs;
    }
    private static final BehaviorSubject<Object> bus = BehaviorSubject.create();
}
