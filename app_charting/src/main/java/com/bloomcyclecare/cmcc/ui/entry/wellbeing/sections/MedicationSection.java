package com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.WellbeingEntryViewModel;

import java.util.List;

import io.reactivex.subjects.Subject;
import timber.log.Timber;

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
    private final List<Medication> mMedications;

    private MedicationItemAdapter(@NonNull WellbeingEntryViewModel viewModel) {
      mViewModel = viewModel;
      mMedications = mViewModel.medications().blockingFirst();
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
      holder.bind(mMedications.get(position));
    }

    @Override
    public int getItemCount() {
      return mMedications.size();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
      private final TextView mMedicationName;

      private Medication mBoundMedication;

      public ViewHolder(@NonNull View itemView, Subject<Pair<Medication, Boolean>> updateSubject) {
        super(itemView);
        mMedicationName = itemView.findViewById(R.id.medication_header);
        SwitchCompat medicationSwitch = itemView.findViewById(R.id.medication_value);
        medicationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          updateSubject.onNext(Pair.create(mBoundMedication, isChecked));
        });
      }

      public void bind(@NonNull Medication medication) {
        mBoundMedication = medication;
        mMedicationName.setText(medication.name);
      }
    }
  }
}
