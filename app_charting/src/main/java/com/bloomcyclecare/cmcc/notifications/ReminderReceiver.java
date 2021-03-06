package com.bloomcyclecare.cmcc.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import timber.log.Timber;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Timber.i("Received reminder broadcast");
    Intent i = new Intent(context, ChartingService.class);
    i.setAction(ChartingService.Action.SET_REMINDER.name());
    // Forward extras which include the restart time.
    i.putExtras(intent);
    context.startService(i);
    //ContextCompat.startForegroundService(context, new Intent(context, ChartingService.class));
  }
}
