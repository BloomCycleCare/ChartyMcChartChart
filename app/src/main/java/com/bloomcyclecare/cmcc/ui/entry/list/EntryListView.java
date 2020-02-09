package com.bloomcyclecare.cmcc.ui.entry.list;

/**
 * Created by parkeroth on 11/13/17.
 */

public interface EntryListView {
  void setTitle(String message);

  void setSubtitle(String message);

  void showList();

  void setOverlay(String key);
}
