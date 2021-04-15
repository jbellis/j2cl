/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal;

/** A common base abstraction for the arrays in Wasm. */
abstract class WasmArray {

  abstract int getLength();

  abstract Object get(int index);

  abstract void set(int index, Object o);

  public static Object[] createMultiDimensional(int[] dimensionLengths, int leafType) {
    return (Object[]) createRecursively(dimensionLengths, 0, leafType);
  }

  private static Object createRecursively(int[] dimensions, int dimensionIndex, int leafType) {
    int length = dimensions[dimensionIndex];
    if (length == -1) {
      // We reach a sub array that doesn't have a dimension (e.g. 3rd dimension in int[3][2][][])
      return null;
    }

    if (dimensionIndex == dimensions.length - 1) {
      // We have reached the leaf dimension.
      return createLeaf(length, leafType);
    } else {
      Object[] array = new Object[length];
      for (int i = 0; i < length; i++) {
        array[i] = createRecursively(dimensions, dimensionIndex + 1, leafType);
      }
      return array;
    }
  }

  private static Object createLeaf(int length, int leafType) {
    switch (leafType) {
      case 1:
        return new boolean[length];
      case 2:
        return new byte[length];
      case 3:
        return new short[length];
      case 4:
        return new char[length];
      case 5:
        return new int[length];
      case 6:
        return new long[length];
      case 7:
        return new float[length];
      case 8:
        return new double[length];
      default:
        return new Object[length];
    }
  }

  static class OfObject extends WasmArray {

    Object[] elements;

    OfObject(int length) {
      elements = new Object[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object o) {
      elements[index] = o;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfByte extends WasmArray {

    private byte[] elements;

    OfByte(int length) {
      elements = new byte[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object b) {
      elements[index] = (byte) b;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfShort extends WasmArray {

    private short[] elements;

    OfShort(int length) {
      elements = new short[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object s) {
      elements[index] = (short) s;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfChar extends WasmArray {

    private char[] elements;

    OfChar(int length) {
      elements = new char[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object c) {
      elements[index] = (char) c;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfInt extends WasmArray {

    private int[] elements;

    OfInt(int length) {
      elements = new int[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object i) {
      elements[index] = (int) i;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfLong extends WasmArray {

    private long[] elements;

    OfLong(int length) {
      elements = new long[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object l) {
      elements[index] = (long) l;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfFloat extends WasmArray {

    private float[] elements;

    OfFloat(int length) {
      elements = new float[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object f) {
      elements[index] = (float) f;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfDouble extends WasmArray {

    private double[] elements;

    OfDouble(int length) {
      elements = new double[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object b) {
      elements[index] = (double) b;
    }

    public int getLength() {
      return elements.length;
    }
  }

  static class OfBoolean extends WasmArray {

    private boolean[] elements;

    OfBoolean(int length) {
      elements = new boolean[length];
    }

    public Object get(int index) {
      return elements[index];
    }

    public void set(int index, Object b) {
      elements[index] = (boolean) b;
    }

    public int getLength() {
      return elements.length;
    }
  }
}