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

package uk.ac.ic.doc.multicore.ogltesting.common.ast.decl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.LayoutQualifier;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.TypeQualifier;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.IAstVisitor;

public class InterfaceBlock extends Declaration {

  private final Optional<LayoutQualifier> layoutQualifier;
  private final TypeQualifier interfaceQualifier;
  private final String structName;
  private final List<String> memberNames;
  private final List<Type> memberTypes;
  private final String identifierName;

  private InterfaceBlock(Optional<LayoutQualifier> layoutQualifier,
      TypeQualifier interfaceQualifier, String structName,
      List<String> memberNames,
      List<Type> memberTypes,
      String identifierName) {
    this.layoutQualifier = layoutQualifier;
    this.interfaceQualifier = interfaceQualifier;
    assert Arrays.asList(TypeQualifier.IN, TypeQualifier.OUT, TypeQualifier.UNIFORM)
        .contains(interfaceQualifier);
    this.structName = structName;
    this.memberNames = new ArrayList<>();
    this.memberNames.addAll(memberNames);
    this.memberTypes = new ArrayList<>();
    this.memberTypes.addAll(memberTypes);
    this.identifierName = identifierName;
  }

  public InterfaceBlock(LayoutQualifier layoutQualifier,
      TypeQualifier interfaceQualifier, String name,
      String memberName,
      Type memberType,
      String identifierName) {
    this(Optional.of(layoutQualifier), interfaceQualifier,
        name, Arrays.asList(memberName), Arrays.asList(memberType), identifierName);
  }

  public List<Type> getMemberTypes() {
    return Collections.unmodifiableList(memberTypes);
  }

  public List<String> getMemberNames() {
    return Collections.unmodifiableList(memberNames);
  }

  public boolean hasLayoutQualifier() {
    return layoutQualifier.isPresent();
  }

  public LayoutQualifier getLayoutQualifier() {
    assert hasLayoutQualifier();
    return layoutQualifier.get();
  }

  public TypeQualifier getInterfaceQualifier() {
    return interfaceQualifier;
  }

  public String getStructName() {
    return structName;
  }

  public String getIdentifierName() {
    return identifierName;
  }

  public Type getMemberType(String name) {
    for (int i = 0; i < memberNames.size(); i++) {
      if (memberNames.get(i).equals(name)) {
        return memberTypes.get(i);
      }
    }
    throw new RuntimeException("Unknown member " + name);
  }

  @Override
  public void accept(IAstVisitor visitor) {
    visitor.visitInterfaceBlock(this);
  }

  @Override
  public InterfaceBlock clone() {
    return new InterfaceBlock(layoutQualifier,
        interfaceQualifier,
        structName,
        memberNames,
        memberTypes.stream().map(item -> item.clone()).collect(Collectors.toList()),
        identifierName);
  }

}
