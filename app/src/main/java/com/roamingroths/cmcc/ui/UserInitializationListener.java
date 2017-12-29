package com.roamingroths.cmcc.ui;

import com.google.firebase.auth.FirebaseUser;

/**
 * Created by parkeroth on 10/8/17.
 */

public interface UserInitializationListener {
  void onUserInitialized(FirebaseUser user);
}
