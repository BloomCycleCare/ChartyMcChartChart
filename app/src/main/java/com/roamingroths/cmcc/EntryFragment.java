package com.roamingroths.cmcc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableSet;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.util.Set;

/**
 * Created by parkeroth on 9/17/17.
 */

public abstract class EntryFragment<E extends Entry> extends Fragment {

  private final int layoutId;
  private Cycle mCycle;
  private LocalDate mEntryDate;
  private CycleProvider mCycleProvider;
  private EntryProvider<E> mEntryProvider;
  private E mExistingEntry;

  EntryFragment(int layoutId) {
    this.layoutId = layoutId;
  }

  @Override
  public final void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    mCycleProvider = CycleProvider.forDb(db);
    mEntryProvider = createEntryProvider(db);
  }

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Bundle args = getArguments();
    String entryDateStr = args.getString(Extras.ENTRY_DATE_STR);
    mEntryDate = DateUtil.fromWireStr(entryDateStr);
    mCycle = args.getParcelable(Cycle.class.getName());
    mExistingEntry = null;

    View view = inflater.inflate(layoutId, container, false);
    duringCreateView(view, args, savedInstanceState);

    mEntryProvider.getEntry(mCycle, getEntryDateStr(), new Callbacks.Callback<E>() {
      @Override
      public void acceptData(E entry) {
        mExistingEntry = processExistingEntry(entry);
        updateUiWithEntry(mExistingEntry);
      }

      @Override
      public void handleNotFound() {
        throw new IllegalStateException("Could not load Entry");
      }

      @Override
      public void handleError(DatabaseError error) {
        error.toException().printStackTrace();
      }
    });

    return view;
  }

  /**
   * Process an existing entry found in DB before storing it as a member.
   *
   * @param existingEntry
   * @return processed entry
   */
  E processExistingEntry(E existingEntry) {
    return existingEntry;
  }

  abstract void duringCreateView(View view, Bundle args, Bundle savedInstanceState);

  abstract EntryProvider<E> createEntryProvider(FirebaseDatabase db);

  abstract void updateUiWithEntry(E entry);

  abstract E getEntryFromUi() throws Exception;

  public Set<ValidationIssue> validateEntryFromUi() throws ValidationException {
    return ImmutableSet.of();
  }

  public void doSave(Cycle cycle, Callbacks.Callback<Void> callback) {
    try {
      getEntryProvider().putEntry(cycle.id, getEntryFromUi(), Listeners.doneOnCompletion(callback));
    } catch (Exception e) {
      callback.handleError(DatabaseError.fromException(e));
    }
  }

  public boolean isDirty() {
    if (mExistingEntry == null) {
      return false;
    }
    try {
      E entryFromUi = getEntryFromUi();
      return !getExistingEntry().equals(entryFromUi);
    } catch (Exception e) {
      return true;
    }
  }

  //public abstract void onDelete(Callbacks.Callback<Void> onDone);

  EntryProvider<E> getEntryProvider() {
    return mEntryProvider;
  }

  CycleProvider getCycleProvider() {
    return mCycleProvider;
  }

  LocalDate getEntryDate() {
    return mEntryDate;
  }

  String getEntryDateStr() {
    return DateUtil.toWireStr(mEntryDate);
  }

  Cycle getCycle() {
    return mCycle;
  }

  E getExistingEntry() {
    return mExistingEntry;
  }

  public static class ValidationIssue {
    public String title;
    public String message;

    public ValidationIssue(String title, String message) {
      this.title = title;
      this.message = message;
    }
  }

  public static class ValidationException extends Exception {
    public ValidationException(String message) {
      super(message);
    }
  }
}
