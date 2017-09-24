package com.roamingroths.cmcc.logic;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;


/**
 * Created by parkeroth on 9/23/17.
 */

public class EntryContainer {
  public final LocalDate entryDate;
  public final ChartEntry chartEntry;

  public EntryContainer(LocalDate entryDate, ChartEntry chartEntry) {
    this.entryDate = entryDate;
    Preconditions.checkArgument(
        chartEntry.getDate().equals(entryDate), entryDate + " != " + chartEntry.getDate());
    this.chartEntry = chartEntry;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EntryContainer) {
      EntryContainer that = (EntryContainer) obj;
      return this.entryDate.equals(that.entryDate)
          && this.chartEntry.equals(that.chartEntry);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(entryDate, chartEntry);
  }
}
