package processing.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Graph class
 * @author ksemertz
 */
public class Graph {
	// fields
	private String name;
	private Map<Integer, Node> nodes;
	public int maxID;
	
	/**
	 * Constructor
	 * @param name 
	 */
	public Graph(String name) {
		this.name = name;
		nodes = new HashMap<>();
	}
	
	/**
	 * Creates a node if it doesn't exist
	 * Returns the node object
	 * @param id
	 * @return
	 */
	public Node getCreateNode(int id) {
		Node n = nodes.get(id);
		
		if (maxID < id)
			maxID = id;
		
		if (n == null) {
			n = new Node(id);
			nodes.put(id, n);
		}
		
		return n;
	}

	/**
	 * Add edge to n1
	 * @param n1
	 * @param n2
	 * @param m
	 */
	public void addEdge(int n1, int n2, Message m) {
		nodes.get(n1).addEdge(nodes.get(n2), m);
	}

	/**
	 * Return nodes as a collection
	 * @return
	 */
	public Collection<Node> getNodes() {
		return nodes.values();
	}

	/**
	 * Return size of graph
	 * @return
	 */
	public int size() { return nodes.size(); }

	/**
	 * Sort times in edges
	 */
	public void sortTimes() {
		// for all nodes
		for (Node n : getNodes()) {
			// for all edges
			for (Edge e : n.getAdjacency())
				e.sortMessagesByTime();
		}		
	}

	/**
	 * Return node object with given id
	 * @param id
	 * @return
	 */
	public Node getNode(int id) {
		return nodes.get(id);
	}
	
	/**
	 * Return graph name
	 * @return
	 */
	public String getName() {
		return name;
	}
}