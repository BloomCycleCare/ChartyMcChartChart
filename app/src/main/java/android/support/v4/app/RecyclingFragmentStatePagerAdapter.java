package android.support.v4.app;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public abstract class RecyclingFragmentStatePagerAdapter<F extends Fragment> extends PagerAdapter {


  private static final String TAG = "FragmentStatePagerAdapt";
  private static final boolean DEBUG = false;

  private final FragmentManager mFragmentManager;
  private FragmentTransaction mCurTransaction = null;

  private ArrayList<F.SavedState> mSavedState = new ArrayList<>();
  private ArrayList<F> mFragments = new ArrayList<>();
  private ArrayList<F> mUnusedFragments = new ArrayList<>();
  private Fragment mCurrentPrimaryItem = null;

  public RecyclingFragmentStatePagerAdapter(FragmentManager fm) {
    mFragmentManager = fm;
  }

  /**
   * Return the Fragment associated with a specified position.
   */
  public abstract F getItem(int position);

  @Override
  public void startUpdate(ViewGroup container) {
    if (container.getId() == View.NO_ID) {
      throw new IllegalStateException("ViewPager with adapter " + this
          + " requires a view id");
    }
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    // If we already have this item instantiated, there is nothing
    // to do.  This can happen when we are restoring the entire pager
    // from its saved state, where the fragment manager has already
    // taken care of restoring the fragments we previously had instantiated.
    if (mFragments.size() > position) {
      F fragment = mFragments.get(position);
      if (fragment != null) {
        return fragment;
      }
    }

    // Try and recycle an unused fragment before creating a new one
    boolean recycling = !mUnusedFragments.isEmpty();
    F fragment = recycling ? mUnusedFragments.remove(0) : getItem(position);
    fragment.setMenuVisibility(false);
    fragment.setUserVisibleHint(false);

    if (mSavedState.size() > position) {
      Fragment.SavedState fss = mSavedState.get(position);
      if (fss != null) {
        fragment.setInitialSavedState(fss);
      }
    }

    while (mFragments.size() <= position) {
      mFragments.add(null);
    }
    mFragments.set(position, fragment);

    if (!recycling) {
      if (mCurTransaction == null) {
        mCurTransaction = mFragmentManager.beginTransaction();
      }
      mCurTransaction.add(container.getId(), fragment);
    }

    return fragment;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    F fragment = (F) object;

    while (mSavedState.size() <= position) {
      mSavedState.add(null);
    }
    mSavedState.set(position, fragment.isAdded()
        ? mFragmentManager.saveFragmentInstanceState(fragment) : null);
    mFragments.set(position, null);

    fragment.initState();

    mUnusedFragments.add(fragment);
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public void setPrimaryItem(ViewGroup container, int position, Object object) {
    Fragment fragment = (Fragment)object;
    if (fragment != mCurrentPrimaryItem) {
      if (mCurrentPrimaryItem != null) {
        mCurrentPrimaryItem.setMenuVisibility(false);
        mCurrentPrimaryItem.setUserVisibleHint(false);
      }
      if (fragment != null) {
        fragment.setMenuVisibility(true);
        fragment.setUserVisibleHint(true);
      }
      mCurrentPrimaryItem = fragment;
    }
  }

  @Override
  public void finishUpdate(ViewGroup container) {
    if (mCurTransaction != null) {
      mCurTransaction.commitNowAllowingStateLoss();
      mCurTransaction = null;
    }
  }

  @Override
  public boolean isViewFromObject(View view, Object object) {
    return ((Fragment)object).getView() == view;
  }

  @Override
  public Parcelable saveState() {
    Bundle state = null;
    if (mSavedState.size() > 0) {
      state = new Bundle();
      Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
      mSavedState.toArray(fss);
      state.putParcelableArray("states", fss);
    }
    for (int i=0; i<mFragments.size(); i++) {
      Fragment f = mFragments.get(i);
      if (f != null && f.isAdded()) {
        if (state == null) {
          state = new Bundle();
        }
        String key = "f" + i;
        mFragmentManager.putFragment(state, key, f);
      }
    }
    return state;
  }

  @Override
  public void restoreState(Parcelable state, ClassLoader loader) {
    if (state != null) {
      Bundle bundle = (Bundle)state;
      bundle.setClassLoader(loader);
      Parcelable[] fss = bundle.getParcelableArray("states");
      mSavedState.clear();
      mFragments.clear();
      if (fss != null) {
        for (int i=0; i<fss.length; i++) {
          mSavedState.add((Fragment.SavedState)fss[i]);
        }
      }
      Iterable<String> keys = bundle.keySet();
      for (String key: keys) {
        if (key.startsWith("f")) {
          int index = Integer.parseInt(key.substring(1));
          F fragment = (F) mFragmentManager.getFragment(bundle, key);
          if (fragment != null) {
            while (mFragments.size() <= index) {
              mFragments.add(null);
            }
            fragment.setMenuVisibility(false);
            mFragments.set(index, fragment);
          } else {
            Log.w(TAG, "Bad fragment at key " + key);
          }
        }
      }
    }
  }
}
