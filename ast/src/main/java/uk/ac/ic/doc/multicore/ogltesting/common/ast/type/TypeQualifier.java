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

package uk.ac.ic.doc.multicore.ogltesting.common.ast.type;

public enum TypeQualifier {

  INVARIANT,
  PRECISE,
  CENTROID,
  SAMPLE,
  CONST,
  ATTRIBUTE,
  VARYING,
  IN,
  OUT,
  INOUT,
  UNIFORM,
  COHERENT,
  VOLATILE,
  RESTRICT,
  READONLY,
  WRITEONLY,
  FLAT,
  SMOOTH,
  NOPERSPECTIVE,
  HIGHP,
  MEDIUMP,
  LOWP;

  @Override
  public String toString() {
    switch (this) {
      case ATTRIBUTE:
        return "attribute";
      case CENTROID:
        return "centroid";
      case COHERENT:
        return "coherent";
      case CONST:
        return "const";
      case FLAT:
        return "flat";
      case HIGHP:
        return "highp";
      case IN:
        return "in";
      case INOUT:
        return "inout";
      case INVARIANT:
        return "invariant";
      case LOWP:
        return "lowp";
      case MEDIUMP:
        return "mediump";
      case NOPERSPECTIVE:
        return "noperspective";
      case OUT:
        return "out";
      case PRECISE:
        return "precise";
      case READONLY:
        return "readonly";
      case RESTRICT:
        return "restrict";
      case SAMPLE:
        return "sample";
      case SMOOTH:
        return "smooth";
      case UNIFORM:
        return "uniform";
      case VARYING:
        return "varying";
      case VOLATILE:
        return "volatile";
      case WRITEONLY:
        return "writeonly";
      default:
        throw new RuntimeException("Attempt to invoke toString on unknown qualifier");
    }
  }

}