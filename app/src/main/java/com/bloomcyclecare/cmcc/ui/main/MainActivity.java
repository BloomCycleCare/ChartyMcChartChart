package com.bloomcyclecare.cmcc.ui.main;

import android.os.Bundle;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.ui.cycle.vertical.CyclePageFragmentArgs;
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
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

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
    mViewModel.initialState()
        .map(MainViewModel.ViewState::defaultViewMode)
        .subscribe(this::configureNavigation);

    Timber.v("Awaiting initial ViewState");
  }

  private void configureNavigation(ViewMode initialViewMode) {
    Timber.d("Configuring navigation with default ViewMode %s", initialViewMode.name());

    NavigationView navigationView = findViewById(R.id.nav_view);
    drawerLayout = findViewById(R.id.drawer_layout);
    NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_content);
    navController = fragment.getNavController();

    CyclePageFragmentArgs initialArgs = new CyclePageFragmentArgs.Builder()
        .setViewMode(initialViewMode)
        .build();
    navController.setGraph(R.navigation.main_nav_graph, initialArgs.toBundle());

    // Show and Manage the Drawer and Back Icon
    NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout);

    // Handle Navigation item clicks
    NavigationUI.setupWithNavController(navigationView, navController);

    mViewModel.viewState().observe(this, this::render);
  }

  @Override
  public boolean onSupportNavigateUp() {
    // Allows NavigationUI to support proper up navigation or the drawer layout
    // drawer menu, depending on the situation.
    return NavigationUI.navigateUp(navController, drawerLayout);

  }

  private void render(MainViewModel.ViewState viewState) {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar == null) {
      Timber.e("ActionBar should have been set in onCreate!");
    } else {
      actionBar.setTitle(viewState.title());
      actionBar.setSubtitle(viewState.subtitle());
    }
  }
}
