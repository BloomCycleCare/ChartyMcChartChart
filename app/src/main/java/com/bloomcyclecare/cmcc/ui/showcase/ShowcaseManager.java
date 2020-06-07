package com.bloomcyclecare.cmcc.ui.showcase;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Pair;
import android.view.View;

import com.google.common.collect.Queues;

import java.util.Deque;

import androidx.annotation.Nullable;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class ShowcaseManager {

  private static final int DELAY_MS = 200;

  public enum SequenceID {
    INCORRECT_STICKER_SELECTION
  }

  public enum ShowcaseID {
    FIRST_INCORRECT_STICKER("Incorrect sticker selections are noted by a strike. Touch the sticker to reopen the selection dialog for more information."),
    INCORRECT_STICKER_REASON("This section explains why the previous selection was incorrect."),
    INCORRECT_STICKER_HINT("Touch here for a hint to determine the correct sticker for this observation.");

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
        .setDelay(DELAY_MS) // optional but starting animations immediately in onCreate can make them choppy
        .singleUse(showcaseID.name())
        .show();
  }

  public static SequenceBuilder sequenceBuilder(SequenceID id) {
    return new SequenceBuilder(id);
  }

  public static class SequenceBuilder {

    private final SequenceID mSequenceID;
    private final Deque<Pair<ShowcaseID, View>> mQueue = Queues.newArrayDeque();

    private SequenceBuilder(SequenceID id) {
      mSequenceID = id;
    }

    public SequenceBuilder addShowcase(ShowcaseID showcaseID, View view) {
      mQueue.push(Pair.create(showcaseID, view));
      return this;
    }

    public void build() {
      if (mQueue.isEmpty()) {
        Timber.d("No showcases in sequence");
        return;
      }

      View firstView = mQueue.peek().second;
      Activity activity = getActivity(firstView);
      if (activity == null) {
        Timber.w("Failed to find activity for view: %d", firstView.getId());
      }

      MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(activity, mSequenceID.name());
      while (!mQueue.isEmpty()) {
        Pair<ShowcaseID, View> p = mQueue.pop();
        sequence.addSequenceItem(p.second, p.first.content, "GOT IT");
      }

      ShowcaseConfig config = new ShowcaseConfig();
      config.setDelay(500);
      sequence.setConfig(config);

      sequence.start();
    }
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
