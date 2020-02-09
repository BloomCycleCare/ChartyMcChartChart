package com.bloomcyclecare.cmcc.logic.goals;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.crypto.Cipherable;

import javax.crypto.SecretKey;

import androidx.annotation.NonNull;

/**
 * Created by parkeroth on 2/28/18.
 */

public class Goal implements Cipherable, Comparable<Goal> {

  public transient String id;
  public transient Status status = Status.UNKNOWN;

  private transient SecretKey mKey;
  private final GoalModel mModel;

  public Goal(SecretKey mKey, GoalModel mModel) {
    this.mKey = mKey;
    this.mModel = mModel;
  }

  public GoalModel model() {
    return mModel;
  }

  @Override
  public SecretKey getKey() {
    return mKey;
  }

  @Override
  public void swapKey(SecretKey key) {
    mKey = key;
  }

  @Override
  public boolean hasKey() {
    return mKey != null;
  }

  public boolean isActive() {
    return status.equals(Status.ACTIVE);
  }

  @Override
  public int compareTo(@NonNull Goal o) {
    return mModel.compareTo(o.model());
  }

  public enum Status {
    UNKNOWN, ACTIVE, ARCHIVED
  }

  @Override
  public String toString() {
    return mModel.toString();
  }

  public int getIconResourceId() {
    switch (mModel.type) {
      case EAT:
        return R.drawable.ic_local_dining_black_24dp;
      case DRINK:
        return R.drawable.ic_local_drink_black_24dp;
      default:
        return R.drawable.ic_flag_black_24dp;
    }
  }
}
