package com.richanna.stepcounter.filters;

import com.richanna.stepcounter.data.DataFilter;
import com.richanna.stepcounter.data.DataPoint;

public class GravityCompensator implements DataFilter {

  private static final float GRAVITY_ALPHA = 0.8f;

  private float[] gravity = { 0, 0, 0 };

  @Override
  public DataPoint apply(final DataPoint dataPoint) {
    float[] filteredValues = dataPoint.getValues().clone();
    for (int i = 0; i < filteredValues.length; i++) {
      gravity[i] = GRAVITY_ALPHA * gravity[i] + (1 - GRAVITY_ALPHA) * dataPoint.getValues()[i];
      filteredValues[i] -= gravity[i];
    }

    return new DataPoint(dataPoint.getTimestamp(), filteredValues);
  }
}
