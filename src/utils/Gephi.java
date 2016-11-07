package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import processing.graph.Edge;
import processing.graph.Graph;
import processing.graph.Node;

/**
 * Gephi class for gdf files construction
 * @author ksemertz
 */
public class Gephi {
	
	/**
	 * Creates gdf file for a graph
	 * each node is labeled with its id and thread_id
	 * each edge is labeled with a weight of 
	 * the communication frequency between two nodes
	 * @param graph
	 * @param mapThreadsToNode
	 * @throws IOException
	 */
	public static void createGDF(Graph graph, Map<Integer, Set<Integer>> mapThreadsToNode) throws IOException {
		System.out.println("Gephi: gdf file extraction started....");
		FileWriter w = new FileWriter(graph.getName() + ".gdf");
		
		w.write("nodedef> name,thread_id VARCHAR\n");
		String threads;

		for (Node n : graph.getNodes()) {
			if (graph.getName().contains("enron")) {
				//FIXME
				if (n.getID() < 149 && mapThreadsToNode.get(n.getID()) != null) {
//					System.out.println(n.getID() + "\t" + mapNodeToThreads.get(n.getID()));
					threads= "" + mapThreadsToNode.get(n.getID());
					threads = threads.replaceAll(",", "");
					w.write(n.getID() + "," + threads + "\n");
				}
			} else {
//				System.out.println(mapNodeToThreads.get(n.getID()));
				threads= "" + mapThreadsToNode.get(n.getID());
				threads = threads.replaceAll(",", "");
				w.write(n.getID() + "," + threads + "\n");
			}
		}
		
		w.write("edgedef> node1 INTEGER,node2 INTEGER,weight INTEGER\n");
		
		for (Node n : graph.getNodes()) {
			if (graph.getName().contains("enron"))
				//FIXME
				if (n.getID() > 148)
					continue;
			
			for (Edge e : n.getAdjacency()) {
				//FIXME
				if (e.getTarget().getID() > 148)
					continue;

				w.write(n.getID() + "," + e.getTarget().getID() + "," + e.getMessages().size() + "\n");
			}
		}
			w.close();
		System.out.println("Gephi: gdf file extraction ended....");
	}
	
	/**
	 * Creates gdf file for a graph
	 * each node is labeled with its id
	 * each edge is labeled with a weight of 
	 * the communication frequency between two nodes
	 * @param graph
	 * @throws IOException
	 */
	public static void createGDF(Graph graph) throws IOException {
		System.out.println("Gephi: gdf file extraction started....");
		FileWriter w = new FileWriter(graph.getName() + ".gdf");
		
		w.write("nodedef> name\n");
		
		for (Node n : graph.getNodes())
			w.write(n.getID() + "\n");
		
		w.write("edgedef> node1 INTEGER,node2 INTEGER,weight INTEGER\n");
		
		for (Node n : graph.getNodes())
			for (Edge e : n.getAdjacency())
				w.write(n.getID() + "," + e.getTarget().getID() + "," + e.getMessages().size() + "\n");

		w.close();
		System.out.println("Gephi: gdf file extraction ended....");
	}
	
	/**
	 * Creates gdf file for a graph from map
	 * each node is labeled with its id
	 * @param mgraph
	 * @param w
	 * @throws IOException
	 */
	public static void createGDF(List<Map<Integer, List<Integer>>> mgraph, FileWriter w) throws IOException {		
		w.write("nodedef> name\n");
		
		Set<Integer> nodes = new HashSet<>();
		
		for (int i = 0; i < mgraph.size(); i++) {
			nodes.addAll(mgraph.get(i).keySet());

			for (Map.Entry<Integer, List<Integer>> mg : mgraph.get(i).entrySet())
				for (int n : mg.getValue())
					nodes.add(n);
		}
				
		for (int n : nodes)
			w.write(n + "\n");
		
		w.write("edgedef> node1 INTEGER,node2 INTEGER\n");
		
		for (int i = 0; i < mgraph.size(); i++) {

			// neighbor must be source since it is given that way
			for (Map.Entry<Integer, List<Integer>> mg : mgraph.get(i).entrySet())
				for (int src : mg.getValue())
					w.write(src + "," + mg.getKey() + "\n");
		}
	}
}