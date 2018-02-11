package com.roamingroths.cmcc.ui.profile;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 1/24/18.
 */

public interface GoalSuggestionViewHolder extends View.OnClickListener {

  class Impl extends RecyclerView.ViewHolder implements GoalSuggestionViewHolder {

    private ImageView mIcon;
    private TextView mTitle;
    private GoalSuggestionAdapter.OnClickHandler mClickHandler;

    public Impl(View itemView, GoalSuggestionAdapter.OnClickHandler clickHandler) {
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

    public void bind(GoalSuggestion suggestion) {
      setIcon(suggestion.action);
      mTitle.setText(suggestion.toString());
    }

    private void setIcon(GoalSuggestion.Action action) {
      switch(action) {
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
