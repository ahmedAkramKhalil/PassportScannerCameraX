package com.foo.ocr.model;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.foo.ocr.mrzdecoder.MrzRecord;
import com.foo.ocr.util.ImageUtil;

import java.io.Serializable;

public class PassportDetails implements Parcelable {
    private  MrzRecord mrzRecord;
    private Bitmap personalPicture;
    private Bitmap passportPhoto;

    public PassportDetails(Bitmap personalPicture, Bitmap passportPhoto) {
        this.personalPicture = personalPicture;
        this.passportPhoto = passportPhoto;

    }

    public PassportDetails() {
    }

    public PassportDetails(MrzRecord mrzRecord, Bitmap personalPicture, Bitmap passportPhoto) {
        this.mrzRecord = mrzRecord;
        this.personalPicture = personalPicture;
        this.passportPhoto = passportPhoto;
    }

    protected PassportDetails(Parcel in) {
        personalPicture = in.readParcelable(Bitmap.class.getClassLoader());
        passportPhoto = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<PassportDetails> CREATOR = new Creator<PassportDetails>() {
        @Override
        public PassportDetails createFromParcel(Parcel in) {
            return new PassportDetails(in);
        }

        @Override
        public PassportDetails[] newArray(int size) {
            return new PassportDetails[size];
        }
    };

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

        this.personalPicture = ImageUtil.compressBitmap(personalPicture);
    }

    public Bitmap getPassportPhoto() {
        return passportPhoto;
    }

    public void setPassportPhoto(Bitmap passportPhoto) {
        this.passportPhoto = ImageUtil.compressBitmap(passportPhoto);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(personalPicture, i);
        parcel.writeParcelable(passportPhoto, i);
        parcel.writeSerializable(mrzRecord);
    }
}
