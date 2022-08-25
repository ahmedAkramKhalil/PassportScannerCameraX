package com.foo.ocr.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foo.ocr.R;
import com.foo.ocr.ui.adapter.MRZRecyclerAdapter;
import com.foo.ocr.ScannerApplication;
import com.foo.ocr.StateData;
import com.foo.ocr.databinding.FragmentHomeBinding;
import com.foo.ocr.model.PassportDetails;
import com.foo.ocr.mrzdecoder.MrzRecord;

public class DetailsFragment extends Fragment {
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
//        ScannerApplication.get().dataRepository.getMrzData().observe(getViewLifecycleOwner(), new Observer<StateData<MrzRecord>>() {
//            @Override
//            public void onChanged(StateData<MrzRecord> mrzDateStateData) {
//                Log.d("LOOG", "viewModel.mrzDateStateData   onChanged");
//                switch (mrzDateStateData.getStatus()) {
//                    case CREATED:
//                    case LOADING:
//                    case SUCCESS:
//                        Log.d("LOOG", "viewModel.getMrzDateStateLiveData   onChanged SUCCESS");
////                        Toast.makeText(getContext(),"MRZ successfully detected",Toast.LENGTH_LONG).show();
////                        initRecycler(mrzDateStateData.getData());
//                        break;
//                    case COMPLETE:
//                    case ERROR:
//                }
//
//            }
//        });

        ScannerApplication.get().dataRepository.getData().observe(getViewLifecycleOwner(), new Observer<StateData<PassportDetails>>() {
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
                        if (passportDetailsStateData.getData() != null) {
                            if (passportDetailsStateData.getData().getPassportPhoto() != null) {
                                initRecycler(passportDetailsStateData.getData().getMrzRecord());
                                binding.imageView2.setImageBitmap(passportDetailsStateData.getData().getPassportPhoto());
                                binding.imageView.setImageBitmap(passportDetailsStateData.getData().getPersonalPicture());
                            }else {
                                Toast.makeText(getContext(), "Failed to detect passport picture.. please try again", Toast.LENGTH_LONG).show();
                                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                                ft.remove(DetailsFragment.this);
                                ft.commit();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to detect passport picture.. please try again", Toast.LENGTH_LONG).show();
                            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                            ft.remove(DetailsFragment.this);
                            ft.commit();
                        }
//                        .get().dataRepository.clear();
                        break;
                    case COMPLETE:
                    case ERROR:
                        Log.d("LOOG", "DetailsFragment => getMrzDateStateLiveData =>  ERROR ERROR");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}