package com.bloomcyclecare.cmcc.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class UiUtil {
  public static boolean hideKeyboard(Context context, View rootView) {
    if (context == null) {
      return false;
    }
    InputMethodManager imm =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
    return true;
  }
}
