package com.bloomcyclecare.cmcc.ui.sharing;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

public class SharingFragment extends Fragment {

  private SharingViewModel mSharingViewModel;
  private MainViewModel mMainViewModel;

  private Button mConnectButton;
  private TextView mSummaryTextView;
  private TextView mDriveLinkTextView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mSharingViewModel = new ViewModelProvider(this).get(SharingViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_sharing, container, false);

    mDriveLinkTextView = view.findViewById(R.id.tv_drive_link);
    mSummaryTextView = view.findViewById(R.id.tv_account_status);
    mConnectButton = view.findViewById(R.id.button_connect_drive);

    mSharingViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    mMainViewModel.updateSubtitle("");

    return view;
  }

  @Override
  public void onResume() {
    mSharingViewModel.checkAccount();
    super.onResume();
  }

  private void render(SharingViewModel.ViewState viewState) {
    if (viewState.account().isPresent()) {
      GoogleSignInAccount account = viewState.account().get();
      mSummaryTextView.setText(String.format("Signed in as %s", account.getEmail()));
      if (viewState.myChartsLink().isPresent()) {
        mDriveLinkTextView.setText(Html.fromHtml(String.format(
            "<a href=\"%s\">Go to \"My Charts\" in Drive</a>",
            viewState.myChartsLink().get()), FROM_HTML_MODE_COMPACT));
        mDriveLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());
      } else {
        mDriveLinkTextView.setText("");
      }
      mConnectButton.setText("Sign Out");
      mConnectButton.setOnClickListener(v -> mSharingViewModel.signOut());
    } else {
      mSummaryTextView.setText("Not currently signed in.");
      mDriveLinkTextView.setText("");
      mConnectButton.setText("Sign In");
      mConnectButton.setOnClickListener(v -> {
        startActivityForResult(GoogleAuthHelper.getPromptIntent(requireContext()), 1);
      });
    }
  }
}
