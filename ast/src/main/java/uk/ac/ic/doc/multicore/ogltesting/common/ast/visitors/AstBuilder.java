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

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import jdk.nashorn.internal.ir.Block;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.TranslationUnit;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ArrayInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.Declaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionDefinition;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.FunctionPrototype;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.Initializer;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ParameterDecl;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.PrecisionDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.ScalarInitializer;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.StructDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariableDeclInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariablesDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayConstructorExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.ArrayIndexExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.BinOp;
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
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.UnOp;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.UnaryExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.expr.VariableIdentifierExpr;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.BlockStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.BreakStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.CaseLabel;
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
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.BuiltinType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.ImageType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.QualifiedType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.SamplerType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.StructType;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.TypeQualifier;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.VoidType;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLBaseVisitor;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Additive_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.And_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Any_identifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Array_specifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Assignment_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Assignment_operatorContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Auxiliary_storage_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Builtin_type_specifier_nonarrayContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Case_labelContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Case_label_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Case_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Case_statement_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Compound_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Compound_statement_no_new_scopeContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.ConditionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Conditional_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.DeclarationContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Declaration_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Equality_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Exclusive_or_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.ExpressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Expression_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Extension_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Extension_statement_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.External_declaration_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.For_init_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.For_rest_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Fully_specified_typeContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_call_genericContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_call_headerContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_call_header_no_parametersContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_call_header_with_parametersContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_definitionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_headerContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_header_with_parametersContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_identifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Function_prototypeContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Inclusive_or_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Init_declarator_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.InitializerContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Interpolation_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Iteration_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Jump_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Layout_defaultsContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Logical_and_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Logical_or_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Logical_xor_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Multiplicative_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Parameter_declarationContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Parameter_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Postfix_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Pragma_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Precision_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Primary_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Relational_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Selection_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Shift_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Single_declarationContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.StatementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Statement_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Statement_no_new_scopeContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Storage_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Struct_declaration_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Struct_declarator_listContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Struct_specifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Switch_bodyContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Switch_statementContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Translation_unitContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Type_qualifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Type_specifierContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Unary_expressionContext;
import uk.ac.ic.doc.multicore.ogltesting.parser.GLSLParser.Unary_operatorContext;

public class AstBuilder extends GLSLBaseVisitor<Object> {

  private List<Declaration> topLevelDeclarations;
  private List<StructType> structs;
  private int anonymousStructCounter;

  private AstBuilder() {
    this.structs = new ArrayList<>();
    this.anonymousStructCounter = 0;
  }

  void addTopLevelDeclaration(Declaration decl) {
    if (decl instanceof VariablesDeclaration) {
      if (((VariablesDeclaration) decl).getDeclInfos().isEmpty()) {
        return;
      }
    }
    topLevelDeclarations.add(decl);
  }

  public static TranslationUnit getTranslationUnit(Translation_unitContext ctx) {
    return new AstBuilder().visitTranslation_unit(ctx);
  }

  private String anonymousStructName() {
    return "_anon_struct_" + anonymousStructCounter++;
  }

  @Override
  public TranslationUnit visitTranslation_unit(Translation_unitContext ctx) {
    topLevelDeclarations = new ArrayList<>();
    visitExtension_statement_list(ctx.extension_statement_list());
    visitExternal_declaration_list(ctx.external_declaration_list());
    return new TranslationUnit(
        new VersionStatement(ctx.version_statement().getText()),
        topLevelDeclarations);
  }

  @Override
  public Void visitExternal_declaration_list(External_declaration_listContext ctx) {
    // These lists can get very large, so that recursion leads to stack overflow.
    // Hence we compute a list of all list prefixes and then go through it iteratively.
    Deque<External_declaration_listContext> declarations = new LinkedList<>();
    {
      External_declaration_listContext temp = ctx;
      while (true) {
        declarations.addFirst(temp);
        if (temp.single != null) {
          break;
        }
        temp = temp.prefix;
      }
    }
    for (External_declaration_listContext decl : declarations) {
      if (decl.single != null) {
        assert decl.prefix == null;
        assert decl.lastDecl == null;
        assert decl.lastExtension == null;
        addTopLevelDeclaration((Declaration) visitExternal_declaration(decl.single));
      } else {
        assert decl.prefix != null;
        if (decl.lastDecl != null) {
          assert decl.lastExtension == null;
          addTopLevelDeclaration((Declaration) visitExternal_declaration(decl.lastDecl));
        } else {
          assert decl.lastExtension != null;
          addTopLevelDeclaration(visitExtension_statement(decl.lastExtension));
        }
      }
    }
    return null;
  }

  @Override
  public Void visitExtension_statement_list(Extension_statement_listContext ctx) {
    if (ctx.extension_statement() != null) {
      visitExtension_statement_list(ctx.extension_statement_list());
      addTopLevelDeclaration(visitExtension_statement(ctx.extension_statement()));
    }
    return null;
  }

  @Override
  public Declaration visitDeclaration(DeclarationContext ctx) {
    if (ctx.init_declarator_list() != null) {
      return visitInit_declarator_list(ctx.init_declarator_list());
    }
    if (ctx.precision_qualifier() != null) {
      return new PrecisionDeclaration(getOriginalSourceText(ctx));
    }
    if (ctx.function_prototype() != null) {
      return visitFunction_prototype(ctx.function_prototype());
    }
    throw new RuntimeException("Unknown declaration at line " + ctx.start.getLine() + ": "
        + getOriginalSourceText(ctx));
  }

  @Override
  public FunctionPrototype visitFunction_prototype(Function_prototypeContext ctx) {
    Function_headerContext header = ctx.function_declarator().function_header();
    if (header != null) {
      Type returnType = visitFully_specified_type(header.fully_specified_type());
      String name = header.variable_identifier().getText();
      return new FunctionPrototype(name, returnType, new ArrayList<>());
    }
    Function_header_with_parametersContext fhp = ctx.function_declarator()
        .function_header_with_parameters();
    List<ParameterDecl> parameters = new LinkedList<>();
    while (fhp.function_header_with_parameters() != null) {
      parameters.add(0, visitParameter_declaration(fhp.parameter_declaration()));
      fhp = fhp.function_header_with_parameters();
    }
    parameters.add(0, visitParameter_declaration(fhp.parameter_declaration()));
    header = fhp.function_header();
    Type returnType = visitFully_specified_type(header.fully_specified_type());
    String name = header.variable_identifier().getText();
    return new FunctionPrototype(name, returnType, parameters);
  }

  @Override
  public Type visitFully_specified_type(Fully_specified_typeContext ctx) {
    return getType(ctx.type_qualifier(), ctx.type_specifier());
  }

  private Type getType(Type_qualifierContext qualifiersCtx, Type_specifierContext specifierCtx) {
    return getType(specifierCtx, getQualifiers(qualifiersCtx));
  }

  private Type getType(Type_specifierContext typeSpecifier, List<TypeQualifier> qualifiers) {
    Type result = null;

    if (typeSpecifier.type_specifier_nonarray().builtin_type_specifier_nonarray() != null) {
      result = new QualifiedType(
          getBuiltinType(typeSpecifier.type_specifier_nonarray().builtin_type_specifier_nonarray()),
          qualifiers);
    } else if (typeSpecifier.type_specifier_nonarray().struct_specifier() != null) {
      result = new QualifiedType(
          recordAndReturnStructType(typeSpecifier.type_specifier_nonarray().struct_specifier()),
          qualifiers);
    } else {
      assert typeSpecifier.type_specifier_nonarray().IDENTIFIER() != null;
      String name = typeSpecifier.type_specifier_nonarray().IDENTIFIER().getText();
      for (StructType s : structs) {
        if (s.getName().equals(name)) {
          result = new QualifiedType(s, qualifiers);
          break;
        }
      }
      if (result == null) {
        throw new RuntimeException("Could not find struct for type name: " + typeSpecifier
            .type_specifier_nonarray().IDENTIFIER().getText() + " at line " + typeSpecifier
            .type_specifier_nonarray().start.getLine());
      }
    }

    if (typeSpecifier.array_specifier() != null) {
      throw new RuntimeException();
    }
    return result;
  }

  private StructType recordAndReturnStructType(Struct_specifierContext ctx) {
    String name = ctx.any_identifier() == null ? null : ctx.any_identifier().getText();
    if (name == null) {
      name = anonymousStructName();
    }
    assert !structs.stream().map(item -> item.getName()).collect(Collectors.toSet()).contains(name);
    StructType result = makeStructType(name, ctx.struct_declaration_list());
    addTopLevelDeclaration(new StructDeclaration(result));
    structs.add(result);
    return result;
  }

  private StructType makeStructType(String name,
      Struct_declaration_listContext structDeclarationListContext) {
    LinkedList<String> fieldNames = new LinkedList<>();
    LinkedList<Type> fieldTypes = new LinkedList<>();
    for (Struct_declaration_listContext ctx = structDeclarationListContext; ctx != null;
        ctx = ctx.struct_declaration_list()) {
      Type baseType = visitFully_specified_type(ctx.struct_declaration().fully_specified_type());
      for (Struct_declarator_listContext declarators = ctx.struct_declaration()
          .struct_declarator_list();
          declarators != null;
          declarators = declarators.struct_declarator_list()) {
        fieldNames.addFirst(declarators.struct_declarator().any_identifier().getText());
        if (declarators.struct_declarator().array_specifier() == null) {
          fieldTypes.addFirst(baseType);
        } else {
          fieldTypes.addFirst(new ArrayType(baseType,
              getArrayInfo(declarators.struct_declarator().array_specifier())));
        }
      }

    }
    return new StructType(name, fieldNames, fieldTypes);
  }

  private ArrayInfo getArrayInfo(Array_specifierContext arraySpecifierContext) {
    if (arraySpecifierContext.array_specifier() != null) {
      throw new RuntimeException("Not yet supporting multi-dimmensional arrays");
    }
    if (arraySpecifierContext.constant_expression() == null) {
      throw new RuntimeException("Not yet supporting arrays with unspecified length");
    }
    Expr expr = (Expr) visit(arraySpecifierContext.constant_expression());
    if (expr instanceof IntConstantExpr) {
      return new ArrayInfo(Integer.parseInt(((IntConstantExpr) expr).getValue()));
    }
    throw new RuntimeException("Unable to construct array info for array with size "
        + expr.getText());
  }

  private BuiltinType getBuiltinType(Builtin_type_specifier_nonarrayContext ctx) {
    if (ctx.VOID_TOK() != null) {
      return VoidType.VOID;
    } else if (ctx.FLOAT_TOK() != null) {
      return BasicType.FLOAT;
    } else if (ctx.INT_TOK() != null) {
      return BasicType.INT;
    } else if (ctx.UINT_TOK() != null) {
      return BasicType.UINT;
    } else if (ctx.BOOL_TOK() != null) {
      return BasicType.BOOL;
    } else if (ctx.VEC2() != null) {
      return BasicType.VEC2;
    } else if (ctx.VEC3() != null) {
      return BasicType.VEC3;
    } else if (ctx.VEC4() != null) {
      return BasicType.VEC4;
    } else if (ctx.BVEC2() != null) {
      return BasicType.BVEC2;
    } else if (ctx.BVEC3() != null) {
      return BasicType.BVEC3;
    } else if (ctx.BVEC4() != null) {
      return BasicType.BVEC4;
    } else if (ctx.IVEC2() != null) {
      return BasicType.IVEC2;
    } else if (ctx.IVEC3() != null) {
      return BasicType.IVEC3;
    } else if (ctx.IVEC4() != null) {
      return BasicType.IVEC4;
    } else if (ctx.UVEC2() != null) {
      return BasicType.UVEC2;
    } else if (ctx.UVEC3() != null) {
      return BasicType.UVEC3;
    } else if (ctx.UVEC4() != null) {
      return BasicType.UVEC4;
    } else if (ctx.MAT2X2() != null) {
      return BasicType.MAT2X2;
    } else if (ctx.MAT2X3() != null) {
      return BasicType.MAT2X3;
    } else if (ctx.MAT2X4() != null) {
      return BasicType.MAT2X4;
    } else if (ctx.MAT3X2() != null) {
      return BasicType.MAT3X2;
    } else if (ctx.MAT3X3() != null) {
      return BasicType.MAT3X3;
    } else if (ctx.MAT3X4() != null) {
      return BasicType.MAT3X4;
    } else if (ctx.MAT4X2() != null) {
      return BasicType.MAT4X2;
    } else if (ctx.MAT4X3() != null) {
      return BasicType.MAT4X3;
    } else if (ctx.MAT4X4() != null) {
      return BasicType.MAT4X4;
    } else if (ctx.SAMPLER1D() != null) {
      return SamplerType.SAMPLER1D;
    } else if (ctx.SAMPLER2D() != null) {
      return SamplerType.SAMPLER2D;
    } else if (ctx.SAMPLER2DRECT() != null) {
      return SamplerType.SAMPLER2DRECT;
    } else if (ctx.SAMPLER3D() != null) {
      return SamplerType.SAMPLER3D;
    } else if (ctx.SAMPLERCUBE() != null) {
      return SamplerType.SAMPLERCUBE;
    } else if (ctx.SAMPLEREXTERNALOES() != null) {
      return SamplerType.SAMPLEREXTERNALOES;
    } else if (ctx.SAMPLER1DSHADOW() != null) {
      return SamplerType.SAMPLER1DSHADOW;
    } else if (ctx.SAMPLER2DSHADOW() != null) {
      return SamplerType.SAMPLER2DSHADOW;
    } else if (ctx.SAMPLER2DRECTSHADOW() != null) {
      return SamplerType.SAMPLER2DRECTSHADOW;
    } else if (ctx.SAMPLERCUBESHADOW() != null) {
      return SamplerType.SAMPLERCUBESHADOW;
    } else if (ctx.SAMPLER1DARRAY() != null) {
      return SamplerType.SAMPLER1DARRAY;
    } else if (ctx.SAMPLER2DARRAY() != null) {
      return SamplerType.SAMPLER2DARRAY;
    } else if (ctx.SAMPLER1DARRAYSHADOW() != null) {
      return SamplerType.SAMPLER1DARRAYSHADOW;
    } else if (ctx.SAMPLER2DARRAYSHADOW() != null) {
      return SamplerType.SAMPLER2DARRAYSHADOW;
    } else if (ctx.SAMPLERBUFFER() != null) {
      return SamplerType.SAMPLERBUFFER;
    } else if (ctx.SAMPLERCUBEARRAY() != null) {
      return SamplerType.SAMPLERCUBEARRAY;
    } else if (ctx.SAMPLERCUBEARRAYSHADOW() != null) {
      return SamplerType.SAMPLERCUBEARRAYSHADOW;
    } else if (ctx.ISAMPLER1D() != null) {
      return SamplerType.ISAMPLER1D;
    } else if (ctx.ISAMPLER2D() != null) {
      return SamplerType.ISAMPLER2D;
    } else if (ctx.ISAMPLER2DRECT() != null) {
      return SamplerType.ISAMPLER2DRECT;
    } else if (ctx.ISAMPLER3D() != null) {
      return SamplerType.ISAMPLER3D;
    } else if (ctx.ISAMPLERCUBE() != null) {
      return SamplerType.ISAMPLERCUBE;
    } else if (ctx.ISAMPLER1DARRAY() != null) {
      return SamplerType.ISAMPLER1DARRAY;
    } else if (ctx.ISAMPLER2DARRAY() != null) {
      return SamplerType.ISAMPLER2DARRAY;
    } else if (ctx.ISAMPLERBUFFER() != null) {
      return SamplerType.ISAMPLERBUFFER;
    } else if (ctx.ISAMPLERCUBEARRAY() != null) {
      return SamplerType.ISAMPLERCUBEARRAY;
    } else if (ctx.USAMPLER1D() != null) {
      return SamplerType.USAMPLER1D;
    } else if (ctx.USAMPLER2D() != null) {
      return SamplerType.USAMPLER2D;
    } else if (ctx.USAMPLER2DRECT() != null) {
      return SamplerType.USAMPLER2DRECT;
    } else if (ctx.USAMPLER3D() != null) {
      return SamplerType.USAMPLER3D;
    } else if (ctx.USAMPLERCUBE() != null) {
      return SamplerType.USAMPLERCUBE;
    } else if (ctx.USAMPLER1DARRAY() != null) {
      return SamplerType.USAMPLER1DARRAY;
    } else if (ctx.USAMPLER2DARRAY() != null) {
      return SamplerType.USAMPLER2DARRAY;
    } else if (ctx.USAMPLERBUFFER() != null) {
      return SamplerType.USAMPLERBUFFER;
    } else if (ctx.USAMPLERCUBEARRAY() != null) {
      return SamplerType.USAMPLERCUBEARRAY;
    } else if (ctx.SAMPLER2DMS() != null) {
      return SamplerType.SAMPLER2DMS;
    } else if (ctx.ISAMPLER2DMS() != null) {
      return SamplerType.ISAMPLER2DMS;
    } else if (ctx.USAMPLER2DMS() != null) {
      return SamplerType.USAMPLER2DMS;
    } else if (ctx.SAMPLER2DMSARRAY() != null) {
      return SamplerType.SAMPLER2DMSARRAY;
    } else if (ctx.ISAMPLER2DMSARRAY() != null) {
      return SamplerType.ISAMPLER2DMSARRAY;
    } else if (ctx.USAMPLER2DMSARRAY() != null) {
      return SamplerType.USAMPLER2DMSARRAY;
    } else if (ctx.IMAGE1D() != null) {
      return ImageType.IMAGE1D;
    } else if (ctx.IMAGE2D() != null) {
      return ImageType.IMAGE2D;
    } else if (ctx.IMAGE3D() != null) {
      return ImageType.IMAGE3D;
    } else if (ctx.IMAGE2DRECT() != null) {
      return ImageType.IMAGE2DRECT;
    } else if (ctx.IMAGECUBE() != null) {
      return ImageType.IMAGECUBE;
    } else if (ctx.IMAGEBUFFER() != null) {
      return ImageType.IMAGEBUFFER;
    } else if (ctx.IMAGE1DARRAY() != null) {
      return ImageType.IMAGE1DARRAY;
    } else if (ctx.IMAGE2DARRAY() != null) {
      return ImageType.IMAGE2DARRAY;
    } else if (ctx.IMAGECUBEARRAY() != null) {
      return ImageType.IMAGECUBEARRAY;
    } else if (ctx.IMAGE2DMS() != null) {
      return ImageType.IMAGE2DMS;
    } else if (ctx.IMAGE2DMSARRAY() != null) {
      return ImageType.IMAGE2DMSARRAY;
    } else if (ctx.IIMAGE1D() != null) {
      return ImageType.IIMAGE1D;
    } else if (ctx.IIMAGE2D() != null) {
      return ImageType.IIMAGE2D;
    } else if (ctx.IIMAGE3D() != null) {
      return ImageType.IIMAGE3D;
    } else if (ctx.IIMAGE2DRECT() != null) {
      return ImageType.IIMAGE2DRECT;
    } else if (ctx.IIMAGECUBE() != null) {
      return ImageType.IIMAGECUBE;
    } else if (ctx.IIMAGEBUFFER() != null) {
      return ImageType.IIMAGEBUFFER;
    } else if (ctx.IIMAGE1DARRAY() != null) {
      return ImageType.IIMAGE1DARRAY;
    } else if (ctx.IIMAGE2DARRAY() != null) {
      return ImageType.IIMAGE2DARRAY;
    } else if (ctx.IIMAGECUBEARRAY() != null) {
      return ImageType.IIMAGECUBEARRAY;
    } else if (ctx.IIMAGE2DMS() != null) {
      return ImageType.IIMAGE2DMS;
    } else if (ctx.IIMAGE2DMSARRAY() != null) {
      return ImageType.IIMAGE2DMSARRAY;
    } else if (ctx.UIMAGE1D() != null) {
      return ImageType.UIMAGE1D;
    } else if (ctx.UIMAGE2D() != null) {
      return ImageType.UIMAGE2D;
    } else if (ctx.UIMAGE3D() != null) {
      return ImageType.UIMAGE3D;
    } else if (ctx.UIMAGE2DRECT() != null) {
      return ImageType.UIMAGE2DRECT;
    } else if (ctx.UIMAGECUBE() != null) {
      return ImageType.UIMAGECUBE;
    } else if (ctx.UIMAGEBUFFER() != null) {
      return ImageType.UIMAGEBUFFER;
    } else if (ctx.UIMAGE1DARRAY() != null) {
      return ImageType.UIMAGE1DARRAY;
    } else if (ctx.UIMAGE2DARRAY() != null) {
      return ImageType.UIMAGE2DARRAY;
    } else if (ctx.UIMAGECUBEARRAY() != null) {
      return ImageType.UIMAGECUBEARRAY;
    } else if (ctx.UIMAGE2DMS() != null) {
      return ImageType.UIMAGE2DMS;
    } else if (ctx.UIMAGE2DMSARRAY() != null) {
      return ImageType.UIMAGE2DMSARRAY;
    } else {
      assert ctx.ATOMIC_UINT() != null;
      return AtomicIntType.ATOMIC_UINT;
    }
  }

  private List<TypeQualifier> getQualifiers(Parameter_qualifierContext ctx) {
    if (ctx.parameter_qualifier() == null) {
      return new LinkedList<>();
    }
    List<TypeQualifier> result = getQualifiers(ctx.parameter_qualifier());
    if (ctx.CONST_TOK() != null) {
      result.add(0, TypeQualifier.CONST);
    } else if (ctx.PRECISE() != null) {
      result.add(0, TypeQualifier.PRECISE);
    } else if (ctx.parameter_direction_qualifier() != null) {
      if (ctx.parameter_direction_qualifier().IN_TOK() != null) {
        result.add(TypeQualifier.IN);
      } else if (ctx.parameter_direction_qualifier().OUT_TOK() != null) {
        result.add(TypeQualifier.OUT);
      } else {
        assert ctx.parameter_direction_qualifier().INOUT_TOK() != null;
        result.add(TypeQualifier.INOUT);
      }
    } else {
      assert ctx.precision_qualifier() != null;
      if (ctx.precision_qualifier().HIGHP() != null) {
        result.add(TypeQualifier.HIGHP);
      } else if (ctx.precision_qualifier().MEDIUMP() != null) {
        result.add(TypeQualifier.MEDIUMP);
      } else {
        assert ctx.precision_qualifier().LOWP() != null;
        result.add(TypeQualifier.LOWP);
      }
    }
    return result;
  }

  private List<TypeQualifier> getQualifiers(Type_qualifierContext ctx) {
    if (ctx == null) {
      return new ArrayList<>();
    }
    List<TypeQualifier> result;
    if (ctx.type_qualifier() != null) {
      result = getQualifiers(ctx.type_qualifier());
    } else {
      result = new ArrayList<>();
    }

    if (ctx.INVARIANT() != null) {
      result.add(TypeQualifier.INVARIANT);
    } else if (ctx.PRECISE() != null) {
      result.add(TypeQualifier.PRECISE);
    } else if (ctx.auxiliary_storage_qualifier() != null) {
      Auxiliary_storage_qualifierContext asq = ctx.auxiliary_storage_qualifier();
      if (asq.CENTROID() != null) {
        result.add(TypeQualifier.CENTROID);
      } else {
        assert asq.SAMPLE() != null;
        result.add(TypeQualifier.SAMPLE);
      }
    } else if (ctx.storage_qualifier() != null) {
      Storage_qualifierContext sq = ctx.storage_qualifier();
      if (sq.CONST_TOK() != null) {
        result.add(TypeQualifier.CONST);
      } else if (sq.ATTRIBUTE() != null) {
        result.add(TypeQualifier.ATTRIBUTE);
      } else if (sq.VARYING() != null) {
        result.add(TypeQualifier.VARYING);
      } else if (sq.IN_TOK() != null) {
        result.add(TypeQualifier.IN);
      } else if (sq.OUT_TOK() != null) {
        result.add(TypeQualifier.OUT);
      } else if (sq.UNIFORM() != null) {
        result.add(TypeQualifier.UNIFORM);
      } else if (sq.COHERENT() != null) {
        result.add(TypeQualifier.COHERENT);
      } else if (sq.VOLATILE() != null) {
        result.add(TypeQualifier.VOLATILE);
      } else if (sq.RESTRICT() != null) {
        result.add(TypeQualifier.RESTRICT);
      } else if (sq.READONLY() != null) {
        result.add(TypeQualifier.READONLY);
      } else {
        assert sq.WRITEONLY() != null;
        result.add(TypeQualifier.WRITEONLY);
      }
    } else if (ctx.interpolation_qualifier() != null) {
      Interpolation_qualifierContext iq = ctx.interpolation_qualifier();
      if (iq.SMOOTH() != null) {
        result.add(TypeQualifier.SMOOTH);
      } else if (iq.FLAT() != null) {
        result.add(TypeQualifier.FLAT);
      } else {
        assert iq.NOPERSPECTIVE() != null;
        result.add(TypeQualifier.NOPERSPECTIVE);
      }
    } else if (ctx.layout_qualifier() != null) {
      // This one cannot be handled just by an enum,
      // so some more design will be required
      throw new NotImplementedException();
    } else {
      assert ctx.precision_qualifier() != null;
      Precision_qualifierContext pq = ctx.precision_qualifier();
      if (pq.HIGHP() != null) {
        result.add(TypeQualifier.HIGHP);
      } else if (pq.MEDIUMP() != null) {
        result.add(TypeQualifier.MEDIUMP);
      } else {
        assert pq.LOWP() != null;
        result.add(TypeQualifier.LOWP);
      }
    }
    return result;
  }

  @Override
  public ParameterDecl visitParameter_declaration(Parameter_declarationContext ctx) {
    List<TypeQualifier> qualifiers = getQualifiers(ctx.parameter_qualifier());
    if (ctx.parameter_type_specifier() != null) {
      return new ParameterDecl(null,
          getType(ctx.parameter_type_specifier().type_specifier(), qualifiers), null);
    }
    return new ParameterDecl(ctx.parameter_declarator().any_identifier().IDENTIFIER().getText(),
        getType(ctx.parameter_declarator().type_specifier(), qualifiers),
        ctx.parameter_declarator().array_specifier() == null ? null :
            getArrayInfo(ctx.parameter_declarator().array_specifier()));
  }

  @Override
  public FunctionDefinition visitFunction_definition(Function_definitionContext ctx) {
    return new FunctionDefinition(
        visitFunction_prototype(ctx.function_prototype()),
        visitCompound_statement_no_new_scope(ctx.compound_statement_no_new_scope()));
  }

  @Override
  public BlockStmt visitCompound_statement_no_new_scope(
      Compound_statement_no_new_scopeContext ctx) {
    if (ctx.statement_list() == null) {
      return new BlockStmt(new ArrayList<>(), false);
    }
    return new BlockStmt(visitStatement_list(ctx.statement_list()), false);
  }

  @Override
  public List<Stmt> visitStatement_list(Statement_listContext ctx) {
    if (ctx.statement_list() == null) {
      List<Stmt> result = new ArrayList<>();
      result.add(visitStatement(ctx.statement()));
      return result;
    }
    List<Stmt> result = visitStatement_list(ctx.statement_list());
    result.add(visitStatement(ctx.statement()));
    return result;
  }

  @Override
  public DeclarationStmt visitDeclaration_statement(Declaration_statementContext ctx) {
    if (ctx.declaration().init_declarator_list() == null) {
      throw new RuntimeException("Error at line " + ctx.start.getLine()
          + ": Only variable declarations are supported in declaration statements");
    }
    return new DeclarationStmt(
        visitInit_declarator_list(ctx.declaration().init_declarator_list()));
  }

  @Override
  public VariablesDeclaration visitInit_declarator_list(Init_declarator_listContext ctx) {
    List<VariableDeclInfo> declInfo = new LinkedList<>();
    Init_declarator_listContext idl = ctx;
    while (idl.single_declaration() == null) {
      declInfo.add(0,
          processVarDeclInfo(idl.any_identifier(), idl.array_specifier(), idl.initializer()));
      idl = idl.init_declarator_list();
    }
    Single_declarationContext sdc = idl.single_declaration();
    if (sdc.any_identifier() != null) {
      // Note: a struct declaration on its own is treated as a variables declaration with zero
      // identifiers.  This seems to be the cleanest approach, since a struct declaration can
      // be followed by declaration of struct instances.
      declInfo.add(0,
          processVarDeclInfo(sdc.any_identifier(), sdc.array_specifier(), sdc.initializer()));
    }
    Type baseType = visitFully_specified_type(sdc.fully_specified_type());
    return new VariablesDeclaration(baseType, declInfo);
  }

  private VariableDeclInfo processVarDeclInfo(Any_identifierContext identifier,
      Array_specifierContext arraySpecifier, InitializerContext initializer) {
    return new VariableDeclInfo(identifier.getText(),
        arraySpecifier == null ? null : getArrayInfo(arraySpecifier),
        visitInitializer(initializer));
  }

  @Override
  public Initializer visitInitializer(InitializerContext ctx) {
    if (ctx == null) {
      return null;
    }
    if (ctx.assignment_expression() != null) {
      return new ScalarInitializer(visitAssignment_expression(ctx.assignment_expression()));
    }
    throw new RuntimeException();
  }

  @Override
  public Stmt visitExpression_statement(Expression_statementContext ctx) {
    if (ctx.expression() == null) {
      return NullStmt.INSTANCE;
    }
    return new ExprStmt(visitExpression(ctx.expression()));
  }

  @Override
  public IfStmt visitSelection_statement(Selection_statementContext ctx) {
    Stmt thenStmt = visitStatement(ctx.selection_rest_statement().statement(0));
    StatementContext maybeElseBranch = ctx.selection_rest_statement().statement(1);
    Stmt elseStmt = (maybeElseBranch == null ? null : visitStatement(maybeElseBranch));
    return new IfStmt(visitExpression(ctx.expression()), thenStmt, elseStmt);
  }

  @Override
  public Expr visitExpression(ExpressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitAssignment_expression);
  }

  @Override
  public Stmt visitSwitch_statement(Switch_statementContext ctx) {
    return new SwitchStmt(visitExpression(ctx.expression()), visitSwitch_body(ctx.switch_body()));
  }

  @Override
  public BlockStmt visitSwitch_body(Switch_bodyContext ctx) {
    if (ctx.case_statement_list() == null) {
      return new BlockStmt(new ArrayList<>(), true);
    }
    Deque<Case_statementContext> cases = new LinkedList<>();
    {
      Case_statement_listContext temp = ctx.case_statement_list();
      while (true) {
        cases.addFirst(temp.case_statement());
        if (temp.case_statement_list() == null) {
          break;
        }
        temp = temp.case_statement_list();
      }
    }
    final List<Stmt> stmts = new ArrayList<>();
    for (Case_statementContext caseCtx : cases) {
      stmts.addAll(visitCase_statement(caseCtx));
    }
    return new BlockStmt(stmts, true);
  }

  @Override
  public List<Stmt> visitCase_statement(Case_statementContext ctx) {
    Deque<StatementContext> statements = new LinkedList<>();
    Case_statementContext temp = ctx;
    while (true) {
      statements.addFirst(temp.statement());
      if (temp.case_statement() == null) {
        break;
      }
      temp = temp.case_statement();
    }
    List<Stmt> stmts = new ArrayList<>();
    stmts.addAll(visitCase_label_list(temp.case_label_list()));
    for (StatementContext statementContext : statements) {
      stmts.add(visitStatement(statementContext));
    }
    return stmts;
  }

  @Override
  public List<CaseLabel> visitCase_label_list(Case_label_listContext ctx) {
    Deque<Case_labelContext> labels = new LinkedList<>();
    {
      Case_label_listContext temp = ctx;
      while (true) {
        labels.addFirst(temp.case_label());
        if (temp.case_label_list() == null) {
          break;
        }
        temp = temp.case_label_list();
      }
    }
    List<CaseLabel> result = new ArrayList<>();
    for (Case_labelContext caseLabel : labels) {
      result.add(visitCase_label(caseLabel));
    }
    return result;
  }

  @Override
  public CaseLabel visitCase_label(Case_labelContext ctx) {
    if (ctx.DEFAULT() != null) {
      return DefaultCaseLabel.INSTANCE;
    }
    return new ExprCaseLabel(visitExpression(ctx.expression()));
  }

  @Override
  public Stmt visitStatement(StatementContext ctx) {
    return (Stmt) super.visitStatement(ctx);
  }

  @Override
  public Stmt visitStatement_no_new_scope(Statement_no_new_scopeContext ctx) {
    return (Stmt) super.visitStatement_no_new_scope(ctx);
  }

  @Override
  public Stmt visitIteration_statement(Iteration_statementContext ctx) {
    if (ctx.DO() != null) {
      return new DoStmt(visitStatement(ctx.statement()), visitExpression(ctx.expression()));
    }
    if (ctx.WHILE() != null) {
      return new WhileStmt(visitCondition(ctx.condition()),
          visitStatement_no_new_scope(ctx.statement_no_new_scope()));
    }
    assert ctx.FOR() != null;
    return new ForStmt(visitFor_init_statement(ctx.for_init_statement()),
        ctx.for_rest_statement().conditionopt().condition() == null ? null :
            visitCondition(ctx.for_rest_statement().conditionopt().condition()),
        ctx.for_rest_statement().expression() == null ? null :
            visitExpression(ctx.for_rest_statement().expression()),
            visitStatement_no_new_scope(ctx.statement_no_new_scope()));
  }

  @Override
  public Expr visitCondition(ConditionContext ctx) {
    if (ctx.expression() != null) {
      return visitExpression(ctx.expression());
    }
    assert ctx.ASSIGN_OP() != null;
    throw new RuntimeException(
        "We do not yet support the case where the condition of a 'for' or 'while' introduces a "
        + "new variable: " + getOriginalSourceText(ctx));
  }

  @Override
  public Stmt visitFor_rest_statement(For_rest_statementContext ctx) {
    throw new RuntimeException();
  }

  @Override
  public Stmt visitFor_init_statement(For_init_statementContext ctx) {
    if (ctx.expression_statement() != null) {
      return visitExpression_statement(ctx.expression_statement());
    }
    assert ctx.declaration_statement() != null;
    return visitDeclaration_statement(ctx.declaration_statement());
  }

  @Override
  public Stmt visitJump_statement(Jump_statementContext ctx) {
    if (ctx.CONTINUE() != null) {
      return ContinueStmt.INSTANCE;
    }
    if (ctx.BREAK() != null) {
      return BreakStmt.INSTANCE;
    }
    if (ctx.RETURN() != null) {
      if (ctx.expression() == null) {
        return new ReturnStmt();
      }
      return new ReturnStmt(visitExpression(ctx.expression()));
    }
    assert ctx.DISCARD() != null;
    return DiscardStmt.INSTANCE;
  }

  @Override
  public BlockStmt visitCompound_statement(Compound_statementContext ctx) {
    if (ctx.statement_list() == null) {
      return new BlockStmt(new ArrayList<>(), true);
    }
    return new BlockStmt(visitStatement_list(ctx.statement_list()), true);
  }

  private String getOriginalSourceText(ParserRuleContext ctx) {
    return ctx.start.getInputStream()
        .getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
  }

  @Override
  public Object visitPragma_statement(Pragma_statementContext ctx) {
    throw new RuntimeException();
  }

  @Override
  public Object visitLayout_defaults(Layout_defaultsContext ctx) {
    throw new RuntimeException();
  }

  @Override
  public Declaration visitExtension_statement(Extension_statementContext ctx) {
    throw new RuntimeException();
  }

  @Override
  public Expr visitPrimary_expression(Primary_expressionContext ctx) {
    if (ctx.variable_identifier() != null) {
      return new VariableIdentifierExpr(ctx.variable_identifier().IDENTIFIER().getText());
    }
    if (ctx.INTCONSTANT() != null) {
      return new IntConstantExpr(ctx.INTCONSTANT().getText());
    }
    if (ctx.UINTCONSTANT() != null) {
      return new UIntConstantExpr(ctx.UINTCONSTANT().getText());
    }
    if (ctx.FLOATCONSTANT() != null) {
      return new FloatConstantExpr(ctx.FLOATCONSTANT().getText());
    }
    if (ctx.BOOLCONSTANT() != null) {
      if (ctx.BOOLCONSTANT().getText().equals("true")) {
        return BoolConstantExpr.TRUE;
      }
      assert (ctx.BOOLCONSTANT().getText().equals("false"));
      return BoolConstantExpr.FALSE;
    }
    assert ctx.LPAREN() != null;
    return new ParenExpr(visitExpression(ctx.expression()));
  }

  @Override
  public Expr visitPostfix_expression(Postfix_expressionContext ctx) {
    if (ctx.primary_expression() != null) {
      return visitPrimary_expression(ctx.primary_expression());
    }
    if (ctx.LBRACKET() != null) {
      return new ArrayIndexExpr(visitPostfix_expression(ctx.postfix_expression()),
          visitExpression(ctx.integer_expression().expression()));
    }
    if (ctx.method_call_generic() != null) {
      // TODO: check grammar
      throw new RuntimeException("Not yet supported: " + getOriginalSourceText(ctx));
    }
    if (ctx.any_identifier() != null) {
      return new MemberLookupExpr(visitPostfix_expression(ctx.postfix_expression()),
          ctx.any_identifier().getText());
    }
    if (ctx.INC_OP() != null) {
      return new UnaryExpr(visitPostfix_expression(ctx.postfix_expression()), UnOp.POST_INC);
    }
    if (ctx.DEC_OP() != null) {
      return new UnaryExpr(visitPostfix_expression(ctx.postfix_expression()), UnOp.POST_DEC);
    }
    assert ctx.function_call_generic() != null;
    Expr result = visitFunction_call_generic(ctx.function_call_generic());
    assert result != null;
    return result;
  }

  @Override
  public Expr visitFunction_call_generic(Function_call_genericContext ctx) {
    if (ctx.function_call_header_no_parameters() != null) {
      return visitFunction_call_header_no_parameters(ctx.function_call_header_no_parameters());
    }
    assert ctx.function_call_header_with_parameters() != null;
    return visitFunction_call_header_with_parameters(ctx.function_call_header_with_parameters());
  }

  @Override
  public Expr visitFunction_call_header_no_parameters(
      Function_call_header_no_parametersContext ctx) {
    if (isBuiltinTypeConstructor(ctx.function_call_header().function_identifier())
        || isStructTypeConstructor(ctx.function_call_header().function_identifier())) {
      throw new RuntimeException(
          "Found type constructor with no arguments at line " + ctx.start.getLine() + ": "
              + getOriginalSourceText(ctx));
    }
    if (isRegularFunction(ctx.function_call_header().function_identifier())) {
      return new FunctionCallExpr(getCallee(ctx.function_call_header().function_identifier()),
          new ArrayList<>());
    }
    throw new RuntimeException("Unsupported function call at line " + ctx.start.getLine() + ": "
        + getOriginalSourceText(ctx));
  }

  @Override
  public Expr visitFunction_call_header_with_parameters(
      Function_call_header_with_parametersContext ctx) {
    List<Expr> params = new LinkedList<>();
    Function_call_header_with_parametersContext fchwp = ctx;
    while (fchwp.function_call_header_with_parameters() != null) {
      params.add(0, visitAssignment_expression(fchwp.assignment_expression()));
      fchwp = fchwp.function_call_header_with_parameters();
    }
    params.add(0, visitAssignment_expression(fchwp.assignment_expression()));
    Function_call_headerContext header = fchwp.function_call_header();
    if (isBuiltinTypeConstructor(header.function_identifier())) {
      if (header.array_specifier() != null) {
        return new ArrayConstructorExpr(
            new ArrayType(
                getBuiltinType(header.function_identifier().builtin_type_specifier_nonarray()),
                getArrayInfo(header.array_specifier())), params);
      }
      return new TypeConstructorExpr(getTypeConstructorName(header.function_identifier()), params);
    }
    if (isStructTypeConstructor(header.function_identifier())) {
      if (header.array_specifier() != null) {
        StructType structType = structs.stream().filter(item -> item.getName()
            .equals(header.function_identifier().variable_identifier().getText()))
            .collect(Collectors.toList()).get(0);
        return new ArrayConstructorExpr(
            new ArrayType(structType,
                getArrayInfo(header.array_specifier())), params);
      }
      return new TypeConstructorExpr(getTypeConstructorName(header.function_identifier()), params);
    }
    if (isRegularFunction(header.function_identifier())) {
      return new FunctionCallExpr(getCallee(header.function_identifier()), params);
    }
    throw new RuntimeException("Unsupported function call: " + getOriginalSourceText(ctx));
  }

  private String getCallee(Function_identifierContext ctx) {
    assert ctx.variable_identifier() != null;
    return ctx.variable_identifier().getText();
  }

  private boolean isRegularFunction(Function_identifierContext ctx) {
    return ctx.variable_identifier() != null;
  }

  private String getTypeConstructorName(Function_identifierContext ctx) {
    if (ctx.builtin_type_specifier_nonarray() != null) {
      return getOriginalSourceText(ctx.builtin_type_specifier_nonarray());
    }
    assert isStructTypeConstructor(ctx);
    return ctx.variable_identifier().getText();
  }

  private boolean isBuiltinTypeConstructor(Function_identifierContext ctx) {
    return ctx.builtin_type_specifier_nonarray() != null;
  }

  private boolean isStructTypeConstructor(Function_identifierContext ctx) {
    return ctx.variable_identifier() != null
        && structs.stream()
            .anyMatch(item -> item.getName().equals(ctx.variable_identifier().getText()));
  }

  @Override
  public Expr visitUnary_expression(Unary_expressionContext ctx) {
    if (ctx.postfix_expression() != null) {
      return visitPostfix_expression(ctx.postfix_expression());
    }
    if (ctx.INC_OP() != null) {
      return new UnaryExpr(visitUnary_expression(ctx.unary_expression()), UnOp.PRE_INC);
    }
    if (ctx.DEC_OP() != null) {
      return new UnaryExpr(visitUnary_expression(ctx.unary_expression()), UnOp.PRE_DEC);
    }
    assert ctx.unary_operator() != null;
    return new UnaryExpr(visitUnary_expression(ctx.unary_expression()),
        processUnaryOperator(ctx.unary_operator()));
  }

  private UnOp processUnaryOperator(Unary_operatorContext uopCtx) {
    if (uopCtx.PLUS_OP() != null) {
      return UnOp.PLUS;
    }
    if (uopCtx.MINUS_OP() != null) {
      return UnOp.MINUS;
    }
    if (uopCtx.NOT_OP() != null) {
      return UnOp.LNOT;
    }
    assert uopCtx.BNEG_OP() != null;
    return UnOp.BNEG;
  }

  @Override
  public Expr visitMultiplicative_expression(Multiplicative_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitUnary_expression);
  }

  @Override
  public Expr visitAdditive_expression(Additive_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitMultiplicative_expression);
  }

  @Override
  public Expr visitShift_expression(Shift_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitAdditive_expression);
  }

  @Override
  public Expr visitRelational_expression(Relational_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitShift_expression);
  }

  @Override
  public Expr visitEquality_expression(Equality_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitRelational_expression);
  }

  @Override
  public Expr visitAnd_expression(And_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitEquality_expression);
  }

  @Override
  public Expr visitExclusive_or_expression(Exclusive_or_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitAnd_expression);
  }

  @Override
  public Expr visitInclusive_or_expression(Inclusive_or_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitExclusive_or_expression);
  }

  @Override
  public Expr visitLogical_and_expression(Logical_and_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitInclusive_or_expression);
  }

  @Override
  public Expr visitLogical_xor_expression(Logical_xor_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitLogical_and_expression);
  }

  @Override
  public Expr visitLogical_or_expression(Logical_or_expressionContext ctx) {
    return handleBinary(ctx.operands, ctx.operators, this::visitLogical_xor_expression);
  }

  @Override
  public Expr visitConditional_expression(Conditional_expressionContext ctx) {
    assert ctx.expression().size() == ctx.assignment_expression().size();

    // First, visit all the args, slapping them into a list
    List<Expr> argsInOrder = new ArrayList<>();
    argsInOrder.add(visitLogical_or_expression(ctx.logical_or_expression()));
    for (int i = 0; i < ctx.expression().size(); i++) {
      argsInOrder.add(visitExpression(ctx.expression(i)));
      argsInOrder.add(visitAssignment_expression(ctx.assignment_expression(i)));
    }
    assert (argsInOrder.size() % 2) == 1; // should be an odd number of args in total

    // Now visit the list in reverse order, building up the ternary
    Expr result = argsInOrder.get(argsInOrder.size() - 1);
    for (int i = argsInOrder.size() - 2; i >= 0; i -= 2) {
      assert (i % 2) == 1;
      result = new TernaryExpr(argsInOrder.get(i - 1), argsInOrder.get(i), result);
    }
    return result;
  }

  @Override
  public Expr visitAssignment_expression(Assignment_expressionContext ctx) {
    if (ctx.conditional_expression() != null) {
      return visitConditional_expression(ctx.conditional_expression());
    }
    return new BinaryExpr(visitUnary_expression(ctx.unary_expression()),
        visitAssignment_expression(ctx.assignment_expression()),
        processAssignmentOperator(ctx.assignment_operator()));
  }

  private BinOp processAssignmentOperator(Assignment_operatorContext op) {
    if (op.ASSIGN_OP() != null) {
      return BinOp.ASSIGN;
    }
    if (op.MUL_ASSIGN() != null) {
      return BinOp.MUL_ASSIGN;
    }
    if (op.DIV_ASSIGN() != null) {
      return BinOp.DIV_ASSIGN;
    }
    if (op.MOD_ASSIGN() != null) {
      return BinOp.MOD_ASSIGN;
    }
    if (op.ADD_ASSIGN() != null) {
      return BinOp.ADD_ASSIGN;
    }
    if (op.SUB_ASSIGN() != null) {
      return BinOp.SUB_ASSIGN;
    }
    if (op.LEFT_ASSIGN() != null) {
      return BinOp.SHL_ASSIGN;
    }
    if (op.RIGHT_ASSIGN() != null) {
      return BinOp.SHR_ASSIGN;
    }
    if (op.AND_ASSIGN() != null) {
      return BinOp.BAND_ASSIGN;
    }
    if (op.XOR_ASSIGN() != null) {
      return BinOp.BXOR_ASSIGN;
    }
    assert op.OR_ASSIGN() != null;
    return BinOp.BOR_ASSIGN;
  }

  private BinOp getBinOp(Token token) {
    for (BinOp op : BinOp.values()) {
      if (token.getText().equals(op.getText())) {
        return op;
      }
    }
    throw new RuntimeException("Unknown binary operator: " + token.getText());
  }

  private <T> Expr handleBinary(List<T> operands, List<Token> operators,
      Function<T, Expr> childVisitor) {
    assert operands.size() == operators.size() + 1;
    assert operands.size() >= 1;
    Expr result = childVisitor.apply(operands.get(0));
    for (int i = 0; i < operators.size(); i++) {
      result = new BinaryExpr(result, childVisitor.apply(operands.get(i + 1)),
          getBinOp(operators.get(i)));
    }
    return result;
  }

}
