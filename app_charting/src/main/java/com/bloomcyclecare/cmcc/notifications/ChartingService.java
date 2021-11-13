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
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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
import org.joda.time.MutableDateTime;

import java.util.Optional;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class ChartingService extends Service {

  public enum Action {
    SET_REMINDER
  }

  public enum ReminderDelay {
    SOON, TONIGHT, TOMORROW
  }

  private static final int REMINDER_HOUR_TONIGHT = 18;
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

    Notification initialNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_assignment_black_24dp)
        .setContentTitle("Checking Chart Data")
        .setContentText("Checking to see if yesterday needs an observation.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build();
    startForeground(1, initialNotification);

    // TODO: fix this to handle turning the notification back on via preferences
    mDisposables.add(stopStreamForApp(ChartingApp.cast(getApplication()))
        .doOnNext(v -> Timber.v("yesterdayHasObservation: %b", v))
        .takeUntil(shouldStop -> shouldStop)
        .doOnNext(v -> Timber.d("Shutting down charting reminder"))
        .distinctUntilChanged()
        // Ensure we call something within the required 5s time limit
        .subscribe(yesterdayHasObservation -> {
          if (yesterdayHasObservation) {
            Timber.i("Observation recorded. Shutting down reminder until tomorrow.");
            clearNotificationAndTerminate(ReminderDelay.TOMORROW, false);
          } else {
            Timber.d("Showing notification");
            Notification notification = createNotification(this, "Input entry for yesterday");
            notificationManager.notify(1, notification);
            // Allow the notification to be dismissed
            // NOTE: This does not stop the service but removes it from being in the foreground
            // state. See method docs for more details.
            //stopForeground(false);
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
    if (intent == null) {
      Timber.w("Received null intent");
    } else if (!Strings.isNullOrEmpty(intent.getAction())) {
      Timber.d("onStartCommand: action=%s", intent.getAction());
      switch (Action.valueOf(intent.getAction())) {
        case SET_REMINDER:
          if (!intent.hasExtra(ReminderDelay.class.getCanonicalName())) {
            Timber.w("Restart intent missing the ReminderDelay extra!");
          } else {
            ReminderDelay delay =
                ReminderDelay.valueOf(intent.getStringExtra(ReminderDelay.class.getCanonicalName()));
            clearNotificationAndTerminate(delay, true);
          }
          break;
        default:
          Timber.w("Fallthrough for action=%s", intent.getAction());
      }
    }
    return START_STICKY;
  }

  private void clearNotificationAndTerminate() {
    Timber.d("Scheduling restart with default delay");
    clearNotificationAndTerminate(ReminderDelay.SOON, false);
  }

  private Optional<DateTime> getRestartDelay(ReminderDelay delay) {
    DateTime delaySoon = DateTime.now().plusMinutes(30);
    if (delay == ReminderDelay.SOON) {
      return Optional.of(delaySoon);
    }
    MutableDateTime dateTime = DateTime.now().toMutableDateTime();
    dateTime.setMinuteOfHour(0);
    if (delay == ReminderDelay.TOMORROW) {
      dateTime.addDays(1);
      dateTime.setHourOfDay(6);
      return Optional.of(dateTime.toDateTime());
    } else if (delay == ReminderDelay.TONIGHT) {
      dateTime.setHourOfDay(REMINDER_HOUR_TONIGHT);
      if (dateTime.toDateTime().isBefore(delaySoon)) {
        dateTime.setTime(delaySoon);
      }
      return Optional.of(dateTime.toDateTime());
    }
    Timber.w("Unsupported delay %s, not scheduling restart", delay.name());
    return Optional.empty();
  }

  private void clearNotificationAndTerminate(ReminderDelay delay, boolean shouldToast) {
    Timber.d("Canceling notification");
    notificationManager.cancel(R.string.charting_reminder);
    getRestartDelay(delay).ifPresent(dateTime -> scheduleRestart(dateTime, shouldToast));
    Timber.i("Stopping reminder service");
    stopSelf();
  }

  private void scheduleRestart(DateTime restartTime, boolean shouldToast) {
    Timber.i("Scheduling restart at %s", restartTime);
    long delayMs = restartTime.getMillis() - DateTime.now().getMillis();
    if (delayMs < 0) {
      Timber.w("Received negative delay ms: %d", delayMs);
    } else {
      Intent restartIntent = new Intent(this, ChartingReceiver.class);
      PendingIntent pi = PendingIntent.getBroadcast(
          this, 0, restartIntent, PendingIntent.FLAG_IMMUTABLE);
      AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      am.set(AlarmManager.RTC_WAKEUP, restartTime.getMillis(), pi);

      String toastText;
      if (delayMs < 60 * 60 * 1000) {
        toastText = String.format("Scheduled reminder in %d minutes", delayMs / (60 * 1000));
      } else {
        String day = restartTime.getDayOfMonth() == DateTime.now().getDayOfMonth() ? "tonight" : "tomorrow";
        toastText = String.format("Scheduled reminder for %02d:%02d %s", restartTime.getHourOfDay(), restartTime.getMinuteOfHour(), day);
      }
      if (shouldToast) {
        new Handler(this.getMainLooper()).post(() -> {
          Timber.v("Toasting: %s", toastText);
          Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
        });
      } else {
        Timber.v("Skipping toast");
      }
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    Timber.d("onTaskRemoved");
    clearNotificationAndTerminate();
    super.onTaskRemoved(rootIntent);
  }

  @Override
  public void onDestroy() {
    Timber.d("onDestroy");
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
    PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0,
        new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    builder.setContentIntent(contentPendingIntent);

    Intent remindShortIntent = new Intent(context, ReminderReceiver.class);
    remindShortIntent.putExtra(ReminderDelay.class.getCanonicalName(), ReminderDelay.SOON.name());
    PendingIntent remindShortPendingIntent = PendingIntent.getBroadcast(context, 1,
        remindShortIntent, PendingIntent.FLAG_IMMUTABLE);
    builder.setDeleteIntent(remindShortPendingIntent);
    builder.addAction(R.drawable.ic_assignment_black_24dp, "Remind me later", remindShortPendingIntent);

    String remindLongText;
    ReminderDelay reminderDelay;
    if (DateTime.now().hourOfDay().get() < REMINDER_HOUR_TONIGHT) {
      remindLongText = "Remind me tonight";
      reminderDelay = ReminderDelay.TONIGHT;
    } else {
      remindLongText = "Remind me tomorrow";
      reminderDelay = ReminderDelay.TOMORROW;
    }
    Intent remindLongIntent = new Intent(context, ReminderReceiver.class);
    remindLongIntent.putExtra(ReminderDelay.class.getCanonicalName(), reminderDelay.name());
    PendingIntent remindLongPendingIntent = PendingIntent.getBroadcast(context, 2,
        remindLongIntent, PendingIntent.FLAG_IMMUTABLE);
    builder.addAction(R.drawable.ic_assignment_black_24dp, remindLongText, remindLongPendingIntent);
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
