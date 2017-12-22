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

public class ArrayIndexExpr extends Expr {

  private Expr array;
  private Expr index;

  public ArrayIndexExpr(Expr array, Expr index) {
    this.array = array;
    this.index = index;
  }

  public Expr getArray() {
    return array;
  }

  public Expr getIndex() {
    return index;
  }

  public void setIndex(Expr index) {
    this.index = index;
  }

  @Override
  public void accept(IAstVisitor visitor) {
    visitor.visitArrayIndexExpr(this);
  }

  @Override
  public ArrayIndexExpr clone() {
    return new ArrayIndexExpr(array.clone(), index.clone());
  }

  @Override
  public Expr getChild(int index) {
    if (index == 0) {
      return array;
    }
    if (index == 1) {
      return this.index;
    }
    throw new IndexOutOfBoundsException("Index for ArrayIndexExpr must be 0 or 1");
  }

  @Override
  public void setChild(int index, Expr expr) {
    if (index == 0) {
      array = expr;
      return;
    }
    if (index == 1) {
      this.index = expr;
      return;
    }
    throw new IndexOutOfBoundsException("Index for ArrayIndexExpr must be 0 or 1");
  }

  @Override
  public int getNumChildren() {
    return 2;
  }

}
