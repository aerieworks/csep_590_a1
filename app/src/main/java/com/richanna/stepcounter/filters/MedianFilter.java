package com.richanna.stepcounter.filters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.richanna.stepcounter.R;
import com.richanna.stepcounter.data.DataFilter;
import com.richanna.stepcounter.data.DataPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MedianFilter implements DataFilter {

  private final Context context;
  private final List<DataPoint> dataPointWindow = new ArrayList<>();

  public MedianFilter(final Context context) {
    this.context = context;
  }

  @Override
  public DataPoint apply(DataPoint dataPoint) {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    final int windowSize = Integer.parseInt(preferences.getString(context.getString(R.string.pref_key_median_filter_window_size), Integer.toString(R.integer.default_median_filter_window_size)));

    dataPointWindow.add(dataPoint);
    if (dataPointWindow.size() >= windowSize) {
      while (dataPointWindow.size() > windowSize) {
        dataPointWindow.remove(0);
      }

      final float[] medianValues = new float[dataPointWindow.get(0).getValues().length];
      for (int i = 0; i < medianValues.length; i++) {
        medianValues[i] = calculateMedian(i);
      }

      Log.d("MedianFilter", String.format("Generated point: %f, %f, %f; list size: %d", medianValues[0], medianValues[1], medianValues[2], dataPointWindow.size()));
      return new DataPoint(dataPoint.getTimestamp(), medianValues);
    }

    return null;
  }

  private float calculateMedian(final int valueIndex) {
    final List<Float> values = new ArrayList<>(dataPointWindow.size());
    for (final DataPoint dataPoint : dataPointWindow) {
      values.add(dataPoint.getValues()[valueIndex]);
    }

    Collections.sort(values);
    return values.get(values.size() / 2);
  }
}
