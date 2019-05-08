package com.roamingroths.cmcc.ui.appointments;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 1/24/18.
 */

public interface AppointmentViewHolder {

  class Impl extends RecyclerView.ViewHolder implements AppointmentViewHolder {

    private TextView mTitle;

    public Impl(View itemView) {
      super(itemView);

      mTitle = itemView.findViewById(R.id.tv_title);

      mTitle.setText("An appointment");
    }
  }
}
