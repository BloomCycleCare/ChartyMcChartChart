package com.bloomcyclecare.cmcc.ui.goals.list;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.mvi.MviView;
import com.bloomcyclecare.cmcc.ui.goals.create.CreateGoalActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by parkeroth on 11/13/17.
 */

public class GoalListFragment extends Fragment implements MviView<GoalListIntent, GoalListViewState> {

  public static int REQUEST_ADD_GOAL = 1;

  private static boolean DEBUG = true;
  private static String TAG = GoalListFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private TextView mAddGoalsView;
  private GoalListAdapter mAdapter;
  private GoalListViewModel mViewModel;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private PublishSubject<GoalListIntent.RefreshIntent> mRefreshIntentPublisher =
      PublishSubject.create();

  public static GoalListFragment newInstance() {
    return new GoalListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAdapter = new GoalListAdapter(getContext());
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mViewModel = ViewModelProviders.of(this, ChartingApp.viewModelFactory()).get(GoalListViewModel.class);

    mDisposables.add(mViewModel.states().subscribe(this::render));
    mViewModel.processIntents(intents());

    // TODO: hook up adapter click listener observable here
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    mDisposables.dispose();
  }

  @Override
  public void onResume() {
    super.onResume();
    mRefreshIntentPublisher.onNext(GoalListIntent.RefreshIntent.create());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO: look for save success and prompt
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_goal_list, container, false);

    mAddGoalsView = view.findViewById(R.id.tv_add_goals);
    mRecyclerView = view.findViewById(R.id.recyclerview_goal_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mAdapter);
    mAdapter.notifyDataSetChanged();

    Activity activity = getActivity();

    if (activity != null) {
      FloatingActionButton fab = activity.findViewById(R.id.fab_new_goal);
      if (fab == null) {
        Log.w(TAG, "FAB reference is NULL!");
      } else {
        fab.setOnClickListener(ignored -> showAddTask());
      }
    }

    hideList();

    return view;
  }

  private void showAddTask() {
    Intent intent = new Intent(getContext(), CreateGoalActivity.class);
    startActivityForResult(intent, REQUEST_ADD_GOAL);
  }

  private void showList() {
    mRecyclerView.setVisibility(View.VISIBLE);
    mAddGoalsView.setVisibility(View.GONE);
  }

  private void hideList() {
    mRecyclerView.setVisibility(View.GONE);
    mAddGoalsView.setVisibility(View.VISIBLE);
  }

  @Override
  public Observable<GoalListIntent> intents() {
    return Observable.merge(initialIntent(), refreshIntent());
  }

  private Observable<GoalListIntent.InitialIntent> initialIntent() {
    return Observable.just(GoalListIntent.InitialIntent.create());
  }

  private Observable<GoalListIntent.RefreshIntent> refreshIntent() {
    return mRefreshIntentPublisher;
  }

  @Override
  public void render(GoalListViewState state) {
    if (state.error() != null) {
      showMessage("Error loading goals");
      return;
    }

    if (state.goals().isEmpty()) {
      switch (state.goalFilterType()) {
        case ALL:
        case ACTIVE:
        case ARCHIVED:
          hideList();
          break;
      }
    } else {
      mAdapter.updateGoals(state.goals());
      showList();
    }
  }

  private void showMessage(String message) {
    View view = getView();
    if (view == null) return;
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }
}
