package com.bloomcyclecare.cmcc.data.db;

import androidx.arch.core.util.Function;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.bloomcyclecare.cmcc.data.models.Entry;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

import static com.google.common.truth.Truth.assertThat;

public class BaseEntryDaoTest {

  private static class TestEntry extends Entry {

    @Override
    public List<String> getSummaryLines() {
      return List.of();
    }
  }

  private static abstract class StubEntryDao<E extends Entry> extends BaseEntryDao<E> {

    private final E e;

    StubEntryDao(Class<E> clazz, Function<LocalDate, E> emptyEntryFn) {
      super(clazz, emptyEntryFn);
      e = emptyEntryFn.apply(LocalDate.now());
    }

    @Override
    public Completable delete(E entry) {
      return Completable.complete();
    }

    @Override
    public Completable delete(List<E> entries) {
      return Completable.complete();
    }

    @Override
    Completable insertInternal(E entry) {
      return Completable.complete();
    }

    @Override
    Completable updateInternal(E entry) {
      return Completable.complete();
    }

    @Override
    protected Maybe<E> doMaybeT(SupportSQLiteQuery query) {
      return Maybe.just(e);
    }

    @Override
    protected Flowable<E> doFlowableT(SupportSQLiteQuery query) {
      return Flowable.just(e);
    }

    @Override
    protected Flowable<List<E>> doFlowableList(SupportSQLiteQuery query) {
      return Flowable.just(List.of(e));
    }
  }

  private static class TestEntryDao extends StubEntryDao<TestEntry> {
    TestEntryDao() {
      super(TestEntry.class, (date) -> new TestEntry());
    }
  }

  TestEntryDao testEntryDao = new TestEntryDao();

  @Test
  public void testInsert_success() {
    TestEntry entry = new TestEntry();
    assertThat(entry.mTimeCreated).isNull();
    assertThat(entry.mTimeUpdated).isNull();
    assertThat(entry.mTimesUpdated).isEqualTo(0);

    testEntryDao.insert(entry).test().assertComplete();
    assertThat(entry.mTimeCreated).isNotNull();
    assertThat(entry.mTimeUpdated).isEqualTo(entry.mTimeCreated);
    assertThat(entry.mTimesUpdated).isEqualTo(1);
  }

  @Test
  public void testInsert_alreadyCreated() {
    TestEntry entry = new TestEntry();
    entry.mTimeCreated = DateTime.now();
    testEntryDao.insert(entry).test().assertError(t -> t instanceof IllegalArgumentException);
  }

  @Test
  public void testUpdate_success() {
    TestEntry entry = new TestEntry();
    entry.mTimeCreated = DateTime.now().minusHours(1);
    entry.mTimeUpdated = entry.mTimeCreated;
    entry.mTimesUpdated = 1;

    testEntryDao.update(entry).test().assertComplete();
    assertThat(entry.mTimeCreated).isNotNull();
    assertThat(entry.mTimeUpdated).isGreaterThan(entry.mTimeCreated);
    assertThat(entry.mTimesUpdated).isEqualTo(2);
  }

  @Test
  public void testUpdate_notCreated() {
    TestEntry entry = new TestEntry();
    entry.mTimeCreated = null;
    testEntryDao.update(entry).test().assertError(t -> t instanceof IllegalArgumentException);
  }

  @Test
  public void testUpdate_noUpdateCount() {
    TestEntry entry = new TestEntry();
    entry.mTimeCreated = DateTime.now();
    entry.mTimesUpdated = 0;
    testEntryDao.update(entry).test().assertError(t -> t instanceof IllegalArgumentException);
  }
}
