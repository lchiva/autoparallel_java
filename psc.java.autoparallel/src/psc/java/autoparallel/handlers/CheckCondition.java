package psc.java.autoparallel.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

public class CheckCondition extends ASTVisitor {

	public List<MethodDeclaration> ReadOnlyList = new ArrayList<>(); //ReadOnly List
	public List<MethodDeclaration> ModifLocalList = new ArrayList<>();	//ModifLocal List
	public List<MethodDeclaration> IsPara = new ArrayList<>();	//Parallelizable list
	public List<MethodDeclaration> ThreadSafeList = new ArrayList<>();	// ThreadSafe List

	@Override
	public boolean visit(MethodDeclaration node) {
		boolean ThreadSafe = false;  //initialize threadsafe as false by default
		CheckConditions CheckCons = new CheckConditions();
		node.accept(CheckCons);
		if(!CheckCons.hasWrite) {  //check if method has write
			ReadOnlyList.add(node); // add method to list
		}
		if(CheckCons.isModifLocal && !CheckCons.hasWrite) {  // check if method has no write and it modifies local variable
			ModifLocalList.add(node); // add method to list
		}
		/*
		 * Check if the given method contains synchronized modifier
		 * or method is threadsafe
		 * or method is readonly
		 */
		if(Flags.isSynchronized(node.getModifiers()) || CheckCons.ThreadSafe || !CheckCons.hasWrite) {
			ThreadSafe = true;
			ThreadSafeList.add(node);
		}
		/*
		 * If method is ThreadSafe, ReadOnly then it is parallelizable
		 */
		if(!CheckCons.hasWrite && ThreadSafe && CheckCons.isParallelisable) {
			IsPara.add(node);
		}
		return super.visit(node);
	}
}
class CheckConditions extends ASTVisitor {
	boolean hasWrite = false;
	boolean isModifLocal = false;
	boolean ThreadSafe = false;
	boolean isParallelisable = true;


	@Override
	public boolean visit(SynchronizedStatement node) { // if we meet a synchronized keyword in method, it is threadSafe
		ThreadSafe = true;
		return false;
	}
	//TODO if the method contains println is it ThreadSafe?
	@Override
	// Visit assignment operator
	public boolean visit(Assignment node) {
		AssignmentVisitor av = new AssignmentVisitor();
		node.getLeftHandSide().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			hasWrite = true ;
		}
		/*
		 * if method modifies local variables and is ReadOnly, then it is modifLocal.
		 */
		if (av.isModifLocal && !av.hasWrite) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
	@Override
	//visit postfixexpression
	public boolean visit(PostfixExpression node) {
		AssignmentVisitor av = new AssignmentVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			hasWrite = true;
		}
		/*
		 * if method modifies local variables and is ReadOnly, then it is modifLocal.
		 */
		if (av.isModifLocal  && !av.hasWrite) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
	@Override
	//visit prefixexpression
	public boolean visit(PrefixExpression node) {
		AssignmentVisitor av = new AssignmentVisitor();
		node.getOperand().accept(av);
		if (av.hasWrite) { // if the method has write then it is not ReadOnly
			hasWrite = true;
		}
		if (av.isModifLocal  && !av.hasWrite) {
			isModifLocal = true;
		}
		return super.visit(node);
	}
	@Override
	//TODO call a method from different class which is not parallelizable
	public boolean visit(MethodInvocation node) {
		/*
		 * Get methodDeclaration from MethodInvocation
		 */
		IMethodBinding binding = (IMethodBinding) node.getName().resolveBinding();
		ICompilationUnit unit = (ICompilationUnit) binding.getJavaElement().getAncestor( IJavaElement.COMPILATION_UNIT );
		if ( unit == null ) {
		   // not available, external declaration
		}
		ASTParser parser = ASTParser.newParser( AST.getJLSLatest() );
		parser.setKind( ASTParser.K_COMPILATION_UNIT );
		parser.setSource( unit );
		parser.setResolveBindings( true );
		CompilationUnit cu = (CompilationUnit) parser.createAST( null );
		MethodDeclaration decl = (MethodDeclaration) cu.findDeclaringNode( binding.getKey() );
		
		CheckConditions CheckCon = new CheckConditions();
		decl.accept(CheckCon);
		if(CheckCon.hasWrite || !CheckCon.ThreadSafe) {
			isParallelisable = false;
		}
		return super.visit(node);
	}
}
class AssignmentVisitor extends ASTVisitor {
	boolean hasWrite = false;
	boolean isModifLocal = false;
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) { //simple name and qualifiedname
			Name nname = (Name) node; 
			IBinding target = nname.resolveBinding();
			if (target instanceof IVariableBinding) {
				IVariableBinding vbind = (IVariableBinding) target;
				if (vbind.isField()) {	//	if the operation is assigned on a field then it is not readOnly
					hasWrite = true;
				}
				else if(!vbind.isField()) { //	if the operation isn't assigned on field then it is modifLocal
					isModifLocal = true;
				}
			}
		}
		super.preVisit(node);
	}
}

