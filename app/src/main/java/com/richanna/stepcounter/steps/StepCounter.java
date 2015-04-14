package com.richanna.stepcounter.steps;

import com.richanna.stepcounter.data.DataSink;

public interface StepCounter extends DataSink {
  public void reset();
  public int getStepCount();
}
