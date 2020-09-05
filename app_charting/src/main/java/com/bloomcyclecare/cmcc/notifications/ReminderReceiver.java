package com.bloomcyclecare.cmcc.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Timber.i("Received reminder broadcast");
    Intent i = new Intent(context, ChartingService.class);
    i.setAction(ChartingService.Action.SET_REMINDER.name());
    context.startService(i);
  }
}
