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

package uk.ac.ic.doc.multicore.ogltesting.common.tool;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.IAstNode;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionDefinition;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionPrototype;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.InterfaceBlock;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ParameterDecl;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.PrecisionDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.StructDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariableDeclInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariablesDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayIndexExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BinaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BoolConstantExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.Expr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.FloatConstantExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.FunctionCallExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.IntConstantExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.MemberLookupExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ParenExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.TernaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.TypeConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.UIntConstantExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.UnaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.VariableIdentifierExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.BlockStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.BreakStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.ContinueStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.DeclarationStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.DefaultCaseLabel;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.DiscardStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.DoStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.ExprCaseLabel;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.ExprStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.ForStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.IfStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.NullStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.ReturnStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.Stmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.SwitchStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.VersionStatement;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.WhileStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.ArrayType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.AtomicIntType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.BasicType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.ImageType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.QualifiedType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.SamplerType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.StructType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.TypeQualifier;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.VoidType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.StandardVisitor;

public class PrettyPrinterVisitor extends StandardVisitor {

  public static final int DEFAULT_INDENTATION_WIDTH = 1;
  public static final Supplier<String> DEFAULT_NEWLINE_SUPPLIER = () -> "\n";
  private final Supplier<String> newLineSupplier;
  private final int indentationWidth;
  private int indentationCount = 0;
  private final PrintStream out;
  private boolean inFunctionDefinition = false;

  public PrettyPrinterVisitor(PrintStream out) {
    this(out, DEFAULT_INDENTATION_WIDTH, DEFAULT_NEWLINE_SUPPLIER);
  }

  public PrettyPrinterVisitor(PrintStream out, int indentationWidth,
        Supplier<String> newLineSupplier) {
    this.out = out;
    this.indentationWidth = indentationWidth;
    this.newLineSupplier = newLineSupplier;
  }

  private String newLine() {
    return newLineSupplier.get();
  }

  /**
   * Returns, via pretty printing, a string representation of the given node.
   *
   * @param node Node for which string representation is required
   * @return String representation of the node
   */
  public static String prettyPrintAsString(IAstNode node) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new PrettyPrinterVisitor(new PrintStream(bytes)).visit(node);
    return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
  }

  @Override
  public void visitVersionStatement(VersionStatement versionStatement) {
    out.append(versionStatement.getText());
  }

  @Override
  public void visitPrecisionDeclaration(PrecisionDeclaration precisionDeclaration) {
    out.append(indent() + precisionDeclaration.getText() + "\n\n");
  }

  @Override
  public void visitDeclarationStmt(DeclarationStmt declarationStmt) {
    out.append(indent());
    super.visitDeclarationStmt(declarationStmt);
    out.append(";" + newLine());
  }

  @Override
  public void visitVariablesDeclaration(VariablesDeclaration variablesDeclaration) {
    Type baseType = variablesDeclaration.getBaseType();
    visit(baseType);
    out.append(" ");
    boolean first = true;
    for (VariableDeclInfo vdi : variablesDeclaration.getDeclInfos()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      out.append(vdi.getName());
      if (vdi.hasArrayInfo()) {
        out.append("[" + vdi.getArrayInfo().getSize() + "]");
        assert !(baseType instanceof ArrayType);
      } else if (baseType instanceof ArrayType) {
        out.append("[" + ((ArrayType) baseType).getArrayInfo().getSize() + "]");
      }
      if (vdi.hasInitializer()) {
        out.append(" = ");
        visit(vdi.getInitializer());
      }
    }
  }

  @Override
  public void visitFunctionDefinition(FunctionDefinition functionDefinition) {
    assert !inFunctionDefinition;
    inFunctionDefinition = true;
    super.visitFunctionDefinition(functionDefinition);
    assert inFunctionDefinition;
    inFunctionDefinition = false;
  }

  @Override
  public void visitFunctionPrototype(FunctionPrototype functionPrototype) {
    visit(functionPrototype.getReturnType());
    out.append(" " + functionPrototype.getName() + "(");
    boolean first = true;
    for (ParameterDecl p : functionPrototype.getParameters()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      visit(p);
    }
    out.append(")");
    if (!inFunctionDefinition) {
      out.append(";");
    }
    out.append(newLine());
  }

  @Override
  public void visitParameterDecl(ParameterDecl parameterDecl) {
    visit(parameterDecl.getType());
    if (parameterDecl.getName() != null) {
      out.append(" " + parameterDecl.getName());
    }
    if (parameterDecl.getArrayInfo() != null) {
      out.append("[" + parameterDecl.getArrayInfo().getSize() + "]");
    }
  }

  @Override
  public void visitBlockStmt(BlockStmt stmt) {
    out.append(indent() + "{" + newLine());
    increaseIndent();
    for (Stmt s : stmt.getStmts()) {
      visit(s);
    }
    decreaseIndent();
    out.append(indent() + "}" + newLine());
  }

  private String indent() {
    String result = "";
    for (int i = 0; i < indentationCount; i++) {
      result += " ";
    }
    return result;
  }

  @Override
  public void visitIfStmt(IfStmt ifStmt) {
    out.append(indent() + "if(");
    visit(ifStmt.getCondition());
    out.append(")" + newLine());
    increaseIndent();
    visit(ifStmt.getThenStmt());
    decreaseIndent();
    if (ifStmt.hasElseStmt()) {
      out.append(indent() + "else" + newLine());
      increaseIndent();
      visit(ifStmt.getElseStmt());
      decreaseIndent();
    }
  }

  @Override
  public void visitBinaryExpr(BinaryExpr binaryExpr) {
    visit(binaryExpr.getLhs());
    out.append(" " + binaryExpr.getOp().getText() + " ");
    visit(binaryExpr.getRhs());
  }

  @Override
  public void visitParenExpr(ParenExpr parenExpr) {
    out.append("(");
    visit(parenExpr.getExpr());
    out.append(")");
  }

  @Override
  public void visitVariableIdentifierExpr(VariableIdentifierExpr variableIdentifierExpr) {
    out.append(variableIdentifierExpr.getName());
  }

  @Override
  public void visitUnaryExpr(UnaryExpr unaryExpr) {
    switch (unaryExpr.getOp()) {
      case PRE_INC:
      case PRE_DEC:
      case PLUS:
      case MINUS:
      case BNEG:
      case LNOT:
        out.append(unaryExpr.getOp().getText() + " ");
        break;
      case POST_DEC:
      case POST_INC:
        break;
      default:
        assert false : "Unknown unary operator " + unaryExpr.getOp();
    }
    visit(unaryExpr.getExpr());
    switch (unaryExpr.getOp()) {
      case POST_DEC:
      case POST_INC:
        out.append(" " + unaryExpr.getOp().getText());
        break;
      case PRE_INC:
      case PRE_DEC:
      case PLUS:
      case MINUS:
      case BNEG:
      case LNOT:
        break;
      default:
        assert false : "Unknown unary operator " + unaryExpr.getOp();
    }
  }

  @Override
  public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
    visit(memberLookupExpr.getStructure());
    out.append("." + memberLookupExpr.getMember());
  }

  @Override
  public void visitIntConstantExpr(IntConstantExpr intConstantExpr) {
    out.append(intConstantExpr.getValue());
  }

  @Override
  public void visitUIntConstantExpr(UIntConstantExpr intConstantExpr) {
    out.append(intConstantExpr.getValue());
  }

  @Override
  public void visitFloatConstantExpr(FloatConstantExpr floatConstantExpr) {
    out.append(floatConstantExpr.getValue());
  }

  @Override
  public void visitBoolConstantExpr(BoolConstantExpr boolConstantExpr) {
    out.append(boolConstantExpr.toString());
  }

  @Override
  public void visitBreakStmt(BreakStmt breakStmt) {
    out.append(indent() + "break");
    out.append(";" + newLine());
  }

  @Override
  public void visitContinueStmt(ContinueStmt continueStmt) {
    out.append(indent() + "continue");
    out.append(";" + newLine());
  }

  @Override
  public void visitDiscardStmt(DiscardStmt discardStmt) {
    out.append(indent() + "discard");
    out.append(";" + newLine());
  }

  @Override
  public void visitReturnStmt(ReturnStmt returnStmt) {
    out.append(indent() + "return");
    if (returnStmt.hasExpr()) {
      out.append(" ");
      visit(returnStmt.getExpr());
    }
    out.append(";" + newLine());
  }

  @Override
  public void visitExprStmt(ExprStmt exprStmt) {
    out.append(indent());
    visit(exprStmt.getExpr());
    out.append(";" + newLine());
  }

  @Override
  public void visitFunctionCallExpr(FunctionCallExpr functionCallExpr) {
    out.append(functionCallExpr.getCallee() + "(");
    boolean first = true;
    for (Expr e : functionCallExpr.getArgs()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      visit(e);
    }
    out.append(")");
  }

  @Override
  public void visitTypeConstructorExpr(TypeConstructorExpr typeConstructorExpr) {
    out.append(typeConstructorExpr.getTypename() + "(");
    boolean first = true;
    for (Expr e : typeConstructorExpr.getArgs()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      visit(e);
    }
    out.append(")");
  }

  @Override
  public void visitQualifiedType(QualifiedType qualifiedType) {
    for (TypeQualifier q : qualifiedType.getQualifiers()) {
      out.append(q + " ");
    }
    visit(qualifiedType.getTargetType());
  }

  @Override
  public void visitBasicType(BasicType basicType) {
    out.append(basicType.toString());
  }

  @Override
  public void visitSamplerType(SamplerType samplerType) {
    out.append(samplerType.toString());
  }

  @Override
  public void visitImageType(ImageType imageType) {
    out.append(imageType.toString());
  }

  @Override
  public void visitVoidType(VoidType voidType) {
    out.append(voidType.toString());
  }

  @Override
  public void visitAtomicIntType(AtomicIntType atomicIntType) {
    out.append(atomicIntType.toString());
  }

  @Override
  public void visitNullStmt(NullStmt nullStmt) {
    out.append(indent() + ";" + newLine());
  }

  @Override
  public void visitWhileStmt(WhileStmt whileStmt) {
    out.append(indent() + "while(");
    visit(whileStmt.getCondition());
    out.append(")" + newLine());
    increaseIndent();
    visit(whileStmt.getBody());
    decreaseIndent();
  }

  @Override
  public void visitForStmt(ForStmt forStmt) {
    out.append(indent() + "for(" + newLine());
    out.append("    ");
    visit(forStmt.getInit());
    out.append("    " + indent());
    visit(forStmt.getCondition());
    out.append(";" + newLine());
    out.append("    " + indent());
    visit(forStmt.getIncrement());
    out.append(newLine());
    out.append(indent() + ")" + newLine());
    increaseIndent();
    visit(forStmt.getBody());
    decreaseIndent();
  }

  @Override
  public void visitDoStmt(DoStmt doStmt) {
    out.append(indent() + "do" + newLine());
    increaseIndent();
    visit(doStmt.getBody());
    decreaseIndent();
    out.append(indent() + "while(");
    visit(doStmt.getCondition());
    out.append(");" + newLine());
  }

  @Override
  public void visitTernaryExpr(TernaryExpr ternaryExpr) {
    visit(ternaryExpr.getTest());
    out.append(" ? ");
    visit(ternaryExpr.getThenExpr());
    out.append(" : ");
    visit(ternaryExpr.getElseExpr());
  }

  @Override
  public void visitArrayIndexExpr(ArrayIndexExpr arrayIndexExpr) {
    visit(arrayIndexExpr.getArray());
    out.append("[");
    visit(arrayIndexExpr.getIndex());
    out.append("]");
  }

  @Override
  public void visitStructType(StructType structType) {
    out.append(structType.getName());
  }

  @Override
  public void visitArrayType(ArrayType arrayType) {
    // Do not generate array info, as this has to come after the associated variable name
    visit(arrayType.getBaseType());
  }

  @Override
  public void visitArrayConstructorExpr(ArrayConstructorExpr arrayConstructorExpr) {
    visit(arrayConstructorExpr.getArrayType());
    out.append("[" + arrayConstructorExpr.getArrayType().getArrayInfo().getSize() + "](");
    boolean first = true;
    for (Expr e : arrayConstructorExpr.getArgs()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      visit(e);
    }
    out.append(")");
  }

  @Override
  public void visitStructDeclaration(StructDeclaration structDeclaration) {
    out.append("struct " + structDeclaration.getType().getName() + " {" + newLine());
    increaseIndent();
    for (String name : structDeclaration.getType().getFieldNames()) {
      out.append(indent());
      visit(structDeclaration.getType().getFieldType(name));
      out.append(" " + name);
      processArrayInfo(structDeclaration.getType().getFieldType(name));
      out.append(";" + newLine());
    }
    decreaseIndent();
    out.append("};" + newLine());
  }

  private void processArrayInfo(Type type) {
    if (!(type.getWithoutQualifiers() instanceof ArrayType)) {
      return;
    }
    ArrayType arrayType = (ArrayType) type.getWithoutQualifiers();
    while (true) {
      out.append("[" + arrayType.getArrayInfo().getSize() + "]");
      if (!(arrayType.getBaseType().getWithoutQualifiers() instanceof ArrayType)) {
        break;
      }
      arrayType = (ArrayType) arrayType.getBaseType().getWithoutQualifiers();
    }
  }

  @Override
  public void visitSwitchStmt(SwitchStmt switchStmt) {
    out.append(indent() + "switch(");
    visit(switchStmt.getExpr());
    out.append(")" + newLine());
    increaseIndent();
    visitBlockStmt(switchStmt.getBody());
    decreaseIndent();
  }

  @Override
  public void visitDefaultCaseLabel(DefaultCaseLabel defaultCaseLabel) {
    out.append(indent());
    out.append("default:" + newLine());
  }

  @Override
  public void visitExprCaseLabel(ExprCaseLabel exprCaseLabel) {
    out.append(indent());
    out.append("case ");
    visit(exprCaseLabel.getExpr());
    out.append(":" + newLine());
  }

  @Override
  public void visitInterfaceBlock(InterfaceBlock interfaceBlock) {
    out.append(indent());
    if (interfaceBlock.hasLayoutQualifier()) {
      out.append("layout(" + interfaceBlock.getLayoutQualifier().getContent() + ") ");
    }
    out.append(interfaceBlock.getInterfaceQualifier() + " "
        + interfaceBlock.getStructName() + " {" + newLine());

    increaseIndent();

    for (String memberName : interfaceBlock.getMemberNames()) {
      out.append(indent());
      visit(interfaceBlock.getMemberType(memberName));
      out.append(" " + memberName);
      processArrayInfo(interfaceBlock.getMemberType(memberName));
      out.append(";" + newLine());
    }

    decreaseIndent();

    out.append("} " + interfaceBlock.getIdentifierName() + ";" + newLine());
  }

  private void decreaseIndent() {
    indentationCount -= indentationWidth;
  }

  private void increaseIndent() {
    indentationCount += indentationWidth;
  }

  @Override
  public String toString() {
    return out.toString();
  }

  @Override
  protected <T extends IAstNode> void visitChildFromParent(Consumer<T> visitorMethod, T child,
      IAstNode parent) {
    super.visitChildFromParent(visitorMethod, child, parent);
    if (parent instanceof TranslationUnit && child instanceof VariablesDeclaration) {
      out.append(";" + newLine() + newLine());
    }
  }

  /**
   * Used by test classes to mimic default indentation.
   * @param level The number of times to indent.
   * @return An appropriate string of blanks.
   */
  public static String defaultIndent(int level) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < level; i++) {
      for (int j = 0; j < PrettyPrinterVisitor.DEFAULT_INDENTATION_WIDTH; j++) {
        result.append(" ");
      }
    }
    return result.toString();
  }

}
