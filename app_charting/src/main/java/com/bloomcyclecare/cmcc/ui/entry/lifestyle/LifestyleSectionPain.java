package com.bloomcyclecare.cmcc.ui.entry.lifestyle;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowId;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.lifestyle.LifestyleEntry;

import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class LifestyleSectionPain {

  public static View inflate(@NonNull LayoutInflater inflater,
                             @NonNull Context context,
                             @NonNull LifestyleEntryViewModel viewModel) {
    View item = inflater.inflate(R.layout.lifestyle_item, null);

    ImageView iconView = item.findViewById(R.id.lifestyle_item_icon);
    iconView.setImageResource(R.drawable.ic_baseline_mood_bad_24);

    LinearLayoutCompat content = item.findViewById(R.id.lifestyle_item_content);
    inflater.inflate(R.layout.lifestyle_item_pain, content);

    RecyclerView painLevelViews = content.findViewById(R.id.pain_level_items);
    painLevelViews.setAdapter(new PainItemAdapter(viewModel));
    painLevelViews.setLayoutManager(new LinearLayoutManager(context) {
      @Override
      public boolean canScrollVertically() {
        return false;
      }
    });

    return item;
  }

  private static class PainItemAdapter extends RecyclerView.Adapter<PainItemAdapter.ViewHolder> {

    private final LifestyleEntryViewModel mViewModel;

    private PainItemAdapter(@NonNull LifestyleEntryViewModel viewModel) {
      mViewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.lifestyle_item_pain_list_item, parent, false);
      return new ViewHolder(view, parent.getContext(), mViewModel.painUpdateSubject);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      if (position > 3) {
        throw new IllegalArgumentException();
      }
      holder.bind(LifestyleEntry.PainObservationTime.values()[position], mViewModel.updatedEntry().blockingFirst());
    }

    @Override
    public int getItemCount() {
      return 4;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
      private final Subject<Pair<LifestyleEntry.PainObservationTime, Integer>> mUpdateSubject;
      private final TextView mHeaderTextView;
      private final TextView mValueTextView;
      private final SeekBar mSeekBar;

      private LifestyleEntry.PainObservationTime mBoundTime;

      public ViewHolder(
          @NonNull View itemView, @NonNull Context context,
          Subject<Pair<LifestyleEntry.PainObservationTime, Integer>> updateSubject) {
        super(itemView);
        mUpdateSubject = updateSubject;
        mHeaderTextView = itemView.findViewById(R.id.pain_level_header);
        mValueTextView = itemView.findViewById(R.id.pain_level_value);
        mSeekBar = itemView.findViewById(R.id.pain_level_seekbar);

        itemView.setOnClickListener(v -> setAndEnable(0));
        itemView.setOnLongClickListener(v -> confirmClear(mBoundTime, context));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Timber.v("Progress change: %s %d", mBoundTime.name(), progress);
            mUpdateSubject.onNext(Pair.create(mBoundTime, progress));
            mValueTextView.setText(String.valueOf(progress));
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
      }

      public void bind(@NonNull LifestyleEntry.PainObservationTime observationTime, @NonNull LifestyleEntry initialEntry) {
        mBoundTime = observationTime;
        mHeaderTextView.setText("Pain Level " + observationTime.name() + ":");

        Integer initialObservation = initialEntry.getPainObservation(observationTime);
        if (initialObservation == null) {
          clearAndDisable();
        } else {
          setAndEnable(initialObservation);
        }
      }

      private void setAndEnable(int value) {
        Timber.v("Enabling and setting %s to %d", mBoundTime.name(), value);
        mSeekBar.setEnabled(true);
        mSeekBar.setProgress(value);
        mValueTextView.setText(String.valueOf(value));
      }

      private void clearAndDisable() {
        Timber.v("Clearing and disabling %s", mBoundTime.name());
        mSeekBar.setProgress(0);
        mSeekBar.setEnabled(false);
        mValueTextView.setText("n/a");
        mUpdateSubject.onNext(Pair.create(mBoundTime, null));
      }

      private boolean confirmClear(@Nullable LifestyleEntry.PainObservationTime time, Context context) {
        if (time == null) {
          throw new IllegalStateException();
        }
        new AlertDialog.Builder(context)
            .setTitle("Clear Pain Entry?")
            .setMessage("Do you want to clear the " + time + " pain entry")
            .setPositiveButton("Yes", (dialog, which) -> {
              clearAndDisable();
              dialog.dismiss();
            })
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
        return true;
      }
    }

  }
}
