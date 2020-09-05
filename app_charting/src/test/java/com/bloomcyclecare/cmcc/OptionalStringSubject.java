package com.bloomcyclecare.cmcc;


import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Optional;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;

public class OptionalStringSubject extends Subject {

  private final Optional<String> actual;

  /**
   * Constructor for use by subclasses. If you want to create an instance of this class itself, call
   * {@code .that(actual)}.
   *
   * @param metadata
   * @param actual
   */
  protected OptionalStringSubject(FailureMetadata metadata, @NullableDecl Optional<String> actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  public void isEmpty() {
    if (actual == null) {
      failWithActual(simpleFact("expected empty optional"));
    } else if (actual.isPresent()) {
      failWithoutActual(
          simpleFact("expected to be empty"), fact("but was present with value", actual.get()));
    }
  }

  public StringSubject value() {
    StringSubject out = null;
    if (actual == null) {
      failWithActual(simpleFact("expected present optional"));
    } else if (!actual.isPresent()) {
      failWithActual(simpleFact("expected present optional"));
    } else {
      out = check("get()").that(actual.get());
    }
    return out;
  }

  public static Subject.Factory<OptionalStringSubject, Optional<String>> optionals() {
    return OptionalStringSubject::new;
  }
}
