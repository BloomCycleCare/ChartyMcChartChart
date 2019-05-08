package com.roamingroths.cmcc.ui.goals.list;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.Goal;

/**
 * Created by parkeroth on 1/24/18.
 */

public interface GoalViewHolder {

  class Impl extends RecyclerView.ViewHolder implements GoalViewHolder {

    private TextView mTitle;
    private ImageView mIconImageView;

    public Impl(View itemView) {
      super(itemView);

      mTitle = itemView.findViewById(R.id.tv_title);
      mIconImageView = itemView.findViewById(R.id.iv_icon);
    }

    public void bind(Goal goal) {
      mTitle.setText(goal.toString());
      mIconImageView.setBackgroundResource(goal.getIconResourceId());
    }
  }
}
