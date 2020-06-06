package com.bloomcyclecare.cmcc.ui.showcase;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.Nullable;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public class ShowcaseManager {

  public enum ShowcaseID {
    FIRST_INCORRECT_STICKER("Incorrect sticker selections are noted by a strike. Touch the sticker to reopen the selection dialog for more information.");

    final String content;

    ShowcaseID(String content) {
      this.content = content;
    }
  }

  public void showShowcase(ShowcaseID showcaseID, View target) {
    Activity activity = getActivity(target);
    if (activity == null) {
      Timber.w("Failed to find activity for view: %d", target.getId());
      return;
    }
    new MaterialShowcaseView.Builder(activity)
        .setTarget(target)
        .setDismissText("GOT IT")
        .setContentText(showcaseID.content)
        .setDelay(200) // optional but starting animations immediately in onCreate can make them choppy
        .singleUse(showcaseID.name())
        .show();
  }

  @Nullable
  private static Activity getActivity(View view) {
    Context context = view.getContext();
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) {
        return (Activity)context;
      }
      context = ((ContextWrapper)context).getBaseContext();
    }
    return null;
  }
}
