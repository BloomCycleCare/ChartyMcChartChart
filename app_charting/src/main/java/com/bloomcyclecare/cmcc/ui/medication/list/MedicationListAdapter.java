package com.bloomcyclecare.cmcc.ui.medication.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


public class MedicationListAdapter extends RecyclerView.Adapter<MedicationListViewHolder> {

  private final Subject<Medication> mEditClickSubject = PublishSubject.create();

  private final Context mContext;
  private List<Medication> mMedications = new ArrayList<>();

  public MedicationListAdapter(Context context) {
    mContext = context;
  }

  public void updateMedications(List<Medication> medications) {
    mMedications.clear();
    mMedications.addAll(medications);
    notifyDataSetChanged();
  }

  public Observable<Medication> editClicks() {
    return mEditClickSubject;
  }

  @NonNull
  @Override
  public MedicationListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
    int layoutIdForListItem = R.layout.list_item_medication;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    View view = inflater.inflate(layoutIdForListItem, viewGroup, false);
    return new MedicationListViewHolder(view, mEditClickSubject::onNext);
  }

  @Override
  public void onBindViewHolder(@NonNull MedicationListViewHolder medicationListViewHolder, int i) {
    medicationListViewHolder.bind(mMedications.get(i));
  }

  @Override
  public int getItemCount() {
    return mMedications.size();
  }
}
