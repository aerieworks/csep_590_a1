package com.richanna.stepcounter.data;

import java.util.ArrayList;
import java.util.List;

public class DataStream implements DataGenerator {

  private final DataGenerator source;
  private final List<DataFilter> filterChain = new ArrayList<>();
  private final List<DataSink> sinks = new ArrayList<>();

  public DataStream(final DataGenerator source) {
    this.source = source;
    source.addSink(new DataSink() {
      @Override
      public void process(DataPoint dataPoint) {
        for (final DataFilter filter : filterChain) {
          dataPoint = filter.apply(dataPoint);
          if (dataPoint == null) {
            return;
          }
        }

        for (final DataSink sink : sinks) {
          sink.process(dataPoint);
        }
      }
    });
  }

  public DataStream addFilter(final DataFilter filter) {
    if (!filterChain.contains(filter)) {
      filterChain.add(filter);
    }

    return this;
  }

  public DataStream addFilter(final int location, final DataFilter filter) {
    if (!filterChain.contains(filter)) {
      filterChain.add(location, filter);
    }

    return this;
  }

  public DataStream removeFilter(final DataFilter filter) {
    filterChain.remove(filter);
    return this;
  }

  @Override
  public void pause() {
    source.pause();
  }

  @Override
  public void resume() {
    source.resume();
  }

  @Override
  public void addSink(final DataSink sink) {
    sinks.add(sink);
  }
}
