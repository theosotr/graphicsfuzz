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

package uk.ac.ic.doc.multicore.ogltesting.common.typing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import org.junit.Test;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BinaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.MemberLookupExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.TypeConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.BasicType;
import uk.ac.ic.doc.multicore.ogltesting.common.util.GlslVersion;
import uk.ac.ic.doc.multicore.ogltesting.common.util.ParseHelper;

public class TyperTest {

  @Test
  public void visitMemberLookupExpr() throws Exception {

    String prog = "struct S { float a; float b; };\n"
        + "struct T { S s; float c; };\n"
        + "void main() {\n"
        + "  T myT = T(S(1.0, 2.0), 3.0);\n"
        + "  myT.s.a = myT.c;\n"
        + "}";

    TranslationUnit tu = ParseHelper.parse(prog, false);

    int actualCount =
        new Typer(tu, new GlslVersion("100", false)) {

          private int count;

          public int getCount() {
            return count;
          }

          @Override
          public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
            super.visitMemberLookupExpr(memberLookupExpr);
            assertNotNull(lookupType(memberLookupExpr));
            count++;
          }
        }.getCount();

    System.out.println(actualCount);

    assertEquals(3, actualCount);

  }

  @Test
  public void testTypeOfScalarConstructors() throws Exception {
    String program = "void main() { float(1); int(1); uint(1); bool(1); }";

    for (BasicType b : Arrays.asList(BasicType.FLOAT, BasicType.INT, BasicType.UINT,
        BasicType.BOOL)) {

      try {

        new Typer(ParseHelper.parse(program, false), new GlslVersion("440", false)) {

          @Override
          public void visitTypeConstructorExpr(TypeConstructorExpr typeConstructorExpr) {
            super.visitTypeConstructorExpr(typeConstructorExpr);
            if (lookupType(typeConstructorExpr) == b) {
              throw new RuntimeException("got_type");
            }
          }

        };

      } catch (RuntimeException re) {
        if (re.getMessage().equals("got_type")) {
          continue;
        }
        throw re;
      }

      assertFalse("Should not get here", true);

    }

  }

  @Test
  public void testMemberLookupTypeFloat() throws Exception {
    final String program = "void main() { vec2 v2 = vec2(1.0);"
        + " v2.x; v2.y;"
        + " vec3 v3 = vec3(1.0);"
        + " v3.x; v3.y; v3.z;"
        + " vec4 v4 = vec4(1.0);"
        + " v4.x; v4.y; v4.z; v4.w; }";
    final TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("100", false)) {
      @Override
      public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
        super.visitMemberLookupExpr(memberLookupExpr);
        assertEquals(BasicType.FLOAT, lookupType(memberLookupExpr));
      }
    };

  }

  @Test
  public void testMemberLookupTypeInt() throws Exception {
    final String program = "void main() { ivec2 v2 = ivec2(1);"
        + " v2.x; v2.y;"
        + " ivec3 v3 = ivec3(1);"
        + " v3.x; v3.y; v3.z;"
        + " ivec4 v4 = ivec4(1);"
        + " v4.x; v4.y; v4.z; v4.w; }";
    final TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("440", false)) {
      @Override
      public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
        super.visitMemberLookupExpr(memberLookupExpr);
        assertEquals(BasicType.INT, lookupType(memberLookupExpr));
      }
    };

  }

  @Test
  public void testMemberLookupTypeUint() throws Exception {
    final String program = "void main() { uvec2 v2 = uvec2(1u);"
        + " v2.x; v2.y;"
        + " uvec3 v3 = uvec3(1u);"
        + " v3.x; v3.y; v3.z;"
        + " uvec4 v4 = uvec4(1u);"
        + " v4.x; v4.y; v4.z; v4.w; }";
    final TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("440", false)) {
      @Override
      public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
        super.visitMemberLookupExpr(memberLookupExpr);
        assertEquals(BasicType.UINT, lookupType(memberLookupExpr));
      }
    };

  }


  @Test
  public void testMemberLookupTypeBool() throws Exception {
    final String program = "void main() { bvec2 v2 = bvec2(true);"
        + " v2.x; v2.y;"
        + " bvec3 v3 = bvec3(true);"
        + " v3.x; v3.y; v3.z;"
        + " bvec4 v4 = bvec4(true);"
        + " v4.x; v4.y; v4.z; v4.w; }";
    final TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("440", false)) {
      @Override
      public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
        super.visitMemberLookupExpr(memberLookupExpr);
        assertEquals(BasicType.BOOL, lookupType(memberLookupExpr));
      }
    };

  }

  @Test
  public void testBooleanVectorType() throws Exception {
    final String program = "void main() { vec3(1.0) > vec3(2.0); }";
    TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("440", false)) {
      @Override
      public void visitBinaryExpr(BinaryExpr binaryExpr) {
        super.visitBinaryExpr(binaryExpr);
        assertEquals(BasicType.BVEC3, lookupType(binaryExpr));
      }
    };
  }

  @Test
  public void testBooleanVectorType2() throws Exception {
    final String program = "void main() { vec3(1.0) > 2.0; }";
    TranslationUnit tu = ParseHelper.parse(program, false);
    new Typer(tu, new GlslVersion("440", false)) {
      @Override
      public void visitBinaryExpr(BinaryExpr binaryExpr) {
        super.visitBinaryExpr(binaryExpr);
        assertEquals(BasicType.BVEC3, lookupType(binaryExpr));
      }
    };
  }

}