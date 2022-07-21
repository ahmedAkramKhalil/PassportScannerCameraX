package com.foo.ocr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.lights.LightsManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.foo.ocr.databinding.ActivityMainBinding;
import com.foo.ocr.mrzdecoder.MrzRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private static final int APP_CAMERA_ACTIVITY_REQUEST_CODE = 150;
    private static final int APP_SETTINGS_ACTIVITY_REQUEST_CODE = 550;
    public static final int PORTRAIT_SCANNING = 0;
    public static final int LANDSCAPE_SCANNING = 1;
    int scanningMode = 0 ;
    public static final String MRZ_RESULT = "MRZ_RESULT";


    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private final String[] REQUIRED_PERMISSION = new String[]{Manifest.permission.CAMERA, Manifest.permission.MANAGE_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanningMode = 0 ;
                requestPermissionForCamera();
            }
        });
        binding.scanBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanningMode =1 ;
                requestPermissionForCamera();
            }
        });

    }


    void openCameraActivity() {
        Intent intent = null ;
        if (scanningMode == 0 ) {
            intent = new Intent(MainActivity.this, CaptureActivity2.class);
        }else {
            intent = new Intent(MainActivity.this, CaptureActivity.class);

        }
        intent.putExtra("mode",scanningMode);
        startActivityForResult(intent, APP_CAMERA_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (allPermissionsGranted()) {
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(REQUIRED_PERMISSION, REQUEST_CODE_PERMISSIONS);
//            } else {
//                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
//            }
//        }

    }

    private void requestPermissionForCamera() {
        String[] permissions = {Manifest.permission.CAMERA};
        boolean isPermissionGranted = PermissionUtil.hasPermissions(this, permissions);
        if (!isPermissionGranted) {
            AppUtil.showAlertDialog(this, getString(R.string.permission_title), getString(R.string.permission_description),
                    getString(R.string.button_ok), false,
                    (dialogInterface, i) ->
                            ActivityCompat.requestPermissions(this, permissions, PermissionUtil.REQUEST_CODE_MULTIPLE_PERMISSIONS));
        } else {
            openCameraActivity();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.REQUEST_CODE_MULTIPLE_PERMISSIONS) {
            int result = grantResults[0];
            if (result == PackageManager.PERMISSION_DENIED) {
                if (!PermissionUtil.showRationale(this, permissions[0])) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, APP_SETTINGS_ACTIVITY_REQUEST_CODE);
                } else {
                    requestPermissionForCamera();
                }
            } else if (result == PackageManager.PERMISSION_GRANTED) {
                openCameraActivity();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case APP_CAMERA_ACTIVITY_REQUEST_CODE:
                    MrzRecord mrzInfo = (MrzRecord) data.getSerializableExtra(MRZ_RESULT);
                    Log.d("Scanned", "MRZ Result data  >>      >>>      >>  >> Getted");
                    if (mrzInfo != null) {

                        FragmentContainerView container = findViewById(R.id.fragment_container_view);
                        container.setVisibility(View.VISIBLE);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        Fragment fragment = MRZInfoFragment.newInstance(mrzInfo);
                        ft.replace(R.id.fragment_container_view, fragment);
                        ft.commit();
//                        Log.d("Scanned", "MRZ Result data  >>      >>>      >>  >> " + mrzInfo.getDocumentNumber());
//                        setMrzData(mrzInfo);
                    } else {
                    }
                    break;
                default:
                    break;
            }
        }
    }


}