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

package uk.ac.ic.doc.multicore.ogltesting.common.ast.expr;

import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.IAstVisitor;

public class UnaryExpr extends Expr {

  private Expr expr;
  private UnOp op;

  public UnaryExpr(Expr expr, UnOp op) {
    this.expr = expr;
    this.op = op;
  }

  public Expr getExpr() {
    return expr;
  }

  public UnOp getOp() {
    return op;
  }

  @Override
  public void accept(IAstVisitor visitor) {
    visitor.visitUnaryExpr(this);
  }

  @Override
  public UnaryExpr clone() {
    return new UnaryExpr(expr.clone(), op);
  }

  @Override
  public Expr getChild(int index) {
    if (index == 0) {
      return expr;
    }
    throw new IndexOutOfBoundsException("Index for UnaryExpr must be 0");
  }

  @Override
  public void setChild(int index, Expr expr) {
    if (index == 0) {
      this.expr = expr;
      return;
    }
    throw new IndexOutOfBoundsException("Index for UnaryExpr must be 0");
  }

  @Override
  public int getNumChildren() {
    return 1;
  }

}
