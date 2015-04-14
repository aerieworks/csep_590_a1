package com.richanna.stepcounter.filters;

import android.opengl.Matrix;
import android.hardware.SensorManager;

import com.richanna.stepcounter.data.DataFilter;
import com.richanna.stepcounter.data.DataPoint;
import com.richanna.stepcounter.data.DataSink;

import java.util.Arrays;

public class Reorienter implements DataFilter, DataSink {

  private float[] rotationMatrix = new float[16];

  @Override
  public DataPoint apply(DataPoint dataPoint) {
    final float[] vector = Arrays.copyOf(dataPoint.getValues(), 4);
    final float[] reoriented = new float[4];
    Matrix.multiplyMV(reoriented, 0, rotationMatrix, 0, vector, 0);

    return new DataPoint(dataPoint.getTimestamp(), Arrays.copyOf(reoriented, 3));
  }

  @Override
  public void process(DataPoint dataPoint) {
    float[] deviceRotationMatrix = new float[16];
    SensorManager.getRotationMatrixFromVector(deviceRotationMatrix, dataPoint.getValues());
    Matrix.invertM(rotationMatrix, 0, deviceRotationMatrix, 0);
  }
}
