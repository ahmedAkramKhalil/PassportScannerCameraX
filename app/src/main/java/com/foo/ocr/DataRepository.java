package com.foo.ocr;

import com.foo.ocr.mrzscanner.StateData;
import com.foo.ocr.mrzscanner.StateLiveData;
import com.foo.ocr.mrzscanner.model.PassportDetails;
import com.foo.ocr.mrzscanner.mrzdecoder.MrzRecord;

public class DataRepository {
    private final StateLiveData<PassportDetails> passportDetailsStateLiveData = new StateLiveData<>();
    private final StateLiveData<MrzRecord> mrzRecordStateLiveData = new StateLiveData<>();

    public StateLiveData<PassportDetails> getPassportDetailsStateLiveData() {
        return passportDetailsStateLiveData;
    }

    public StateLiveData<MrzRecord> getMrzRecordStateLiveData() {
        return mrzRecordStateLiveData;
    }

    public void updatePassportDetailsStateLiveData(StateData<PassportDetails> passportDetailsStateLiveData) {
     this.passportDetailsStateLiveData.setValue(passportDetailsStateLiveData);
    }
    public void updateMrzRecordStateLiveData(StateData<MrzRecord> mrzRecordStateData) {
     mrzRecordStateLiveData.setValue(mrzRecordStateData);
    }
}
