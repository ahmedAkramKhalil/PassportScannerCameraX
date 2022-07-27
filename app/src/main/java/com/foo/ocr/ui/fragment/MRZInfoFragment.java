package com.foo.ocr.ui.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.foo.ocr.R;
import com.foo.ocr.databinding.FragmentMrzInfoBinding;
import com.foo.ocr.ml.FaceDetectorHelper;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.observers.DisposableObserver;

public class MRZInfoFragment extends Fragment {

    FragmentMrzInfoBinding binding;
    private static final String ARG_PARAM1 = "param1";


    public MRZInfoFragment() {
        // Required empty public constructor
    }

    public static MRZInfoFragment newInstance(MrzRecord mrzRecord) {
        MRZInfoFragment fragment = new MRZInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, mrzRecord);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    MrzRecord mrzInfo;
    TextView name, gName, documentNumber, issuingCountry, nationality, dateOfBirth, gender, expirationDate, others;
    CheckBox documentCH, birthCH, expirationCH;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if ( getArguments().getSerializable(ARG_PARAM1) != null) {
            mrzInfo = (MrzRecord) getArguments().getSerializable(ARG_PARAM1);
            binding.setMrzRecord(mrzInfo);

        }
        FaceDetectorHelper.getFaceDetectorObserver()
                .subscribeWith(new DisposableObserver<PassportDetails>() {
                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull PassportDetails passportDetails) {

                        binding.personPicture.setImageBitmap(passportDetails.getPersonalPicture());
                        binding.fullPicture.setImageBitmap(passportDetails.getPassportPhoto());
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_mrz_info, container, false);
        View view = binding.getRoot();
        return view;
    }
}