package psc.java.autoparallel.handlers;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

/**
 * Classe impl�mentant l'interface visiteur et visitant des Method d�clarations
 * pour pouvoir classer les m�thodes en cat�gorie
 * 
 * @author Teillet & Capitanio
 *
 */
public class MethodVisitor extends ASTVisitor {

	private Map<String, Set<String>> methodTag;
	private boolean notParallelisable = false;
	private boolean readOnly = true;
	private boolean threadSafe = false;

	public MethodVisitor(Map<String, Set<String>> map) {
		this.methodTag = map;
	}

	public boolean isNotParallelisable() {
		return notParallelisable;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isThreadSafe() {
		return threadSafe;
	}

	@Override
	public boolean visit(Assignment node) {
		AssignVisitor av = new AssignVisitor();
		node.getLeftHandSide().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			threadSafe = false;
			notParallelisable = true;
		} else {
			threadSafe = true;
		}
		return false;
	}
	@Override
	public boolean visit(MethodInvocation node) {
		IBinding binding = node.resolveMethodBinding();
		System.out.println(binding.getKey());
		if(binding.getKey().contains("Ljava/io/PrintStream;")) {
			notParallelisable = true;
		}
		if (methodTag.get("NotParallelizable").contains(binding.getKey())) {
			notParallelisable = true;
			threadSafe = false;
			readOnly = false;
		} else if (methodTag.get("ReadOnly").contains(binding.getKey())) {
			readOnly = true;
			threadSafe = true;
		} else if (methodTag.get("ThreadSafe").contains(binding.getKey())) {
			threadSafe = true;
		}
		return false;
	}
	@Override
	public boolean visit(PostfixExpression node) {
		AssignVisitor av = new AssignVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			threadSafe = false;
			notParallelisable = true;
		} else {
			threadSafe = true;
		}
		return false;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		AssignVisitor av = new AssignVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			threadSafe = false;
			notParallelisable = true;
		} else {
			threadSafe = true;
		}
		return false;
	}
	@Override
	public boolean visit(SynchronizedStatement node) {
		threadSafe = true;
		return false;
	}
}

class AssignVisitor extends ASTVisitor {
	boolean hasWrite = false;

//	boolean isModifLocal = false;
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) { // simple name and qualifiedname
			Name nname = (Name) node;
			IBinding target = nname.resolveBinding();
			if (target instanceof IVariableBinding) {
				IVariableBinding vbind = (IVariableBinding) target;
				if (vbind.isField()) { // if the operation is assigned on a field then it is not readOnly
					hasWrite = true;
				}
			}
		}
		super.preVisit(node);
	}
}
