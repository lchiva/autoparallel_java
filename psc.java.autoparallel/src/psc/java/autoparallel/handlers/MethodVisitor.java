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

/*
 * This class for visit all the given arguments, prefix, postfix, assignment, methodinovation...
 */
public class MethodVisitor extends ASTVisitor {

	private Map<String, Set<String>> methodTag; // store all methodtag
	//boolean for checking if a method is notparallelisable
	private boolean notParallelisable = false;
	//boolean for checking if a method is readonly
	private boolean readOnly = true;
	//boolean for checking if a method is threadSafe
	private boolean threadSafe = true;
	//boolean for ordered stream
	private boolean isOrdered = false;
	//boolean for SIOs(stateful intermediate operation)
	private boolean hasSIOs = false;
	//boolean for modifValue
	private boolean modifHeap = false;
	private boolean roms = false;
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
	 * Method that return value of hasSIOs
	 */
	public boolean hasSIOs() {
		return hasSIOs();
	}
	/*
	 * Method that return value of threadSafe
	 */
	public boolean isThreadSafe() {
		return threadSafe;
	}
	/*
	 * Method that return the type of order stream
	 */
	public boolean isOrdered() {
		return isOrdered;
	}
	/*
	 * Method that return the value of modifValue
	 */
	public boolean modifHeap() {
		return modifHeap;
	}
	/*
	 *
	 */
	public boolean rom() {
		return roms ;
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
	//visit only once.
	public boolean alreadyvisited = false;


	/*
	 * Method to visit MethodInvocation in method body.
	 * methodInvocation = method that is called in that method.
	 */
	@Override
	public boolean visit(MethodInvocation node) {
		IBinding binding = node.resolveMethodBinding();
//		System.out.println(node.resolveMethodBinding().getDeclaringClass());
		//Case of Array
		//Call method from class Array
		if(binding.getKey().contains("Ljava/util/ArrayList")) {
			if(binding.getKey().contains(".add") || binding.getKey().contains(".clear") || binding.getKey().contains(".remove")
					|| binding.getKey().contains(".removeAll")
					|| binding.getKey().contains(".addAll")
					|| binding.getKey().contains(".removeIf")
					|| binding.getKey().contains(".replaceAll")
					|| binding.getKey().contains(".set")
					|| binding.getKey().contains(".sort")
					) {
				AssignVisitor av = new AssignVisitor();
				node.getExpression().accept(av);
				if(av.hasWrite) {
					readOnly = false;
					notParallelisable = true;
					threadSafe = false;
				}
			}
		}
		//Case HashSet
		//Call method from class HashSet
		if(binding.getKey().contains("Ljava/util/HashSet")) {
			if(binding.getKey().contains(".add")
					|| binding.getKey().contains(".clear")
					|| binding.getKey().contains(".remove")
					|| binding.getKey().contains(".removeAll")
					|| binding.getKey().contains(".addAll")
					) {
				AssignVisitor av = new AssignVisitor();
				node.getExpression().accept(av);
				if(av.hasWrite) {
					readOnly = false;
					notParallelisable = true;
					threadSafe = false;
				}
			}
		}
		//Case priorityQueue
		//Call method from class PriorityQueue
		if(binding.getKey().contains("Ljava/util/PriorityQueue")) {
			if(binding.getKey().contains(".add")
					|| binding.getKey().contains(".clear")
					|| binding.getKey().contains(".poll")
					|| binding.getKey().contains(".remove")
					|| binding.getKey().contains(".removeAll")
					|| binding.getKey().contains(".addAll")
					) {
				AssignVisitor av = new AssignVisitor();
				node.getExpression().accept(av);
				if(av.hasWrite) {
					readOnly = false;
					notParallelisable = true;
					threadSafe = false;
				}
			}
		}
		if(binding.getKey().contains("Ljava/io/PrintStream;") ) {// if it calls println, it isnt threadSafe
			threadSafe = false;
		}
		if (methodTag.get("NotParallelizable").contains(binding.getKey())) { // if it call notpara method, then it is not
			notParallelisable = true;										// para, not threadsafe, not readonly
			threadSafe = false;
			readOnly = false;
		}
		return true;
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
	boolean modifLocal = false;
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) { // simple name and qualifiedname
			Name nname = (Name) node;
			IBinding target = nname.resolveBinding();
			if (target instanceof IVariableBinding) {
				IVariableBinding vbind = (IVariableBinding) target; //get variable
				if (vbind.isField()) { // if the operation is assigned on a field then it is not readOnly
					hasWrite = true;
				}else {
					modifLocal = true;
				}
			}
		}
		super.preVisit(node);
	}
}
