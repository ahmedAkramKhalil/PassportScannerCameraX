package com.foo.ocr.mrzscanner.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.foo.ocr.mrzscanner.mrzdecoder.MrzRecord;
import com.foo.ocr.mrzscanner.util.BitmapUtil;

public class PassportDetails implements Parcelable {
    private  MrzRecord mrzRecord;
    private Bitmap personalPicture;
    private Bitmap passportPhoto;

    public PassportDetails(Bitmap personalPicture, Bitmap passportPhoto) {
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

        this.personalPicture = BitmapUtil.compressBitmap(personalPicture);
    }

    public Bitmap getPassportPhoto() {
        return passportPhoto;
    }

    public void setPassportPhoto(Bitmap passportPhoto) {
        this.passportPhoto = BitmapUtil.compressBitmap(passportPhoto);
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
