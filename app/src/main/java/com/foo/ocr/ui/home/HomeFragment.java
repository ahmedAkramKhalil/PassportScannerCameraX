package com.foo.ocr.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
//import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foo.ocr.MRZRecyclerAdapter;
import com.foo.ocr.MyApplication;
import com.foo.ocr.PassportScanner;
import com.foo.ocr.StateData;
import com.foo.ocr.databinding.FragmentHomeBinding;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        viewModel = ViewModelProviders.of(getActivity()).get(PassportDetailedViewModel.class);
//        if (PassportScanner.getInstance() != null) {

//        viewModel.getMrzDateStateLiveData().observe(getViewLifecycleOwner(), new Observer<StateData<MrzRecord>>() {
        MyApplication.get().dataRepository.getMrzData().observe(getViewLifecycleOwner(), new Observer<StateData<MrzRecord>>() {
            @Override
            public void onChanged(StateData<MrzRecord> mrzDateStateData) {
                Log.d("LOOG", "viewModel.mrzDateStateData   onChanged");

                switch (mrzDateStateData.getStatus()) {
                    case CREATED:
                    case LOADING:
                    case SUCCESS:
                        Log.d("LOOG", "viewModel.getMrzDateStateLiveData   onChanged SUCCESS");

//                        popBackStackTillEntry(0);
//                        FragmentContainerView container = binding.fragmentContainerView;
//                        if (container != null)
//                        container.setVisibility(View.GONE);
//                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//                        getFragmentManager().popBackStack();
//                        ft.replace(R.id.fragment_container_view, null);
//                        ft.commit();

                        initRecycler(mrzDateStateData.getData());
                        break;
                    case COMPLETE:
                    case ERROR:

                }

            }
        });

//        MyApplication.get().dataRepository.getData().observe();

//        viewModel.getPassportDetailsMutableLiveData().observe(getViewLifecycleOwner(), new Observer<StateData<PassportDetails>>() {
        MyApplication.get().dataRepository.getData().observe(getViewLifecycleOwner(), new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                Log.d("LOOG", "viewModel.onChanged   onChanged");

                switch (passportDetailsStateData.getStatus()) {
                    case CREATED:
                    case LOADING:
                        Log.d("LOOG", "viewModel.getPassportDetailsMutableLiveData   LOADING");
                        break;
                    case SUCCESS:
                        Log.d("LOOG", "viewModel.getPassportDetailsMutableLiveData   SUCCESS");
                        if (passportDetailsStateData.getData() != null && passportDetailsStateData.getData().getPersonalPicture() != null)
                            binding.imageView.setImageBitmap(passportDetailsStateData.getData().getPersonalPicture());
                        if (passportDetailsStateData.getData() != null && passportDetailsStateData.getData().getPassportPhoto() != null)
                            binding.imageView2.setImageBitmap(passportDetailsStateData.getData().getPassportPhoto());
                        MyApplication.get().dataRepository.clear();

                        break;
                    case COMPLETE:
                    case ERROR:
                }
            }
        });
        return root;
    }

    void initRecycler(MrzRecord mrzRecord) {
        if (mrzRecord != null) {
            RecyclerView recyclerView = binding.mrzRecycler;
            MRZRecyclerAdapter adapter = new MRZRecyclerAdapter(mrzRecord.toList());
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}