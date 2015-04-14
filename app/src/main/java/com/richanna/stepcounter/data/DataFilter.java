package com.richanna.stepcounter.data;

/**
 * Created by annabelle on 4/10/15.
 */
public interface DataFilter {
  public DataPoint apply(final DataPoint dataPoint);
}
