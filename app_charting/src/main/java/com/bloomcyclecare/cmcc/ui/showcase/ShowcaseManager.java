package com.bloomcyclecare.cmcc.ui.showcase;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class ShowcaseManager {

  private final Context mContext;

  private static final int DELAY_MS = 200;

  public ShowcaseManager(Context context) {
    mContext = context;
  }

  public enum SequenceID {
    INCORRECT_STICKER_SELECTION,
    ENTRY_DETAIL_PAGE
  }

  public enum ShowcaseID {
    FIRST_INCORRECT_STICKER(
        "Incorrect sticker selections are noted by a strike. Touch the sticker to reopen the selection dialog for more information.", true),
    INCORRECT_STICKER_REASON(
        "This section explains why the previous selection was incorrect.", false),
    INCORRECT_STICKER_HINT(
        "Touch here for a hint to determine the correct sticker for this observation.", false),
    ENTRY_DETAIL_INPUT_OBSERVATION(
        "Touch here to input your observation using the vaginal discharge recording system (VDRS).", true, true),
    ENTRY_DETAIL_EXPLAIN_DESCRIPTION(
        "Here is the summary of your description \n" +
            "Use to verify your description input.", false, true),
    ENTRY_DETAIL_EXPLAIN_PEAK_DAY(
        "Use this switch to indicate if the day was a peak day. \n" +
            "You can return to this entry on previous days to accurately mark a peak day.", false),
    ENTRY_DETAIL_EXPLAIN_INTERCOURSE(
        "Use this switch to record an act of intercourse ", false),
    ENTRY_DETAIL_EXPLAIN_FIRST_DAY_NEW_CYCLE(
        "Use this switch to indicate the beginning of a new cycle. \n" +
        "This will end the current cycle and create the next.", false),
    ENTRY_DETAIL_EXPLAIN_NOTE(
        "Use this space to record any special comments for the day that you want to remember. This is a good way to record things you want to discuss with your practitioner at your next follow up appointment.", false, true),
    ENTRY_DETAIL_EXPLAIN_EXTRA_TOGGLES("Please note that this screen will adapt and change as your instructions change.", false),
    ENTRY_DETAIL_EXPLAIN_MENU("Look here for additional observational entry options", true),
    ENTRY_DETAIL_EXPLAIN_SAVE("Touch here to save your description. You can always return to make any changes.", true),
    OBSERVATION_INPUT("Touch here to record your observation", true, true),
    STICKER_SELECTION("After recording an observation, you can select a sticker for the day. Touch Here.", true, true)
    ;

    final String content;
    final boolean useRectangle;
    final boolean targetTouchable;

    ShowcaseID(String content, boolean targetTouchable) {
      this(content, targetTouchable, false);
    }

    ShowcaseID(String content, boolean targetTouchable, boolean useRectangle) {
      this.content = content;
      this.targetTouchable = targetTouchable;
      this.useRectangle = useRectangle;
    }
  }

  public void preemptAllMatching(Predicate<ShowcaseID> ifTrue) {
    SharedPreferences prefs = mContext.getSharedPreferences(
        ShowcaseManager.class.getCanonicalName(), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear();
    Arrays.stream(ShowcaseID.values()).filter(ifTrue).forEach(id -> {
      Timber.d("Storing preemption for %s", id.name());
      editor.putBoolean(id.name(), true);
    });
    Timber.d("Committing preemptions");
    editor.apply();
  }

  public boolean shouldShowcase(ShowcaseID id) {
    SharedPreferences prefs = mContext.getSharedPreferences(
        ShowcaseManager.class.getCanonicalName(), Context.MODE_PRIVATE);
    return !prefs.getBoolean(id.name(), false);
  }

  public void showShowcase(ShowcaseID showcaseID, View target) {
    if (!shouldShowcase(showcaseID)) {
      Timber.d("Preempting showcase %s", showcaseID.name());
      return;
    }
    Timber.d("Showing showcase %s", showcaseID.name());
    Activity activity = getActivity(target);
    if (activity == null) {
      Timber.w("Failed to find activity for view: %d", target.getId());
      return;
    }
    MaterialShowcaseView.Builder builder = new MaterialShowcaseView.Builder(activity);
    if (showcaseID.useRectangle) {
      builder = builder.withRectangleShape();
    }
    builder
        .renderOverNavigationBar()
        .setTarget(target)
        .setDismissText("GOT IT")
        .setContentText(showcaseID.content)
        .setTargetTouchable(showcaseID.targetTouchable)
        .setDelay(2000) // optional but starting animations immediately in onCreate can make them choppy
        .singleUse(showcaseID.name())
        .show();
  }

  public static SequenceBuilder sequenceBuilder(SequenceID id, Activity activity) {
    return new SequenceBuilder(id, activity);
  }

  public void maybeAddShowcase(ShowcaseID id, View view, SequenceBuilder builder) {
    if (shouldShowcase(id)) {
      builder.addShowcase(id, view);
    }
  }

  public static class SequenceBuilder {

    private final Activity mActivity;
    private final MaterialShowcaseSequence mSequence;

    private SequenceBuilder(SequenceID id, Activity activity) {
      mActivity = activity;
      mSequence = new MaterialShowcaseSequence(activity, id.name());

      ShowcaseConfig config = new ShowcaseConfig();
      config.setDelay(DELAY_MS);
      mSequence.setConfig(config);
    }

    public SequenceBuilder addShowcase(ShowcaseID showcaseID, View view, IShowcaseListener listener) {
      MaterialShowcaseView.Builder builder = new MaterialShowcaseView.Builder(mActivity);
      if (showcaseID.useRectangle) {
        builder = builder.withRectangleShape();
      }
      if (listener != null) {
        builder = builder.setListener(listener);
      }
      mSequence.addSequenceItem(builder
          .setTarget(view)
          .setDismissText("GOT IT")
          .setContentText(showcaseID.content)
          .setDelay(DELAY_MS) // optional but starting animations immediately in onCreate can make them choppy
          .singleUse(showcaseID.name())
          .build());
      return this;
    }

    public SequenceBuilder addShowcase(ShowcaseID showcaseID, View view) {
      return addShowcase(showcaseID, view, null);
    }

    public void build() {
      mSequence.start();
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
