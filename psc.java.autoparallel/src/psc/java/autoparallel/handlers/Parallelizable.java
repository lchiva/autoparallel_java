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
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
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
public class Parallelizable extends AbstractMultiFix implements ICleanUp {
	// Keyword for methodTag that store readOnly method
	private static final String READ_ONLY = "ReadOnly";
	// Keyword for methodTag that store threadSafe method
	private static final String THREAD_SAFE = "ThreadSafe";
	// Keyword for methodTag that store NotParallelisable method
	private static final String NOT_PAR = "NotParallelizable";

	private CleanUpOptions fOptions;
	private RefactoringStatus fStatus;

	// Map that store the set of method and its corresponding methodTag
	public HashMap<String, Set<String>> methodTag;

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
			fStatus = null;
		}
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {
		if (fOptions.isEnabled("cleanup.graph_method")) { //$NON-NLS-1$
			fStatus = new RefactoringStatus();
			// list of compilation unit that parsed the argument in
			List<CompilationUnit> parsedCu = JavaParserHelper.parseSources(project, compilationUnits, monitor);
			// invocation graph
			MethodGraph graph = GraphBuilder.collectGraph(parsedCu);

			// exportGraph(graph);
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
			for (Integer i : graphAdjacent) { // iteration on all node(method)
				IMethodBinding bind = graph.getNodes().get(i);
				MethodDeclaration methodDecla = graph.getNodes().get(bind); // get method declaration correspond to
																			// index i
				// we treat the case that method doesn't create cycle
				if (!inCycle(methodDecla, cycleMethod)) { // verify if this method doesnt create cycle
					MethodVisitor visit = new MethodVisitor(methodTag); // call constructor in MethodVisitor
																		// this give methodTag to class MethodVisitor.
					methodDecla.accept(visit);
					// if visit is readonly we add that method in list of method that has tag
					// READ_ONLY
					if (visit.isReadOnly()) {
						methodTag.get(READ_ONLY).add(methodDecla.resolveBinding().getKey());
					}
					// if visit is threadSafe and is readOnly we add that method in list of method
					// that has tag THREAD_SAFE
					if (Flags.isSynchronized(methodDecla.getModifiers())
							|| (visit.isThreadSafe() && visit.isReadOnly())) {
						methodTag.get(THREAD_SAFE).add(methodDecla.resolveBinding().getKey());
					}
					// if visit is notPara we add that method in list of method that has tag NOT_PAR
					if (visit.isNotParallelisable()) {
						methodTag.get(NOT_PAR).add(methodDecla.resolveBinding().getKey());
					}
					// if visit is not readonly or not threadSafe, we add that method in list of
					// method that has tag NOT_PAR
					if (!visit.isThreadSafe()) {
						// !Flags.isSync..... is for verifier if the modifier consist synchronized
						// keyword
						// check if there is synchronized modifier
						if (!visit.isReadOnly() || !Flags.isSynchronized(methodDecla.getModifiers())) {
							methodTag.get(NOT_PAR).add(methodDecla.resolveBinding().getKey());
						}
					}
				}
			}
			// we treat the case that method create cycle(it calls other method).
			for (List<MethodDeclaration> methodDeclaration : cycleMethod) {
				for (MethodDeclaration method : methodDeclaration) {
					MethodVisitor visit = new MethodVisitor(methodTag);
					method.accept(visit);
					if (visit.isReadOnly()) {
						methodTag.get(READ_ONLY).add(method.resolveBinding().getKey());
					}
					if (Flags.isSynchronized(method.getModifiers()) || (visit.isThreadSafe() && visit.isReadOnly())) {
						methodTag.get(THREAD_SAFE).add(method.resolveBinding().getKey());
					}
					if (visit.isNotParallelisable()) {
						methodTag.get(NOT_PAR).add(method.resolveBinding().getKey());
					}
					if (!visit.isThreadSafe()) {
						if (!visit.isReadOnly() || !Flags.isSynchronized(method.getModifiers())) {
							methodTag.get(NOT_PAR).add(method.resolveBinding().getKey());
						}
					}
				}
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
		if (cu == null || !fOptions.isEnabled("cleanup.graph_method")) {
			return null;
		}
		List<CompilationUnitRewriteOperation> rewriteOperations = new ArrayList<>();

		// add .parallel() to a stream
		cu.accept(new ASTVisitor() {
			// stateful operation
			private boolean hasSIOs = false;
			// side effect
			private boolean sideEffect = false;
			// reduction ordered matter
			private boolean roms = true;
			// treated = already study
			private boolean treated = true;
			// ordered of stream
			private boolean unordered = false;

			@Override
			public boolean visit(MethodInvocation node) {

				IBinding binding = node.resolveMethodBinding();
				// if stream is treated we set everything to default and we study another stream
				if (treated) {
					hasSIOs = false;
					sideEffect = false;
					roms = true;
					unordered = false;
				}
				// we analyse a stream by beginning with terminal operation
				if (binding.getKey().contains(".forEach") || binding.getKey().contains(".collect")
						|| binding.getKey().contains(".reduce")) {
					treated = false;
					// TreeSet preserve ordered
					if (binding.getKey().contains(".collect") && binding.getKey().contains("Ljava/util/TreeSet")) {
						roms = true;
					}
					// HashSet doesn't preserve ordered
					if (binding.getKey().contains(".collect") && binding.getKey().contains("Ljava/util/HashSet")) {
						roms = false;
					}
					if (binding.getKey().contains(".collect") && binding.getKey().contains("Ljava/util/Set")) {
						roms = false;
					}
				}
				if (binding.getKey().contains(".sum") || binding.getKey().contains(".allMatch")
						|| binding.getKey().contains(".anyMatch") || binding.getKey().contains("nonMatch")) {
					treated = false;
				}
				// check if the reduction ordered matter
				// ref : https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html
				// two operations that return void and scalar which ROMs = true
				if (binding.getKey().contains("Ljava/util/Optional")) {
					if (binding.getKey().contains(".findFirst()") || binding.getKey().contains(".forEachOrdered()")) {
						roms = true;
					} else {
						roms = false;
					}
					// Here we begin the analysis of the stream pipeline, Ljava/util/Optional :
					// ifPresent, etc.
					treated = false;
				}
				// Check if there exists a SIOs in the pipeline
				// reference:
				// https://www.oreilly.com/library/view/introduction-to-programming/9781788839129/50f54a6f-dd25-40bc-89d2-31b73d95b6b7.xhtml
				if (binding.getKey().contains(".reduce") || binding.getKey().contains(".sorted")
						|| binding.getKey().contains(".limit") || binding.getKey().contains(".skip")
						|| binding.getKey().contains(".distinct")) {
					hasSIOs = true;
					if(unordered){
						unordered = false;
					}
				}
				// Study on each lambda expression
				if (binding.getKey().contains("Ljava/util/function/")) {
					// Represents a function that accepts a valued argument and produces a result.
					if (binding.getKey().contains("Ljava/util/function/") && binding.getKey().contains("Function")) {
						// we check if it calls Function, except in map
						if (binding.getKey().contains(".map")) {
							
							//we check if there is a lambda expression exist in .map()
							if (node.arguments().get(0) instanceof LambdaExpression) {
								ASTNode ast = ((LambdaExpression) node.arguments().get(0)).getBody();
								//visit the body after the lambda expression
								ast.accept(new ASTVisitor() {
									@Override
									public boolean visit(MethodInvocation node) {
										if (methodTag.get(NOT_PAR).contains(node.resolveMethodBinding().getKey())) {
											sideEffect = true;
										}
										if (callNotParallelisableMethod(node.resolveMethodBinding().getKey())) {
											sideEffect = true;
										}
										return true;
									}

									@Override
									public boolean visit(Assignment node) {
										AssignVisitor av = new AssignVisitor();
										node.getLeftHandSide().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}

									@Override
									public boolean visit(PostfixExpression node) {
										System.out.println(node);
										AssignVisitor av = new AssignVisitor();
										node.getOperand().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}

									@Override
									public boolean visit(PrefixExpression node) {
										System.out.println(node);
										AssignVisitor av = new AssignVisitor();
										node.getOperand().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}
								});
							} 
							//check if there is a method reference
							else {
								MethodReference method = ((MethodReference) node.arguments().get(0));
								if (methodTag.get(NOT_PAR).contains(method.resolveMethodBinding().getKey())) {
									sideEffect = true;
								}
								if (callNotParallelisableMethod(node.resolveMethodBinding().getKey())) {
									sideEffect = true;
								}
							}
						}
					}
					// check if lambda expression produces side effect, for example .add(),
					// system.out.println
					if (binding.getKey().contains("Consumer")) {
						if (binding.getKey().contains(".forEach") || binding.getKey().contains(".peek")) {
							if (node.arguments().get(0) instanceof LambdaExpression) {
								ASTNode ast = ((LambdaExpression) node.arguments().get(0)).getBody();
								
								ast.accept(new ASTVisitor() {
									@Override
									public boolean visit(MethodInvocation node) {
										System.out.println(node);
										if(node.toString().contains("System.out.println")) {
											sideEffect = true;
										}
										if (methodTag.get(NOT_PAR).contains(node.resolveMethodBinding().getKey())) {
											sideEffect = true;
										}
										if (callNotParallelisableMethod(node.resolveMethodBinding().getKey())) {
											sideEffect = true;
										}
										return true;
									}
									@Override
									public boolean visit(Assignment node) {
										AssignVisitor av = new AssignVisitor();
										node.getLeftHandSide().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}
									@Override
									public boolean visit(PostfixExpression node) {
										System.out.println(node);
										AssignVisitor av = new AssignVisitor();
										node.getOperand().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}

									@Override
									public boolean visit(PrefixExpression node) {
										System.out.println(node);
										AssignVisitor av = new AssignVisitor();
										node.getOperand().accept(av);
										if (av.hasWrite) {
											sideEffect = true;
										}
										return true;
									}
								});
							} else {
								sideEffect = true;
							}
						}
					}
				}
				// check if stream contains .unordered(), sert à verifier la 3eme propriété
				if (binding.getKey().contains(".unordered")) {
					unordered = true;
				}
				// check if stream already run in parallel, if it is, then we stop the analysis
				// by assigning treated = true;
				if (binding.getKey().contains(".parallel")) {
					treated = true;
				}
				// if the stream is not yet studied
				if (!treated) {
					// focus on the methodinvocation that contains .stream .i.e set.stream()
					if (binding.getKey().contains(".stream")) {
						String str = node.getExpression().getRoot().toString();
						String str1 = node.getExpression().toString();
						//analyse the string after the " = new"
						if (str.contains(node.getExpression().resolveTypeBinding().getName() + " " + str1)) {
							int indexStart = str.indexOf(node.getExpression().resolveTypeBinding().getName() + " " + str1);
							int indexEnd = 0;
							for(int i = indexStart ; i < str.length();i++) {
								if(str.charAt(i) == ';') {
									indexEnd = i;
									break;
								}
							}
							String analyseStr = str.substring(indexStart, indexEnd);
							
							if (analyseStr.contains("PriorityQueue<>()") || analyseStr.contains("HashSet<>")) {
								if (!sideEffect) {
									rewriteOperations.add(new StreamToParallel(node));
									treated = true;
								}
							}
						}
						// check if the stream is Ordered
						if (binding.getKey().contains("Ljava/util/Arrays")) {
							// P3
							if (!sideEffect) {
								if (hasSIOs) {
									if (!roms) {
										if (unordered) {
											rewriteOperations.add(new StreamToParallel(node));
											treated = true;
										}
									}
								}
								// P1 and //P2
								else {
									rewriteOperations.add(new StreamToParallel(node));
									treated = true;
								}
							}
						} else if (binding.getKey().contains("Ljava/util/Collection")) {
							// Check ordered or undordered
							if (!sideEffect) {
								if (hasSIOs) {
									if (!roms) {
										if (unordered) {
											rewriteOperations.add(new StreamToParallel(node));
											treated = true;
										}
									}
								} else {
									rewriteOperations.add(new StreamToParallel(node));
									treated = true;
								}
							}
						}
						treated = true;
					}
					if (binding.getKey().contains("Ljava/util/stream/Stream<>;")) {
						if (binding.getKey().contains(".of") || binding.getKey().contains(".range")
								|| binding.getKey().contains("iterate")) {
							if (!sideEffect) {
								if (hasSIOs) {
									if (!roms) {
										if (unordered) {
											rewriteOperations.add(new StreamToParallel(node));
											treated = true;
										}
									}
								} else {
									rewriteOperations.add(new StreamToParallel(node));
									treated = true;
								}
							}
							treated = true;
						}
						//ordered stream from creation Stream.generate
						else if(binding.getKey().contains(".generate")) {
							if (!sideEffect) {
								rewriteOperations.add(new StreamToParallel(node));
								treated = true;
							}
						}
					}
				}
				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		} else
			return new CompilationUnitRewriteOperationsFix("", cu,
					rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	/*
	 * check if the method is in cycle (got called by other functions)
	 */
	private boolean inCycle(MethodDeclaration node, List<List<MethodDeclaration>> cycleMeth) {
		for (List<MethodDeclaration> methodDeclar : cycleMeth) {
			for (MethodDeclaration methodDeclaration : methodDeclar) {
				if (methodDeclaration.resolveBinding().getKey().equals(node.resolveBinding().getKey()))
					return true;
			}
		}
		return false;
	}
	/*
	 * cycle of methodDeclaration
	 */

	private List<List<MethodDeclaration>> cycleMethodDeclaration(List<List<Integer>> cycle, DependencyNodes nodes) {
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
		fOptions = options;
	}

	private boolean callNotParallelisableMethod(String str) {
		if (str.contains("Ljava/util")) {
			{
				if (str.contains(".add") || str.contains(".clear") || str.contains(".remove")
						|| str.contains(".removeAll") || str.contains(".addAll") || str.contains(".removeIf")
						|| str.contains(".replaceAll") || str.contains(".set") || str.contains(".sort")) {
					return true;
				}
			}
		}
		return false;
	}
}