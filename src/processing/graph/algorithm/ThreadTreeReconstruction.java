package processing.graph.algorithm;

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

import processing.graph.Graph;
import processing.graph.Message;
import processing.graph.Node;
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
	// messageID -> threadID
	public Map<Integer, Integer> mapThreadToMessage;
	private Graph graph;
	private List<Map<Integer, List<Integer>>> mtotalGraph;
		
	/**
	 * Constructor 
	 * @param mapNodesToThread
	 * @param mapMessagesToThread
	 * @param mapThreadToMessage 
	 */
	public ThreadTreeReconstruction(Graph graph, Map<Integer, Set<Integer>> mapNodesToThread,
			Map<Integer, Set<Integer>> mapMessagesToThread, Map<Integer, Integer> mapThreadToMessage) {
		this.graph = graph;
		this.mapMessagesToThread = mapMessagesToThread;
		this.mapNodesToThread = mapNodesToThread;
		this.mapThreadToMessage = mapThreadToMessage;
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
				// for enron dataset
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
		
		// sort messages by time
		Collections.sort(m_list, new Comparator<Message>() {
			@Override
			public int compare(Message object1, Message object2) {
				return object1.getTime().compareTo(object2.getTime());
			}
		});

		Map<Integer, List<Integer>> m_graph = new HashMap<>();
		List<Integer> recipients_id, recipients_id1;
		int sender_id1;
		Message m, m1;
		boolean flag = false;
		int count = 0;
		
		for (int i = 1; i < m_list.size(); i++) {
			m = m_list.get(i);
			m_graph.put(m.getID(), new ArrayList<Integer>());
			
			recipients_id = m.getRecipientsID();
			System.out.println(recipients_id);
			flag = false;
			count = 0;
			
			while (!flag) {
				for (int j = i - 1; j >= 0; j--) {
					m1 = m_list.get(j);
					
					sender_id1 = m1.getSenderID();
					recipients_id1 = m1.getRecipientsID();
					
					if (count > 0)  {
						for (int k : recipients_id1) {
							if (recipients_id.contains(k)) {
								m_graph.get(m.getID()).add(m1.getID());
								flag = true;
								break;
							}
						}
						
						if (flag)
							break;
					} else {
						if (recipients_id.get(0).equals(sender_id1)) {
							System.out.println(sender_id1 + " , " + m.getID() + "-" + m1.getID());
							System.out.println(m.getSenderID() + "--" + m1.getSenderID());
							System.out.println("-----------");
							m_graph.get(m.getID()).add(m1.getID());
							flag = true;
							break;
						} else if (recipients_id.size() > 1 && recipients_id.get(1).equals(sender_id1)) {
							m_graph.get(m.getID()).add(m1.getID());
							flag = true;
							break;
						}
					}
				}
				
				if (count == 2)
					break;
				
				count++;
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