package com.richanna.stepcounter.ui;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.ui.TextOrientationType;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.richanna.stepcounter.R;
import com.richanna.stepcounter.data.DataPoint;
import com.richanna.stepcounter.data.DataSink;
import com.richanna.stepcounter.sensors.SensorInfo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SensorPlotFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SensorPlotFragment extends Fragment implements DataSink {
  private static final long PLOT_TIMESPAN = 3L * 1000L * 1000L * 1000L; // In nanoseconds

  private static final String ARG_MAX_PLOT_RANGE_PREFERENCE_ID = "maxPlotRangePreference";
  private static final String ARG_MAX_PLOT_RANGE_DEFAULT_ID = "maxPlotRangeDefault";

  private String maxPlotRangePreference;
  private float maxPlotRangeDefault;
  private SensorPlot[] plots;

  public static SensorPlotFragment newInstance(final SensorInfo sensorInfo) {
    SensorPlotFragment fragment = new SensorPlotFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_MAX_PLOT_RANGE_PREFERENCE_ID, sensorInfo.getMaxPlotRangePreferenceId());
    args.putInt(ARG_MAX_PLOT_RANGE_DEFAULT_ID, sensorInfo.getMaxPlotRangeDefaultId());
    fragment.setArguments(args);
    return fragment;
  }

  public SensorPlotFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Bundle args = getArguments();
    maxPlotRangePreference = getString(args.getInt(ARG_MAX_PLOT_RANGE_PREFERENCE_ID));
    maxPlotRangeDefault = Float.parseFloat(getString(args.getInt(ARG_MAX_PLOT_RANGE_DEFAULT_ID)));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    final View view = inflater.inflate(R.layout.fragment_sensor_plot, container, false);

    plots = new SensorPlot[] {
        new SensorPlot(view, R.id.xPlot, R.xml.line_point_formatter_sensor_x),
        new SensorPlot(view, R.id.yPlot, R.xml.line_point_formatter_sensor_y),
        new SensorPlot(view, R.id.zPlot, R.xml.line_point_formatter_sensor_z)
    };

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    for (final SensorPlot plot : plots) {
      plot.reset();
    }
  }

  @Override
  public void process(final DataPoint dataPoint) {
    for (int i = 0; i < plots.length && i < dataPoint.getValues().length; i++) {
      plots[i].update(dataPoint.getTimestamp(), dataPoint.getValues()[i]);
    }
  }

  private float getMaxPlotRange() {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
    final String maxPlotRangeStr = preferences.getString(maxPlotRangePreference, null);
    if (maxPlotRangeStr == null) {
      return maxPlotRangeDefault;
    } else {
      return Float.parseFloat(maxPlotRangeStr);
    }
  }


  private class SensorPlot {

    private final XYPlot plot;
    private final SimpleXYSeries series;
    private final Queue<Long> timestampQueue = new LinkedList<>();

    public SensorPlot(final View view, final int plotId, final int formatterId) {
      series = new SimpleXYSeries(Collections.<Number>emptyList(), SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "Acceleration");

      plot = (XYPlot) view.findViewById(plotId);
      plot.centerOnRangeOrigin(0);

      final LineAndPointFormatter formatter = new LineAndPointFormatter();
      formatter.configure(getActivity().getApplicationContext(), formatterId);

      plot.addSeries(series, formatter);
      plot.setTicksPerRangeLabel(3);
      plot.getGraphWidget().setDomainLabelPaint(null);
      plot.getGraphWidget().setDomainOriginLabelPaint(null);
      plot.getLayoutManager().remove(plot.getLegendWidget());
      plot.getLayoutManager().remove(plot.getTitleWidget());
      plot.getLayoutManager().remove(plot.getDomainLabelWidget());
      plot.getRangeLabelWidget().setOrientation(TextOrientationType.HORIZONTAL);
    }

    public void reset() {
      timestampQueue.clear();
      final Number maxPlotRange = getMaxPlotRange();
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
