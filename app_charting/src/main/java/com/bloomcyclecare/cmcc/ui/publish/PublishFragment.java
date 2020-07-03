package com.bloomcyclecare.cmcc.ui.publish;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

public class PublishFragment extends Fragment {

  private PublishViewModel mPublishViewModel;
  private MainViewModel mMainViewModel;

  private Switch mPublishEnabledSwitch;
  private Button mConnectButton;
  private TextView mSummaryTextView;
  private TextView mDriveLinkTextView;
  private TextView mTimeLastPublishedTextView;
  private TextView mTimeLastTriggeredTextView;
  private TextView mNumOutstandingTextView;
  private Group mStatsGroup;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mPublishViewModel = new ViewModelProvider(this).get(PublishViewModel.class);

    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    mMainViewModel.updateTitle("Cloud Publish");
    mMainViewModel.updateSubtitle("");

    View view = inflater.inflate(R.layout.fragment_cloud_publish, container, false);

    mDriveLinkTextView = view.findViewById(R.id.tv_drive_link);
    mSummaryTextView = view.findViewById(R.id.tv_account_status);
    mConnectButton = view.findViewById(R.id.button_connect_drive);
    mPublishEnabledSwitch = view.findViewById(R.id.publish_enabled_switch);
    mTimeLastPublishedTextView = view.findViewById(R.id.tv_time_last_published);
    mTimeLastTriggeredTextView = view.findViewById(R.id.tv_time_last_triggered);
    mNumOutstandingTextView = view.findViewById(R.id.tv_renders_outstanding);
    mStatsGroup = view.findViewById(R.id.stats_group);


    mPublishViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    RxCompoundButton.checkedChanges(mPublishEnabledSwitch)
        .skipInitialValue()
        .subscribe(mPublishViewModel.publishEnabledObserver());

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_publish, menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_share:
        NavHostFragment.findNavController(this)
            .navigate(PublishFragmentDirections.actionShare(ViewMode.CHARTING));
        return true;
      case R.id.action_sync:
        mPublishViewModel.manualTriggerObserver().onNext(true);
        return true;
      default:
        return NavigationUI.onNavDestinationSelected(
            item, NavHostFragment.findNavController(this));
    }
  }

  @Override
  public void onResume() {
    mPublishViewModel.checkAccount();
    super.onResume();
  }

  private void render(PublishViewModel.ViewState viewState) {
    boolean hasAccount = viewState.account().isPresent();

    // Configure publish enable switch
    mPublishEnabledSwitch.setEnabled(hasAccount);
    if (mPublishEnabledSwitch.isChecked() != viewState.publishEnabled()) {
      mPublishEnabledSwitch.setChecked(viewState.publishEnabled());
    }

    // Configure sign in/out button
    mConnectButton.setText(hasAccount ? "Sign Out" : "Sign In");
    if (hasAccount) {
      mConnectButton.setOnClickListener(v -> mPublishViewModel.signOut());
    } else {
      mConnectButton.setOnClickListener(v -> {
        startActivityForResult(GoogleAuthHelper.getPromptIntent(requireContext()), 1);
      });
    }

    // Configure sign in/out summary
    if (hasAccount) {
      GoogleSignInAccount account = viewState.account().get();
      mSummaryTextView.setText(String.format("Signed in as %s", account.getEmail()));
    } else {
      mSummaryTextView.setText("Not currently signed in.");
    }

    // Configure Drive link
    if (hasAccount && viewState.publishEnabled() && viewState.myChartsLink().isPresent()) {
      mDriveLinkTextView.setText(Html.fromHtml(String.format(
          "<a href=\"%s\">Go to \"My Charts\" in Drive</a>",
          viewState.myChartsLink().get()), FROM_HTML_MODE_COMPACT));
      mDriveLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());
    } else {
      mDriveLinkTextView.setText("");
    }

    // Configured stats visibility
    mStatsGroup.setVisibility(viewState.publishEnabled() ? View.VISIBLE : View.GONE);

    // Configure time last published
    if (!viewState.publishEnabled()) {
      mTimeLastPublishedTextView.setText("Feature disabled");
    } else if (!viewState.lastSuccessTimeMs().isPresent()){
      mTimeLastPublishedTextView.setText("TBD");
    } else {
      mTimeLastPublishedTextView.setText(DateUtil.toUiTimeStr(viewState.lastSuccessTimeMs().get()));
    }

    // Configure time last triggered
    if (!viewState.publishEnabled()) {
      mTimeLastTriggeredTextView.setText("Feature disabled");
    } else if (!viewState.lastEncueueTimeMs().isPresent()){
      mTimeLastTriggeredTextView.setText("TBD");
    } else {
      mTimeLastTriggeredTextView.setText(DateUtil.toUiTimeStr(viewState.lastEncueueTimeMs().get()));
    }

    // Configure num outstanding
    if (!viewState.publishEnabled()) {
      mNumOutstandingTextView.setText("Feature disabled");
    } else {
      mNumOutstandingTextView.setText(String.valueOf(viewState.itemsOutstanding().orElse(0)));
    }
  }
}
