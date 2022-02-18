package com.bloomcyclecare.cmcc.ui.cycle.stickers;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;

import org.parceler.Parcels;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class StickerDialogFactory {

  public static DialogFragment create(
      StickerSelectionViewModel viewModel,
      RenderedEntry renderedEntry,
      Context context,
      CompositeDisposable disposables) {
    DialogFragment fragment = new StickerDialogFragmentV2(result -> {
      Timber.i("Selection: %s", result.selection);
      disposables.add(viewModel.updateSticker(renderedEntry.entryDate(), result.selection).subscribe(
          () -> Timber.d("Done updating selection"),
          t -> Timber.e(t, "Error updating selection")));
      if (!result.ok()) {
        Toast.makeText(context, "Incorrect selection", Toast.LENGTH_SHORT).show();
      }
    }, renderedEntry.entryDate());

    Bundle args = new Bundle();
    renderedEntry.manualStickerSelection().ifPresent(
        selection -> args.putParcelable(StickerDialogFragment.Args.PREVIOUS_SELECTION.name(), Parcels.wrap(selection)));
    args.putInt(StickerDialogFragment.Args.VIEW_MODE.name(), viewModel.viewMode().ordinal());
    args.putParcelable(
        StickerDialogFragment.Args.SELECTION_CONTEXT.name(), Parcels.wrap(renderedEntry.stickerSelectionContext()));
    args.putBoolean(StickerDialogFragment.Args.CAN_SELECT_YELLOW_STAMPS.name(), renderedEntry.canSelectYellowStamps());

    fragment.setArguments(args);
    return fragment;
  }

}
