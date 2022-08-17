package com.foo.ocr;

import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

public class DataRepository {
    private final StateLiveData<PassportDetails> data = new StateLiveData<>();
    private final StateLiveData<MrzRecord> mrzData = new StateLiveData<>();

    public StateLiveData<PassportDetails> getData() {
        return data;
    }

    public StateLiveData<MrzRecord> getMrzData() {
        return mrzData;
    }

    public void updateText(StateData<PassportDetails> passportDetailsStateLiveData) {
     data.setValue(passportDetailsStateLiveData);
    }
    public void updateData(StateData<MrzRecord> mrzRecordStateData) {
     mrzData.setValue(mrzRecordStateData);
    }
    public void clear(){
        mrzData.postLoading();
        data.postLoading();
    }
}
