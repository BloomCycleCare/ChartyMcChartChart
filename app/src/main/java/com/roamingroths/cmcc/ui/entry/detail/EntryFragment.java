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
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.Extras;
import com.roamingroths.cmcc.application.FirebaseApplication;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.Set;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/**
 * Created by parkeroth on 9/17/17.
 */

public abstract class EntryFragment<E extends Entry> extends Fragment {

  public interface EntryListener {
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
    mEntryProvider = createEntryProvider(
        FirebaseDatabase.getInstance(),
        FirebaseApplication.getCryptoUtil());
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
    mExistingEntry = getArguments().getParcelable(Entry.class.getSimpleName());
  }

  @Override
  public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v(mTag, "onCreateView: " + String.valueOf(savedInstanceState != null));

    View view = inflater.inflate(layoutId, container, false);
    duringCreateView(view, getArguments(), savedInstanceState);

    if (mExistingEntry != null) {
      updateUiWithEntry(processExistingEntry(mExistingEntry));
    }

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

  public Single<E> getEntryFromUiRx() {
    return Single.create(new SingleOnSubscribe<E>() {
      @Override
      public void subscribe(SingleEmitter<E> e) throws Exception {
        e.onSuccess(getEntryFromUi());
      }
    });
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

  abstract EntryProvider<E> createEntryProvider(FirebaseDatabase db, RxCryptoUtil cryptoUtil);

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
