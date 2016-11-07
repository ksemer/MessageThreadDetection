package processing.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Node class
 * @author ksemertz
 */
public class Node {
	// fields
	private int id;
	private Map<Integer, Edge> adjacency;
	private List<Integer> components;
	private Set<Message> messages;
	
	/**
	 * Constructor
	 * @param id
	 */
	public Node(int id) {
		this.id = id;
		this.adjacency = new HashMap<>();
		this.components = new ArrayList<>();
	}

	/**
	 * Return id
	 * @return
	 */
	public int getID() { return id; }
	
	public List<Integer> getComponentsIDs() { return components; }
	
	public void addComponent(int id) {  components.add(id); }

	/**
	 * Add edge
	 * @param node
	 * @param m
	 */
	public void addEdge(Node node, Message m) {
		Edge e;
		
		e = adjacency.get(node.id);
		
		if (e == null) {
			e = new Edge(node);
			
			adjacency.put(node.getID(), e);
		}
		
		e.getMessages().add(m);
	}

	/**
	 * Return all edges as a collection
	 * @return
	 */
	public Collection<Edge> getAdjacency() {
		return adjacency.values();
	}

	/**
	 * Return adjacency as ids
	 * @return
	 */
	public Set<Integer> getAdjacencyAsIDs() {
		if (adjacency == null)
			return new HashSet<Integer>();
		return adjacency.keySet();
	}

	/**
	 * Get messages
	 * @return
	 */
	public Set<Message> getMessages() {
		
		if (adjacency.isEmpty()) {
			
			if (messages == null)
				messages = new HashSet<>();
			
			return messages;
		}
		
		Set<Message> messages = new HashSet<>();

		for (Edge e : adjacency.values()) {
			messages.addAll(e.getMessages());
		}
		
		return messages;
	}
}