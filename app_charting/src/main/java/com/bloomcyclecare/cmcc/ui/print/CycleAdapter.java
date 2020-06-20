package com.bloomcyclecare.cmcc.ui.print;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.logic.print.PageRenderer;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.parceler.Parcels;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.annotations.Nullable;

/**
 * Created by parkeroth on 4/18/17.
 */

public class CycleAdapter extends RecyclerView.Adapter<CycleAdapter.CycleAdapterViewHolder> {

  private static final boolean DEBUG = true;
  private static final String TAG = CycleAdapter.class.getSimpleName();

  private final Activity mActivity;
  private final ImmutableList<ViewModel> mViewModels;
  private final Set<Integer> mSelectedIndexes;
  private ImmutableSet<Integer> mBreakAfterIndexs;

  private enum BundleKey {
    SELECTED_INDEXES, VIEW_MODELS
  }

  @Nullable
  public static CycleAdapter fromBundle(Activity activity, Bundle savedState) {
    if (savedState == null
        || !savedState.containsKey(BundleKey.VIEW_MODELS.name())
        || !savedState.containsKey(BundleKey.SELECTED_INDEXES.name())) {
      return null;
    }
    List<ViewModel> viewModels = savedState.getParcelableArrayList(BundleKey.VIEW_MODELS.name());
    SortedSet<Integer> selectedIndexes = new TreeSet<>();
    selectedIndexes.addAll(savedState.getIntegerArrayList(BundleKey.SELECTED_INDEXES.name()));
    return new CycleAdapter(activity, viewModels, selectedIndexes);
  }

  public static CycleAdapter fromViewModels(Activity activity, List<ViewModel> viewModels) {
    // Initialize selected items with most recent cycles
    SortedSet<Integer> selectedIndexes = new TreeSet<>();
    int rowsLeftBeforeNextBreak = PageRenderer.numRowsPerPage();
    for (int i=0; i < viewModels.size(); i++) {
      ViewModel viewModel = viewModels.get(i);
      rowsLeftBeforeNextBreak -= PageRenderer.numRows(viewModel.mNumEntries);
      if (rowsLeftBeforeNextBreak >= 0) {
        selectedIndexes.add(i);
      }
    }
    return new CycleAdapter(activity, viewModels, selectedIndexes);
  }

  private CycleAdapter(Activity activity, List<ViewModel> viewModels, SortedSet<Integer> selectedIndexes) {
    if (DEBUG) Log.v(TAG, "Create CycleAdapter");
    mActivity = activity;
    mViewModels = ImmutableList.copyOf(viewModels);
    mSelectedIndexes = selectedIndexes;
    updatePageBreaks();
  }

  public void fillBundle(Bundle bundle) {
    bundle.putParcelableArrayList(BundleKey.VIEW_MODELS.name(), Lists.newArrayList(mViewModels));
    bundle.putIntegerArrayList(BundleKey.SELECTED_INDEXES.name(), Lists.newArrayList(mSelectedIndexes));
  }

  public boolean hasValidSelection() {
    boolean selectionStarted = false;
    boolean selectionFinished = false;
    for (int i=0; i < mViewModels.size(); i++) {
      if (mSelectedIndexes.contains(i)) {
        if (!selectionStarted) {
          selectionStarted = true;
        }
        if (selectionFinished) {
          return false;
        }
      } else {
        if (selectionStarted && !selectionFinished) {
          selectionFinished = true;
        }
      }
    }
    return true;
  }

  public Set<Cycle> getSelectedCycles() {
    Set<Cycle> cycles = new HashSet<>();
    for (Integer index : mSelectedIndexes) {
      cycles.add(mViewModels.get(index).mCycle);
    }
    return cycles;
  }

  /**
   * This gets called when each new ViewHolder is created. This happens when the RecyclerView
   * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
   *
   * @param parent   The ViewGroup that these ViewHolders are contained within.
   * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
   *                 can use this viewType integer to provide a different layout. See
   *                 {@link RecyclerView.Adapter#getItemViewType(int)}
   *                 for more details.
   * @return A new EntryAdapterViewHolder that holds the View for each list item
   */
  @Override
  public CycleAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_cycle_select;
    LayoutInflater inflater = LayoutInflater.from(mActivity);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new CycleAdapterViewHolder(view);
  }

  /**
   * OnBindViewHolder is called by the RecyclerView to display the data at the specified
   * position. In this method, we update the contents of the ViewHolder to display the weather
   * details for this particular position, using the "position" argument that is conveniently
   * passed into us.
   *
   * @param holder   The ViewHolder which should be updated to represent the
   *                 contents of the item at the given position in the data set.
   * @param position The position of the item within the adapter's data set.
   */
  @Override
  public void onBindViewHolder(CycleAdapterViewHolder holder, int position) {
    ViewModel viewModel = mViewModels.get(position);
    holder.mCycleDataTextView.setText("Starting: " + DateUtil.toPrintUiStr(viewModel.mCycle.startDate));
    holder.mSelectBox.setChecked(mSelectedIndexes.contains(position));
    if (mBreakAfterIndexs.contains(position + 1)) {
      holder.showPageSeparator();
    } else {
      holder.hidePageSeparator();
    }
  }

  private void updateCheckStates(int position, boolean val) {
    if (val) {
      mSelectedIndexes.add(position);
    } else {
      mSelectedIndexes.remove(position);
    }
    updatePageBreaks();
  }

  private void updatePageBreaks() {
    int firstIndex = 0;
    for (int i=0; i < mViewModels.size(); i++) {
      if (mSelectedIndexes.contains(i) && firstIndex < 0) {
        firstIndex = i;
        break;
      }
    }
    ImmutableSet.Builder<Integer> breakBuilder = ImmutableSet.builder();
    int rowsLeftBeforeNextBreak = PageRenderer.numRowsPerPage();
    for (int i=firstIndex; i < mViewModels.size(); i++) {
      rowsLeftBeforeNextBreak -= PageRenderer.numRows(mViewModels.get(i).mNumEntries);
      if (rowsLeftBeforeNextBreak < 0) {
        breakBuilder.add(i);
        rowsLeftBeforeNextBreak = PageRenderer.numRowsPerPage();
      }
    }
    mBreakAfterIndexs = breakBuilder.build();
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return mViewModels.size();
  }

  public class CycleAdapterViewHolder extends RecyclerView.ViewHolder {
    public final TextView mCycleDataTextView;
    public final CheckBox mSelectBox;
    public final View mCycleSeparator;
    public final View mPageSeparator;

    public CycleAdapterViewHolder(View itemView) {
      super(itemView);
      mCycleDataTextView = itemView.findViewById(R.id.tv_cycle_data);
      mSelectBox = itemView.findViewById(R.id.checkbox_select);
      mSelectBox.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          updateCheckStates(getAdapterPosition(), mSelectBox.isChecked());
        }
      });
      mCycleSeparator = itemView.findViewById(R.id.cycle_separator);
      mPageSeparator = itemView.findViewById(R.id.page_separator);
      itemView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          // Toggle checkbox
          mSelectBox.setChecked(!mSelectBox.isChecked());
          updateCheckStates(getAdapterPosition(), mSelectBox.isChecked());
        }
      });
    }

    public void showPageSeparator() {
      mPageSeparator.setVisibility(View.VISIBLE);
      mCycleSeparator.setVisibility(View.GONE);
    }

    public void hidePageSeparator() {
      mPageSeparator.setVisibility(View.GONE);
      mCycleSeparator.setVisibility(View.VISIBLE);
    }
  }

  public static class ViewModel implements Parcelable {
    public final Cycle mCycle;
    public final int mNumEntries;

    public ViewModel(Cycle mCycle, int mNumEntries) {
      this.mCycle = mCycle;
      this.mNumEntries = mNumEntries;
    }

    protected ViewModel(Parcel in) {
      mCycle = in.readParcelable(Cycle.class.getClassLoader());
      mNumEntries = in.readInt();
    }

    public static final Creator<ViewModel> CREATOR = new Creator<ViewModel>() {
      @Override
      public ViewModel createFromParcel(Parcel in) {
        return new ViewModel(in);
      }

      @Override
      public ViewModel[] newArray(int size) {
        return new ViewModel[size];
      }
    };

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(Parcels.wrap(mCycle), flags);
      dest.writeInt(mNumEntries);
    }
  }
}
