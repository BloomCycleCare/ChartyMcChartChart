package com.roamingroths.cmcc.ui.init;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.roamingroths.cmcc.R;

public class SplashFragment extends Fragment {

  private static boolean DEBUG = true;
  private static String TAG = SplashFragment.class.getSimpleName();

  private ProgressBar mProgressBar;
  private TextView mErrorView;
  private TextView mStatusView;

  public SplashFragment() {
    // Required empty public constructor
  }

  public static SplashFragment newInstance() {
    SplashFragment fragment = new SplashFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_splash, container, false);

    mProgressBar = view.findViewById(R.id.splash_progress);
    mErrorView = view.findViewById(R.id.splash_error_tv);
    mStatusView = view.findViewById(R.id.splash_status_tv);

    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  public void showProgress(final String message) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mProgressBar.setVisibility(View.VISIBLE);

        updateStatus(message);
        mStatusView.setVisibility(View.VISIBLE);

        mErrorView.setText("");
        mErrorView.setVisibility(View.INVISIBLE);
      }
    });
  }

  public void updateStatus(final String status) {
    if (DEBUG) Log.v(TAG, "Update: " + status);
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mStatusView.setText(status);
      }
    });
  }

  public void showError(final String errorText) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mProgressBar.setVisibility(View.INVISIBLE);

        mStatusView.setVisibility(View.INVISIBLE);

        mErrorView.setText(errorText);
        mErrorView.setVisibility(View.VISIBLE);
      }
    });
  }

}
