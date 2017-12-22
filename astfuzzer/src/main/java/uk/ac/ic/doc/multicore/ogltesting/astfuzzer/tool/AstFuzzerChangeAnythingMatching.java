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

package uk.ac.ic.doc.multicore.ogltesting.astfuzzer.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.IRandom;
import uk.ac.ic.doc.multicore.ogltesting.astfuzzer.util.ExprInterchanger;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.IAstNode;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BinaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.Expr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.FunctionCallExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.TernaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.TypeConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.StandardVisitor;
import uk.ac.ic.doc.multicore.ogltesting.common.typing.Typer;
import uk.ac.ic.doc.multicore.ogltesting.common.util.GlslVersion;

public class AstFuzzerChangeAnythingMatching extends AstFuzzer {

  private Map<Expr, Expr> mapOfReplacements;

  public AstFuzzerChangeAnythingMatching(int numberOfVariants,
        GlslVersion glslVersion, IRandom random) {
    super(numberOfVariants, glslVersion, random);
  }

  @Override
  TranslationUnit generateNewShader(TranslationUnit tu) {

    TranslationUnit result = tu.clone();
    traverseTreeAndMakeMappings(result);

    new StandardVisitor() {

      @Override
      protected <T extends IAstNode> void visitChildFromParent(Consumer<T> visitorMethod,
            T child,
            IAstNode parent) {
        super.visitChildFromParent(visitorMethod, child, parent);
        if (mapOfReplacements.containsKey(child)) {
          //some IAstNodes don't allow replaceChild
          try {
            parent.replaceChild(child, mapOfReplacements.get(child));
          } catch (Exception exception) {
            System.err.println(exception.getMessage());
          }
        }
        ;
      }

    }.visit(result);
    return result;

  }

  private void traverseTreeAndMakeMappings(TranslationUnit tu) {

    Typer typer = new Typer(tu, getGlslVersion());
    mapOfReplacements = new HashMap<>();

    new StandardVisitor() {

      @Override
      public void visitFunctionCallExpr(FunctionCallExpr functionCallExpr) {
        super.visitFunctionCallExpr(functionCallExpr);
        fuzzExpr(functionCallExpr, typer);

      }

      @Override
      public void visitBinaryExpr(BinaryExpr binaryExpr) {
        super.visitBinaryExpr(binaryExpr);
        if (!binaryExpr.getOp().isSideEffecting()) {
          fuzzExpr(binaryExpr, typer);
        }
      }

      @Override
      public void visitTernaryExpr(TernaryExpr ternaryExpr) {
        super.visitTernaryExpr(ternaryExpr);
        fuzzExpr(ternaryExpr, typer);
      }

      @Override
      public void visitTypeConstructorExpr(TypeConstructorExpr typeConstructorExpr) {
        super.visitTypeConstructorExpr(typeConstructorExpr);
        fuzzExpr(typeConstructorExpr, typer);
      }
    }.visit(tu);

  }

  private void fuzzExpr(Expr expr, Typer typer) {

    if (getRandom().nextBoolean()) {
      Expr replacement = findReplacement(expr, typer);
      mapOfReplacements.put(expr, replacement);
    }
  }

  private Expr findReplacement(Expr expr, Typer typer) {

    Type returnType = typer.lookupType(expr);
    List<Expr> args = new ArrayList<>();
    for (int i = 0; i < expr.getNumChildren(); i++) {
      args.add(expr.getChild(i));

    }
    Signature signature = new Signature(returnType,
          args.stream().map(x -> typer.lookupType(x)).collect(Collectors.toList()));

    List<ExprInterchanger> matches = getFunctionLists()
          .getInterchangeableForSignature(signature);
    if (!matches.isEmpty()) {
      int randomIndex = getRandom().nextInt(matches.size());
      return matches.get(randomIndex).interchangeExpr(args);
    }
    return expr;
  }
}
