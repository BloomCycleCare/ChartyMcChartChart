package com.roamingroths.cmcc.ui.entry.detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableSet;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.Extras;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.Set;

/**
 * Created by parkeroth on 9/17/17.
 */

public abstract class EntryFragment<E extends Entry> extends Fragment {

  public interface EntryListener {
    void onExistingEntryLoaded(Entry entry, Class<? extends Entry> clazz);

    void onEntryUpdated(Entry entry, Class<? extends Entry> clazz);
  }

  private final Class<E> mClazz;
  private final String mTag;
  private final int layoutId;
  private final EntryProvider<E> mEntryProvider;
  private EntryListener mUpdateListener;
  private Cycle mCycle;
  private LocalDate mEntryDate;
  private E mExistingEntry;

  protected boolean mUiActive = false;

  EntryFragment(Class<E> clazz, String tag, int layoutId) {
    this.mClazz = clazz;
    this.mTag = tag;
    this.layoutId = layoutId;
    mEntryProvider = createEntryProvider(FirebaseDatabase.getInstance());
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      mUpdateListener = (EntryListener) context;
    } catch (ClassCastException cce) {
      throw new ClassCastException(context.toString() + " must implement EntryUpdateListener");
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(mTag, "onCreate: " + String.valueOf(savedInstanceState != null));

    String entryDateStr = getArguments().getString(Extras.ENTRY_DATE_STR);
    mEntryDate = DateUtil.fromWireStr(entryDateStr);
    mCycle = getArguments().getParcelable(Cycle.class.getName());
    mExistingEntry = null;

    mEntryProvider.getEntry(mCycle, getEntryDateStr(), new Callbacks.Callback<E>() {
      @Override
      public void acceptData(E entry) {
        mExistingEntry = processExistingEntry(entry);
        mUpdateListener.onExistingEntryLoaded(mExistingEntry, mClazz);
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
  }

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(mTag, "onCreateView: " + String.valueOf(savedInstanceState != null));

    if (mExistingEntry != null) {
      updateUiWithEntry(mExistingEntry);
    }

    View view = inflater.inflate(layoutId, container, false);
    duringCreateView(view, getArguments(), savedInstanceState);

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

    mUiActive = true;
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    try {
      mUpdateListener.onEntryUpdated(getEntryFromUi(), mClazz);
    } catch (Exception e) {
      // TODO: something better
      Log.w(mTag, "Could not notify EntryUpdateListener: " + e.getMessage());
    }
  }

  public Class<? extends E> getClazz() {
    return mClazz;
  }

  public EntryProvider<E> getEntryProvider() {
    return mEntryProvider;
  }

  public abstract E getEntryFromUi() throws Exception;

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

  public Set<ValidationIssue> validateEntry(E entry) {
    return ImmutableSet.of();
  }

  //public abstract void onDelete(Callbacks.Callback<Void> onDone);

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
}
