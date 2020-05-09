package com.bloomcyclecare.cmcc.ui.print;

import android.os.Bundle;
import android.print.PrintJob;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.print.ChartPrinter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
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
import io.reactivex.schedulers.Schedulers;

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
    RWCycleRepo cycleRepo = myApp.cycleRepo();
    RWChartEntryRepo entryRepo = myApp.entryRepo();
    RWInstructionsRepo instructionsRepo = myApp.instructionsRepo();

    CycleAdapter adapter = CycleAdapter.fromBundle(this, savedInstanceState);
    if (adapter != null) {
      mRecyclerView.setAdapter(adapter);
    } else {

      mDisposables.add(cycleRepo
          .getStream()
          .firstOrError()
          .flatMapObservable(Observable::fromIterable)
          .flatMap(cycle -> entryRepo
              .getStreamForCycle(Flowable.just(cycle))
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
                entryRepo.getStreamForCycle(Flowable.just(cycle)).firstOrError(),
                instructionsRepo.getAll().firstOrError(),
                (entries, instructions) -> new CycleRenderer(cycle, Optional.empty(), entries, instructions))
                .toObservable());

        mDisposables.add(renderers.toList()
            .map(r -> ChartPrinter.create(PrintChartActivity.this, r))
            .flatMapCompletable(ChartPrinter::savePDF)
            .subscribeOn(Schedulers.computation())
            .subscribe(() -> printJobComplete()));
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
