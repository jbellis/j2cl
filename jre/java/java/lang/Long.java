/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Long.html">the
 * official Java API doc</a> for details.
 */
public class Long extends Number {
  private long value;

  public Long(long value) {
    this.value = value;
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public double doubleValue() {
    return (double) value;
  }

  @Override
  public float floatValue() {
    return (float) value;
  }

  @Override
  public int intValue() {
    return (int) value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Long) && (((Long) o).value == value);
  }

  @Override
  public int hashCode() {
    return (int) (this.longValue() ^ (this.longValue() >>> 32));
  }

  @Override
  public String toString() {
    return "" + value;
  }

  public static Long valueOf(long l) {
    //TODO: caching.
    return new Long(l);
  }
}
