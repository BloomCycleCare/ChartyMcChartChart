package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.data.entities.Pregnancy;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;

public interface RWPregnancyRepo extends ROPregnancyRepo {

  Completable insertAll(List<Pregnancy> pregnancies);

  Completable update(Pregnancy pregnancy);

  Completable delete(Pregnancy pregnancy);

  Completable startPregnancy(LocalDate dateOfTest);

  Completable revertPregnancy(LocalDate dateOfTest);
}
