package com.roamingroths.cmcc.data;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 1/6/18.
 */

public class UpdateHandle {
  public Set<Action> actions = new HashSet<>();
  public Map<String, Object> updates = new HashMap<>();

  public void merge(UpdateHandle other) {
    this.actions.addAll(other.actions);
    this.updates.putAll(other.updates);
  }

  public Action allActions() {
    return new Action() {
      @Override
      public void run() throws Exception {
        for (Action action : actions) {
          action.run();
        }
      }
    };
  }

  public static Completable run(Single<UpdateHandle> handle, final DatabaseReference ref) {
    return handle.flatMapCompletable(new Function<UpdateHandle, CompletableSource>() {
      @Override
      public CompletableSource apply(UpdateHandle updateHandle) throws Exception {
        return RxFirebaseDatabase.updateChildren(ref, updateHandle.updates).doOnComplete(updateHandle.allActions());
      }
    });
  }

  public static Function<List<UpdateHandle>, UpdateHandle> merge() {
    return new Function<List<UpdateHandle>, UpdateHandle>() {
      @Override
      public UpdateHandle apply(List<UpdateHandle> updateHandles) throws Exception {
        return merge(updateHandles);
      }
    };
  }

  public static UpdateHandle merge(Iterable<UpdateHandle> handles) {
    UpdateHandle handle = new UpdateHandle();
    for (UpdateHandle h : handles) {
      handle.merge(h);
    }
    return handle;
  }

  public static Single<UpdateHandle> merge(Single<UpdateHandle> h1, Single<UpdateHandle> h2) {
    return merge(Single.concatArray(h1, h2));
  }

  public static Single<UpdateHandle> merge(Single<UpdateHandle> h1, Single<UpdateHandle> h2, Single<UpdateHandle> h3) {
    return merge(Single.concatArray(h1, h2, h3));
  }

  private static Single<UpdateHandle> merge(Flowable<UpdateHandle> handles) {
    return handles.toList().map(new Function<List<UpdateHandle>, UpdateHandle>() {
      @Override
      public UpdateHandle apply(List<UpdateHandle> updateHandles) throws Exception {
        UpdateHandle handle = new UpdateHandle();
        for (UpdateHandle h : updateHandles) {
          handle.merge(h);
        }
        return handle;
      }
    });
  }

  public static Single<UpdateHandle> mergeSingles(Iterable<Single<UpdateHandle>> handles) {
    return merge(Single.concat(handles));
  }
}
