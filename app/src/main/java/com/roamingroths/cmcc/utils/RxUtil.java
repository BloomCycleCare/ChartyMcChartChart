package com.roamingroths.cmcc.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;
import timber.log.Timber;

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

  public static <T extends Copyable<T>, X> FlowableTransformer<T, T> update(Flowable<X> source, BiConsumer<T, X> consumerFn, String name) {
    return upstream -> Flowable
        .combineLatest(upstream, stallWarning(source, name), (t, x) -> {
          T copyOfT = t.copy();
          consumerFn.accept(copyOfT, x);
          return copyOfT;
        });
  }

  public static <T> Flowable<T> stallWarning(Flowable<T> source, String name) {
    return source
        .timeout(Flowable.empty().delay(30, TimeUnit.SECONDS), x -> Flowable.never())
        .doOnError(t -> Timber.w("Stall warning %s", name))
        .onErrorResumeNext(source);
  }

}
