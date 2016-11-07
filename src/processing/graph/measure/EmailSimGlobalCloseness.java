package processing.graph.measure;

import java.util.HashSet;
import java.util.Set;

import processing.text.enron.model.Email;

/**
 * EmailSimGlobalCloseness Sim_socialnetwork(e1, e2) =
 * SUM(NeighbourhoodOverlap(users of e1 and e2) / 2log|e1 users union e2 users|
 * @author ksemertz
 */
public class EmailSimGlobalCloseness {
	private NeighbourhoodOverlap nO;
	
	/**
	 * Constructor
	 * @param dataset_name
	 */
	public EmailSimGlobalCloseness(String dataset_name) {
		//System.out.println("EmailSimGlobalCloseness is running...");
		nO = new  NeighbourhoodOverlap(dataset_name);
	}
	
	/**
	 * Returns the similarity of two emails regarding the
	 * global closeness of their participants
	 * @param e1
	 * @param e2
	 * @return
	 */
	public double getSimilarity(Email e1, Email e2) {
		Set<Long> s_e1 = new HashSet<>(e1.getReceiversid()), s_e2 = new HashSet<>(e2.getReceiversid()), union = new HashSet<>();
		s_e1.add(e1.getSender());
		s_e2.add(e2.getSender());
		
		double sum = 0;
		
		for (long p1 : s_e1) {
			for (long p2 : s_e2) {
				sum+= this.nO.getCloseness(p1, p2);
			}
		}
		
		union.addAll(s_e1);
		union.addAll(s_e2);
		
		return sum / 2 * Math.log10((double) union.size());
	}
}