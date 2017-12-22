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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.Expr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.IAstVisitor;

public class QualifiedType extends Type {

  private Type targetType;
  private List<TypeQualifier> qualifiers;

  public QualifiedType(Type targetType, List<TypeQualifier> qualifiers) {
    this.targetType = targetType;
    this.qualifiers = qualifiers;
  }

  public List<TypeQualifier> getQualifiers() {
    return Collections.unmodifiableList(qualifiers);
  }

  public void removeQualifier(TypeQualifier qualifier) {
    if (!hasQualifier(qualifier)) {
      throw new UnsupportedOperationException("Attempt to remove absent qualifier " + qualifier);
    }
    qualifiers.remove(qualifier);
  }

  @Override
  public boolean hasQualifier(TypeQualifier qualifier) {
    return qualifiers.contains(qualifier);
  }

  public boolean hasQualifiers() {
    return !qualifiers.isEmpty();
  }

  public Type getTargetType() {
    return targetType;
  }

  public void setTargetType(StructType targetType) {
    this.targetType = targetType;
  }

  @Override
  public void accept(IAstVisitor visitor) {
    visitor.visitQualifiedType(this);
  }

  @Override
  public String toString() {
    String result = "";
    for (TypeQualifier q : getQualifiers()) {
      result += q + " ";
    }
    result += targetType;
    return result;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (!(that instanceof QualifiedType)) {
      return false;
    }
    QualifiedType thatQualifiedType = (QualifiedType) that;
    Set<TypeQualifier> thisQualifiers = this.qualifiers.stream().collect(Collectors.toSet());
    Set<TypeQualifier> thatQualifiers = thatQualifiedType.qualifiers.stream()
        .collect(Collectors.toSet());
    return thisQualifiers.equals(thatQualifiers) && this.targetType
        .equals(thatQualifiedType.targetType);
  }

  @Override
  public int hashCode() {
    // TODO revisit if we end up storing large sets of types
    Set<TypeQualifier> qualifiersSet = qualifiers.stream().collect(Collectors.toSet());
    return qualifiersSet.hashCode() + targetType.hashCode();
  }

  @Override
  public QualifiedType clone() {
    List<TypeQualifier> newQualifiers = new ArrayList<>();
    for (TypeQualifier q : qualifiers) {
      newQualifiers.add(q);
    }
    return new QualifiedType(targetType.clone(), newQualifiers);
  }

  @Override
  public boolean hasCanonicalConstant() {
    return targetType.hasCanonicalConstant();
  }

  @Override
  public Expr getCanonicalConstant() {
    return targetType.getCanonicalConstant();
  }

  @Override
  public Type getWithoutQualifiers() {
    return targetType.getWithoutQualifiers();
  }

}
