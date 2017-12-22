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

enum GlslSpecVersion {
  V100,
  V110,
  V120,
  V130,
  V140,
  V150,
  V300es,
  V310es,
  V330,
  V400,
  V410,
  V420,
  V430,
  V440,
  V450;

  static GlslSpecVersion fromString(String version) {
    for (GlslSpecVersion v : values()) {
      if (version.equals(v.toString())) {
        return v;
      }
    }
    throw new RuntimeException("Unknown GLSL version " + version);
  }

  @Override
  public String toString() {
    switch (this) {
      case V100:
        return "100";
      case V110:
        return "110";
      case V120:
        return "120";
      case V130:
        return "130";
      case V140:
        return "140";
      case V150:
        return "150";
      case V300es:
        return "300 es";
      case V310es:
        return "310 es";
      case V330:
        return "330";
      case V400:
        return "400";
      case V410:
        return "410";
      case V420:
        return "420";
      case V430:
        return "430";
      case V440:
        return "440";
      case V450:
        return "450";
      default:
        throw new RuntimeException("toString not implemented for a GLSL version");
    }
  }

  boolean before(@SuppressWarnings("SameParameterValue") GlslSpecVersion version) {
    return this.compareTo(version) < 0;
  }

  boolean after(GlslSpecVersion version) {
    return this.compareTo(version) > 0;
  }

  public boolean avoidDeprecatedGlFragColor() {
    return this == V300es || this == V310es;
  }

}
