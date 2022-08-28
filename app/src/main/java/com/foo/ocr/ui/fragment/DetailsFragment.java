package com.foo.ocr.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foo.ocr.R;
import com.foo.ocr.ui.adapter.MRZRecyclerAdapter;
import com.foo.ocr.MyApplication;
import com.foo.ocr.mrzscanner.StateData;
import com.foo.ocr.databinding.FragmentHomeBinding;
import com.foo.ocr.mrzscanner.model.PassportDetails;
import com.foo.ocr.mrzscanner.mrzdecoder.MrzRecord;

public class DetailsFragment extends Fragment {
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        MyApplication.get().dataRepository.getPassportDetailsStateLiveData().observe(getViewLifecycleOwner(), new Observer<StateData<PassportDetails>>() {
            @Override
            public void onChanged(StateData<PassportDetails> passportDetailsStateData) {
                switch (passportDetailsStateData.getStatus()) {
                    case SUCCESS:
                        if (passportDetailsStateData.getData() != null || passportDetailsStateData.getData().getPassportPhoto() != null) {
                            initRecycler(passportDetailsStateData.getData().getMrzRecord());
                            binding.imageView2.setImageBitmap(passportDetailsStateData.getData().getPassportPhoto());
                            binding.imageView.setImageBitmap(passportDetailsStateData.getData().getPersonalPicture());
                        } else {
                            // Back to the home screen and remove fragment if failure happened in MRZ detector
                            Toast.makeText(getContext(), getResources().getString(R.string.failed_to_passport_detailes), Toast.LENGTH_LONG).show();
                            backToHomeScreen();
                        }
                        break;
                    case ERROR:
                        backToHomeScreen();
                        break;

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

    private void backToHomeScreen() {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.remove(DetailsFragment.this);
        ft.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}