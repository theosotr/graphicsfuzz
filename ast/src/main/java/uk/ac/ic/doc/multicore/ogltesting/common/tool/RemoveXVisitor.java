package uk.ac.ic.doc.multicore.ogltesting.common.tool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariableDeclInfo;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.decl.VariablesDeclaration;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.BlockStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.DeclarationStmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.stmt.Stmt;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.type.Type;
import uk.ac.ic.doc.multicore.ogltesting.common.ast.visitors.StandardVisitor;


public class RemoveXVisitor extends StandardVisitor {

  @Override
  public void visitVariablesDeclaration(VariablesDeclaration variablesDeclaration) {
    Type baseType = variablesDeclaration.getBaseType();
    visit(baseType);
    boolean hasVarX = false;
    for (VariableDeclInfo vdi : variablesDeclaration.getDeclInfos()) {
      if (vdi.getName().equals("x")) {
        hasVarX = true;
        break;
      }
    }
    if (hasVarX) {
      int numDecls = variablesDeclaration.getNumDecls();
      while (numDecls != 0) {
        variablesDeclaration.removeDeclInfo(numDecls - 1);
        numDecls--;
      }
    }
  }

  @Override
  public void visitBlockStmt(BlockStmt blockStmt) {
    super.visitBlockStmt(blockStmt);
    List<Stmt> rmStmts = new ArrayList<>();
    for (int i = 0; i < blockStmt.getNumStmts(); i++) {
      Stmt stmt = blockStmt.getStmt(i);
      if (!(stmt instanceof DeclarationStmt)) {
        continue;
      }
      if (((DeclarationStmt) stmt).getVariablesDeclaration().getNumDecls() == 0) {
        rmStmts.add(stmt);
      }
    }
    for (Stmt stmt : rmStmts) {
      blockStmt.removeStmt(stmt);
    }
  }
}