package com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.WellbeingEntryViewModel;

import org.w3c.dom.Text;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EnergySection {

  public static View inflate(@NonNull LayoutInflater inflater,
                             @NonNull Context context,
                             @NonNull WellbeingEntryViewModel viewModel) {
    View item = inflater.inflate(R.layout.wellbeing_item, null);

    ImageView iconView = item.findViewById(R.id.wellbeing_section_icon);
    iconView.setImageResource(R.drawable.ic_baseline_bolt_24);

    LinearLayoutCompat content = item.findViewById(R.id.wellbeing_section_content);
    inflater.inflate(R.layout.wellbeing_section_energy, content);

    TextView valueView = content.findViewById(R.id.energy_level_value);
    SeekBar seekBar = content.findViewById(R.id.energy_level_seekbar);
    Function<Integer, Void> enableAndSetSeekbar = (value) -> {
      Timber.v("Progress change: %d", value);
      seekBar.setEnabled(true);
      seekBar.setProgress(value);
      valueView.setText(String.valueOf(value));
      viewModel.energyUpdateSubject.onNext(Optional.of(value));
      return null;
    };

    Integer initialEnergyLevel = viewModel.updatedEntry().blockingFirst().wellbeingEntry.energyLevel;
    if (initialEnergyLevel != null) {
      enableAndSetSeekbar.apply(initialEnergyLevel);
    } else {
      seekBar.setEnabled(false);
    }

    content.setOnLongClickListener(v -> confirmClear(context, seekBar, valueView, viewModel));
    content.setOnClickListener(v -> enableAndSetSeekbar.apply(0));
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        enableAndSetSeekbar.apply(progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    return item;
  }

  private static boolean confirmClear(Context context, SeekBar seekBar, TextView valueView, WellbeingEntryViewModel viewModel) {
    if (!seekBar.isEnabled()) {
      return false;
    }
    new AlertDialog.Builder(context)
        .setTitle("Clear Pain Entry?")
        .setMessage("Do you want to clear the energy level entry?")
        .setPositiveButton("Yes", (dialog, which) -> {
          valueView.setText("n/a");
          seekBar.setProgress(0);
          seekBar.setEnabled(false);
          viewModel.energyUpdateSubject.onNext(Optional.empty());
          dialog.dismiss();
        })
        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
        .create()
        .show();
    return true;
  }
}
