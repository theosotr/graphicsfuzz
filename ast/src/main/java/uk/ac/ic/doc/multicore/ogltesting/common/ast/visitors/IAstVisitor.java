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

package uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors;

import uk.ac.ic.doc.multicore.ogltesting.common.ast.IAstNode;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ArrayInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ArrayInitializer;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionDefinition;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionPrototype;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.InterfaceBlock;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ParameterDecl;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.PrecisionDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ScalarInitializer;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.StructDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariableDeclInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariablesDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayIndexExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BinaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BoolConstantExpr;
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
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.VoidType;

public interface IAstVisitor {

  void visit(IAstNode node);

  void visitFunctionDefinition(FunctionDefinition functionDefinition);

  void visitTranslationUnit(TranslationUnit translationUnit);

  void visitVersionStatement(VersionStatement versionStatement);

  void visitBlockStmt(BlockStmt stmt);

  void visitFunctionPrototype(FunctionPrototype functionPrototype);

  void visitIfStmt(IfStmt ifStmt);

  void visitDeclarationStmt(DeclarationStmt declarationStmt);

  void visitVariablesDeclaration(VariablesDeclaration variablesDeclaration);

  void visitPrecisionDeclaration(PrecisionDeclaration precisionDeclaration);

  void visitArrayInitializer(ArrayInitializer arrayInitializer);

  void visitScalarInitializer(ScalarInitializer scalarInitializer);

  void visitBinaryExpr(BinaryExpr binaryExpr);

  void visitParenExpr(ParenExpr parenExpr);

  void visitVariableIdentifierExpr(VariableIdentifierExpr variableIdentifierExpr);

  void visitUnaryExpr(UnaryExpr unaryExpr);

  void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr);

  void visitDiscardStmt(DiscardStmt discardStmt);

  void visitBreakStmt(BreakStmt breakStmt);

  void visitContinueStmt(ContinueStmt continueStmt);

  void visitReturnStmt(ReturnStmt returnStmt);

  void visitFunctionCallExpr(FunctionCallExpr functionCallExpr);

  void visitExprStmt(ExprStmt exprStmt);

  void visitTypeConstructorExpr(TypeConstructorExpr typeConstructorExpr);

  void visitBasicType(BasicType basicType);

  void visitSamplerType(SamplerType samplerType);

  void visitImageType(ImageType imageType);

  void visitVoidType(VoidType voidType);

  void visitAtomicIntType(AtomicIntType atomicIntType);

  void visitQualifiedType(QualifiedType qualifiedType);

  void visitForStmt(ForStmt forStmt);

  void visitNullStmt(NullStmt nullStmt);

  void visitDoStmt(DoStmt doStmt);

  void visitWhileStmt(WhileStmt whileStmt);

  void visitTernaryExpr(TernaryExpr ternaryExpr);

  void visitParameterDecl(ParameterDecl parameterDecl);

  void visitArrayInfo(ArrayInfo arrayInfo);

  void visitVariableDeclInfo(VariableDeclInfo variableDeclInfo);

  void visitArrayIndexExpr(ArrayIndexExpr arrayIndexExpr);

  void visitIntConstantExpr(IntConstantExpr intConstantExpr);

  void visitUIntConstantExpr(UIntConstantExpr uintConstantExpr);

  void visitFloatConstantExpr(FloatConstantExpr floatConstantExpr);

  void visitBoolConstantExpr(BoolConstantExpr boolConstantExpr);

  void visitStructType(StructType structType);

  void visitStructDeclaration(StructDeclaration structDeclaration);

  void visitArrayConstructorExpr(ArrayConstructorExpr arrayConstructorExpr);

  void visitArrayType(ArrayType arrayType);

  void visitSwitchStmt(SwitchStmt switchStmt);

  void visitDefaultCaseLabel(DefaultCaseLabel defaultCaseLabel);

  void visitExprCaseLabel(ExprCaseLabel exprCaseLabel);

  void visitInterfaceBlock(InterfaceBlock interfaceBlock);

}
