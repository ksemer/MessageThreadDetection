package processing.graph.algorithm;

import java.util.*;

import processing.graph.Graph;

/**
 * Finding all strongly connected components using Tarjan's algorithm.
 * @author ksemertz
 * Originally written by: Yancy Vance M. Paredes.
 */
public class SCCTarjan {

	private Graph graph;
	private boolean[] visited;
	private Stack<Integer> stack;
	private int time;
	private int[] lowlink;
	private List<List<Integer>> components;
	
	/**
	 * Constructor
	 * @param g
	 * @return
	 */
	public List<List<Integer>> scc(Graph g) {
		System.out.println("Components algorithm started....");
	    int n = g.maxID + 1;
	    this.graph = g;
	    visited = new boolean[n];
	    stack = new Stack<>();
	    time = 0;
	    lowlink = new int[n];
	    components = new ArrayList<>();

	    for (int u = 0; u < n; u++) {
	      if (!visited[u] && graph.getNode(u) != null)
	        dfs(u);
	    }
	    
		System.out.println("Components algorithm ended....");
	    return components;	
	}

	/**
	 * Start from each node as root 
	 * @param u
	 */
	private void dfs(int u) {
		lowlink[u] = time++;
		visited[u] = true;
		stack.add(u);
		boolean isComponentRoot = true;

		for (int v : graph.getNode(u).getAdjacencyAsIDs()) {
			if (!visited[v])
				dfs(v);

			if (lowlink[u] > lowlink[v]) {
				lowlink[u] = lowlink[v];
				isComponentRoot = false;
			}
		}

		if (isComponentRoot) {
			List<Integer> component = new ArrayList<>();
			while (true) {
				int x = stack.pop();
				component.add(x);
				lowlink[x] = Integer.MAX_VALUE;

				if (x == u)
					break;
			}
			components.add(component);
		}
	}
	
//	// Usage example
//	public static void main(String[] args) {
//		Graph g = new Graph();
//		for (int i = 0; i < 3; i++)
//			g.getCreateNode(i);
//
//		g.getNode(2).addEdge(g.getNode(0), 1, null);
//		g.getNode(2).addEdge(g.getNode(1), 1, null);
//		g.getNode(0).addEdge(g.getNode(1), 1, null);
//		g.getNode(1).addEdge(g.getNode(0), 1, null);
//
//		List<List<Integer>> components = new SCCTarjan().scc(g);
//		System.out.println(components);
//	}
}