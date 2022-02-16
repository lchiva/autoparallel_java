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


public class IsReadOnlyVisitor extends ASTVisitor {
	
	public List<MethodDeclaration> withWrite = new ArrayList<>();
	
	@Override
	public boolean visit(MethodDeclaration node) {
		FindWriteOnFields findWrite = new FindWriteOnFields();
		node.accept(findWrite);
		if (findWrite.hasWrite) {
			System.out.println(node.getName() + " With a write !");
			withWrite.add(node);
		} else {
			System.out.println(node.getName() + " No write");
		}
		return super.visit(node);
	}
}
class FindWriteOnFields extends ASTVisitor {
	boolean hasWrite = false;
	
	public boolean isHasWrite() {
		return hasWrite;
	}
	@Override
	public boolean visit(Assignment node) { 
		AssignVisitor av = new AssignVisitor();
		node.getLeftHandSide().accept(av);
		//System.out.print(node.getLeftHandSide());
		if (av.hasWrite) {
			hasWrite = true;
		}
		return super.visit(node);
	}@Override
	public boolean visit(PostfixExpression node) {
		AssignVisitor av = new AssignVisitor();	
		node.getOperand().accept(av); 
		//System.out.print(node.getOperand());
		if(av.hasWrite) {
			hasWrite = true;
		}
		return super.visit(node);
	}
	@Override
	public boolean visit(PrefixExpression node) {
		AssignVisitor av = new AssignVisitor();
		node.getOperand().accept(av);
		//System.out.print(node.getOperand());
		if(av.hasWrite) {
			hasWrite = true;
		}
		return super.visit(node);
	}
}
class AssignVisitor extends ASTVisitor {
	boolean hasWrite = false;
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) { //simple name and qualifiedname
			Name nname = (Name) node;
			//System.out.print(nname);
			IBinding target = nname.resolveBinding(); 
			//System.out.print(target);
			if (target instanceof IVariableBinding) {
				IVariableBinding vbind = (IVariableBinding) target;
				//System.out.print(vbind);
				if (vbind.isField()) {
					hasWrite = true;
				}
			}
		}
		super.preVisit(node);
	}
}








