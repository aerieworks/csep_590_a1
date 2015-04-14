package com.richanna.stepcounter.sensors;

import android.hardware.Sensor;

import com.richanna.stepcounter.R;

public enum SensorInfo {
  Accelerometer(Sensor.TYPE_ACCELEROMETER,
      R.string.sensor_name_accelerometer,
      R.string.pref_key_max_plot_range,
      R.string.default_max_plot_range,
      true),
  Gyroscope(Sensor.TYPE_GYROSCOPE,
      R.string.sensor_name_gyroscope,
      R.string.pref_key_max_plot_range,
      R.string.default_max_plot_range,
      true),
  Gravity(Sensor.TYPE_GRAVITY,
      R.string.sensor_name_gravity,
      R.string.pref_key_max_plot_range,
      R.string.default_max_plot_range,
      true),
  Rotation(Sensor.TYPE_ROTATION_VECTOR,
      R.string.sensor_name_rotation,
      R.string.pref_key_max_plot_range,
      R.string.default_max_plot_range,
      true),
  StepCounter(Sensor.TYPE_STEP_COUNTER,
      R.string.sensor_name_step_counter,
      R.string.pref_key_max_plot_range,
      R.string.default_max_plot_range,
      false);

  private final int sensorType;
  private final int sensorNameId;
  private final int maxPlotRangePreferenceId;
  private final int maxPlotRangeDefaultId;
  private final boolean isDirty;

  SensorInfo(final int sensorType,
             final int sensorNameId,
             final int maxPlotRangePreferenceId,
             final int maxPlotRangeDefaultId,
             final boolean isDirty) {
    this.sensorType = sensorType;
    this.sensorNameId = sensorNameId;
    this.maxPlotRangePreferenceId = maxPlotRangePreferenceId;
    this.maxPlotRangeDefaultId = maxPlotRangeDefaultId;
    this.isDirty = isDirty;
  }

  public int getSensorType() { return sensorType; }
  public int getSensorNameId() { return sensorNameId; }
  public int getMaxPlotRangePreferenceId() { return maxPlotRangePreferenceId; }
  public int getMaxPlotRangeDefaultId() { return maxPlotRangeDefaultId; }
  public boolean isDirty() { return isDirty; }
}
