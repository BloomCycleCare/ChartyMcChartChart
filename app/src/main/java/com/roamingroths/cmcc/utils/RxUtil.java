package com.roamingroths.cmcc.utils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.reactivex.Flowable;

public class RxUtil {

  public static <T> Flowable<List<T>> zip(Collection<Flowable<T>> in) {
    if (in.isEmpty()) {
      return Flowable.just(ImmutableList.of());
    }
    return Flowable.zip(in, objects -> {
      List<T> out = new ArrayList<>(in.size());
      for (Object o : objects) {
        out.add((T) o);
      }
      return out;
    });
  }

  public static <T> Flowable<List<T>> combineLatest(Collection<Flowable<T>> in) {
    if (in.isEmpty()) {
      return Flowable.just(ImmutableList.of());
    }
    return Flowable.combineLatest(in, objects -> {
      List<T> out = new ArrayList<>(in.size());
      for (Object o : objects) {
        out.add((T) o);
      }
      return out;
    });
  }

  public static <T> List<T> filterEmpty(Collection<Optional<T>> in) {
    List<T> out = new ArrayList<>();
    for (Optional<T> item : in) {
      item.ifPresent(out::add);
    }
    return out;
  }

  public static <T> List<T> flatten(Collection<? extends Collection<T>> in) {
    List<T> out = new ArrayList<>();
    for (Collection<T> in1 : in) {
      out.addAll(in1);
    }
    return out;
  }
}
