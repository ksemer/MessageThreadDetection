package processing.graph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.Gephi;

/**
 * ThreadTreeReconstruction
 * @author ksemertz
 */
public class ThreadTreeReconstruction {
	// threadID -> messageIDS
	private Map<Integer, Set<Integer>> mapMessagesToThread;
	// threadID -> nodeIDS
	private Map<Integer, Set<Integer>> mapNodesToThread;
	private Graph graph;
	private List<Map<Integer, List<Integer>>> mtotalGraph;
		
	/**
	 * Constructor 
	 * @param mapNodesToThread
	 * @param mapMessagesToThread
	 */
	public ThreadTreeReconstruction(Graph graph, Map<Integer, Set<Integer>> mapNodesToThread,
			Map<Integer, Set<Integer>> mapMessagesToThread) {
		this.graph = graph;
		this.mapMessagesToThread = mapMessagesToThread;
		this.mapNodesToThread = mapNodesToThread;
		this.mtotalGraph = new ArrayList<>();
	}

	/**
	 * Run method
	 * @throws IOException 
	 */
	public void run() throws IOException {
		Set<Integer> nodes, messages_set;
		int thread_id;
		FileWriter w = new FileWriter(graph.getName() + "_chronological_order");
		FileWriter w1 = new FileWriter(graph.getName() + "_conversation_tree");
		FileWriter w2 = new FileWriter(graph.getName() + "_conversation_tree.gdf");
		
		System.out.println("chronological file extraction started....");
		System.out.println("conversation_tree file extraction started....");

		// for each thread get nodes
		for(Map.Entry<Integer, Set<Integer>> mnt : mapNodesToThread.entrySet()) {
			
			thread_id = mnt.getKey();
			nodes = mnt.getValue();
			messages_set = mapMessagesToThread.get(thread_id);
			Set<Message> messages = new HashSet<Message>();

			// update thread messages structure by adding
			for (int n : nodes) {
				//FIXME
				if (graph.getName().contains("enron") && n > 148)
					continue;

				Node node = graph.getNode(n);
		
				// check if node's messages id belong to the thread
				for (Message m : node.getMessages()) {
					if (messages_set.contains(m.getID()))
						messages.add(m);
				}
			}
			
			w.write("thread_id: " + thread_id + "\n");
			w1.write("thread_id: " + thread_id + "\n");

			chronologicalOrder(messages, w);
			conversationTrees(messages, w1, w2);
		}
		
		w.close();
		System.out.println("chronological file extraction ended....");
		w1.close();
		System.out.println("conversation_tree file extraction ended....");
		System.out.println("Gephi: gdf file extraction started....");
		Gephi.createGDF(mtotalGraph, w2);
		w2.close();
		System.out.println("Gephi: gdf file extraction ended....");
	}

	/**
	 * Write a set of messages in a tree order
	 * @param messages 
	 * @param w
	 * @param w1
	 * @throws IOException 
	 */
	private void conversationTrees(Set<Message> messages, FileWriter w, FileWriter w1) throws IOException {
		List<Message> m_list = new ArrayList<>(messages);
		
		Collections.sort(m_list, new Comparator<Message>() {
			@Override
			public int compare(Message object1, Message object2) {
				return object1.getTime().compareTo(object2.getTime());
			}
		});
		
		Map<Integer, List<Integer>> m_graph = new HashMap<>();
		Set<Integer> recipients_id, recipients_id1;
		int sender_id, sender_id1;
		Message m, m1;
		
		for (int i = 0; i < m_list.size(); i++) {
			m = m_list.get(i);
			m_graph.put(m.getID(), new ArrayList<Integer>());
			
			sender_id = m.getSenderID();
			recipients_id = new HashSet<>(m.getRecipientsID());
						
			for (int j = i+1; j < m_list.size(); j++) {
				m1 = m_list.get(j);
				
				sender_id1 = m1.getSenderID();
				recipients_id1 = new HashSet<>(m1.getRecipientsID());
				
				// FIXME or due to global email addresses
				if (recipients_id.contains(sender_id1) || recipients_id1.contains(sender_id))
					m_graph.get(m.getID()).add(m1.getID());
			}
		}
		
		for (Map.Entry<Integer, List<Integer>> mg : m_graph.entrySet())
			w.write(mg.getValue() + " --> " + mg.getKey() + "\n");
		
		this.mtotalGraph.add(m_graph);
		
		w.write("-----------------------------------\n");
	}

	/**
	 * Write a set of messages in a chronological order
	 * @param messages
	 * @param w
	 * @throws IOException
	 */
	private void chronologicalOrder(Set<Message> messages, FileWriter w) throws IOException {
		List<Message> messages_list = new ArrayList<>(messages);
		
		Collections.sort(messages_list, new Comparator<Message>() {
			@Override
			public int compare(Message object1, Message object2) {
				return object1.getTime().compareTo(object2.getTime());
			}
		});
		
		for (int i = 0; i < messages_list.size(); i++) 
			w.write("email_id: " + messages_list.get(i).getID() + "\t time: " + messages_list.get(i).getTime() + "\n");

		w.write("-----------------------------------\n");
	}	
}