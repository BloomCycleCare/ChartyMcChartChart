package com.bloomcyclecare.cmcc.data.repos;

import com.bloomcyclecare.cmcc.application.ViewMode;

import java.util.Optional;

import timber.log.Timber;

public abstract class RepoFactory<T> {

  private final ViewMode mFallbackViewMode;

  protected RepoFactory(ViewMode fallbackViewMode) {
    this.mFallbackViewMode = fallbackViewMode;
  }

  protected abstract Optional<T> forViewModeInternal(ViewMode viewMode);

  public final T forViewMode(ViewMode viewMode) {
    Optional<T> t = forViewModeInternal(viewMode);
    if (t.isPresent()) {
      return t.get();
    }
    Timber.w("Using fallback report for unsupported ViewMode: %s", viewMode.name());
    t = forViewModeInternal(mFallbackViewMode);
    if (t.isPresent()) {
      return t.get();
    }
    Timber.wtf("This shouldn't happen");
    return null;
  }
}
