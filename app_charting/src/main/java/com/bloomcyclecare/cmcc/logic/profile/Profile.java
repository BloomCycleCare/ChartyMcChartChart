package com.bloomcyclecare.cmcc.logic.profile;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 1/13/18.
 */

public class Profile {

  public String mPreferredName;
  public SystemGoal mGoal;
  public LocalDate mDateOfBirth;
  public int heightCm;
  public int weightKg;

  public enum SystemGoal {
    ACHIEVE, AVOID, UNSPECIFIED
  }
}
