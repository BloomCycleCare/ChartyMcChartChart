package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MedicationDetailFragment extends Fragment {

  private final CompositeDisposable mDisposable = new CompositeDisposable();

  private MainViewModel mMainViewModel;
  private MedicationDetailViewModel mMedicationDetailViewModel;

  @Override
  public void onDestroy() {
    mDisposable.clear();
    super.onDestroy();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    MedicationDetailFragmentArgs args = MedicationDetailFragmentArgs.fromBundle(requireArguments());
    MedicationDetailViewModel.Factory factory = new MedicationDetailViewModel.Factory(
        requireActivity().getApplication(), args);
    mMedicationDetailViewModel = new ViewModelProvider(this, factory)
        .get(MedicationDetailViewModel.class);
    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        onBackPressed();
      }
    });
  }

  private void onBackPressed() {
    NavController navController = Navigation.findNavController(requireView());
    Context context = requireContext();
    mDisposable.add(mMedicationDetailViewModel.dirty().observeOn(AndroidSchedulers.mainThread()).subscribe(isDirty -> {
      if (!isDirty) {
        navController.popBackStack();
        return;
      }
      new AlertDialog.Builder(context)
          .setTitle("Save Changes?")
          .setMessage("Would you like to save your changes?")
          .setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            doSave(context, navController);
          })
          .setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
            navController.popBackStack();
          })
          .show();
    }));
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_medication_detail, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.action_save:
        doSave(requireContext(), Navigation.findNavController(requireView()));
        return true;
      case R.id.action_delete:
        mDisposable.add(mMedicationDetailViewModel.delete().observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
          Toast.makeText(requireContext(), "Medication deleted", Toast.LENGTH_SHORT).show();
          Navigation.findNavController(requireView()).popBackStack();
        }));
        return true;
    }
    return false;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_medication_detail, container, false);

    TextView nameView = view.findViewById(R.id.tv_medication_name_value);
    TextView descriptionView = view.findViewById(R.id.tv_medication_description_value);

    TextView noCurrentPrescription = view.findViewById(R.id.tv_no_current_prescription);
    View currentPrescriptionContainer = view.findViewById(R.id.current_prescription_container);
    TextView currentPrescriptionSummary = currentPrescriptionContainer.findViewById(R.id.tv_prescription_summary);
    View currentPrescriptionEditButton = currentPrescriptionContainer.findViewById(R.id.iv_edit_medication);

    TextView noPastPrescriptions = view.findViewById(R.id.tv_no_past_prescriptions);
    RecyclerView pastPrescriptionsView = view.findViewById(R.id.prescriptions);
    pastPrescriptionsView.setLayoutManager(new LinearLayoutManager(requireActivity()));
    PrescriptionAdapter pastPrescriptionAdapter = new PrescriptionAdapter(
        p -> navigateToDetailView(mMedicationDetailViewModel.initialValue(), p));
    pastPrescriptionsView.setAdapter(pastPrescriptionAdapter);

    FloatingActionButton fab = view.findViewById(R.id.prescription_fab);
    fab.setOnClickListener(v -> {
      Medication medication = mMedicationDetailViewModel.initialValue();
      if (medication.id() > 0) {
        navigateToDetailView(medication, null);
        return;
      }
      new AlertDialog.Builder(requireContext())
          .setTitle("Save Required")
          .setMessage("Please save the medication before adding prescriptions. Would you like to do this now?")
          .setPositiveButton("Yes", (dialog, which) -> {
            mDisposable.add(mMedicationDetailViewModel.save()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(savedMedication -> navigateToDetailView(savedMedication, null)));
          })
          .setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
          })
          .show();
    });

    AtomicBoolean initialized = new AtomicBoolean();
    mMedicationDetailViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      Timber.d("Rendering ViewState");
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);

      maybeUpdate(nameView, viewState.medication.name());
      maybeUpdate(descriptionView, viewState.medication.description());

      Optional<Prescription> currentPrescription = viewState.currentPrescription;
      if (currentPrescription.isPresent()) {
        noCurrentPrescription.setVisibility(View.GONE);
        currentPrescriptionContainer.setVisibility(View.VISIBLE);
        currentPrescriptionSummary.setText(currentPrescription.get().getSummary());
        currentPrescriptionEditButton.setOnClickListener(v -> {
          navigateToDetailView(viewState.medication, currentPrescription.get());
        });
        fab.setVisibility(View.GONE);
      } else {
        noCurrentPrescription.setVisibility(View.VISIBLE);
        currentPrescriptionContainer.setVisibility(View.GONE);
        currentPrescriptionSummary.setText("");
        fab.setVisibility(View.VISIBLE);
      }

      pastPrescriptionAdapter.updatePrescriptions(viewState.pastPrescriptions);
      boolean hasPastPrescriptions = !viewState.pastPrescriptions.isEmpty();
      noPastPrescriptions.setVisibility(hasPastPrescriptions ? View.GONE : View.VISIBLE);
      pastPrescriptionsView.setVisibility(hasPastPrescriptions ? View.VISIBLE : View.GONE);

      if (initialized.compareAndSet(false, true)) {
        RxTextView.textChanges(nameView).map(CharSequence::toString)
            .subscribe(mMedicationDetailViewModel.nameSubject);
        RxTextView.textChanges(descriptionView).map(CharSequence::toString)
            .subscribe(mMedicationDetailViewModel.descriptionSubject);
      }
    });

    return view;
  }

  private void navigateToDetailView(@NonNull Medication medication, @Nullable Prescription prescription) {
    MedicationDetailFragmentDirections.ActionEditPrescription action =
        MedicationDetailFragmentDirections.actionEditPrescription();
    action.setMedication(medication);
    if (prescription != null) {
      action.setPrescription(prescription);
    }
    Navigation.findNavController(requireView()).navigate(action);
  }

  private void doSave(Context context, NavController navController) {
    mDisposable.add(mMedicationDetailViewModel.save()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(medication -> {
          Toast.makeText(context, "Medication updated", Toast.LENGTH_SHORT).show();
          navController.popBackStack();
        }));
  }

  private static void maybeUpdate(TextView view, String value) {
    if (view.getText().toString().equals(value)) {
      return;
    }
    view.setText(value);
  }

  private static class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.ViewHolder> {

    private final List<Prescription> mPrescriptions = new ArrayList<>();
    private final Consumer<Prescription> editClickConsumer;

    private PrescriptionAdapter(Consumer<Prescription> editClickConsumer) {
      this.editClickConsumer = editClickConsumer;
    }


    public void updatePrescriptions(List<Prescription> prescriptions) {
      if (prescriptions.size() != mPrescriptions.size()) {
        mPrescriptions.clear();
        mPrescriptions.addAll(prescriptions);
        notifyDataSetChanged();
        return;
      }
      List<Prescription> toRemove = new ArrayList<>();
      List<Prescription> toAdd = new ArrayList<>();
      for (int i=0; i < mPrescriptions.size(); i++) {
        Prescription prescription = mPrescriptions.get(i);
        Prescription newPrescription = prescriptions.get(i);
        if (prescription.equals(newPrescription)) {
          continue;
        }
        toRemove.add(prescription);
        toAdd.add(newPrescription);
      }
      if (toRemove.isEmpty() && toAdd.isEmpty()) {
        return;
      }
      mPrescriptions.removeAll(toRemove);
      mPrescriptions.addAll(toAdd);
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_item_prescription, parent, false);
      return new ViewHolder(view, editClickConsumer);
    }

    @Override
    public int getItemCount() {
      return mPrescriptions.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      holder.bind(mPrescriptions.get(position));
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

      private final TextView text;
      private final View editButton;
      private final Consumer<Prescription> editClickConsumer;

      public ViewHolder(@NonNull View itemView, Consumer<Prescription> editClickConsumer) {
        super(itemView);
        text = itemView.findViewById(R.id.tv_prescription_summary);
        editButton = itemView.findViewById(R.id.iv_edit_medication);
        this.editClickConsumer = editClickConsumer;
      }

      public void bind(Prescription prescription) {
        text.setText(prescription.getSummary());
        editButton.setOnClickListener(v -> {
          editClickConsumer.accept(prescription);
        });
      }
    }
  }
}
