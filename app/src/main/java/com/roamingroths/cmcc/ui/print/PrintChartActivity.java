package com.roamingroths.cmcc.ui.print;

import android.os.Bundle;
import android.print.PrintJob;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.logic.chart.ChartEntryList;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.logic.print.ChartPrinter;
import com.roamingroths.cmcc.providers.CycleEntryProvider;
import com.roamingroths.cmcc.providers.CycleProvider;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import static org.joda.time.DateTimeZone.getProvider;

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
          new BiFunction<CycleProvider, FirebaseUser, Observable<Cycle>>() {
            @Override
            public Observable<Cycle> apply(CycleProvider cycleProvider, FirebaseUser firebaseUser) throws Exception {
              return cycleProvider.getAllCycles(firebaseUser);
            }
          }).toObservable())
          .sorted(new Comparator<Cycle>() {
            @Override
            public int compare(Cycle o1, Cycle o2) {
              return o2.startDate.compareTo(o1.startDate);
            }
          })
          .flatMap(CycleAdapter.cycleToViewModel(MyApplication.chartEntryProvider()))
          .toList()
          .subscribe(new Consumer<List<CycleAdapter.ViewModel>>() {
            @Override
            public void accept(List<CycleAdapter.ViewModel> viewModels) throws Exception {
              mRecyclerView.setAdapter(
                  CycleAdapter.fromViewModels(PrintChartActivity.this, viewModels));
            }
          });
    }

    FloatingActionButton fab = findViewById(R.id.fab);
    final Toast invalidSelectionToast =
        Toast.makeText(this, "Continuous selection required", Toast.LENGTH_LONG);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
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
            .flatMapObservable(emitPrintJob())
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
