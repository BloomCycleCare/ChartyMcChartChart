package com.roamingroths.cmcc.ui.appointments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 11/13/17.
 */

public class AppointmentListFragment extends Fragment implements AppointmentAdapter.OnClickHandler {

  private static boolean DEBUG = true;
  private static String TAG = AppointmentListFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private TextView mAddAppointmentsView;
  private AppointmentAdapter mAdapter;

  public AppointmentListFragment() {
    if (DEBUG) Log.v(TAG, "Construct");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAdapter = new AppointmentAdapter(getContext());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_appointment_list, container, false);

    mAddAppointmentsView = view.findViewById(R.id.tv_add_appointments);
    mRecyclerView = view.findViewById(R.id.recyclerview_appointment_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mAdapter);
    mAdapter.notifyDataSetChanged();

    hideList();

    return view;
  }

  private void showList() {
    mRecyclerView.setVisibility(View.VISIBLE);
    mAddAppointmentsView.setVisibility(View.GONE);
  }

  private void hideList() {
    mRecyclerView.setVisibility(View.GONE);
    mAddAppointmentsView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onClick(int index) {

  }
}
