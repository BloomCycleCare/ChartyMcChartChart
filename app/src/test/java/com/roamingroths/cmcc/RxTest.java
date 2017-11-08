package com.roamingroths.cmcc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

// I need the internet to compile the code :'(

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class RxTest {

  @Test
  public void runZipTest() throws Exception {
    Single<String> foo = Single.just("foo").cache();
    Single<String> bar = Completable.mergeArray(Completable.complete()).andThen(foo);
    System.out.println(bar.blockingGet());
  }

  @Test
  public void runExceptionTest() throws Exception {
    Maybe<String> foo = Maybe.empty();
    Maybe<String> bar = Maybe.empty();
    Maybe<String> baz = Maybe.zip(foo, bar, new BiFunction<String, String, String>() {
      @Override
      public String apply(@NonNull String s, @NonNull String s2) throws Exception {
        System.out.println("ZIP");
        return s + s2;
      }
    });
    System.out.println("Result: " + baz.isEmpty().blockingGet());
  }

  @Test
  public void runEmptyTest() throws Exception {
    Maybe<String> foo = Maybe.empty()
        .flatMap(new Function<Object, MaybeSource<String>>() {
          @Override
          public MaybeSource<String> apply(@NonNull Object o) throws Exception {
            System.out.print("Foo was empty");
            return Maybe.just("Bar");
          }
        })
        .switchIfEmpty(new MaybeSource<String>() {
          @Override
          public void subscribe(@NonNull MaybeObserver<? super String> observer) {
            observer.onComplete();
          }
        });
    System.out.println("Get: " + foo.isEmpty().blockingGet());
  }
}
