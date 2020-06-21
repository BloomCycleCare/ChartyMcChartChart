package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;

public interface ROPregnancyRepo {

  Flowable<List<Pregnancy>> getAll();

  Maybe<Pregnancy> get(Long id);
}
