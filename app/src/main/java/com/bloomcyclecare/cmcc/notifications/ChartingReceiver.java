package com.bloomcyclecare.cmcc.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import timber.log.Timber;

public class ChartingReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Timber.i("Received intent to start charting reminder");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(new Intent(context, ChartingService.class));
    } else {
      context.startService(new Intent(context, ChartingService.class));
    }
  }
}
