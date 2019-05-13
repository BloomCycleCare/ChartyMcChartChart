package com.roamingroths.cmcc.ui.goals.create;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.GoalModel;

/**
 * Created by parkeroth on 1/24/18.
 */

public interface GoalModelViewHolder extends View.OnClickListener {

  class Impl extends RecyclerView.ViewHolder implements GoalModelViewHolder {

    private ImageView mIcon;
    private TextView mTitle;
    private GoalModelAdapter.OnClickHandler mClickHandler;

    public Impl(View itemView, GoalModelAdapter.OnClickHandler clickHandler) {
      super(itemView);
      itemView.setOnClickListener(this);

      mTitle = itemView.findViewById(R.id.tv_title);
      mIcon = itemView.findViewById(R.id.icon);
      mClickHandler = clickHandler;
    }

    @Override
    public void onClick(View v) {
      mClickHandler.onClick(getAdapterPosition());
    }

    public void bind(GoalModel template) {
      setIcon(template.type);
      mTitle.setText(template.toString());
    }

    private void setIcon(GoalModel.Type type) {
      switch(type) {
        case EAT:
          mIcon.setBackgroundResource(R.drawable.ic_local_dining_black_24dp);
          break;
        case DRINK:
          mIcon.setBackgroundResource(R.drawable.ic_local_drink_black_24dp);
          break;
        default:
          mIcon.setBackgroundResource(R.drawable.ic_flag_black_24dp);
          break;
      }
    }
  }
}
