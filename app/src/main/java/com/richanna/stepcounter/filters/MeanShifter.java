package com.richanna.stepcounter.filters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.richanna.stepcounter.R;
import com.richanna.stepcounter.data.DataFilter;
import com.richanna.stepcounter.data.DataPoint;

import java.util.ArrayList;
import java.util.List;

public class MeanShifter implements DataFilter {

  private final Context context;
  private final List<DataPoint> dataPointWindow = new ArrayList<>();
  private int emitCount = 0;
  private float[] currentMean = null;

  public MeanShifter(final Context context) {
    this.context = context;
  }

  @Override
  public DataPoint apply(DataPoint dataPoint) {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    final int windowSize = Integer.parseInt(preferences.getString(context.getString(R.string.pref_key_mean_shifter_window_size), Integer.toString(R.integer.default_mean_shifter_window_size)));

    dataPointWindow.add(dataPoint);
    if (dataPointWindow.size() >= windowSize) {
      while (dataPointWindow.size() > windowSize) {
        dataPointWindow.remove(0);
      }

      final DataPoint source = dataPointWindow.get(windowSize / 2);
      if (emitCount == 0) {
        currentMean = new float[source.getValues().length];
        for (int i = 0; i < currentMean.length; i++) {
          currentMean[i] = calculateMean(i);
        }
        Log.d("MeanShifter", String.format("New mean: %f, %f, %f", currentMean[0], currentMean[1], currentMean[2]));
      }
      emitCount = (emitCount + 1) % (windowSize / 2);

      final float[] adjustedValues = new float[] {
          source.getValues()[0] - currentMean[0],
          source.getValues()[1] - currentMean[1],
          source.getValues()[2] - currentMean[2]
      };
      Log.d("MeanShifter", String.format("Generated point: %f, %f, %f; list size: %d", adjustedValues[0], adjustedValues[1], adjustedValues[2], dataPointWindow.size()));
      return new DataPoint(dataPoint.getTimestamp(), adjustedValues);
    }

    return null;
  }

  private float calculateMean(final int valueIndex) {
    float total = 0;
    for (final DataPoint dataPoint : dataPointWindow) {
      total += dataPoint.getValues()[valueIndex];
    }

    return total / (float)dataPointWindow.size();
  }
}
