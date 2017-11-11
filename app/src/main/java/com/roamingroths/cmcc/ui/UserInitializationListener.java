package com.roamingroths.cmcc.ui;

import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.crypto.CryptoUtil;

/**
 * Created by parkeroth on 10/8/17.
 */

public interface UserInitializationListener {
  void onUserInitialized(FirebaseUser user, CryptoUtil cryptoUtil);
}
