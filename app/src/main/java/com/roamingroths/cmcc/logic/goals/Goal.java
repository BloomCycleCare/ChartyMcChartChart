package com.roamingroths.cmcc.logic.goals;

import com.roamingroths.cmcc.crypto.Cipherable;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 2/28/18.
 */

public class Goal implements Cipherable {

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

  public enum Status {
    UNKNOWN, ACTIVE, ARCHIVED
  }
}
