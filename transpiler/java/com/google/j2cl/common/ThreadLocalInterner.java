/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.common;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/** An interner that provides per thread isolation. */
// TODO(rlubble): This class should extend com.google.common.collect.Interner<T> but that class
// is marked @GwtIncompatible.
public class ThreadLocalInterner<T> {
  private final ThreadLocal<Interner<T>> interner = new ThreadLocal<>();

  public T intern(T t) {
    return get().intern(t);
  }

  private Interner<T> get() {
    if (interner.get() == null) {
      interner.set(Interners.newStrongInterner());
    }
    return interner.get();
  }
}
