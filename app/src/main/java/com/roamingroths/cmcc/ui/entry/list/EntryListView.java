package com.roamingroths.cmcc.ui.entry.list;

/**
 * Created by parkeroth on 11/13/17.
 */

public interface EntryListView {
  void showProgress();

  void showError(String message);

  void showList();
}
