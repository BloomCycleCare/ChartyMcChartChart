package com.bloomcyclecare.cmcc.ui.print;

import android.os.Bundle;
import android.print.PrintJob;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
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

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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

    MyApplication myApp = MyApplication.cast(requireActivity().getApplication());
    RWCycleRepo cycleRepo = myApp.cycleRepo();
    RWChartEntryRepo entryRepo = myApp.entryRepo();
    RWInstructionsRepo instructionsRepo = myApp.instructionsRepo();

    CycleAdapter adapter = CycleAdapter.fromBundle(requireActivity(), savedInstanceState);
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
              CycleAdapter.fromViewModels(requireActivity(), viewModels))));
    }

    FloatingActionButton fab = view.findViewById(R.id.fab);
    final Toast invalidSelectionToast =
        Toast.makeText(requireActivity(), "Continuous selection required", Toast.LENGTH_LONG);
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
            .map(r -> ChartPrinter.create(requireActivity(), r))
            .flatMapObservable(ChartPrinter::print)
            .flatMap(printJob -> pollPrintJob(printJob))
            .toList()
            .timeout(10, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(jobs -> printJobComplete(), t -> printJobFailed()));
      }
    });

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

  private CycleAdapter getAdapter() {
    return (CycleAdapter) mRecyclerView.getAdapter();
  }
}
