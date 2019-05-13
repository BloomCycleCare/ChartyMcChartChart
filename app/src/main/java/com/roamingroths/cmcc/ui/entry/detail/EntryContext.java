package com.roamingroths.cmcc.ui.entry.detail;

import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;

import org.parceler.Parcel;

@Parcel
public class EntryContext {

  public Cycle currentCycle;
  public boolean hasPreviousCycle;
  public boolean expectUnusualBleeding;
  public boolean isFirstEntry;
  public boolean shouldAskEssentialSameness;
  public ChartEntry chartEntry;

}
