package com.roamingroths.cmcc.ui.instructions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.common.base.Optional;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.entities.Instructions;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class InstructionsListActivity extends AppCompatActivity {

  private SectionsPagerAdapter mPagerAdapter;
  private ViewPager mViewPager;
  private InstructionsListViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_appointment_list);

    mViewModel = ViewModelProviders.of(this).get(InstructionsListViewModel.class);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Instructions");

    mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mPagerAdapter);

    final AtomicBoolean initialLoad = new AtomicBoolean(false);
    final AtomicBoolean dialogActive = new AtomicBoolean(false);
    mViewModel.viewState().observe(this, viewState -> {
      Timber.i("Updating ViewState");
      boolean shouldReloadFragments = mPagerAdapter.mInstructions.size() != viewState.instructions.size();
      mPagerAdapter.updateInstructions(viewState.instructions);
      if (shouldReloadFragments) {
        Timber.d("Reloading fragments");
        mViewPager.setAdapter(mPagerAdapter);
      }
      Optional<Integer> index = mPagerAdapter.getIndex(viewState.instructionsForFocus);
      if (index.isPresent()) {
        Timber.d("Setting focus to %d", index.get());
        mViewPager.setCurrentItem(index.get());
        mViewModel.clearFocus();
      }
      mViewPager.clearOnPageChangeListeners();
      mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
          if (position == 0) {
            toolbar.setTitle("New Instructions");
            if (!viewState.hasStartedNewInstructions && dialogActive.compareAndSet(false, true)) {
              new AlertDialog.Builder(InstructionsListActivity.this)
                  .setTitle("New Instructions?")
                  .setMessage("Would you like to start a new set of instructions?")
                  .setPositiveButton("Yes", (dialogInterface, i) -> {
                    mViewModel
                        .createNewInstructions(LocalDate.now())
                        .subscribe(() -> {
                          Timber.i("New instructions started");
                          dialogInterface.dismiss();
                          dialogActive.set(false);
                        });
                  })
                  .setNegativeButton("No", (dialogInterface, i) -> {
                    mViewPager.setCurrentItem(1);
                    dialogInterface.dismiss();
                    dialogActive.set(false);
                  })
                  .setCancelable(false)
                  .show();
            }
          } else if (position == 1) {
            toolbar.setTitle("Current Instructions");
          } else {
            toolbar.setTitle("Previous Instructions");
            if (position == mPagerAdapter.getCount() - 1 && dialogActive.compareAndSet(false, true)) {
              new AlertDialog.Builder(InstructionsListActivity.this)
                  .setTitle("New Instructions?")
                  .setMessage("Would you like to add a previous set of instructions?")
                  .setPositiveButton("Yes", (dialogInterface, i) -> {
                    Instructions lastEntry = mPagerAdapter.getLastEntry();
                    DatePickerDialog dialog = new DatePickerDialog(InstructionsListActivity.this, (d, year, month, day) -> {
                      mViewModel.addPreviousInstructions(new LocalDate(year, month + 1, day)).subscribe();
                    }, lastEntry.startDate.getYear(), lastEntry.startDate.getMonthOfYear() - 1, lastEntry.startDate.getDayOfMonth());
                    dialog.setTitle("Start Date");
                    dialog.getDatePicker().setMaxDate(lastEntry.startDate.toDate().getTime());
                    dialog.setOnCancelListener(dialogInterface1 -> mViewPager.setCurrentItem(mPagerAdapter.getCount() - 2));
                    dialog.show();
                    dialogInterface.dismiss();
                    dialogActive.set(false);
                  })
                  .setNegativeButton("No", (dialogInterface, i) -> {
                    mViewPager.setCurrentItem(mPagerAdapter.getCount() - 2);
                    dialogInterface.dismiss();
                    dialogActive.set(false);
                  })
                  .setCancelable(false)
                  .show();
            }
          }
        }
        @Override
        public void onPageScrollStateChanged(int state) {}
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
      });
      if (initialLoad.compareAndSet(false, true)) {
        Timber.i("Defaulting to current page (index 1)");
        mViewPager.setCurrentItem(1);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_instructions, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      mViewModel.isDirty().subscribe(isDirty -> {
        if (isDirty) {
          new AlertDialog.Builder(this)
              .setTitle("Save Changes?")
              .setMessage("Would you like to save your changes?")
              .setPositiveButton("Yes", (d, i) -> {
                mViewModel.save().subscribe(() -> {
                  onBackPressed();
                });
                d.dismiss();
              })
              .setNegativeButton("No", (d, i) -> {
                mViewModel.clearPending().subscribe(() -> {
                  onBackPressed();
                });
                d.dismiss();
              })
              .show();
        } else {
          onBackPressed();
        }
      });
      return true;
    }
    if (id == R.id.action_delete) {
      if (mPagerAdapter.mInstructions.size() < 2) {
        new AlertDialog.Builder(this)
            .setTitle("Cannot Delete")
            .setMessage("No other instruction sets are available for merging.")
            .setPositiveButton("Ok", (d, i) -> d.dismiss())
            .show();
      } else {
        new AlertDialog.Builder(this)
            .setTitle("Delete Instructions?")
            .setMessage("Are you sure you want to delete these instructions?")
            .setPositiveButton("Yes", (dialogInterface, i) -> {
              Instructions currentInstructions =
                  mPagerAdapter.get(mViewPager.getCurrentItem());
              mViewModel.delete(currentInstructions).subscribe();
            })
            .setNegativeButton("No", (dialogInterface, i) -> {
              dialogInterface.dismiss();
            })
            .show();
      }
      return true;
    }
    if (id == R.id.action_insert_after) {
      Instructions instructions = mPagerAdapter.mInstructions.get(mViewPager.getCurrentItem());
      DatePickerDialog dialog = new DatePickerDialog(InstructionsListActivity.this, (d, year, month, day) -> {
        mViewModel.addPreviousInstructions(new LocalDate(year, month + 1, day)).subscribe();
      }, instructions.startDate.getYear(), instructions.startDate.getMonthOfYear() - 1, instructions.startDate.getDayOfMonth());
      dialog.setTitle("Start Date");
      dialog.getDatePicker().setMaxDate(instructions.startDate.toDate().getTime());
      dialog.show();
      return true;
    }
    if (id == R.id.action_save) {
      mViewModel
          .save()
          .doOnSubscribe(s -> Timber.i("Saving instructions"))
          .subscribe(() -> {
            Timber.i("Done saving instructions");
            finish();
          });
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final List<Instructions> mInstructions = new ArrayList<>();
    private boolean mExtraAtFront = false;

    SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    void updateInstructions(List<Instructions> instructions) {
      mInstructions.clear();
      mInstructions.addAll(instructions);
      Instructions currentInstructions = instructions.get(0);
      mExtraAtFront = !currentInstructions.startDate.equals(LocalDate.now());
      notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
      Fragment f = new InstructionsListFragment();
      Bundle args = new Bundle();
      int index = mExtraAtFront ? position - 1 : position;
      if (index >= 0 && index < mInstructions.size()) {
        Instructions instructions = mInstructions.get(index);
        Timber.v("Creating fragment for Instructions starting on %s", instructions.startDate);
        args.putParcelable(Instructions.class.getCanonicalName(), Parcels.wrap(instructions));
      } else {
        Timber.v("Creating fragment without Instructions");
      }
      f.setArguments(args);
      return f;
    }

    public Instructions get(int index) {
      return mInstructions.get(index - (mExtraAtFront ? 1 : 0));
    }

    public Optional<Integer> getIndex(@Nullable Instructions instructions) {
      if (instructions == null) {
        return Optional.absent();
      }
      for (Instructions i : mInstructions) {
        if (i.startDate.equals(instructions.startDate)) {
          return Optional.of(mInstructions.indexOf(i) + (mExtraAtFront ? 1 : 0));
        }
      }
      return Optional.absent();
    }

    public Instructions getLastEntry() {
      return mInstructions.get(mInstructions.size()-1);
    }

    @Override
    public int getCount() {
      int extraPages = mExtraAtFront ? 2 : 1;
      return mInstructions.size() + extraPages;
    }
  }
}
