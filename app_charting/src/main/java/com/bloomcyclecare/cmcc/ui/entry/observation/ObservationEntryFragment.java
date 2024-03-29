package com.bloomcyclecare.cmcc.ui.entry.observation;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailActivity;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailViewModel;
import com.bloomcyclecare.cmcc.ui.showcase.ShowcaseManager;
import com.google.common.base.Strings;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.functions.Action;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * Created by parkeroth on 9/11/17.
 */

public class ObservationEntryFragment extends Fragment {

  public static final int OK_RESPONSE = 0;

  private EntryDetailViewModel mEntryDetailViewModel;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mEntryDetailViewModel = ViewModelProviders.of(getActivity()).get(EntryDetailViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_entry, container, false);

    View clarifyingQuestionsLayout = view.findViewById(R.id.clarifying_question_layout);
    RecyclerView clarifyingQuestionsView = view.findViewById(R.id.clarifying_question_recyclerview);
    clarifyingQuestionsView.setLayoutManager(new LinearLayoutManager(getActivity()));
    clarifyingQuestionsView.addItemDecoration(new DividerItemDecoration(getContext(), 0));

    ClarifyingQuestionAdapter clarifyingQuestionAdapter = new ClarifyingQuestionAdapter(
        getContext(), mEntryDetailViewModel.clarifyingQuestionUpdates);
    clarifyingQuestionsView.setAdapter(clarifyingQuestionAdapter);

    TextView observationDescriptionTextView =
        view.findViewById(R.id.tv_modify_observation_description);
    EditText observationEditText = view.findViewById(R.id.et_modify_observation_value);
    TextView noteTextView =
        view.findViewById(R.id.et_modify_observation_note);
    Switch peakDaySwitch = view.findViewById(R.id.switch_peak_day);
    Switch intercourseSwitch = view.findViewById(R.id.switch_intercourse);
    Switch uncertainSwitch = view.findViewById(R.id.switch_uncertain);
    Switch firstDaySwitch = view.findViewById(R.id.switch_new_cycle);
    Switch pointOfChangeSwitch = view.findViewById(R.id.switch_point_of_change);
    Switch pregnancyTestSwitch = view.findViewById(R.id.switch_pregancy_test);
    Spinner intercourseSpinner = view.findViewById(R.id.spinner_intercourse);

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.intercourse_times_of_day, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    intercourseSpinner.setAdapter(adapter);
    intercourseSpinner.setVisibility(View.GONE);

    View pointOfChangeLayout = view.findViewById(R.id.point_of_change_layout);
    View firstDayLayout = view.findViewById(R.id.layout_new_cycle);
    View pregnancyTestLayout = view.findViewById(R.id.layout_pregancy_test);

    Action connectStreams = () -> {
      Timber.d("Connecting RX streams for UI updates");
      RxCompoundButton
          .checkedChanges(peakDaySwitch)
          .subscribe(mEntryDetailViewModel.peakDayUpdates);
      RxCompoundButton
          .checkedChanges(intercourseSwitch)
          .subscribe(mEntryDetailViewModel.intercourseUpdates);
      RxCompoundButton
          .checkedChanges(firstDaySwitch)
          .subscribe(mEntryDetailViewModel.firstDayOfCycleUpdates);
      RxCompoundButton
          .checkedChanges(pregnancyTestSwitch)
          .subscribe(mEntryDetailViewModel.positivePregnancyTestUpdates);
      RxCompoundButton
          .checkedChanges(pointOfChangeSwitch)
          .subscribe(mEntryDetailViewModel.pointOfChangeUpdates);
      RxCompoundButton
          .checkedChanges(uncertainSwitch)
          .subscribe(mEntryDetailViewModel.uncertainUpdates);

      RxAdapterView.itemSelections(intercourseSpinner)
          .map(ordinal -> IntercourseTimeOfDay.values()[ordinal])
          .subscribe(mEntryDetailViewModel.timeOfDayUpdates);

      RxTextView.afterTextChangeEvents(observationEditText)
          .map(event -> event.view().getText().toString())
          .subscribe(mEntryDetailViewModel.observationUpdates);

      RxTextView.afterTextChangeEvents(noteTextView)
          .map(event -> event.view().getText().toString())
          .subscribe(mEntryDetailViewModel.noteUpdates);
    };

    final AtomicBoolean uiInitialized = new AtomicBoolean(false);
    mEntryDetailViewModel.viewStates().observe(getViewLifecycleOwner(), (viewState -> {
      Timber.d("Updating ViewState");
      boolean hideOnFirstEntry = viewState.entryModificationContext.isFirstEntry && (
          !viewState.entryModificationContext.hasPreviousCycle || viewState.isInPregnancy);
      if (hideOnFirstEntry || viewState.chartEntry.observationEntry.positivePregnancyTest) {
        firstDayLayout.setVisibility(View.GONE);
      }

      boolean showClarifyingQuestions = viewState.renderClarifyingQuestions() && !viewState.clarifyingQuestionState.isEmpty();
      clarifyingQuestionsLayout.setVisibility(showClarifyingQuestions ? View.VISIBLE : View.GONE);
      clarifyingQuestionAdapter.updateQuestions(viewState.clarifyingQuestionState);

      boolean hasValidObservation = false;
      ObservationEntry observationEntry = viewState.chartEntry.observationEntry;
      if (!Strings.isNullOrEmpty(viewState.observationErrorText)) {
        Timber.d("Found invalid observation");
        String existingErrorText = observationEditText.getError() == null
            ? "" : observationEditText.getError().toString();
        if (!existingErrorText.equals(viewState.observationErrorText)) {
          observationEditText.setError(viewState.observationErrorText);
          observationDescriptionTextView.setText(null);
        } else {
          Timber.v("Error text already updated");
        }
      } else {
        Timber.d("Found valid observation");
        observationEditText.setError(null);
        Observation observation = observationEntry.observation;
        if (observation != null) {
          hasValidObservation = true;
          String newText = observation.toString();
          String existingText = observationEditText.getText() == null ? null : observationEditText.getText().toString();
          if (existingText == null || existingText.equals(newText)) {
            observationEditText.setText(newText);
            observationEditText.setSelection(newText.length());
            observationDescriptionTextView.setText(observation.getDescription());
          }
          observationEditText.setText(observation.toString());
          observationEditText.setSelection(newText.length());
          observationDescriptionTextView.setText(observation.getDescription());
        }
      }
      intercourseSpinner.setSelection(observationEntry.intercourseTimeOfDay.ordinal());
      if (observationEntry.intercourse) {
        intercourseSpinner.setVisibility(View.VISIBLE);
      } else {
        intercourseSpinner.setVisibility(View.GONE);
      }
      if (maybeUpdate(pregnancyTestSwitch, observationEntry.positivePregnancyTest)) {
        pregnancyTestLayout.setVisibility(View.VISIBLE);
        Toast.makeText(requireContext(), "Congrats!", Toast.LENGTH_LONG).show();
      } else {
        pregnancyTestLayout.setVisibility(View.GONE);
      }
      maybeUpdate(intercourseSwitch, observationEntry.intercourse);
      maybeUpdate(peakDaySwitch, observationEntry.peakDay);
      maybeUpdate(firstDaySwitch, observationEntry.firstDay);
      maybeUpdate(pointOfChangeSwitch, observationEntry.pointOfChange);
      maybeUpdate(uncertainSwitch, observationEntry.uncertain);
      if (!Strings.isNullOrEmpty(observationEntry.note)
          && !observationEntry.note.equals(noteTextView.getText().toString())) {
        noteTextView.setText(observationEntry.note);
      }

      pointOfChangeLayout.setVisibility(viewState.showPointOfChange() ? View.VISIBLE : View.GONE);

      if (uiInitialized.compareAndSet(false, true)) {
        try {
          connectStreams.run();
        } catch (Exception e) {
          Timber.e(e);
        }
      }

      ShowcaseManager showcaseManager =
          ChartingApp.cast(requireActivity().getApplication()).showcaseManager();
      showcaseManager.showShowcase(ShowcaseManager.ShowcaseID.ENTRY_DETAIL_INPUT_OBSERVATION, observationEditText);

      EntryDetailActivity activity = (EntryDetailActivity) requireActivity();
      Menu menu = activity.getMenu();

      if (hasValidObservation) {
        ShowcaseManager.SequenceBuilder builder = ShowcaseManager.sequenceBuilder(ShowcaseManager.SequenceID.ENTRY_DETAIL_PAGE, requireActivity());
        if (showcaseManager.shouldShowcase(ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_DESCRIPTION)) {
          builder.addShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_DESCRIPTION,
              observationDescriptionTextView,
              new IShowcaseListener() {
                @Override
                public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
                  InputMethodManager inputManager = (InputMethodManager)
                      requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                  inputManager.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(),
                      InputMethodManager.HIDE_NOT_ALWAYS);
                }
                @Override
                public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {}
              });
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_PEAK_DAY, peakDaySwitch, builder);
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_INTERCOURSE, intercourseSwitch, builder);
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_FIRST_DAY_NEW_CYCLE, firstDaySwitch, builder);
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_NOTE, noteTextView, builder);
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_EXTRA_TOGGLES, view.findViewById(R.id.observation_entry_layout), builder);
        }
        if (menu == null) {
          Timber.w("Null menu, skipping ENTRY_DETAIL_EXPLAIN_SAVE showcase");
        } else {
          showcaseManager.maybeAddShowcase(
              ShowcaseManager.ShowcaseID.ENTRY_DETAIL_EXPLAIN_SAVE,
              activity.findViewById(activity.getMenu().findItem(R.id.action_save).getItemId()),
              builder);
        }
        builder.build();
      }
    }));

    return view;
  }

  private static boolean maybeUpdate(Switch view, boolean value) {
    if (view.isChecked() != value) {
      Timber.d("Updating %s", view.getResources().getResourceEntryName(view.getId()));
      view.setChecked(value);
    }
    return value;
  }
}
