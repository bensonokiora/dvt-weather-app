package com.dvt.weatherapp.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.dvt.weatherapp.R;
import com.dvt.weatherapp.databinding.FragmentCityInfoBinding;
import com.dvt.weatherapp.model.CityInfo;


public class CityInfoFragment extends DialogFragment {

    private final CityInfo info;
    private Activity activity;
    private FragmentCityInfoBinding binding;

    public CityInfoFragment(CityInfo info) {
        this.info = info;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCityInfoBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        initVariables(view);
        return view;
    }

    private void initVariables(View view) {
        activity = getActivity();
        if (activity != null) {
            TextView city = view.findViewById(R.id.city);
            TextView country = view.findViewById(R.id.country);

            city.setText(info.getName());
            country.setText(info.getCountry());


        }

        binding.closeButton.setOnClickListener(v -> {
            dismiss();
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });


    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        return dialog;
    }

}
