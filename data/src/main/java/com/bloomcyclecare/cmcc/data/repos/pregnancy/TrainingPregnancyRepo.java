package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

public class TrainingPregnancyRepo implements RWPregnancyRepo {

  @Override
  public Flowable<List<Pregnancy>> getAll() {
    return Flowable.just(ImmutableList.of());
  }

  @Override
  public Maybe<Pregnancy> get(Long id) {
    return Maybe.empty();
  }

  @Override
  public Completable insertAll(List<Pregnancy> pregnancies) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable update(Pregnancy pregnancy) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable delete(Pregnancy pregnancy) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable startPregnancy(LocalDate dateOfTest) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable revertPregnancy(LocalDate dateOfTest) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }
}
