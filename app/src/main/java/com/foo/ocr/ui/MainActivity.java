package com.foo.ocr.ui;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.foo.ocr.databinding.ActivityMainBinding;
import com.foo.ocr.ui.fragment.DetailsFragment;
import com.foo.ocr.mrzscanner.util.AppUtil;
import com.foo.ocr.mrzscanner.util.PermissionUtil;
import com.foo.ocr.R;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        FragmentContainerView container = findViewById(R.id.fragment_container_view);
                        container.setVisibility(View.VISIBLE);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        Fragment f = new DetailsFragment();
                        ft.replace(R.id.fragment_container_view, f);
                        ft.commit();
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Toast.makeText(getApplicationContext(), getResources().getText(R.string.process_failed).toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissionForCamera();
            }
        });
    }


    private void openCameraActivity() {
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        activityResultLauncher.launch(intent);
    }


    private void requestPermissionForCamera() {
        String[] permissions = {Manifest.permission.CAMERA};
        boolean isPermissionGranted = PermissionUtil.hasPermissions(this, permissions);
        if (!isPermissionGranted) {
            AppUtil.showAlertDialog(this, getString(R.string.permission_title), getString(R.string.permission_description),
                    getString(R.string.button_ok), false,
                    (dialogInterface, i) -> ActivityCompat.requestPermissions(this, permissions, PermissionUtil.REQUEST_CODE_MULTIPLE_PERMISSIONS));
        } else {
            openCameraActivity();
        }
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
                    activityResultLauncher.launch(intent);
                } else {
                    requestPermissionForCamera();
                }
            } else if (result == PackageManager.PERMISSION_GRANTED) {
                openCameraActivity();
            }
        }
    }


}