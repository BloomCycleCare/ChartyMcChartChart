package com.bloomcyclecare.cmcc.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import timber.log.Timber;

public class ChartingReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Timber.i("Received intent to start charting reminder");
    ContextCompat.startForegroundService(context, new Intent(context, ChartingService.class));
    //context.startService(new Intent(context, ChartingService.class));
  }
}
