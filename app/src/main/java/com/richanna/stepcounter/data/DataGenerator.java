package com.richanna.stepcounter.data;

public interface DataGenerator {
  public void addSink(final DataSink sink);
  public void pause();
  public void resume();
}
