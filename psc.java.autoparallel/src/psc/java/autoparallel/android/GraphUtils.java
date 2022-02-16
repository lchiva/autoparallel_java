package psc.java.autoparallel.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GraphUtils {

	public static void collectPrefix(Set<Integer> safeNodes, MatrixCol graph) {
		// work with predecessor relationship
		MatrixCol tgraph = graph.transpose();
		collectSuffix(safeNodes, tgraph);
	}

	public static void collectSuffix(Set<Integer> safeNodes, MatrixCol graph) {
	
		Set<Integer> seen = new HashSet<>();
		List<Integer> todo = new ArrayList<>(safeNodes);
		while (! todo.isEmpty()) {
			List<Integer> next = new ArrayList<>();
			seen.addAll(todo);
			for (int n : todo) {
				SparseIntArray succ = graph.getColumn(n);
				for (int i=0; i < succ.size() ; i++) {
					int pre = succ.keyAt(i);
					if (seen.add(pre)) {
						next.add(pre);
					}
				}
			}
			todo = next;			
		}
		safeNodes.addAll(seen);
	}

}
