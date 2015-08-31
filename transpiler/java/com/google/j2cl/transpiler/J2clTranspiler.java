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
package com.google.j2cl.transpiler;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.FixBoxingOnSideEffectOperationsVisitor;
import com.google.j2cl.ast.visitors.InsertImplicitCastsVisitor;
import com.google.j2cl.ast.visitors.InsertInstanceInitCallsVisitor;
import com.google.j2cl.ast.visitors.MakeExplicitEnumConstructionVisitor;
import com.google.j2cl.ast.visitors.NormalizeCastsVisitor;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructorsVisitor;
import com.google.j2cl.common.VelocityUtil;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.JavaScriptGenerator;

import org.apache.velocity.app.VelocityEngine;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Translation tool for generating JavaScript source files from Java sources.
 */
public class J2clTranspiler {

  /**
   * Indicates that errors were found and reported and transpiler execution should now end silently.
   * <p>
   * Is preferred over System.exit() since it is caught here and will not end an external caller.
   */
  private class ExitGracefullyException extends RuntimeException {}

  /**
   * Entry point for the tool, which runs the entire J2CL pipeline.
   */
  public static void main(String[] args) {
    J2clTranspiler transpiler = new J2clTranspiler(args);
    try {
      transpiler.run();
    } catch (ExitGracefullyException e) {
      // Error already reported. End execution now.
    }
  }

  private List<CompilationUnit> convertUnits(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath) {
    List<CompilationUnit> compilationUnits =
        CompilationUnitBuilder.build(jdtUnitsByFilePath, options, errors);
    maybeExitGracefully();
    return compilationUnits;
  }

  private final String[] args;
  private final Errors errors = new Errors();
  private FrontendOptions options;
  private final VelocityEngine velocityEngine = VelocityUtil.createEngine();

  private J2clTranspiler(String[] args) {
    this.args = args;
  }

  private Map<String, org.eclipse.jdt.core.dom.CompilationUnit> createJdtUnits() {
    JdtParser parser = new JdtParser(options, errors);
    Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath =
        parser.parseFiles(options.getSourceFiles());
    maybeExitGracefully();
    return jdtUnitsByFilePath;
  }

  private void generateJsSources(List<CompilationUnit> j2clCompilationUnits) {
    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      // The parameters may be changed after the previous passes are implemented.
      Charset charset = Charset.forName(options.getEncoding());

      JavaScriptGenerator jsGenerator =
          new JavaScriptGenerator(
              errors,
              options.getOutputFileSystem(),
              options.getOutput(),
              charset,
              j2clCompilationUnit,
              velocityEngine);
      jsGenerator.writeToFile();
    }

    options.maybeCloseFileSystem();
  }

  private void generateSourceMaps(@SuppressWarnings("unused") List<CompilationUnit> j2clUnits) {
    // TODO: implement.
  }

  private void loadOptions() {
    FrontendFlags flags = new FrontendFlags(errors);
    flags.parse(args);
    maybeExitGracefully();

    options = new FrontendOptions(errors, flags);
    maybeExitGracefully();
  }

  private void maybeExitGracefully() {
    if (errors.errorCount() > 0) {
      errors.report();
      throw new ExitGracefullyException();
    }
  }

  private void normalizeUnits(@SuppressWarnings("unused") List<CompilationUnit> j2clUnits) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      // Class structure normalizations.
      MakeExplicitEnumConstructionVisitor.doMakeEnumConstructionExplicit(j2clUnit);
      InsertInstanceInitCallsVisitor.doInsertInstanceInitCall(j2clUnit);
      NormalizeNestedClassConstructorsVisitor.doNormalizeNestedClassConstructors(j2clUnit);

      // Statement/Expression normalizations
      ControlStatementFormatter.doFormatControlStatements(j2clUnit);
      InsertImplicitCastsVisitor.doInsertImplicitCasts(j2clUnit);
      NormalizeCastsVisitor.doNormalizeCasts(j2clUnit);
      FixBoxingOnSideEffectOperationsVisitor.doFixBoxingOnSideEffectOperations(j2clUnit);
    }
  }

  /**
   * Runs the entire J2CL pipeline.
   */
  private void run() {
    loadOptions();
    List<CompilationUnit> j2clUnits = convertUnits(createJdtUnits());
    normalizeUnits(j2clUnits);
    generateJsSources(j2clUnits);
    generateSourceMaps(j2clUnits);
  }
}
