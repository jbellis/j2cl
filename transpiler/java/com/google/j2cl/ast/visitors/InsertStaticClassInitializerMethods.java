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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;

/** The first line of a static method should be a call to the Class's clinit. */
public class InsertStaticClassInitializerMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteMethod(Method method) {
      boolean isStaticMethod = method.getDescriptor().isStatic();
      boolean isJsConstructor = method.getDescriptor().isJsConstructor();
      if (isStaticMethod || isJsConstructor) {
        MethodDescriptor clinitDescriptor =
            MethodDescriptor.newBuilder()
                .setStatic(true)
                .setEnclosingClassTypeDescriptor(
                    method.getDescriptor().getEnclosingClassTypeDescriptor())
                .setName("$clinit")
                .setJsInfo(JsInfo.RAW)
                .build();
        MethodCall call = MethodCall.Builder.from(clinitDescriptor).build();
        return Method.Builder.from(method).addStatement(0, call.makeStatement()).build();
      }
      return method;
    }
  }
}
