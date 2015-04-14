package com.richanna.stepcounter.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.richanna.stepcounter.data.DataGenerator;
import com.richanna.stepcounter.data.DataPoint;
import com.richanna.stepcounter.data.DataSink;

import java.util.ArrayList;
import java.util.List;

public class SensorMonitor implements DataGenerator, SensorEventListener {

  private final SensorManager sensorManager;
  private final List<DataSink> sinks = new ArrayList<>();
  private final int sensorType;

  public SensorMonitor(final SensorManager sensorManager, final int sensorType) {
    this.sensorManager = sensorManager;
    this.sensorType = sensorType;
  }

  @Override
  public void addSink(final DataSink sink) {
    sinks.add(sink);
  }

  @Override
  public void pause() {
    sensorManager.unregisterListener(this);
  }

  @Override
  public void resume() {
    sensorManager.registerListener(this, sensorManager.getDefaultSensor(sensorType), SensorManager.SENSOR_DELAY_FASTEST);
  }

  @Override
  public void onSensorChanged(final SensorEvent event) {

    if (event.sensor.getType() == sensorType) {
      final DataPoint dataPoint = new DataPoint(event.timestamp, event.values);
      for (final DataSink sink : sinks) {
        sink.process(dataPoint);
      }
    }
  }

  @Override
  public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    // Do nothing for this event.
  }
}
