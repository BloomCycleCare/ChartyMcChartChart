package com.bloomcyclecare.cmcc.ui.print;

import android.os.Bundle;
import android.print.PrintJob;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.print.ChartPrinter;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PrintChartFragment extends Fragment {

  private RecyclerView mRecyclerView;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private MainViewModel mMainViewModel;
  private PrintChartViewModel mPrintChartViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewMode viewMode = PrintChartFragmentArgs.fromBundle(requireArguments()).getViewMode();

    PrintChartViewModel.Factory factory = new PrintChartViewModel.Factory(requireActivity().getApplication(), viewMode);
    mPrintChartViewModel = new ViewModelProvider(this, factory).get(PrintChartViewModel.class);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_print_chart, container, false);

    mMainViewModel.updateTitle("Select cycles to print");

    mRecyclerView = view.findViewById(R.id.recyclerview_cycle_entry);
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);

    mDisposables.add(mPrintChartViewModel
        .getCycles()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe(cyclesWithEntries -> {
          CycleAdapter adapter = CycleAdapter.create(requireActivity(), cyclesWithEntries);
          mRecyclerView.setAdapter(adapter);
          FloatingActionButton fab = view.findViewById(R.id.fab);
          fab.setOnClickListener(v -> {
            if (!adapter.hasValidSelection()) {
              Toast.makeText(requireActivity(), "Continuous selection required", Toast.LENGTH_LONG).show();
              return;
            }
            Observable<CycleRenderer> renderers = mPrintChartViewModel.getRenderers(adapter.getSelectedCycles());
            mDisposables.add(renderers.toList()
                .map(r -> ChartPrinter.create(requireActivity(), r))
                .flatMapObservable(ChartPrinter::print)
                .flatMap(printJob -> pollPrintJob(printJob))
                .toList()
                .timeout(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jobs -> printJobComplete(), t -> printJobFailed()));
          });
        }));
    return view;
  }

  private void printJobComplete() {
    NavHostFragment.findNavController(this).popBackStack();
  }

  private void printJobFailed() {
    Toast.makeText(requireActivity(), "Print failed", Toast.LENGTH_LONG).show();
  }

  private Observable<PrintJob> pollPrintJob(PrintJob printJob) {
    return Observable.interval(100, TimeUnit.MILLISECONDS)
        .takeWhile(i -> !printJob.isCompleted() && !printJob.isCancelled() && !printJob.isFailed())
        .ignoreElements()
        .doOnComplete(() -> Timber.i("Print job complete: %s", printJob.getId()))
        .andThen(Observable.just(printJob));
  }
}
