package com.bloomcyclecare.cmcc.practitioner.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.practitioner.R;

import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main, container, false);

    Button appLinkButton = view.findViewById(R.id.button_app_link);
    appLinkButton.setOnClickListener(v -> {
      Toast.makeText(requireContext(), "Click", Toast.LENGTH_SHORT).show();
    });

    return view;
  }
}
