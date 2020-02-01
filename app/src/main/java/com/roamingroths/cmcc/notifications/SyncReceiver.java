package com.roamingroths.cmcc.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import timber.log.Timber;

public class SyncReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Timber.i("Received intent to start sync service");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(new Intent(context, SyncService.class));
    } else {
      context.startService(new Intent(context, SyncService.class));
    }
  }
}
