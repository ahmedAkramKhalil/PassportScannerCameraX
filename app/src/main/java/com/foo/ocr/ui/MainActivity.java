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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;


import com.foo.ocr.databinding.ActivityMainBinding;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.ui.home.HomeFragment;
import com.foo.ocr.util.AppUtil;
import com.foo.ocr.util.PermissionUtil;
import com.foo.ocr.R;
import com.foo.ocr.mrzdecoder.MrzRecord;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public static final String PASSPORT_DETAILS_RESULT = "MRZ_RESULT";
    public static PassportDetails passportDetails;
    public static boolean isObjectDetection = false;


    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();

//                        PassportDetails passportDetails = (PassportDetails) intent.getSerializableExtra(PASSPORT_DETAILS_RESULT);
//                        PassportDetails passportDetails = ;
//                                if (passportDetails != null) {
                                    FragmentContainerView container = findViewById(R.id.fragment_container_view);
                                    container.setVisibility(View.VISIBLE);
                                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                                    Fragment f = new HomeFragment();
                                    ft.replace(R.id.fragment_container_view, f);
                                    ft.commit();
//                                }

//                        observer = RxBus.getRecord().subscribeWith(new DisposableObserver<MrzRecord>() {
//                            @Override
//                            public void onNext(@io.reactivex.rxjava3.annotations.NonNull MrzRecord mrzRecord) {
//                                /**
//                                 * This code should be replaced with targeted UI Component to show the MRZ Result Data
//                                 * NOTE: for showing Picture of person and passport image you should subscribe to FaceDetectorHelper class Observer
//                                 * such as the example in MRZInfoFragment class
//                                 */
//                                if (mrzRecord != null) {
//                                    FragmentContainerView container = findViewById(R.id.fragment_container_view);
//                                    container.setVisibility(View.VISIBLE);
//                                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//                                    Fragment fragment = MRZInfoFragment.newInstance(mrzRecord);
//                                    ft.replace(R.id.fragment_container_view, fragment);
//                                    ft.commit();
//                                }
//
//                            }
//                            @Override
//                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
//                                /**
//                                 * Camera Process Finished with failed detecting MRZ code
//                                 * handling it depending on situation
//                                 */
//                            }
//                            @Override
//                            public void onComplete() {
//                            }
//                        });
                    }}
            }
    );



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//
//        if (savedInstanceState == null) {
//            CameraFragment fragment = CameraFragment.newInstance() ;
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.fragment_container_view, fragment)
//                    .commit();
//        }



//        viewModel = ViewModelProviders.of(this).get(PassportDetailedViewModel.class);
////        if (PassportScanner.getInstance() != null) {
//            PassportScanner.getMrzLiveData().observe(this, new Observer<StateData<MrzRecord>>() {
//                @Override
//                public void onChanged(StateData<MrzRecord> mrzDateStateData) {
//                    switch (mrzDateStateData.getStatus()) {
//                        case CREATED:
//                        case LOADING:
//                        case SUCCESS:
//                            Log.d("LOOG", "viewModel.getPassportDetailsMutableLiveData   onChanged");
//
////                        popBackStackTillEntry(0);
////                        FragmentContainerView container = binding.fragmentContainerView;
////                        if (container != null)
////                        container.setVisibility(View.GONE);
////                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
////                        getFragmentManager().popBackStack();
////                        ft.replace(R.id.fragment_container_view, null);
////                        ft.commit();
//
//                            initRecycler(mrzDateStateData.getData());
//                            break;
//                        case COMPLETE:
//                        case ERROR:
//
//                    }
//
//                }
//            });
//
//
//            PassportScanner.getPassportDetailsLiveData().observe(this, new Observer<StateData<PassportDetails>>() {
//                @Override
//                public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
//                    switch (passportDetailsStateData.getStatus()) {
//                        case CREATED:
//                        case LOADING:
//                            Log.d("LOOG", "viewModel.getPassportDetailsMutableLiveData   LOADING");
//                            break;
//                        case SUCCESS:
//                            Log.d("LOOG", "viewModel.getPassportDetailsMutableLiveData   SUCCESS");
//                            if (passportDetailsStateData.getData() != null && passportDetailsStateData.getData().getPersonalPicture() != null)
//                                binding.imageView.setImageBitmap(passportDetailsStateData.getData().getPersonalPicture());
//                            if (passportDetailsStateData.getData() != null && passportDetailsStateData.getData().getPassportPhoto() != null)
//                                binding.imageView2.setImageBitmap(passportDetailsStateData.getData().getPassportPhoto());
//                            break;
//                        case COMPLETE:
//                        case ERROR:
//
//                    }
//                }
//            });

//        }

//        FragmentContainerView container = findViewById(R.id.fragment_container_view);
//        container.setVisibility(View.VISIBLE);
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        Fragment fragment = CameraFragment.newInstance();
//        ft.replace(R.id.fragment_container_view, fragment);
//        ft.commit();


        binding.scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissionForCamera();
            }
        });


    }

    public void popBackStackTillEntry(int entryIndex) {

        if (getSupportFragmentManager() == null) {
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() <= entryIndex) {
            return;
        }
        FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(
                entryIndex);
        if (entry != null) {
            getSupportFragmentManager().popBackStackImmediate(entry.getId(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }


    }

//    void initRecycler(MrzRecord mrzRecord) {
//        if (mrzRecord != null) {
//            RecyclerView recyclerView = binding.mrzRecycler;
//            MRZRecyclerAdapter adapter = new MRZRecyclerAdapter(mrzRecord.toList());
//            recyclerView.setHasFixedSize(true);
//            recyclerView.setLayoutManager(new LinearLayoutManager(this));
//            recyclerView.setAdapter(adapter);
//        }
//    }


    void openCameraActivity() {


        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        activityResultLauncher.launch(intent);

//        passportDetails = null;
//        if (fragment != null)
//        fragment.finish();
//        getSupportFragmentManager().popBackStack();
//        startActivity(intent);


//        FragmentContainerView container = findViewById(R.id.fragment_container_view);
//        container.setVisibility(View.VISIBLE);
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        Fragment fragment = CameraFragment.newInstance();
//        ft.replace(R.id.fragment_container_view, fragment);
//        ft.commit();
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

//                    openCameraActivity();
                    activityResultLauncher.launch(intent);


                } else {
                    requestPermissionForCamera();
                }
            } else if (result == PackageManager.PERMISSION_GRANTED) {
                openCameraActivity();
            }
        }
    }




    public void navigateToMRZDetail() {
//        Fragment fragment = MRZInfoFragment.newInstance();
//
//NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
//        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
//        NavController navController = navHostFragment.getNavController();
//        setupActionBarWithNavController(this, navController);
//        navController.navigate(R.id.action_cameraFragment_to_MRZInfoFragment);

//        Navigation.findNavController(this,R.id.my_nav_host_fragment).navigate(R.id.action_cameraFragment_to_MRZInfoFragment);

//        getSupportFragmentManager().beginTransaction()
//                .addToBackStack("null")
//                .add(R.id.fragment_container_view, fragment)
//                .commit();
////
//        FragmentContainerView container = findViewById(R.id.my_nav_host_fragment);
//        container.setVisibility(View.VISIBLE);
//        FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
//        Fragment fragment = BlankFragment.newInstance("ff","ff");
//        ft.replace(R.id.fragment_container_view, fragment);
//        ft.add(fragment,"ff");

//        ft.commit();


    }


}