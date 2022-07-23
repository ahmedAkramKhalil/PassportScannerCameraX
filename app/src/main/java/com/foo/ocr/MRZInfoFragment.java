package com.foo.ocr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.foo.ocr.mrzdecoder.MrzRecord;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MRZInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MRZInfoFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
//    private MrzRecord mParam1 ;
//    private String mParam2;

    public MRZInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MRZInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
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
//        if (getArguments() != null) {
//            mParam1 = getArguments().getParcelable(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    MrzRecord mrzInfo;
    TextView name, gName, documentNumber, issuingCountry, nationality, dateOfBirth, gender, expirationDate, others;
    CheckBox documentCH, birthCH, expirationCH;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if ( getArguments().getSerializable(ARG_PARAM1) != null) {
            mrzInfo = (MrzRecord) getArguments().getSerializable(ARG_PARAM1);

            boolean validDocumentNumber = mrzInfo.validDocumentNumber;
            boolean validDateOfBirth = mrzInfo.validDateOfBirth;
            boolean validExpirationDate = mrzInfo.validExpirationDate;

            documentCH = view.findViewById(R.id.validDocumentNumber);
            birthCH = view.findViewById(R.id.validDateOfBirth);
            expirationCH = view.findViewById(R.id.validExpirationDate);

            name = view.findViewById(R.id.name);
            gName = view.findViewById(R.id.gname);
            documentNumber = view.findViewById(R.id.document_number);
            issuingCountry = view.findViewById(R.id.issuing_c);
            nationality = view.findViewById(R.id.nationality);
            dateOfBirth = view.findViewById(R.id.date_of_birth);
            gender = view.findViewById(R.id.gender);
            expirationDate = view.findViewById(R.id.expiration_date);
            others = view.findViewById(R.id.others);
            name.setText(mrzInfo.surname);
            gName.setText(mrzInfo.givenNames);
            documentNumber.setText(mrzInfo.documentNumber);
            issuingCountry.setText(mrzInfo.issuingCountry);
            nationality.setText(mrzInfo.nationality);
            dateOfBirth.setText(mrzInfo.dateOfBirth.toString());
            gender.setText(mrzInfo.sex.name());
            expirationDate.setText(mrzInfo.expirationDate.toString());
            others.setText(mrzInfo.toString());
            documentCH.setChecked(validDocumentNumber);
            birthCH.setChecked(validDateOfBirth);
            expirationCH.setChecked(validExpirationDate);

        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mrz_info, container, false);
    }
}