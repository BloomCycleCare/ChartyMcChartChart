package com.bloomcyclecare.cmcc.ui.main;

import android.os.Bundle;
import android.view.Menu;

import com.bloomcyclecare.cmcc.apps.practitioner.R;
import com.google.android.material.navigation.NavigationView;

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
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private DrawerLayout drawerLayout;
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
  }

  private void configureNavigation() {
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

  private void render(MainViewModel.ViewState viewState) {
  }
}
