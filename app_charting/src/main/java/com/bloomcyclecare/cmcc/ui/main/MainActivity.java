package com.bloomcyclecare.cmcc.ui.main;

import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.BuildConfig;
import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.google.android.material.navigation.NavigationView;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private AppBarConfiguration appBarConfiguration;
  private NavigationView navView;
  private NavController navController;
  private MainViewModel mViewModel;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Toolbar toolbar = findViewById(R.id.app_bar);
    setSupportActionBar(toolbar);

    mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

    mDisposables.add(mViewModel.initialState()
        .map(MainViewModel.ViewState::defaultViewMode)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::configureNavigation));

    /*mDisposables.add(mViewModel.instructionsInitialized().subscribe(initialized -> {
      if (!initialized) {
        Dialog dialog = new AlertDialog.Builder(this)
            .setTitle("Initialize Instructions")
            .setMessage("Before you can begin charting we need to know your current set of instructions.")
            .setPositiveButton("Continue", (d, which) -> {
              startActivity(new Intent(this, InstructionsListActivity.class));
              d.dismiss();
            })
            .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
      }
    }));*/

    Timber.v("Awaiting initial ViewState");
  }

  @Override
  public void onBackPressed() {
    String fragmentLabel = navController.getCurrentDestination().getLabel().toString();
    if (fragmentLabel.equals("Your Cycles")) {
      Timber.d("Finishing MainActivity for back press on label %s", fragmentLabel);
      finish();
      return;
    }
    super.onBackPressed();
  }

  private void configureNavigation(ViewMode initialViewMode) {
    Timber.d("Configuring navigation with default ViewMode %s", initialViewMode.name());

    navView = findViewById(R.id.nav_view);
    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
    NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_content);
    navController = fragment.getNavController();

    navController.setGraph(R.navigation.main_nav_graph);

    Set<Integer> topLevelDestinations = new HashSet<>();
    topLevelDestinations.add(R.id.main);
    topLevelDestinations.add(R.id.chart_pager);

    appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
        .setOpenableLayout(drawerLayout)
        .build();

    // Show and Manage the Drawer and Back Icon
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    // Handle Navigation item clicks
    NavigationUI.setupWithNavController(navView, navController);

    navView.setNavigationItemSelectedListener(item -> {
      boolean disableForRelease = !BuildConfig.DEBUG && DISABLE_FOR_RELEASE.contains(item.getItemId());
      if (disableForRelease || !NavigationUI.onNavDestinationSelected(item, navController)) {
        Toast.makeText(this, item.getTitle() + " coming soon!", Toast.LENGTH_SHORT).show();
      }
      drawerLayout.closeDrawer(navView);
      return true;
    });

    mViewModel.viewState().observe(this, this::render);
  }

  private static final ImmutableSet<Integer> DISABLE_FOR_RELEASE = ImmutableSet.of();

  @Override
  public boolean onSupportNavigateUp() {
    // Allows NavigationUI to support proper up navigation or the drawer layout
    // drawer menu, depending on the situation.
    return NavigationUI.navigateUp(navController, appBarConfiguration);
  }

  public void hideUp() {
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
  }

  private void render(MainViewModel.ViewState viewState) {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar == null) {
      Timber.e("ActionBar should have been set in onCreate!");
    } else {
      actionBar.setTitle(viewState.title());
      actionBar.setSubtitle(viewState.subtitle());
    }
    Menu menu = navView.getMenu();
    menu.findItem(R.id.pregnancy_list).setVisible(viewState.showPregnancyMenuItem());
  }
}
