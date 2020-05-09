package com.bloomcyclecare.cmcc.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.ui.main.MainActivity;

import org.joda.time.DateTime;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import io.reactivex.Flowable;
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

    RWChartEntryRepo entryRepo = myApp.entryRepo();
    RWCycleRepo cycleRepo = myApp.cycleRepo();
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    Flowable<Boolean> stopStream = entryRepo
        .getLatestN(2)
        .map(entries -> {
          if (entries.size() != 2) {
            throw new IllegalStateException();
          }
          ChartEntry entryForYesterday = entries.get(0);
          return entryForYesterday.observationEntry.observation != null;
        })
        .flatMap(yesterdayHadObservation -> {
          if (yesterdayHadObservation) {
            return Flowable.just(true);
          }
          return cycleRepo.getCurrentCycle()
              .map(Cycle::isPregnancy)
              .toSingle(false)
              .toFlowable();
        });
    mDisposables.add(stopStream
        .doOnNext(v -> Timber.v("yesterdayHasObservation: %b", v))
        .takeUntil(shouldStop -> shouldStop)
        .distinctUntilChanged()
        .doOnComplete(() -> clearNotificationAndTerminate(manager))
        .subscribe(yesterdayHasObservation -> {
          Timber.d("Showing notification");
          Notification notification = createNotification(this, "Input entry for yesterday");
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(R.string.charting_reminder, notification);
          } else {
            manager.notify(R.string.charting_reminder, notification);
          }
          if (yesterdayHasObservation) {
            clearNotificationAndTerminate(manager);
          }
        }, t -> {
          Timber.e(t);
          clearNotificationAndTerminate(manager);
        }));
  }

  private void clearNotificationAndTerminate(NotificationManager manager) {
    Timber.d("Canceling notification");
    manager.cancel(R.string.charting_reminder);
    scheduleRestart(DateTime.now().plusMinutes(30));
    Timber.i("Stopping reminder service");
    stopSelf();
  }

  private void scheduleRestart(DateTime restartTime) {
    Timber.i("Scheduling restart at %s", restartTime);
    Intent restartIntent = new Intent(this, ChartingReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(
        this, 0, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.set(AlarmManager.RTC_WAKEUP, restartTime.getMillis(), pi);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
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
        new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
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
