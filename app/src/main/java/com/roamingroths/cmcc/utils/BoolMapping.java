package com.roamingroths.cmcc.utils;

import com.google.common.collect.ForwardingMap;

import java.util.HashMap;
import java.util.Map;

public class BoolMapping extends ForwardingMap<String, Boolean> {

  public final Map<String, Boolean> delegate;

  public BoolMapping() {
    this(new HashMap<>());
  }

  public BoolMapping(Map<String, Boolean> in) {
    delegate = in;
  }

  @Override
  protected Map<String, Boolean> delegate() {
    return delegate;
  }
}
