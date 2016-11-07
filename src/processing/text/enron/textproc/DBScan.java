package processing.text.enron.textproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.common.collect.Table;

import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;

public class DBScan extends Clustering {

	private ArrayList<Email> emails;
	private HashMap<Integer, Boolean> emails_visited;
	private HashMap<Integer, Boolean> emails_clusterized;
	private Table<Integer, Integer, Double> score_table;
	private double threshold;

	public DBScan(ArrayList<Email> emails, Table<Integer, Integer, Double> score_table, double threshold) {
		this.emails_visited = new HashMap<>();
		this.emails_clusterized = new HashMap<>();
		for (Email e : emails) {
			emails_visited.put(e.getId(), false);
			emails_clusterized.put(e.getId(), false);
		}
		this.emails = emails;
		this.score_table = score_table;
		clusters = new ArrayList<>();
		this.threshold = threshold;
	}

	public ArrayList<Cluster> emailsClustering() {
		for (Email e : emails) {
			if (emails_visited.get(e.getId()))
				continue;
			emails_visited.put(e.getId(), true);
			ArrayList<Email> neighborhood = getNeighborhood(e);
			clusters.add(clusterCreation(e, neighborhood));
		}

		/*
		 * for(ArrayList<Email> cluster : clusters){
		 * System.out.println("\n**************"); for(Email e : cluster)
		 * System.out.println(e.getThreadid()+"\t"+e.getId()+"\t"+e.getSubject()
		 * +"\t"+e.getTimestamp()); }
		 */

		return clusters;
	}

	private ArrayList<Email> getNeighborhood(Email e) {
		ArrayList<Email> neighborhood = new ArrayList<>();		
		Map<Integer, Double> e_simmap = score_table.columnMap().get(e.getId());
		ValueComparatorInt bvc = new ValueComparatorInt(e_simmap);
		Map<Integer, Double> sortedPrediction = new TreeMap<Integer, Double>(bvc);
		sortedPrediction.putAll(e_simmap);
		for (Entry<Integer, Double> entry : sortedPrediction.entrySet()) {
			if (entry.getValue() <= threshold)
				break;
			for (Email ei : emails) {
				if (ei.getId() == entry.getKey()) {
					neighborhood.add(ei);
					break;
				}
			}
		}
		return neighborhood;
	}

	private Cluster clusterCreation(Email e, ArrayList<Email> neighborhood) {
		Cluster cluster = new Cluster();
		cluster.insertEmail(e);
		emails_clusterized.put(e.getId(), true);
		while (neighborhood.size() > 0) {
			// for(Email neig : neighborhood){
			int i = 0;
			Email neig = neighborhood.get(i);
			while (emails_visited.get(neig.getId()) && i < neighborhood.size() - 1) {
				i++;
				neig = neighborhood.get(i);
			}
			if (emails_visited.get(neig.getId())) {
				break;
			}
			emails_visited.put(neig.getId(), true);
			neighborhood.remove(neig);
			neighborhood.addAll(this.getNeighborhood(neig));
			if (!emails_clusterized.get(neig.getId()))
				cluster.insertEmail(neig);
		}
		return cluster;
	}

}
