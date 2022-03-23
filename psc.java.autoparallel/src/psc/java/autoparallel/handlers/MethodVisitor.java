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

	private Map<String, Set<String>> methodTag; // store all methodtag
	private boolean notParallelisable = false; 
	private boolean readOnly = true;
	private boolean threadSafe = true;

	/*
	 * Constructor that initialize methodTag
	 * this constructor is used in Parallelizable.java 
	 */
	public MethodVisitor(Map<String, Set<String>> map) {
		this.methodTag = map;
	}
	/*
	 * Method that return value of notParallelisable
	 */
	public boolean isNotParallelisable() {
		return notParallelisable;
	}
	/*
	 * Method that return value of readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
	/*
	 * Method that return value of threadSafe 
	 */
	public boolean isThreadSafe() {
		return threadSafe;
	}
	
	/*
	 * Method to visit Assignment in method body. 
	 */
	@Override
	public boolean visit(Assignment node) {
		AssignVisitor av = new AssignVisitor();
		node.getLeftHandSide().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			notParallelisable = true;
			threadSafe = false;
		}
		return false;
	}
	/*
	 * Method to visit MethodInvocation in method body. 
	 * methodInvocation = method that is called in that method.
	 */
	@Override
	public boolean visit(MethodInvocation node) {
		IBinding binding = node.resolveMethodBinding();
		if(binding.getKey().contains("Ljava/io/PrintStream;")) {// if it calls println, it isnt threadSafe 
			threadSafe = false;
		}
		if (methodTag.get("NotParallelizable").contains(binding.getKey())) { // if it call notpara method, then it is not
			notParallelisable = true;										// para, not threadsafe, not readonly
			threadSafe = false;
			readOnly = false;
		}
		return false;
	}
	/*
	 * Method to visit PostfixExpression in method body. 
	 */
	@Override
	public boolean visit(PostfixExpression node) {
		AssignVisitor av = new AssignVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			notParallelisable = true;
			threadSafe = false;
		}
		return false;
	}
	/*
	 * Method to visit PrefixExpression in method body. 
	 */
	@Override
	public boolean visit(PrefixExpression node) {
		AssignVisitor av = new AssignVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			readOnly = false;
			notParallelisable = true;
			threadSafe = false;
		}
		return false;
	}
	/*
	 * Method to visit if there is synchronized statement in body
	 */
	@Override
	public boolean visit(SynchronizedStatement node) {
		threadSafe = true;
		return false;
	}
}
/*
 * Class to verify if the modification in the body is done on global variable or local variable
 */
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
