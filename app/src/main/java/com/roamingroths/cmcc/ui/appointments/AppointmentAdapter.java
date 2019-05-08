package com.roamingroths.cmcc.ui.appointments;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 1/24/18.
 */

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder.Impl> {

  private final Context mContext;

  public AppointmentAdapter(Context context) {
    mContext = context;
  }

  @Override
  public AppointmentViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_goal;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new AppointmentViewHolder.Impl(view);
  }

  @Override
  public void onBindViewHolder(AppointmentViewHolder.Impl holder, int position) {

  }

  @Override
  public int getItemCount() {
    return 0;
  }

  public interface OnClickHandler {
    void onClick(int index);
  }
}
