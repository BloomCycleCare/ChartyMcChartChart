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
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.ui.main.MainActivity;
import com.google.common.base.Strings;

import org.joda.time.DateTime;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class ChartingService extends Service {

  public enum Action {
    SET_REMINDER
  }

  private static final String CHANNEL_ID = "charting_reminder";
  private static final String CHANNEL_NAME = "Charting Reminder";

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private NotificationManager notificationManager;

  @Override
  public void onCreate() {
    super.onCreate();
    Timber.i("Creating service");

    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    initNotificationChannel();

    // TODO: fix this to handle turning the notification back on via preferences
    mDisposables.add(stopStreamForApp(ChartingApp.cast(getApplication()))
        .doOnNext(v -> Timber.v("yesterdayHasObservation: %b", v))
        .takeUntil(shouldStop -> shouldStop)
        .doOnNext(v -> Timber.d("Shutting down charting reminder"))
        .distinctUntilChanged()
        .doOnComplete(this::clearNotificationAndTerminate)
        .subscribe(yesterdayHasObservation -> {
          if (yesterdayHasObservation) {
            clearNotificationAndTerminate();
          } else {
            Timber.d("Showing notification");
            Notification notification = createNotification(this, "Input entry for yesterday");
            notificationManager.notify(R.string.charting_reminder, notification);
          }
        }, t -> {
          Timber.e(t);
          clearNotificationAndTerminate();
        }));
  }

  private Flowable<Boolean> stopStreamForApp(ChartingApp app) {
    RWChartEntryRepo entryRepo = app.entryRepo(ViewMode.CHARTING);
    RWCycleRepo cycleRepo = app.cycleRepo(ViewMode.CHARTING);
    PreferenceRepo preferenceRepo = app.preferenceRepo();
    Flowable<Boolean> entryStopStream = entryRepo
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
    Flowable<Boolean> preferenceDisableStream = preferenceRepo
        .summaries()
        .map(summary -> !summary.enableChartingReminder());
    return Flowable.combineLatest(
        entryStopStream.doOnNext(v -> { if (v) Timber.d("Entry stop"); }),
        preferenceDisableStream.doOnNext(v -> { if (v) Timber.d("Preference stop"); }),
        (a, b) -> a || b);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("onStartCommand: action=%s", intent.getAction());
    if (!Strings.isNullOrEmpty(intent.getAction())) {
      switch (Action.valueOf(intent.getAction())) {
        case SET_REMINDER:
          clearNotificationAndTerminate();
          break;
        default:
          Timber.w("Fallthrough for action=%s", intent.getAction());
      }
    }
    return START_STICKY;
  }


  private void clearNotificationAndTerminate() {
    Timber.d("Canceling notification");
    notificationManager.cancel(R.string.charting_reminder);
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
    PendingIntent dismissIntent = PendingIntent.getBroadcast(context, 0,
        new Intent(context, ReminderReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
    builder.setContentIntent(contentIntent);
    builder.setDeleteIntent(dismissIntent);
    return builder.build();
  }

  private void initNotificationChannel() {
    Timber.d("Initializing notification channel");
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
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
