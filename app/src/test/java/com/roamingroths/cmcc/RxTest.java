package com.roamingroths.cmcc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.MaybeSource;
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
    Set<Maybe<String>> maybes = new HashSet<>();
    maybes.add(Maybe.just("foo"));
    //maybes.add(Maybe.<String>empty());
    //maybes.add(Maybe.<String>error(new Throwable()));
    Maybe<String> baz = Maybe.zip(maybes, new Function<Object[], String>() {
      @Override
      public String apply(Object[] objects) throws Exception {
        System.out.println(objects.length);
        return null;
      }
    }).switchIfEmpty(Maybe.create(new MaybeOnSubscribe<String>() {
      @Override
      public void subscribe(MaybeEmitter<String> e) throws Exception {
        System.out.println("FOO");
      }
    }));
    baz.test().dispose();
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
