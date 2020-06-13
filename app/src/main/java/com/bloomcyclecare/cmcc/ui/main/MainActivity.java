package com.bloomcyclecare.cmcc.ui.main;

import android.os.Bundle;
import android.view.Menu;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private NavigationView navView;
  private NavController navController;
  private DrawerLayout drawerLayout;
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

  private void configureNavigation(ViewMode initialViewMode) {
    Timber.d("Configuring navigation with default ViewMode %s", initialViewMode.name());

    navView = findViewById(R.id.nav_view);
    drawerLayout = findViewById(R.id.drawer_layout);
    NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_content);
    navController = fragment.getNavController();

    navController.setGraph(R.navigation.main_nav_graph);

    // Show and Manage the Drawer and Back Icon
    NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout);

    // Handle Navigation item clicks
    NavigationUI.setupWithNavController(navView, navController);

    mViewModel.viewState().observe(this, this::render);
  }

  @Override
  public boolean onSupportNavigateUp() {
    // Allows NavigationUI to support proper up navigation or the drawer layout
    // drawer menu, depending on the situation.
    return NavigationUI.navigateUp(navController, drawerLayout);

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
