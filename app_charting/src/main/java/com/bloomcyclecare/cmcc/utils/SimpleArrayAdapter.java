package com.bloomcyclecare.cmcc.utils;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class SimpleArrayAdapter<T, H extends SimpleArrayAdapter.SimpleViewHolder<T>> extends ArrayAdapter<T> {

  private final Context mContext;
  private final Consumer<T> mClickHandler;
  private final int mRowResourceId;
  private final ArrayList<T> mData;
  private final SparseArray<H> mViewHolders = new SparseArray<>();
  private final Function<View, H> mHolderFactoryFn;

  public SimpleArrayAdapter(Context context, int rowResourceId, Function<View, H> holderFactoryFn, Consumer<T> clickHandler) {
    this(context, rowResourceId, holderFactoryFn, clickHandler, new ArrayList<>());
  }

  private SimpleArrayAdapter(Context context, int rowResourceId, Function<View, H> holderFactoryFn, Consumer<T> clickHandler, ArrayList<T> data) {
    super(context, -1, data);
    mData = data;
    mContext = context;
    mClickHandler = clickHandler;
    mRowResourceId = rowResourceId;
    mHolderFactoryFn = holderFactoryFn;
  }

  @Override
  @NonNull
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    LayoutInflater inflater = (LayoutInflater) mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(mRowResourceId, parent, false);
    try {
      H holder = mHolderFactoryFn.apply(rowView);
      mViewHolders.append(position, holder);
      holder.bind(mData.get(position));
      rowView.setOnClickListener(v -> {
        try {
          mClickHandler.accept(holder.getCurrent());
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    return rowView;
  }

  public H holderForItem(T item) {
    return mViewHolders.get(mData.indexOf(item));
  }

  public void updateData(Collection<T> data) {
    mData.clear();
    mData.addAll(data);
    mViewHolders.clear();
    notifyDataSetChanged();
  }

  public static abstract class SimpleViewHolder<T> extends RecyclerView.ViewHolder {

    private T mBoundT;

    public SimpleViewHolder(View view) {
      super(view);
    }

    protected final T getCurrent() {
      return mBoundT;
    }

    public final void bind(T data) {
      mBoundT = data;
      updateUI(mBoundT);
    }

    protected abstract void updateUI(T data);
  }
}
