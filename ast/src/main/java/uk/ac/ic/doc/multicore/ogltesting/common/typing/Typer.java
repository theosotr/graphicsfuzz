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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.IAstNode;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionDefinition;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionPrototype;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ParameterDecl;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.StructDeclaration;
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
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.ArrayType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.BasicType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.QualifiedType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.StructType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.util.GlslVersion;
import uk.ac.ic.doc.multicore.ogltesting.common.util.OpenGlConstants;

public class Typer extends ScopeTreeBuilder {

  private Map<Expr, Type> types;

  private Map<String, Set<FunctionPrototype>> userDefinedFunctions;

  private Map<String, StructType> structTypeMap;

  private GlslVersion glslVersion;

  public Map<String, Set<FunctionPrototype>> getUserDefinedFunctions() {
    return userDefinedFunctions;
  }

  public Typer(IAstNode node, GlslVersion glslVersion) {
    this.types = new HashMap<>();
    this.userDefinedFunctions = new HashMap<>();
    this.structTypeMap = new HashMap<>();
    this.glslVersion = glslVersion;
    visit(node);
  }

  @Override
  public void visitParenExpr(ParenExpr parenExpr) {
    super.visitParenExpr(parenExpr);
    Type type = lookupType(parenExpr.getExpr());
    if (type != null) {
      types.put(parenExpr, type);
    }
  }

  @Override
  public void visitFunctionPrototype(FunctionPrototype functionPrototype) {
    super.visitFunctionPrototype(functionPrototype);
    String name = functionPrototype.getName();
    if (!userDefinedFunctions.containsKey(name)) {
      userDefinedFunctions.put(name, new HashSet<>());
    }
    userDefinedFunctions.get(name).add(functionPrototype);
  }

  @Override
  public void visitFunctionDefinition(FunctionDefinition functionDefinition) {
    super.visitFunctionDefinition(functionDefinition);
    String name = functionDefinition.getPrototype().getName();
    if (!userDefinedFunctions.containsKey(name)) {
      userDefinedFunctions.put(name, new HashSet<>());
    }
    userDefinedFunctions.get(name).add(functionDefinition.getPrototype());
  }

  @Override
  public void visitFunctionCallExpr(FunctionCallExpr functionCallExpr) {
    super.visitFunctionCallExpr(functionCallExpr);

    // TODO: Just doing builtins for now

    List<FunctionPrototype> candidateBuiltins = TyperHelper.getBuiltins(glslVersion)
        .get(functionCallExpr.getCallee());
    if (candidateBuiltins != null) {
      for (FunctionPrototype prototype : candidateBuiltins) {
        if (prototypeMatches(prototype, functionCallExpr)) {
          types.put(functionCallExpr, prototype.getReturnType());
        }
      }
    }

    Set<FunctionPrototype> candidateUserDefined =
        userDefinedFunctions.get(functionCallExpr.getCallee());
    if (candidateUserDefined != null) {
      for (FunctionPrototype prototype : candidateUserDefined) {
        if (prototypeMatches(prototype, functionCallExpr)) {
          types.put(functionCallExpr, prototype.getReturnType());
        }
      }
    }

  }

  /**
   * Determines whether a given function prototype might correspond to the function being invoked
   * by a function call expression.
   *
   * <p>Ignores type qualifiers.</p>
   *
   * <p>Behaves in an approximate manner when type information is
   * incomplete.</p>
   *
   * @param prototype Function prototype to be checked
   * @param functionCallExpr Function call expression to be checked
   * @return True if there is a possible match
   */
  public boolean prototypeMatches(FunctionPrototype prototype, FunctionCallExpr functionCallExpr) {
    if (prototype.getNumParameters() != functionCallExpr.getNumArgs()) {
      return false;
    }
    for (int i = 0; i < prototype.getNumParameters(); i++) {
      Type argType = lookupType(functionCallExpr.getArg(i));
      if (argType == null) {
        return false;
      }
      // Not yet worked out how to deal with array info
      assert prototype.getParameters().get(i).getArrayInfo() == null;
      if (!argType.getWithoutQualifiers()
          .equals(prototype.getParameters().get(i).getType().getWithoutQualifiers())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void visitVariableIdentifierExpr(VariableIdentifierExpr variableIdentifierExpr) {
    super.visitVariableIdentifierExpr(variableIdentifierExpr);
    Type type = currentScope.lookupType(variableIdentifierExpr.getName());
    if (type != null) {
      types.put(variableIdentifierExpr, type);
    } else if (variableIdentifierExpr.getName().equals(OpenGlConstants.GL_FRAG_COORD)) {
      types.put(variableIdentifierExpr, BasicType.VEC4);
    }
  }

  @Override
  public void visitTypeConstructorExpr(TypeConstructorExpr typeConstructorExpr) {
    super.visitTypeConstructorExpr(typeConstructorExpr);
    switch (typeConstructorExpr.getTypename()) {
      case "float":
        types.put(typeConstructorExpr, BasicType.FLOAT);
        return;
      case "int":
        types.put(typeConstructorExpr, BasicType.INT);
        return;
      case "uint":
        types.put(typeConstructorExpr, BasicType.UINT);
        return;
      case "bool":
        types.put(typeConstructorExpr, BasicType.BOOL);
        return;
      case "vec2":
        types.put(typeConstructorExpr, BasicType.VEC2);
        return;
      case "vec3":
        types.put(typeConstructorExpr, BasicType.VEC3);
        return;
      case "vec4":
        types.put(typeConstructorExpr, BasicType.VEC4);
        return;
      case "ivec2":
        types.put(typeConstructorExpr, BasicType.IVEC2);
        return;
      case "ivec3":
        types.put(typeConstructorExpr, BasicType.IVEC3);
        return;
      case "ivec4":
        types.put(typeConstructorExpr, BasicType.IVEC4);
        return;
      case "uvec2":
        types.put(typeConstructorExpr, BasicType.UVEC2);
        return;
      case "uvec3":
        types.put(typeConstructorExpr, BasicType.UVEC3);
        return;
      case "uvec4":
        types.put(typeConstructorExpr, BasicType.UVEC4);
        return;
      case "bvec2":
        types.put(typeConstructorExpr, BasicType.BVEC2);
        return;
      case "bvec3":
        types.put(typeConstructorExpr, BasicType.BVEC3);
        return;
      case "bvec4":
        types.put(typeConstructorExpr, BasicType.BVEC4);
        return;
      case "mat2x2":
      case "mat2":
        types.put(typeConstructorExpr, BasicType.MAT2X2);
        return;
      case "mat2x3":
        types.put(typeConstructorExpr, BasicType.MAT2X3);
        return;
      case "mat2x4":
        types.put(typeConstructorExpr, BasicType.MAT2X4);
        return;
      case "mat3x2":
        types.put(typeConstructorExpr, BasicType.MAT3X2);
        return;
      case "mat3x3":
      case "mat3":
        types.put(typeConstructorExpr, BasicType.MAT3X3);
        return;
      case "mat3x4":
        types.put(typeConstructorExpr, BasicType.MAT3X4);
        return;
      case "mat4x2":
        types.put(typeConstructorExpr, BasicType.MAT4X2);
        return;
      case "mat4x3":
        types.put(typeConstructorExpr, BasicType.MAT4X3);
        return;
      case "mat4x4":
      case "mat4":
        types.put(typeConstructorExpr, BasicType.MAT4X4);
        return;
      default:
        if (structTypeMap.containsKey(typeConstructorExpr.getTypename())) {
          types.put(typeConstructorExpr, structTypeMap.get(typeConstructorExpr.getTypename()));
          return;
        }
        return; // We cannot type the constructor.
    }
  }

  @Override
  public void visitUnaryExpr(UnaryExpr unaryExpr) {
    super.visitUnaryExpr(unaryExpr);

    // TODO: need to check, but as a first approximation a unary always returns the same type as
    // its argument

    Type argType = types.get(unaryExpr.getExpr());
    if (argType != null) {
      types.put(unaryExpr, argType);
    }
  }

  @Override
  public void visitTernaryExpr(TernaryExpr ternaryExpr) {
    super.visitTernaryExpr(ternaryExpr);
    Type thenType = types.get(ternaryExpr.getThenExpr());
    if (thenType != null) {
      types.put(ternaryExpr, thenType);
    } else {
      Type elseType = types.get(ternaryExpr.getElseExpr());
      if (elseType != null) {
        types.put(ternaryExpr, elseType);
      }
    }
  }

  @Override
  public void visitBinaryExpr(BinaryExpr binaryExpr) {
    super.visitBinaryExpr(binaryExpr);
    Type lhsType = types.get(binaryExpr.getLhs());
    Type rhsType = types.get(binaryExpr.getRhs());
    if (lhsType instanceof QualifiedType) {
      lhsType = ((QualifiedType) lhsType).getTargetType();
    }
    if (rhsType instanceof QualifiedType) {
      rhsType = ((QualifiedType) rhsType).getTargetType();
    }
    switch (binaryExpr.getOp()) {
      case MUL: {
        Type resolvedType = TyperHelper.resolveTypeOfMul(lhsType, rhsType);
        if (resolvedType != null) {
          types.put(binaryExpr, resolvedType);
        }
        return;
      }
      case ADD:
      case SUB:
      case DIV:
      case SHL:
      case SHR:
      case MOD:
      case BAND:
      case BOR:
      case BXOR: {
        Type resolvedType = TyperHelper.resolveTypeOfCommonBinary(lhsType, rhsType);
        if (resolvedType != null) {
          types.put(binaryExpr, resolvedType);
        }
        return;
      }
      case EQ:
      case GE:
      case GT:
      case LAND:
      case LE:
      case LOR:
      case LT:
      case LXOR:
      case NE:
        types.put(binaryExpr, resolveBooleanResultType(lhsType, rhsType));
        return;
      case ADD_ASSIGN:
        break;
      case ASSIGN:
        break;
      case BAND_ASSIGN:
        break;
      case BOR_ASSIGN:
        break;
      case BXOR_ASSIGN:
        break;
      case COMMA:
        break;
      case DIV_ASSIGN:
        break;
      case MOD_ASSIGN:
        break;
      case MUL_ASSIGN:
        break;
      case SHL_ASSIGN:
        break;
      case SHR_ASSIGN:
        break;
      case SUB_ASSIGN:
        break;
      default:
        break;

    }
  }

  @Override
  public void visitBoolConstantExpr(BoolConstantExpr boolConstantExpr) {
    types.put(boolConstantExpr, BasicType.BOOL);
  }

  @Override
  public void visitIntConstantExpr(IntConstantExpr intConstantExpr) {
    types.put(intConstantExpr, BasicType.INT);
  }

  @Override
  public void visitUIntConstantExpr(UIntConstantExpr uintConstantExpr) {
    types.put(uintConstantExpr, BasicType.UINT);
  }

  @Override
  public void visitFloatConstantExpr(FloatConstantExpr floatConstantExpr) {
    types.put(floatConstantExpr, BasicType.FLOAT);
  }

  @Override
  public void visitMemberLookupExpr(MemberLookupExpr memberLookupExpr) {
    super.visitMemberLookupExpr(memberLookupExpr);
    Type structureType = lookupType(memberLookupExpr.getStructure());

    if (structureType == null) {
      // In due course we should extend the typer so that it can type everything.
      return;
    }

    // The structure type is either a builtin, like a vector, or an actual struct

    if (BasicType.allVectorTypes().contains(structureType.getWithoutQualifiers())) {
      BasicType vecType = (BasicType) structureType.getWithoutQualifiers();
      // It is a swizzle, so lookups must be xyzw, rgba or stpq
      for (int i = 0; i < memberLookupExpr.getMember().length(); i++) {
        assert ("xyzw" + "rgba" + "stpq")
            .contains(String.valueOf(memberLookupExpr.getMember().charAt(i)));
      }
      types.put(memberLookupExpr, BasicType
          .makeVectorType(vecType.getElementType(), memberLookupExpr.getMember().length()));
    }

    if (structureType.getWithoutQualifiers() instanceof StructType) {
      types.put(memberLookupExpr,
          ((StructType) structureType.getWithoutQualifiers())
              .getFieldType(memberLookupExpr.getMember()));
    }

    // take care of cases where you get the x coordinate of a vec2 variable and similar
    if (structureType.getWithoutQualifiers() instanceof BasicType) {
      BasicType vecType = (BasicType) structureType.getWithoutQualifiers();
      for (int i = 0; i < memberLookupExpr.getMember().length(); i++) {
        assert ("xyzw" + "rgba" + "stpq")
            .contains(String.valueOf(memberLookupExpr.getMember().charAt(i)));
      }

      final BasicType v = BasicType.makeVectorType(vecType.getElementType(),
          memberLookupExpr.getMember().length());
      types.put(memberLookupExpr, v);
    }
  }

  private Type resolveBooleanResultType(Type lhsType, Type rhsType) {
    return maybeComputeBooleanVectorType(lhsType)
        .orElse(maybeComputeBooleanVectorType(rhsType)
            .orElse(BasicType.BOOL));
  }

  private Optional<Type> maybeComputeBooleanVectorType(Type lhsType) {
    if (lhsType instanceof BasicType) {
      final int numElements = ((BasicType) lhsType).getNumElements();
      if (1 < numElements && numElements <= 4) {
        return Optional.of(BasicType.makeVectorType(BasicType.BOOL, numElements));
      }
    }
    return Optional.empty();
  }

  public Set<Expr> getTypedExpressions() {
    return Collections.unmodifiableSet(types.keySet());
  }

  public Type lookupType(Expr expr) {
    return types.get(expr);
  }

  public boolean hasType(Expr expr) {
    return lookupType(expr) != null;
  }

  public Set<FunctionPrototype> getPrototypes(String name) {
    Set<FunctionPrototype> result = new HashSet<>();
    if (userDefinedFunctions.containsKey(name)) {
      result.addAll(userDefinedFunctions.get(name));
    }
    if (TyperHelper.getBuiltins(glslVersion).containsKey(name)) {
      result.addAll(TyperHelper.getBuiltins(glslVersion).get(name));
    }
    return result;
  }

  @Override
  public void visitStructDeclaration(StructDeclaration structDeclaration) {
    super.visitStructDeclaration(structDeclaration);
    final StructType structType = structDeclaration.getType();
    if (structTypeMap.containsKey(structType.getName())) {
      assert structTypeMap.get(structType.getName()) == structType
          : "We should not be duplicating struct types";
    }
    structTypeMap.put(structType.getName(), structType);
  }

  @Override
  public void visitArrayIndexExpr(ArrayIndexExpr arrayIndexExpr) {
    super.visitArrayIndexExpr(arrayIndexExpr);
    Type arrayType = lookupType(arrayIndexExpr.getArray());
    if (arrayType == null) {
      return;
    }
    arrayType = arrayType.getWithoutQualifiers();
    Type elementType;
    if (BasicType.allVectorTypes().contains(arrayType)) {
      elementType = ((BasicType) arrayType).getElementType();
    } else if (BasicType.allMatrixTypes().contains(arrayType)) {
      elementType = ((BasicType) arrayType).getColumnType();
    } else {
      assert arrayType instanceof ArrayType;
      elementType = ((ArrayType) arrayType).getBaseType();
    }
    types.put(arrayIndexExpr, elementType);
  }

  public static void main(String[] args) {

    try {
      if (args.length != 1) {
        System.err
            .println("Usage: uk.ac.ic.doc.multicore.ogltesting.common.typing.Typer <GLSL version>");
        System.err.println(" e.g.: uk.ac.ic.doc.multicore.ogltesting.common.typing.Typer 450");
        System.exit(1);
      }

      // This generates a test function for each builtin
      GlslVersion glslVersion = new GlslVersion(args[0], false);
      System.out.println("#version " + glslVersion);

      System.out.println("precision mediump float;");

      int counter = 0;
      for (String name : TyperHelper.getBuiltins(glslVersion).keySet()) {
        for (FunctionPrototype fp : TyperHelper.getBuiltins(glslVersion).get(name)) {
          counter++;
          System.out.print(fp.getReturnType() + " test" + counter + "_" + fp.getName() + "(");
          boolean first = true;
          for (ParameterDecl decl : fp.getParameters()) {
            if (!first) {
              System.out.print(", ");
            }
            first = false;
            System.out.print(decl.getType() + " " + decl.getName());
          }

          System.out.println(") {");
          System.out.print("  return " + fp.getName() + "(");
          first = true;
          for (ParameterDecl decl : fp.getParameters()) {
            if (!first) {
              System.out.print(", ");
            }
            first = false;
            System.out.print(decl.getName());
          }
          System.out.println(");");

          System.out.println("}\n");
        }
      }
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      System.exit(1);
    }
  }

}
