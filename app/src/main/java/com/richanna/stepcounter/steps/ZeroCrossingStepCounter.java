package com.richanna.stepcounter.steps;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.richanna.stepcounter.R;
import com.richanna.stepcounter.data.DataPoint;

public class ZeroCrossingStepCounter implements StepCounter {

  private final Context context;
  private boolean inStep = false;
  private int stepCount = 0;

  public ZeroCrossingStepCounter(final Context context) {
    this.context = context;
    reset();
  }

  @Override
  public void reset() {
    stepCount = 0;
    inStep = false;
  }

  @Override
  public int getStepCount() {
    return stepCount;
  }

  @Override
  public void process(final DataPoint dataPoint) {
    final float zAccel = dataPoint.getValues()[2];
    if (inStep && zAccel < 0) {
      stepCount += 1;
      inStep = false;
    } else if (!inStep) {
      final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
      final float stepThresholdZ = Float.parseFloat(preferences.getString(context.getString(R.string.pref_key_step_z_threshold), context.getString(R.string.default_step_threshold_z)));
      if (zAccel > stepThresholdZ) {
        inStep = true;
      }
    }
  }
}
