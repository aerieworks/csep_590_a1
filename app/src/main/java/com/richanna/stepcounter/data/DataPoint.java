package com.richanna.stepcounter.data;

public class DataPoint {
  private final long timestamp;
  private final float[] values;

  public DataPoint(final long timestamp, final float[] values) {
    this.timestamp = timestamp;
    this.values = values;
  }

  public long getTimestamp() { return timestamp; }
  public float[] getValues() { return values; }
}
