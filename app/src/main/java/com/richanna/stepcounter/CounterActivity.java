package com.richanna.stepcounter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class CounterActivity extends ActionBarActivity implements SensorEventListener {

  private static final String LOG_KEY = "CounterActivity";
  private static final long PLOT_TIMESPAN = 3L * 1000L * 1000L * 1000L; // In nanoseconds
  private static final float GRAVITY_ALPHA = 0.8f;

  private SharedPreferences preferences;
  private SensorManager sensorManager;
  private AccelerationPlot[] plots;
  private StepCounter stepCounter = new StepCounter();
  private TextView lblStepCount;
  private float[] gravity = { 0, 0, 0 };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_counter);

    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    lblStepCount = (TextView) findViewById(R.id.lblStepCount);

    plots = new AccelerationPlot[] {
      new AccelerationPlot(R.id.xAccelPlot, R.xml.line_point_formatter_x_accel),
      new AccelerationPlot(R.id.yAccelPlot, R.xml.line_point_formatter_y_accel),
      new AccelerationPlot(R.id.zAccelPlot, R.xml.line_point_formatter_z_accel)
    };
  }

  @Override
  protected void onResume() {
    super.onResume();
    for (int i = 0; i < 3; i++) {
      gravity[i] = 0;
      plots[i].reset();
    }
    stepCounter = new StepCounter();
    lblStepCount.setText("0");
    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
  }

  @Override
  protected void onPause() {
    super.onPause();
    sensorManager.unregisterListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_counter, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSensorChanged(final SensorEvent event) {

    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      Log.d(LOG_KEY, String.format("Time: %d, values: (%f, %f, %f)", event.timestamp, event.values[0], event.values[1], event.values[2]));
      final float[] filteredValues = filterValues(event.values);
      for (int i = 0; i < 3; i++) {
        plots[i].update(event.timestamp, filteredValues[i]);
      }

      if (stepCounter.checkStep(event.timestamp, filteredValues[2])) {
        lblStepCount.setText(Integer.toString(stepCounter.getStepCount()));
      }
    }
  }

  private float[] filterValues(final float[] values) {
    float[] filteredValues = new float[3];
    for (int i = 0; i < 3; i++) {
      filteredValues[i] = values[i];
      if (preferences.getBoolean(SettingsActivity.KEY_PREF_APPLY_GRAVITY_FILTER, SettingsActivity.DEF_PREF_APPLY_GRAVITY_FILTER)) {
        gravity[i] = GRAVITY_ALPHA * gravity[i] + (1 - GRAVITY_ALPHA) * values[i];
        filteredValues[i] -= gravity[i];
      }
    }

    return filteredValues;
  }

  @Override
  public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    // Do nothing for this event.
  }

  private class StepCounter {

    private long lastZero = 0;
    private boolean hitThreshold = false;
    private int stepCount = 0;

    public boolean checkStep(final long timestamp, final float value) {
      boolean isStep = false;
      final long maxStepDuration = Long.parseLong(preferences.getString(SettingsActivity.KEY_PREF_MAX_STEP_DURATION, SettingsActivity.DEF_PREF_MAX_STEP_DURATION)) * 1000000L;
      final float stepThresholdZ = Float.parseFloat(preferences.getString(SettingsActivity.KEY_PREF_STEP_THRESHOLD_Z, SettingsActivity.DEF_PREF_STEP_THRESHOLD_Z));

      if (value <= 0) {
        if (hitThreshold) {
          isStep = (timestamp - lastZero) < maxStepDuration;
          Log.d("StepCounter", String.format("From threshold to zero: %d", timestamp - lastZero));
          if (isStep) {
            Log.d("StepCounter", "Took step: " + stepCount);
            stepCount += 1;
          }
        }
        hitThreshold = false;
        lastZero = timestamp;
      } else if (value > stepThresholdZ) {
        hitThreshold = true;
        Log.d("StepCounter", String.format("Hit threshold: %d, %f", timestamp, value));
      }

      return isStep;
    }

    public int getStepCount() {
      return stepCount;
    }
  }

  private class AccelerationPlot {

    private final XYPlot plot;
    private final SimpleXYSeries series;
    private final Queue<Long> timestampQueue = new LinkedList<Long>();

    public AccelerationPlot(final int plotId, final int formatterId) {
      series = new SimpleXYSeries(Collections.<Number>emptyList(), SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Acceleration");

      plot = (XYPlot) findViewById(plotId);
      plot.centerOnRangeOrigin(0);

      final LineAndPointFormatter formatter = new LineAndPointFormatter();
      formatter.configure(getApplicationContext(), formatterId);

      plot.addSeries(series, formatter);
      plot.setTicksPerRangeLabel(3);
      plot.getGraphWidget().setDomainLabelOrientation(-45);
    }

    public void reset() {
      timestampQueue.clear();

      final Number maxPlotRange = Float.parseFloat(preferences.getString(SettingsActivity.KEY_PREF_MAX_PLOT_RANGE, SettingsActivity.DEF_PREF_MAX_PLOT_RANGE));
      plot.setRangeBoundaries(-maxPlotRange.floatValue(), maxPlotRange, BoundaryMode.FIXED);
      series.setModel(Collections.<Number>emptyList(), SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
      plot.redraw();
    }

    public void update(final long timestamp, final float value) {
      timestampQueue.add(timestamp);
      while (timestamp - timestampQueue.peek() > PLOT_TIMESPAN) {
        timestampQueue.remove();
        series.removeFirst();
      }

      series.addLast(timestamp, value);
      plot.redraw();
    }
  }
}
