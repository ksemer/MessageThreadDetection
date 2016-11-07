package processing.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Edge class
 * @author ksemertz
 */
public class Edge {
	// fields
	private Node target;
	private List<Message> messages;

	/**
	 * Constructor
	 * @param n
	 */
	public Edge(Node n) {
		target = n;
		messages = new ArrayList<>();
	}
	
	/**
	 * Get target node
	 * @return
	 */
	public Node getTarget() { return target; }

	/**
	 * Sort the list of messages by time
	 */
	public void sortMessagesByTime() {
		// for java 8
//		messages.stream().sorted((object1, object2) -> object1.getTime().compareTo(object2.getTime()));

		Collections.sort(messages, new Comparator<Message>() {
			@Override
			public int compare(Message object1, Message object2) {
				return object1.getTime().compareTo(object2.getTime());
			}
		});
	}

	/**
	 * Return messages
	 * @return
	 */
	public List<Message> getMessages() {
		return messages;
	}
}