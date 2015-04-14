package com.richanna.stepcounter.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.richanna.stepcounter.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO: document your custom view class.
 */
public class SensorPlotView extends LinearLayout {
  private static final long PLOT_TIMESPAN = 3L * 1000L * 1000L * 1000L; // In nanoseconds

  private final String maxPlotRangePreference;
  private final float maxPlotRangeDefault;
  private final SensorPlot[] plots;

  public SensorPlotView(Context context) {
    this(context, null);
  }

  public SensorPlotView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Load attributes
    final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SensorPlotView);
    maxPlotRangePreference = a.getString(R.styleable.SensorPlotView_maxPlotRangePreference);
    maxPlotRangeDefault = a.getFloat(R.styleable.SensorPlotView_maxPlotRangeDefault, Float.parseFloat(context.getString(R.string.default_max_plot_range)));
    a.recycle();

    setOrientation(LinearLayout.VERTICAL);
    setGravity(Gravity.CENTER_VERTICAL);

    final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View view = inflater.inflate(R.layout.sensor_plot_view, this, true);

    plots = new SensorPlot[] {
        new SensorPlot(view, R.id.xPlot, R.xml.line_point_formatter_sensor_x),
        new SensorPlot(view, R.id.yPlot, R.xml.line_point_formatter_sensor_y),
        new SensorPlot(view, R.id.zPlot, R.xml.line_point_formatter_sensor_z)
    };
  }

  private float getMaxPlotRange() {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    final String maxPlotRangeStr = preferences.getString(maxPlotRangePreference, null);
    if (maxPlotRangeStr == null) {
      return maxPlotRangeDefault;
    } else {
      return Float.parseFloat(maxPlotRangeStr);
    }
  }

  public void reset() {
    for (final SensorPlot plot : plots) {
      plot.reset();
    }
  }

  public void update(final long timestamp, final float[] values) {
    for (int i = 0; i < plots.length && i < values.length; i++) {
      plots[i].update(timestamp, values[i]);
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
      formatter.configure(getContext(), formatterId);

      plot.addSeries(series, formatter);
      plot.setTicksPerRangeLabel(3);
      plot.getGraphWidget().setDomainLabelPaint(null);
      plot.getGraphWidget().setDomainOriginLabelPaint(null);
      plot.getLayoutManager().remove(plot.getLegendWidget());
      plot.getLayoutManager().remove(plot.getTitleWidget());
      plot.getLayoutManager().remove(plot.getDomainLabelWidget());
      plot.getLayoutManager().remove(plot.getRangeLabelWidget());
    }

    public void reset() {
      timestampQueue.clear();
      final Number maxPlotRange = SensorPlotView.this.getMaxPlotRange();
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
