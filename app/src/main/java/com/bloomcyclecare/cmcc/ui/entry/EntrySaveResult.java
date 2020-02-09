package com.bloomcyclecare.cmcc.ui.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;

import org.parceler.Parcel;

import java.util.ArrayList;

/**
 * Created by parkeroth on 11/19/17.
 */
@Parcel
public class EntrySaveResult {
  public ArrayList<Cycle> droppedCycles = new ArrayList<>();
  public ArrayList<Cycle> changedCycles = new ArrayList<>();
  public ArrayList<Cycle> newCycles = new ArrayList<>();
  public Cycle cycleToShow;

  public static EntrySaveResult forCycle(Cycle cycleToShow) {
    EntrySaveResult result = new EntrySaveResult();
    result.cycleToShow = cycleToShow;
    return result;
  }

  private EntrySaveResult() {}
}
