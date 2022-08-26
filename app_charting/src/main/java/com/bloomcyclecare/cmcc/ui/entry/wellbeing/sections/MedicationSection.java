package com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections;

import android.content.Context;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationRef;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.WellbeingEntryViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.subjects.Subject;

public class MedicationSection {

  public static View inflate(@NonNull LayoutInflater inflater,
                             @NonNull Context context,
                             @NonNull WellbeingEntryViewModel viewModel) {
    View item = inflater.inflate(R.layout.wellbeing_item, null);

    ImageView iconView = item.findViewById(R.id.wellbeing_section_icon);
    iconView.setImageResource(R.drawable.ic_baseline_local_pharmacy_24);

    LinearLayoutCompat content = item.findViewById(R.id.wellbeing_section_content);
    inflater.inflate(R.layout.wellbeing_section_medication, content);

    RecyclerView painLevelViews = content.findViewById(R.id.medication_items);
    painLevelViews.setAdapter(new MedicationItemAdapter(viewModel));
    painLevelViews.setLayoutManager(new LinearLayoutManager(context) {
      @Override
      public boolean canScrollVertically() {
        return false;
      }
    });

    return item;
  }

  private static class MedicationItemAdapter extends RecyclerView.Adapter<MedicationItemAdapter.ViewHolder> {

    private final WellbeingEntryViewModel mViewModel;
    private final List<MedicationItem> mMedications = new ArrayList<>();

    private MedicationItemAdapter(@NonNull WellbeingEntryViewModel viewModel) {
      mViewModel = viewModel;
      for (Medication medication : viewModel.medications().blockingFirst()) {
        for (Prescription.TimeOfDay time : Prescription.TimeOfDay.values()) {
          /* if (medication.shouldTake(time)) {
            mMedications.add(new MedicationItem(medication, time));
          }*/
        }
      }
      mMedications.sort(Comparator.comparing(m -> m.time));
    }

    @NonNull
    @Override
    public MedicationItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.wellbeing_section_medication_list_item, parent, false);
      return new MedicationItemAdapter.ViewHolder(view, mViewModel.medicationUpdateSubject);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationItemAdapter.ViewHolder holder, int position) {
      holder.bind(mMedications.get(position), mViewModel.updatedEntry().blockingFirst().medicationRefs);
    }

    @Override
    public int getItemCount() {
      return mMedications.size();
    }

    private static class MedicationItem {
      @Nullable public final Prescription.TimeOfDay time;
      public final Medication medication;

      private MedicationItem(Medication medication, @Nullable Prescription.TimeOfDay time) {
        this.time = time;
        this.medication = medication;
      }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
      private final TextView mMedicationName;
      private final SwitchCompat mMedicationSwitch;

      private MedicationItem mBoundItem;

      public ViewHolder(@NonNull View itemView, Subject<WellbeingEntryViewModel.MedicationUpdate> updateSubject) {
        super(itemView);
        mMedicationName = itemView.findViewById(R.id.medication_header);
        mMedicationSwitch = itemView.findViewById(R.id.medication_value);
        mMedicationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateSubject.onNext(
            new WellbeingEntryViewModel.MedicationUpdate(
                mBoundItem.medication, mBoundItem.time, isChecked)));
      }

      public void bind(@NonNull MedicationItem item, List<MedicationRef> initialRefs) {
        mBoundItem = item;
        mMedicationName.setText(String.format("%s - %s", item.medication.name(), item.time.name()));
        mMedicationSwitch.setChecked(initialRefs.stream().anyMatch(r -> r.medicationId == item.medication.id() && r.time == item.time));
      }
    }
  }
}
