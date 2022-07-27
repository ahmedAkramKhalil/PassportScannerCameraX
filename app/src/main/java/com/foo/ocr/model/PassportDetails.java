package com.foo.ocr.model;

import android.graphics.Bitmap;

import com.foo.ocr.mrzdecoder.MrzRecord;

public class PassportDetails {
    MrzRecord mrzRecord;
    Bitmap personalPicture;
    Bitmap passportPhoto;

    public PassportDetails(Bitmap personalPicture, Bitmap passportPhoto) {
        this.personalPicture = personalPicture;
        this.passportPhoto = passportPhoto;
    }

    public MrzRecord getMrzRecord() {
        return mrzRecord;
    }

    public void setMrzRecord(MrzRecord mrzRecord) {
        this.mrzRecord = mrzRecord;
    }

    public Bitmap getPersonalPicture() {
        return personalPicture;
    }

    public void setPersonalPicture(Bitmap personalPicture) {
        this.personalPicture = personalPicture;
    }

    public Bitmap getPassportPhoto() {
        return passportPhoto;
    }

    public void setPassportPhoto(Bitmap passportPhoto) {
        this.passportPhoto = passportPhoto;
    }
}
