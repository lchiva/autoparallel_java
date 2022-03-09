package psc.java.autoparallel.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.ui.fix.AbstractMultiFix;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import psc.java.autoparallel.graph.GraphBuilder;
import psc.java.autoparallel.graph.JavaParserHelper;
import psc.java.autoparallel.graph.MethodGraph;
import psc.java.autoparallel.handlers.CheckCondition;
import psc.java.autoparallel.handlers.StreamToParallel;


@SuppressWarnings("restriction")
public class Testing extends AbstractMultiFix implements ICleanUp  {
	private CleanUpOptions fOptions;
	private RefactoringStatus fStatus;
	List<CompilationUnit> parsedCu;
	List<MethodDeclaration> parallelizableList = new ArrayList<>();	
	List<MethodDeclaration> readOnlyList = new ArrayList<>();	
	List<MethodDeclaration> modifLocalList = new ArrayList<>();	
	List<MethodDeclaration> ThreadSafeList = new ArrayList<>();	
	
	/*
	 * Method for CleanUp
	 */
	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return fOptions.isEnabled("cleanup.graph_method");
	}
	/*
	 * Method for CleanUp
	 */
	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		try {
			if (fStatus == null || fStatus.isOK()) {
				return new RefactoringStatus();
			} else {
				return fStatus;
			}
		} finally {
			fStatus= null;
		}
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {
		if (fOptions.isEnabled("cleanup.graph_method")) { //$NON-NLS-1$
			fStatus= new RefactoringStatus();
			//this is to export graph
			parsedCu = JavaParserHelper.parseSources(project, compilationUnits,monitor);
			MethodGraph graph = GraphBuilder.collectGraph(parsedCu);
			
			//exportGraph(graph);
			try {
				graph.exportDot(project.getProject().getLocation().toFile().getCanonicalPath() + "/graph.dot");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//print CheckCondition: is parrallelizable or not.
			for (CompilationUnit cu : parsedCu) {
				CheckCondition checkcon = new CheckCondition();
				cu.accept(checkcon);
				for(MethodDeclaration med : checkcon.IsPara) {
					parallelizableList.add(med);
				}
				System.out.println("Parallelizable method: " + parallelizableList);
//				for(MethodDeclaration med : checkcon.ReadOnlyList) {
//					readOnlyList.add(med);
//				}
//				System.out.println("ReadOnly method: " + readOnlyList);
//				for(MethodDeclaration med : checkcon.ModifLocalList) {
//					modifLocalList.add(med);
//				}
//				System.out.println("ModifLocal method: " + modifLocalList);
			}
		}
		return new RefactoringStatus();
	}
	/*
	 * Method for CleanUp
	 */
	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}
	/*
	 * Method for CleanUp, to rewrite
	 */
	@Override
	protected ICleanUpFix createFix(CompilationUnit cu) throws CoreException {
		if(cu == null || !fOptions.isEnabled("cleanup.graph_method")) {return null;}
		List<CompilationUnitRewriteOperation> rewriteOperations = new ArrayList<>();
		
		/*
		 * Check if a parallelizable method is in class stream, we add .parallel() 
		 */
		
		cu.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node) {
				// x = coll.stream().filter().collect()		
				// look for method invocation with type Stream such that : type of expression is not Stream
				ITypeBinding resType = node.resolveTypeBinding();
				if (isStreamType(resType)) {
					if (! isStreamType(node.getExpression().resolveTypeBinding())) {
						// node = coll.stream(), expr = coll
						//TODO : determine if it is safe to do !
						if(false) {
							rewriteOperations.add(new StreamToParallel(node));
						}
					}
				}
				return true;
			}
		});
		
		if(rewriteOperations.isEmpty()) {
			return null;
		}
		else return new CompilationUnitRewriteOperationsFix("", cu,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}
	private boolean isStreamType(ITypeBinding resType) {
		// TODO : test subtype somehow ?
		// type.isSubTypeCompatible(streamType.);
		try {
			String qname = resType.getName().replaceAll("<.*", "");
			String qpkg = resType.getPackage().getName();
			return qname.equals("Stream") && qpkg.equals("java.util.stream");
		} catch (NullPointerException e) {
			return false;
		}
	};
	/*
	 * Method for CleanUp
	 */
	@Override
	public void setOptions(CleanUpOptions options) {
		Assert.isLegal(options != null);
		Assert.isTrue(fOptions == null);
		fOptions= options;
	}
}
