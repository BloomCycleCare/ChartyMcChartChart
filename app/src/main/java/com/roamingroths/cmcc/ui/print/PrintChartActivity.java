package com.roamingroths.cmcc.ui.print;

import android.os.Bundle;
import android.print.PrintJob;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.models.ChartEntryList;
import com.roamingroths.cmcc.logic.print.ChartPrinter;
import com.roamingroths.cmcc.providers.CycleEntryProvider;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class PrintChartActivity extends AppCompatActivity {

  private static final boolean DEBUG = true;
  private static final String TAG = PrintChartActivity.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private CycleAdapter mCycleAdapter;

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

    CycleAdapter adapter = CycleAdapter.fromBundle(this, savedInstanceState);
    if (adapter != null) {
      mRecyclerView.setAdapter(adapter);
    } else {
      Observable.merge(Single.zip(
          MyApplication.cycleProvider(),
          MyApplication.getCurrentUser().toSingle(),
          (cycleProvider, firebaseUser) -> cycleProvider.getAllCycles(firebaseUser)).toObservable())
          .flatMap(CycleAdapter.cycleToViewModel(MyApplication.chartEntryProvider()))
          .sorted((o1, o2) -> o2.mCycle.startDate.compareTo(o1.mCycle.startDate))
          .toList()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(AndroidSchedulers.mainThread())
          .subscribe(viewModels -> mRecyclerView.setAdapter(
              CycleAdapter.fromViewModels(PrintChartActivity.this, viewModels)));
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
        Observable<ChartEntryList> entryLists = MyApplication.cycleEntryProvider().flatMapObservable(new Function<CycleEntryProvider, ObservableSource<? extends ChartEntryList>>() {
          @Override
          public ObservableSource<? extends ChartEntryList> apply(CycleEntryProvider cycleEntryProvider) throws Exception {
            return cycleEntryProvider.getChartEntryLists(getAdapter().getSelectedCycles(), Preferences.fromShared(PrintChartActivity.this));
          }
        });
        ChartPrinter.create(PrintChartActivity.this, entryLists)
            .print()
            .flatMap(emitPrintJob())
            .filter(new Predicate<PrintJob>() {
              @Override
              public boolean test(PrintJob printJob) throws Exception {
                return printJob.isCancelled() || printJob.isFailed() || printJob.isCompleted();
              }
            })
            .firstOrError()
            .subscribe(new Consumer<PrintJob>() {
              @Override
              public void accept(PrintJob printJob) throws Exception {
                if (printJob.isCompleted()) {
                  printJobComplete();
                  return;
                }
                if (printJob.isFailed()) {
                  printJobFailed();
                  return;
                }
              }
            });
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
    return new Function<PrintJob, ObservableSource<PrintJob>>() {
      @Override
      public ObservableSource<PrintJob> apply(final PrintJob printJob) throws Exception {
        return Observable.interval(100, TimeUnit.MILLISECONDS).map(new Function<Long, PrintJob>() {
          @Override
          public PrintJob apply(Long aLong) throws Exception {
            return printJob;
          }
        });
      }
    };
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
