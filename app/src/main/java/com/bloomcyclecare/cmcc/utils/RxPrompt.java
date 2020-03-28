package com.bloomcyclecare.cmcc.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class RxPrompt implements Disposable {
  private final String title;
  private final String message;
  private final Completable onPositive;
  private final Completable onNegative;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public RxPrompt(String title, String message, Completable onPositive, Completable onNegative) {
    this.title = title;
    this.message = message;
    this.onPositive = onPositive;
    this.onNegative = onNegative;
  }


  @Override
  public void dispose() {
    mDisposables.dispose();
  }

  @Override
  public boolean isDisposed() {
    return mDisposables.isDisposed();
  }

  public Disposable doPrompt(Context context) {
    new AlertDialog.Builder(context)
        //set message, title, and icon
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("Yes", (dialog, whichButton) -> mDisposables.add(onPositive
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(dialog::dismiss, t -> Timber.e(t, "Error completing positive action"))))
        .setNegativeButton("No", (dialog, which) -> mDisposables.add(onNegative
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(dialog::dismiss, t -> Timber.e(t, "Error completing negative action"))))
        .create()
        .show();
    return this;
  }
}
