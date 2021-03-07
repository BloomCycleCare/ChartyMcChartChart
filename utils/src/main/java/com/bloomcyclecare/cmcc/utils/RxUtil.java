package com.bloomcyclecare.cmcc.utils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.SingleSubject;

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

  public static <T> Flowable<List<T>> combineLatest(Collection<? extends Flowable<T>> in) {
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

  public static <K, V> Flowable<List<V>> aggregateLatest(Flowable<V> upstream, Function<V, K> keyExtractor) {
    return upstream
        .scan(ImmutableMap.<K, V>of(), (m, v) -> {
          Map<K, V> b = new HashMap<>(m);
          b.put(keyExtractor.apply(v), v);
          return ImmutableMap.copyOf(b);
        })
        .flatMap(m -> Observable
            .fromIterable(m.entrySet())
            .map(Map.Entry::getValue)
            .toList()
            .toFlowable());
  }

  public static <T> List<T> filterEmpty(Collection<Optional<T>> in) {
    List<T> out = new ArrayList<>();
    for (Optional<T> item : in) {
      if (item.isPresent()) {
        out.add(item.get());
      }
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

  public static <T extends Copyable<T>, X> FlowableTransformer<T, T> update(Flowable<X> source, BiConsumer<T, X> consumerFn, String name) {
    return upstream -> Flowable
        .combineLatest(upstream.distinctUntilChanged(), stallWarning(source, name), (t, x) -> {
          T copyOfT = t.copy();
          consumerFn.accept(copyOfT, x);
          return copyOfT;
        });
  }

  public static <T> Flowable<T> stallWarning(Flowable<T> source, String name) {
    return source
        .timeout(Flowable.empty().delay(30, TimeUnit.SECONDS), x -> Flowable.never())
        .onErrorResumeNext(source);
  }

  static class Pair<L, R> {
    L first;
    R second;

    Pair(L first, R second) {
      this.first = first;
      this.second = second;
    }
  }

  public static <T, X> FlowableTransformer<T, T> takeWhile(Flowable<X> filterStream, Predicate<X> filterPredicate) {
    return upstream -> Flowable
        .combineLatest(upstream, filterStream, Pair::new)
        .filter(p -> filterPredicate.test(p.second))
        .map(p -> p.first);
  }

  public static <T, X> FlowableTransformer<T, T> onceAvailable(SingleSubject<? extends Optional<?>> resource) {
    return upstream -> Flowable
        .combineLatest(upstream, resource.toFlowable().filter(Optional::isPresent), (item, r) -> item);
  }

}
