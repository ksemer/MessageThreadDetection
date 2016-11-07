package processing.graph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import processing.graph.algorithm.SCCTarjan;
import processing.graph.algorithm.ThreadTreeReconstruction;
import utils.Gephi;

public class Enron {
	/***********************************************/
	// Graph instance
	private static Graph graph;
	// threadID -> messageIDS
	private static Map<Integer, Set<Integer>> mapMessagesToThread;
	// messageID -> threadID
	private static Map<Integer, Integer> mapThreadToMessage;
	// threadID -> nodeIDS
	private static Map<Integer, Set<Integer>> mapNodesToThread;
	// nodeID -> threadIDS
	private static Map<Integer, Set<Integer>> mapThreadsToNode;
	
	// graph direction
	private static boolean DIRECTED = true;
	
	// gephi visualisation
	private static boolean VISUALISATION = false;

	private static boolean COMPONENTS_RUN = false;
	
	private static boolean THREAD_TREE_RECONSTRUCTION = true;
	
	/***********************************************/

	/**
	 * Main
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("Enron graph processing started");

		DBCommunication dbc = new DBCommunication("enron_threads", DIRECTED);
		
		// get connections
		graph = dbc.getGraph();

		mapMessagesToThread = dbc.getMessagesToThread();
		mapThreadToMessage = dbc.getThreadToMessage();
		mapNodesToThread = dbc.getNodesToThread();
		mapThreadsToNode = dbc.getThreadsToNode();

		if (COMPONENTS_RUN)
			componentsResult();
		
		if (VISUALISATION)
			Gephi.createGDF(graph, mapThreadsToNode);
		
		if (THREAD_TREE_RECONSTRUCTION)
			new ThreadTreeReconstruction(graph, mapNodesToThread, mapMessagesToThread, mapThreadToMessage).run();
	}
	
	/**
	 * Generates for each thread the nodes and their components
	 * @throws IOException
	 */
	private static void componentsResult() throws IOException {
		FileWriter w = new FileWriter(graph.getName() + "_components_result");
		
		List<List<Integer>> components = new SCCTarjan().scc(graph);
		
		for (int i = 0; i < components.size(); i++) {
			for (int j = 0; j < components.get(i).size(); j++) {
				 graph.getNode(components.get(i).get(j)).addComponent(i);
			}
		}
		
		w.write("thread: id | node_id ->[components],....,node_id ->[components]");
		for (int t : mapNodesToThread.keySet()) {
			w.write("thread: " + t + " | ");
			
			String threads = "";
			for (int n : mapNodesToThread.get(t))
				threads+= n + "->" + graph.getNode(n).getComponentsIDs() + " ";
			w.write(threads + "\n");
		}
		
		//TODO
//		for (int i = 0; i < components.size(); i++) {
//			String threads = "";
//			Set<Integer> mes = new HashSet<>();
//
//			for (int j = 0; j < components.get(i).size(); j++) {
//				// node id
//				threads+= components.get(i).get(j) + "->";
//
//				for (Message m : graph.getNode(components.get(i).get(j)).getMessages())
//					mes.add(Main.mapThreadToMessage.get(m.getID()));
//				
//				for (int mid : mes)
//					threads+= mid + ",";
//				threads+= " ";
//			}
//			threads = threads.substring(0, threads.length()-2);
//			System.out.print("thread ids: "  + threads + " | component: " + components.get(i) + "\n");
//		}
		w.close();
	}
}