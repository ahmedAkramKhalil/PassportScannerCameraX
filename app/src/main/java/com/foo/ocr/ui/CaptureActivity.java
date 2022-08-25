package com.foo.ocr.ui;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.foo.ocr.ScannerApplication;
import com.foo.ocr.PassportScanner;
import com.foo.ocr.R;
import com.foo.ocr.StateData;
import com.foo.ocr.databinding.ActivityCaptureBinding;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

public class CaptureActivity extends AppCompatActivity {

    private ActivityCaptureBinding binding;
    private PreviewView mPreviewView;
    PassportScanner passportScanner;
    ProgressDialog nDialog;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);
        mPreviewView = binding.camera;
        passportScanner = new PassportScanner(this, mPreviewView, binding.view, binding.overlayView);
        passportScanner.setOverlayView(binding.view);
        passportScanner.setScreenFrameView(binding.frame);
        passportScanner.setMrzView(binding.overlayView);
        passportScanner.toggleFlash(binding.flashSwitch);
        binding.flashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                passportScanner.toggleFlash(binding.flashSwitch);
                binding.flashSwitch.setText(b ? " Flash ON" : "Flash OFF");
            }
        });
//        DrawView drawView = new DrawView(this);
//        drawView.setBackgroundColor(Color.WHITE);
//        binding.frame.addView(drawView);
//        setContentView(drawView);
//        Log.d("LOOG", "VIEW 1 " + r.top + "  " + r.left);
//        int[] l1 = new int[2];
//        binding.view2.getLocationInWindow(l1);
//        Log.d("LOOG", "VIEW 2 " + l1[0] + "  " + l1[1]);
        passportScanner.getMrzLiveData().observe(this, new Observer<StateData<MrzRecord>>() {
            @Override
            public void onChanged(StateData<MrzRecord> mrzRecordStateData) {
                Log.d("LOOG", "" + mrzRecordStateData.getStatus().toString());
                ScannerApplication.get().dataRepository.updateData(mrzRecordStateData);
                switch (mrzRecordStateData.getStatus()) {
                    case SUCCESS:
                        showSpinner();
                        break;
                }
            }
        });

        passportScanner.getPassportDetailsLiveData().observe(this, new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                Log.d("LOOG", ">>>>>>" + passportDetailsStateData.getStatus());
                ScannerApplication.get().dataRepository.updateText(passportDetailsStateData);
                Intent returnIntent = new Intent();
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


    private void showSpinner() {
      try {
        if (nDialog == null) {
            nDialog = new ProgressDialog(CaptureActivity.this,R.style.progressDialog);
        nDialog.setMessage(getResources().getString(R.string.processing));
            nDialog.setContentView(R.layout.progress_dialog);
            nDialog.setIndeterminate(false);
            nDialog.setCancelable(false);
            nDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (!nDialog.isShowing())
            nDialog.show();
      }catch (Exception e){
          Log.w("Log",e.getMessage());
      }
//        Toast.makeText(getApplicationContext(),"MRZ Detected, processing...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        passportScanner.start();
        if (nDialog != null) {
            nDialog.dismiss();
        }

    }
    @Override
    protected void onDestroy() {
        passportScanner.stop();
        super.onDestroy();
    }

}