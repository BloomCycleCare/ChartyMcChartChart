package com.bloomcyclecare.cmcc.data.models;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public interface Entry {
  LocalDate getDate();

  DateTime timeCreated();
  void setTimeCreated(DateTime dateTime);

  DateTime timeUpdated();
  void setTimeUpdated(DateTime dateTime);

  int timesUpdated();
}
