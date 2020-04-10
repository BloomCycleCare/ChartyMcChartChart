package com.bloomcyclecare.cmcc.data.repos;

import com.bloomcyclecare.cmcc.application.ViewMode;

import java.util.Optional;

import timber.log.Timber;

public abstract class RepoFactory<T> {

  protected abstract Optional<T> forViewMode(ViewMode viewMode);

  public final T forViewMode(ViewMode viewMode, ViewMode fallback) {
    Optional<T> t = forViewMode(viewMode);
    if (t.isPresent()) {
      return t.get();
    }
    Timber.w("Using fallback report for unsupported ViewMode: %s", viewMode.name());
    t = forViewMode(fallback);
    if (t.isPresent()) {
      return t.get();
    }
    Timber.wtf("This shouldn't happen");
    return null;
  }
}
