package com.foo.ocr.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import com.foo.ocr.MyApplication;
import com.foo.ocr.mrzscanner.MRZScanner;
import com.foo.ocr.R;
import com.foo.ocr.mrzscanner.StateData;
import com.foo.ocr.databinding.ActivityCaptureBinding;
import com.foo.ocr.mrzscanner.model.PassportDetails;
import com.foo.ocr.mrzscanner.mrzdecoder.MrzRecord;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

public class CaptureActivity extends AppCompatActivity {

    private ActivityCaptureBinding binding;
    private PreviewView mPreviewView;
    private MRZScanner MRZScanner;
    private ProgressDialog progressDialog;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        mPreviewView = binding.camera;
        /**
         * create and set PassportScanner Object to start scanning library
         */
        MRZScanner = new MRZScanner(this, mPreviewView, binding.overlayView, binding.mrzAreaView);
        MRZScanner.toggleFlash(binding.flashSwitch);
        binding.flashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MRZScanner.toggleFlash(binding.flashSwitch);
                binding.flashSwitch.setText(b ? getResources().getString(R.string.flash_on) : getResources().getString(R.string.flash_off));
            }
        });

        Intent returnIntent = new Intent();
        MRZScanner.getMrzLiveData().observe(this, new Observer<StateData<MrzRecord>>() {
            @Override
            public void onChanged(StateData<MrzRecord> mrzRecordStateData) {
                MyApplication.get().dataRepository.updateMrzRecordStateLiveData(mrzRecordStateData);
                switch (mrzRecordStateData.getStatus()) {
                    case SUCCESS:
                        showLoadingIcon();
                        break;

                }
            }
        });

        MRZScanner.getPassportDetailsLiveData().observe(this, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                MyApplication.get().dataRepository.updatePassportDetailsStateLiveData(passportDetailsStateData);
                switch (passportDetailsStateData.getStatus()) {
                    case SUCCESS:
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                        break;
                    case ERROR:
                        setResult(Activity.RESULT_CANCELED, returnIntent);
                        finish();
                        break;
                }
            }
        });
    }


    private void showLoadingIcon() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(CaptureActivity.this, R.style.progressDialog);
            progressDialog.setMessage(getResources().getString(R.string.processing));
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MRZScanner.startScanning();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        MRZScanner.stopScanning();
        super.onDestroy();
    }

}