package com.roamingroths.cmcc.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class ChartingService extends Service {

  private static final String CHANNEL_ID = "charting_reminder";
  private static final String CHANNEL_NAME = "Charting Reminder";

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  @Override
  public void onCreate() {
    super.onCreate();
    Timber.i("Creating service");
    MyApplication myApp = MyApplication.cast(getApplication());

    initNotificationChannel(this);

    ChartEntryRepo entryRepo = new ChartEntryRepo(myApp.db());
    mDisposables.add(entryRepo
        .getLatestN(2)
        .map(entries -> {
          if (entries.size() != 2) {
            throw new IllegalStateException();
          }
          ChartEntry entryForYesterday = entries.get(0);
          return entryForYesterday.observationEntry.observation != null;
        })
        .doOnNext(v -> Timber.v("yesterdayHasObservation: %b", v))
        .distinctUntilChanged()
        .subscribe(yesterdayHasObservation -> {
          NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
          if (!yesterdayHasObservation) {
            Timber.d("Showing notification");
            manager.notify(R.string.charting_reminder, createNotification(
                this, "Input entry for yesterday"));
          } else {
            Timber.d("Not showing notification");
            manager.cancel(R.string.charting_reminder);
          }
        }, Timber::e));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.i("Starting service");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      //startForegroundService(intent);
    }
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    mDisposables.dispose();
    super.onDestroy();
  }

  private static Notification createNotification(Context context, String text) {
    Timber.d("Creating notification with text: %s", text);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_assignment_black_24dp)
        .setContentTitle("Charting Reminder")
        .setContentText(text)
        .setOnlyAlertOnce(true)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        new Intent(context, ChartEntryListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
    builder.setContentIntent(contentIntent);
    return builder.build();
  }

  private static void initNotificationChannel(Context context) {
    Timber.d("Initializing notification channel");
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (notificationManager == null) {
      Timber.w("NotificaitonManager is null!");
      return;
    }
    NotificationChannel channel = new NotificationChannel(
        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription("Channel description");
    channel.enableVibration(false);
    notificationManager.createNotificationChannel(channel);
  }
}
