package psc.java.autoparallel.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class isModifLocalVisitor extends ASTVisitor {
	public List<MethodDeclaration> ModifLocalList = new ArrayList<>();

	@Override
	public boolean visit(MethodDeclaration node) {
		FindModifLocalOnField findModif = new FindModifLocalOnField();
		node.accept(findModif);
		if(findModif.hasModifLocal()) {
			//System.out.println(node.getName()+ " is modiflocal");
			ModifLocalList.add(node);
		}else {
			//System.out.println(node.getName()+ " isn't modifLocal");
		}
		return super.visit(node);
	}
}

class FindModifLocalOnField extends ASTVisitor {
	boolean isModifLocal = false;

	public boolean hasModifLocal() {
		return isModifLocal;
	}
	@Override
	public boolean visit(Assignment node) {  // assignment
		AssignVisitor1 av = new AssignVisitor1();
		node.getLeftHandSide().accept(av);
		if(av.isModifLocal) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
	@Override
	public boolean visit(PostfixExpression node) {
		AssignVisitor1 av = new AssignVisitor1();
		node.getOperand().accept(av);
		if(av.isModifLocal) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
	@Override
	public boolean visit(PrefixExpression node) {
		AssignVisitor1 av = new AssignVisitor1();
		node.getOperand().accept(av);
		if(av.isModifLocal) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
}
class AssignVisitor1 extends ASTVisitor {
	boolean isModifLocal = false;
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) { //simple name and qualifiedname
			Name nname = (Name) node;
			IBinding target = nname.resolveBinding();
			if (target instanceof IVariableBinding) {
				IVariableBinding vbind = (IVariableBinding) target;
				if (!vbind.isField()) {
					isModifLocal = true;
				}
			}
		}
		super.preVisit(node);
	}
}
