package com.richanna.stepcounter.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.richanna.stepcounter.R;
import com.richanna.stepcounter.SettingsActivity;
import com.richanna.stepcounter.data.DataPoint;
import com.richanna.stepcounter.data.DataSink;
import com.richanna.stepcounter.data.DataStream;
import com.richanna.stepcounter.filters.GravityCompensator;
import com.richanna.stepcounter.filters.MeanShifter;
import com.richanna.stepcounter.filters.MedianFilter;
import com.richanna.stepcounter.filters.Reorienter;
import com.richanna.stepcounter.sensors.SensorInfo;
import com.richanna.stepcounter.sensors.SensorMonitor;
import com.richanna.stepcounter.steps.StepCounter;
import com.richanna.stepcounter.steps.ZeroCrossingStepCounter;

public class SensorMonitorActivity extends ActionBarActivity implements ActionBar.TabListener, SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  PagerAdapter pagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  ViewPager viewPager;

  private final Map<SensorInfo, DataStream> sensorDataStreams = new HashMap<>();
  private final Map<SensorInfo, MedianFilter> medianFilters = new HashMap<>();
  private GravityCompensator gravityCompensator = new GravityCompensator();
  private Reorienter reorienter = new Reorienter();
  private MeanShifter meanShifter;
  private StepCounter stepCounter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sensor_monitor);

    // Set up the action bar.
    final ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    pagerAdapter = new PagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    viewPager = (ViewPager) findViewById(R.id.pager);
    viewPager.setAdapter(pagerAdapter);

    // When swiping between different sections, select the corresponding
    // tab. We can also use ActionBar.Tab#select() to do this if we have
    // a reference to the Tab.
    viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        actionBar.setSelectedNavigationItem(position);
      }
    });

    // For each of the sections in the app, add a tab to the action bar.
    for (int i = 0; i < pagerAdapter.getCount(); i++) {
      // Create a tab with text corresponding to the page title defined by
      // the adapter. Also specify this Activity object, which implements
      // the TabListener interface, as the callback (listener) for when
      // this tab is selected.
      actionBar.addTab(
          actionBar.newTab()
              .setText(pagerAdapter.getPageTitle(i))
              .setTabListener(this));
    }

    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    //stepCounter = new HackyStepCounter(preferences, getApplicationContext());
    stepCounter = new ZeroCrossingStepCounter(this);
    for (final SensorInfo sensorInfo : SensorInfo.values()) {
      if (sensorInfo.isDirty()) {
        medianFilters.put(sensorInfo, new MedianFilter(this));
      }
    }

    final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    meanShifter = new MeanShifter(this);

    final DataStream rotationStream = new DataStream(new SensorMonitor(sensorManager, SensorInfo.Rotation.getSensorType()));
    rotationStream.addSink(reorienter);
    sensorDataStreams.put(SensorInfo.Rotation, rotationStream);

    sensorDataStreams.put(SensorInfo.Gravity, new DataStream(new SensorMonitor(sensorManager, SensorInfo.Gravity.getSensorType())));
    sensorDataStreams.put(SensorInfo.Gyroscope, new DataStream(new SensorMonitor(sensorManager, SensorInfo.Gyroscope.getSensorType())));

    final DataStream accelerometerStream = new DataStream(new SensorMonitor(sensorManager, SensorInfo.Accelerometer.getSensorType()));
    sensorDataStreams.put(SensorInfo.Accelerometer, accelerometerStream);

    accelerometerStream.addSink(stepCounter);
    accelerometerStream.addSink(
        new DataSink() {
          @Override
          public void process(final DataPoint dataPoint) {
            updateStepCount();
          }
        });

    onSharedPreferenceChanged(preferences, getString(R.string.pref_key_apply_gravity_filter));
    onSharedPreferenceChanged(preferences, getString(R.string.pref_key_apply_median_filter));
    onSharedPreferenceChanged(preferences, getString(R.string.pref_key_apply_mean_shifter));
    onSharedPreferenceChanged(preferences, getString(R.string.pref_key_apply_reorienter));
    preferences.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    resetStepCounter();
    for (final DataStream stream : sensorDataStreams.values()) {
      stream.resume();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    for (final DataStream stream : sensorDataStreams.values()) {
      stream.pause();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_sensor_monitor, menu);
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
    } else if (id == R.id.action_reset) {
      confirmResetStepCount();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    viewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key == getString(R.string.pref_key_apply_gravity_filter)) {
      final boolean applyGravityFilter = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_apply_gravity_filter));
      final DataStream accelerometerStream = sensorDataStreams.get(SensorInfo.Accelerometer);
      if (applyGravityFilter) {
        accelerometerStream.addFilter(0, gravityCompensator);
      } else {
        accelerometerStream.removeFilter(gravityCompensator);
      }
    } else if (key == getString(R.string.pref_key_apply_median_filter)) {
      final boolean applyMedianFilter = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_apply_median_filter));
      for (final Map.Entry<SensorInfo, DataStream> entry : sensorDataStreams.entrySet()) {
        if (entry.getKey().isDirty()) {
          if (applyMedianFilter) {
            entry.getValue().addFilter(0, medianFilters.get(entry.getKey()));
          } else {
            entry.getValue().removeFilter(medianFilters.get(entry.getKey()));
          }
        }
      }
    } else if (key == getString(R.string.pref_key_apply_mean_shifter)) {
      final boolean applyMeanShifter = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_apply_mean_shifter));
      if (applyMeanShifter) {
        sensorDataStreams.get(SensorInfo.Accelerometer).addFilter(meanShifter);
      } else {
        sensorDataStreams.get(SensorInfo.Accelerometer).removeFilter(meanShifter);
      }
    } else if (key == getString(R.string.pref_key_apply_reorienter)) {
      final boolean applyReorienter = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_apply_median_filter));
      if (applyReorienter) {
        sensorDataStreams.get(SensorInfo.Accelerometer).addFilter(reorienter);
      } else {
        sensorDataStreams.get(SensorInfo.Accelerometer).removeFilter(reorienter);
      }
    }
  }

  private void confirmResetStepCount() {
    new AlertDialog.Builder(this)
        .setMessage(R.string.confirm_reset_step_count)
        .setTitle(R.string.confirmation_title)
        .setPositiveButton(R.string.confirmation_yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            resetStepCounter();
          }
        })
        .setNegativeButton(R.string.confirmation_no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Do nothing.
          }
        })
        .create()
        .show();
  }

  private void resetStepCounter() {
    stepCounter.reset();
    updateStepCount();
  }

  private void updateStepCount() {
    setTitle(String.format(getString(R.string.app_name_parameterized), stepCounter.getStepCount()));
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class PagerAdapter extends FragmentPagerAdapter {

    public PagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      final SensorInfo sensorInfo = SensorInfo.values()[position];
      final SensorPlotFragment fragment = SensorPlotFragment.newInstance(sensorInfo);
      sensorDataStreams.get(sensorInfo).addSink(fragment);

      return fragment;
    }

    @Override
    public int getCount() {
      return SensorInfo.values().length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      Locale l = Locale.getDefault();
      final SensorInfo[] sensorInfos = SensorInfo.values();
      if (position < sensorInfos.length) {
        return getString(sensorInfos[position].getSensorNameId()).toUpperCase(l);
      }
      return null;
    }
  }

}
