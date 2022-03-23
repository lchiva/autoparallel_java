package psc.java.autoparallel.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.ui.fix.AbstractMultiFix;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import psc.java.autoparallel.graph.AdjacencyList;
import psc.java.autoparallel.graph.DependencyNodes;
import psc.java.autoparallel.graph.GraphBuilder;
import psc.java.autoparallel.graph.JavaParserHelper;
import psc.java.autoparallel.graph.MethodGraph;


@SuppressWarnings("restriction")
public class Parallelizable extends AbstractMultiFix implements ICleanUp  {
	private static final String READ_ONLY = "ReadOnly";
	private static final String THREAD_SAFE = "ThreadSafe";
	private static final String NOT_PAR = "NotParallelizable";
	private CleanUpOptions fOptions;
	private RefactoringStatus fStatus;
	private HashMap<String, Set<String>> methodTag;

	
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
			
			List<CompilationUnit> parsedCu = JavaParserHelper.parseSources(project, compilationUnits, monitor);
			MethodGraph graph = GraphBuilder.collectGraph(parsedCu);

			//exportGraph(graph);
			try {
				graph.exportDot(project.getProject().getLocation().toFile().getCanonicalPath() + "/graph.dot");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*
			 * initialise the methodTag for each method
			 */
			initialiseMethodTag();
			/*
			 * Create an graph adjacent that store the invocation graph
			 */
			AdjacencyList graphAdjacent = new AdjacencyList(graph.getUseGraph().getGraph());
			/*
			 * cycle in invocation graph by calling a function findCycles()
			 */
			List<List<Integer>> cycle = graphAdjacent.findCycles(); 
			/*
			 * list of list of methods that create cycle in invocation graph
			 */
			List<List<MethodDeclaration>> cycleMethod = cycleMethodDeclaration(cycle, graph.getNodes()); 
			for(Integer i : graphAdjacent) {
				IMethodBinding bind = graph.getNodes().get(i);
				MethodDeclaration methodDecla =  graph.getNodes().get(bind);
				if(!inCycle(methodDecla,cycleMethod)) {
					MethodVisitor visit = new MethodVisitor(methodTag);
					methodDecla.accept(visit);
					if(visit.isNotParallelisable()) {
						methodTag.get(NOT_PAR).add(methodDecla.resolveBinding().getKey());
					}if(visit.isReadOnly()) {
						methodTag.get(READ_ONLY).add(methodDecla.resolveBinding().getKey());
					}if(visit.isThreadSafe() && visit.isReadOnly()) {
						methodTag.get(THREAD_SAFE).add(methodDecla.resolveBinding().getKey());
					}
				}
			}
			for (List<MethodDeclaration> methodDeclaration : cycleMethod) {
				for (MethodDeclaration method : methodDeclaration) {
					MethodVisitor visit = new MethodVisitor(methodTag);
					method.accept(visit);
					if(visit.isNotParallelisable()) {
						methodTag.get(NOT_PAR).add(method.resolveBinding().getKey());
					}if(visit.isReadOnly()) {
						methodTag.get(READ_ONLY).add(method.resolveBinding().getKey());
					}if(visit.isThreadSafe() && visit.isReadOnly()) {
						methodTag.get(THREAD_SAFE).add(method.resolveBinding().getKey());
					}
				}
			}
			System.out.println(methodTag);
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
		//add .parallel() to a stream
		
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				ITypeBinding resType = node.resolveTypeBinding();
				if (isStreamType(resType)) {
					if (! isStreamType(node.getExpression().resolveTypeBinding())) {
						// node = coll.stream(), expr = coll
//						rewriteOperations.add(new StreamToParallel(node));
					}
				}
//				IBinding binding = node.resolveMethodBinding();
//				if (!methodTag.get(NOT_PAR).contains(binding.getKey())) {
//					
//				}
				return true;
			}
			
		});

		if(rewriteOperations.isEmpty()) {
			return null;
		}
		else return new CompilationUnitRewriteOperationsFix("", cu,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}
	
	/*
	 * To check if the method call is stream type
	 */
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
	}
	
	/*
	 * check if the method is in cycle (got called by other functions)
	 */
	
	private boolean inCycle(MethodDeclaration node, List<List<MethodDeclaration>> cycleMeth) {
		for (List<MethodDeclaration> methodDeclar : cycleMeth) {
			for (MethodDeclaration methodDeclaration : methodDeclar) {
				if (methodDeclaration.resolveBinding().getKey().equals(node.resolveBinding().getKey()))return true;
			}
		}
		return false;
	}
	/*
	 * cycle of methodDeclaration
	 */
	
	private List<List<MethodDeclaration>> cycleMethodDeclaration(List<List<Integer>> cycle, DependencyNodes nodes){
		List<List<MethodDeclaration>> listlistMethod = new ArrayList<>();
		for (List<Integer> list : cycle) {
			List<MethodDeclaration> listMethod = new ArrayList<>();
			listlistMethod.add(listMethod);
			for (Integer i : list) {
				listMethod.add(nodes.get(nodes.get(i)));
			}
		}
		return listlistMethod;
	}

	/*
	 * initialise the methodTag for each method
	 */
	private void initialiseMethodTag() {
		methodTag = new HashMap<>();
		methodTag.put(READ_ONLY, new HashSet<>());
		methodTag.put(THREAD_SAFE, new HashSet<>());
		methodTag.put(NOT_PAR, new HashSet<>());
	}

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
