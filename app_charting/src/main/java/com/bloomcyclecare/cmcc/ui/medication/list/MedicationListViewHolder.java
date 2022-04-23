package com.bloomcyclecare.cmcc.ui.medication.list;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.function.Consumer;

public class MedicationListViewHolder extends RecyclerView.ViewHolder {
  private final TextView mNameView;

  private Medication mBoundMedication;

  public MedicationListViewHolder(@NonNull View itemView, Consumer<Medication> editClickConsumer) {
    super(itemView);
    mNameView = itemView.findViewById(R.id.tv_medication_name);

    itemView.findViewById(R.id.iv_edit_medication)
        .setOnClickListener(v -> editClickConsumer.accept(mBoundMedication));
  }

  public void bind(Medication medication) {
    mBoundMedication = medication;
    String name = medication.name;
    if (!medication.active) {
      name += " (Inactive)";
    }
    mNameView.setText(name);
  }
}
