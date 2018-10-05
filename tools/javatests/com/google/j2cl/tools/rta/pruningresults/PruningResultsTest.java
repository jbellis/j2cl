/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.tools.rta.pruningresults;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.j2cl.tools.minifier.J2clMinifier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for the line pruning mechanism. */
@RunWith(JUnit4.class)
public class PruningResultsTest {
  private static class J2clFileInfo {
    private final String path;
    private final String content;

    private J2clFileInfo(String path, String content) {
      this.path = path;
      this.content = content;
    }
  }

  private static final String FILE_DIRECTORY = "com/google/j2cl/tools/rta/pruningresults/";

  private static J2clMinifier j2clMinifier;
  private static String j2clZipfilePath;
  private static J2clFileInfo fooFile;
  private static J2clFileInfo barFile;

  @BeforeClass
  public static void setUp() throws Exception {
    j2clMinifier = new J2clMinifier();
    readFilesFromZipFile();
  }

  private static void readFilesFromZipFile() throws IOException {
    j2clZipfilePath = checkNotNull(System.getProperty("j2cl_zip_file"));
    ZipFile j2clZipFile = new ZipFile(j2clZipfilePath);

    fooFile = readFileFromZipFile(j2clZipFile, "Foo.impl.java.js");
    barFile = readFileFromZipFile(j2clZipFile, "Bar.impl.java.js");
  }

  private static J2clFileInfo readFileFromZipFile(ZipFile zipFile, String fileName) {
    return zipFile.stream()
        .filter(ze -> ze.getName().endsWith(fileName))
        .findAny()
        .map(entry -> readZipEntry(entry, zipFile))
        .get();
  }

  private static J2clFileInfo readZipEntry(ZipEntry entry, ZipFile zipFile) {
    String path = createAbsoluteZipEntryPath(entry.getName());
    String content;

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), UTF_8))) {
      content = CharStreams.toString(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new J2clFileInfo(path, content);
  }

  @Test
  public void testFooImplFileIsCorrectlyPruned() {
    ImmutableList<String> removedLinesRegexs =
        ImmutableList.of(
            "constructor\\(\\) \\{", "create..\\(\\) \\{", "\\(\"This is unused\"\\);"
            // TODO(b/116175766): getter and setter need to be removed before enabling this check
            // "unusedStaticField"
            );

    ImmutableList<String> notRemovedLinesChecks =
        ImmutableList.of(
            "entryPoint() {",
            "$clinit() {",
            "$isInstance(instance) {",
            "$isAssignableFrom(classConstructor) {",
            "$loadModules() {");

    // Because the input is generated by j2cl and can change, first assert the content of the file
    // contains the expected lines.
    removedLinesRegexs.forEach(
        regex ->
            assertWithMessage("Input file is incorrect:")
                .that(fooFile.content)
                .containsMatch(regex));
    notRemovedLinesChecks.forEach(
        line -> assertWithMessage("Input file is incorrect:").that(fooFile.content).contains(line));

    // assert that only the correct lines are removed
    String contentAfterLineRemoval = j2clMinifier.minify(fooFile.path, fooFile.content);
    removedLinesRegexs.forEach(
        regex -> assertThat(contentAfterLineRemoval).doesNotContainMatch(regex));
    notRemovedLinesChecks.forEach(line -> assertThat(contentAfterLineRemoval).contains(line));

    System.err.println(fooFile.content);
    System.err.println(contentAfterLineRemoval);

    // assert we don't change the number of lines for not breaking source map.
    assertThat(numberOfLinesOf(fooFile.content))
        .isEqualTo(numberOfLinesOf(contentAfterLineRemoval));
  }

  @Test
  public void testBarImplFileIsNotPruned() {
    assertFileContentIsNotPruned(barFile.path, barFile.content);
  }

  @Test
  public void testUnusedTypeFilesArePruned() {
    assertFileIsPruned(createAbsoluteZipEntryPath(FILE_DIRECTORY + "UnusedType.impl.java.js"));
    assertFileIsPruned(createAbsoluteZipEntryPath(FILE_DIRECTORY + "UnusedType.java.js"));
  }

  @Test
  public void testHeaderFilesAreNotPruned() {
    // We don't care about the real content of the file as it should not be modified. By passing
    // a one line content, the test will  either throw an exception if J2CLPruner try to remove a
    // line with an index > 0 or fail if J2clPruner removes the first line or the entire file.
    // Otherwise it will success as expected.
    String fakeContent = "fake";

    assertFileContentIsNotPruned(
        createAbsoluteZipEntryPath(FILE_DIRECTORY + "Foo.java.js"), fakeContent);
    assertFileContentIsNotPruned(
        createAbsoluteZipEntryPath(FILE_DIRECTORY + "Bar.java.js"), fakeContent);
  }

  private void assertFileIsPruned(String filePath) {
    String content = "Fake file content";
    assertWithMessage("Unused file [%s] has not been pruned.", filePath)
        .that(j2clMinifier.minify(filePath, content))
        .isEmpty();
  }

  private void assertFileContentIsNotPruned(String filePath, String fileContent) {
    String minifiedAndPruned = j2clMinifier.minify(filePath, fileContent);
    String onlyMinified = j2clMinifier.minify(fileContent);

    assertWithMessage("File [%s] has been pruned.", filePath)
        .that(minifiedAndPruned)
        .isEqualTo(onlyMinified);
  }

  private static String createAbsoluteZipEntryPath(String entryName) {
    return j2clZipfilePath + "!/" + entryName;
  }

  private static int numberOfLinesOf(String content) {
    return Splitter.on(System.lineSeparator()).splitToList(content).size();
  }
}
