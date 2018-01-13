package com.roamingroths.cmcc.utils;

import com.google.common.base.Preconditions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.CompletableSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 1/6/18.
 */

public class UpdateHandle {

  public final DatabaseReference rootRef;
  public final Set<Action> actions = new HashSet<>();
  public final Map<String, Object> updates = new HashMap<>();

  private UpdateHandle(DatabaseReference rootRef) {
    this.rootRef = rootRef;
  }

  public static UpdateHandle forDb(FirebaseDatabase db) {
    return forRef(db.getReference());
  }

  public static UpdateHandle forRef(DatabaseReference ref) {
    return new UpdateHandle(ref);
  }

  public static UpdateHandle copy(UpdateHandle handle) {
    UpdateHandle copy = forRef(handle.rootRef);
    copy.actions.addAll(handle.actions);
    copy.updates.putAll(handle.updates);
    return copy;
  }

  public void merge(UpdateHandle other) {
    Preconditions.checkArgument(other.rootRef.equals(rootRef));
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

  public static Function<UpdateHandle, CompletableSource> run() {
    return new Function<UpdateHandle, CompletableSource>() {
      @Override
      public CompletableSource apply(UpdateHandle handle) throws Exception {
        return RxFirebaseDatabase.updateChildren(handle.rootRef, handle.updates).doOnComplete(handle.allActions());
      }
    };
  }

  public static BiConsumer<UpdateHandle, UpdateHandle> collector() {
    return new BiConsumer<UpdateHandle, UpdateHandle>() {
      @Override
      public void accept(UpdateHandle collection, UpdateHandle item) throws Exception {
        collection.merge(item);
      }
    };
  }
}
