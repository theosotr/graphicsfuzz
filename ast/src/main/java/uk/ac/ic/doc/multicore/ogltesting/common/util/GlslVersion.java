// Copyright 2017 Imperial College London
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package uk.ac.ic.doc.multicore.ogltesting.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class GlslVersion {

  private final GlslSpecVersion specVersion;
  private final boolean isWebGl;
  private final boolean isVulkan;

  private GlslVersion(GlslSpecVersion specVersion, boolean isWebGl, boolean isVulkan) {
    this.specVersion = specVersion;
    this.isWebGl = isWebGl;
    this.isVulkan = isVulkan;
  }

  private GlslVersion(String versionString, boolean isWebGl, boolean isVulkan) {
    this(GlslSpecVersion.fromString(versionString), isWebGl, isVulkan);
  }

  public GlslVersion(String versionString, boolean isWebGl) {
    this(GlslSpecVersion.fromString(versionString), isWebGl, false);
  }

  public boolean isSpecVersion(String versionString) {
    return specVersion.equals(GlslSpecVersion.fromString(versionString));
  }

  public boolean after(String versionString) {
    return specVersion.after(GlslSpecVersion.fromString(versionString));
  }

  public boolean before(String versionString) {
    return specVersion.before(GlslSpecVersion.fromString(versionString));
  }

  public boolean isWebGl() {
    return isWebGl;
  }

  @Override
  public String toString() {
    return specVersion.toString();
  }

  public boolean avoidDeprecatedGlFragColor() {
    return
        isVulkan || specVersion.avoidDeprecatedGlFragColor();
  }

  public static final GlslVersion VULKAN = new GlslVersion("450", false, true);

  public static GlslVersion getGlslVersionFromShader(File shaderFilename)
        throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(shaderFilename));
    final String firstLine = br.readLine();
    final String secondLine = br.readLine();
    br.close();
    String[] components = firstLine.trim().split(" ");
    if (!firstLine.startsWith("#version") || components.length < 2) {
      final String message = "File must specify a version on the first line, using #version";
      System.err
            .println(message);
      throw new RuntimeException(
            message);
    }
    String version = components[1];
    for (int i = 2; i < components.length; i++) {
      version += " " + components[i];
    }
    return new GlslVersion(version, secondLine != null && secondLine.startsWith("//WebGL"));
  }

}