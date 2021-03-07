package com.bloomcyclecare.cmcc.utils;

import com.google.common.base.Preconditions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ErrorOr<T> {

  @Nullable private final T mValue;
  @Nullable private final Throwable mError;

  public static <T> ErrorOr<T> forValue(@Nullable T value) {
    return new ErrorOr<>(value, null);
  }

  public static <T> ErrorOr<T> forError(@NonNull Throwable error) {
    Preconditions.checkNotNull(error);
    return new ErrorOr<>(null, error);
  }

  private ErrorOr(T value, Throwable error) {
    mValue = value;
    mError = error;
  }

  public boolean isEmpty() {
    return !hasError() && mValue == null;
  }

  public boolean hasValue() {
    return mValue != null;
  }

  public boolean hasError() {
    return mError != null;
  }

  @Nullable
  public T get() {
    if (hasError()) {
      throw new IllegalStateException(mError);
    }
    return mValue;
  }

  public T or(T defaultValue) {
    return hasError() ? defaultValue : get();
  }

  public Throwable error() {
    return mError;
  }

  @Override
  public String toString() {
    return "ErrorOr{" +
        "mValue=" + mValue +
        ", mError=" + mError +
        '}';
  }
}
