package com.roamingroths.cmcc.ui.print;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.print.PrintJob;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.logic.print.ChartPrinter;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PrintChartActivity extends AppCompatActivity {

  private static final boolean DEBUG = true;
  private static final String TAG = PrintChartActivity.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_print_chart);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    setTitle("Select cycles to print");

    mRecyclerView = findViewById(R.id.recyclerview_cycle_entry);
    boolean shouldReverseLayout = false;
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, shouldReverseLayout);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);

    MyApplication myApp = MyApplication.cast(getApplication());
    CycleRepo cycleRepo = new CycleRepo(myApp.db());
    ChartEntryRepo entryRepo = new ChartEntryRepo(myApp.db());
    InstructionsRepo instructionsRepo = new InstructionsRepo(myApp);

    /* Request user permissions in runtime */
    String[] PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    for (String permission : PERMISSIONS) {
      if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        Timber.w("%s NOT granted", permission);
      }
    }
    ActivityCompat.requestPermissions(this,
        new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        },
        100);
    /* Request user permissions in runtime */

    CycleAdapter adapter = CycleAdapter.fromBundle(this, savedInstanceState);
    if (adapter != null) {
      mRecyclerView.setAdapter(adapter);
    } else {

      mDisposables.add(cycleRepo
          .getStream()
          .firstOrError()
          .flatMapObservable(Observable::fromIterable)
          .flatMap(cycle -> entryRepo
              .getStream(Flowable.just(cycle))
              .firstOrError()
              .map(entries -> new CycleAdapter.ViewModel(cycle, entries.size()))
              .toObservable())
          .sorted((o1, o2) -> o2.mCycle.startDate.compareTo(o1.mCycle.startDate))
          .toList()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(AndroidSchedulers.mainThread())
          .subscribe(viewModels -> mRecyclerView.setAdapter(
              CycleAdapter.fromViewModels(PrintChartActivity.this, viewModels))));
    }

    FloatingActionButton fab = findViewById(R.id.fab);
    final Toast invalidSelectionToast =
        Toast.makeText(this, "Continuous selection required", Toast.LENGTH_LONG);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        CycleAdapter adapter = getAdapter();

        if (adapter == null) {
          return;
        }

        if (!getAdapter().hasValidSelection()) {
          invalidSelectionToast.show();
          return;
        }
        Observable<CycleRenderer> renderers = Observable
            .fromIterable(adapter.getSelectedCycles())
            .sorted((c1, c2) -> c1.startDate.compareTo(c2.startDate))
            .flatMap(cycle -> Single.zip(
                entryRepo.getStream(Flowable.just(cycle)).firstOrError(),
                instructionsRepo.getAll().firstOrError(),
                (entries, instructions) -> new CycleRenderer(cycle, entries, instructions))
                .toObservable());

        mDisposables.add(ChartPrinter.create(PrintChartActivity.this, renderers)
            .print()
            .flatMap(emitPrintJob())
            .filter(new Predicate<PrintJob>() {
              @Override
              public boolean test(PrintJob printJob) throws Exception {
                return printJob.isCancelled() || printJob.isFailed() || printJob.isCompleted();
              }
            })
            .firstOrError()
            .subscribeOn(Schedulers.computation())
            .subscribe(printJob -> {
              if (printJob.isCompleted()) {
                printJobComplete();
                return;
              }
              if (printJob.isFailed()) {
                printJobFailed();
                return;
              }
            }));
      }
    });
  }

  private void printJobComplete() {
    finish();
  }

  private void printJobFailed() {
    Toast.makeText(this, "Print failed", Toast.LENGTH_LONG).show();
  }

  private Function<PrintJob, ObservableSource<PrintJob>> emitPrintJob() {
    return printJob -> Observable
        .interval(100, TimeUnit.MILLISECONDS)
        .map(l -> printJob);
  }

  private CycleAdapter getAdapter() {
    return (CycleAdapter) mRecyclerView.getAdapter();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    getAdapter().fillBundle(outState);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
