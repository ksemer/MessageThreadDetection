package processing.text.enron.textproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.lti.ws4j.RelatednessCalculator;
import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;
import weka.classifiers.Classifier;

public class DBSCANSupervised extends Clustering {

	/*
	 * private ArrayList<Email> train_emails; private ArrayList<String>
	 * features; private String classifierName; private int numInstSameThread;
	 * private int numInstDifferentThread;
	 */
	private WekaClassification wekamodel;

	private ArrayList<Email> test_emails;
	private HashMap<Integer, Boolean> emails_visited;
	private HashMap<Integer, Boolean> emails_clusterized;
	private double threshold;
	private Table<Integer, Integer, Double> score_AlreadyCalc;

	public DBSCANSupervised(ArrayList<Email> train_emails, ArrayList<Email> test_emails, double threshold,
			String classifierName, int numInstSameThread, int numInstDifferentThread, ArrayList<String> features,
			long randomSeed, String DATASET, RelatednessCalculator wordnet, Table<Integer, Integer, Double> score_AlreadyCalc) {
		this.test_emails = test_emails;
		
		this.threshold = threshold;
		this.emails_visited = new HashMap<>();
		this.emails_clusterized = new HashMap<>();
		for (Email e : test_emails) {
			emails_visited.put(e.getId(), false);
			emails_clusterized.put(e.getId(), false);
		}
		clusters = new ArrayList<>();

		this.score_AlreadyCalc=score_AlreadyCalc;
		
		this.wekamodel = new WekaClassification(features, DATASET, wordnet);
		wekamodel.buildClassifier(train_emails, classifierName, numInstSameThread, numInstDifferentThread, randomSeed);
		
	}
	
	
	public DBSCANSupervised(ArrayList<Email> test_emails, double threshold,
			Classifier classifier, ArrayList<String> features,String DATASET, RelatednessCalculator wordnet, Table<Integer, Integer, Double> score_AlreadyCalc) {
this.test_emails = test_emails;
		
		this.threshold = threshold;
		this.emails_visited = new HashMap<>();
		this.emails_clusterized = new HashMap<>();
		for (Email e : test_emails) {
			emails_visited.put(e.getId(), false);
			emails_clusterized.put(e.getId(), false);
		}
		clusters = new ArrayList<>();

		this.score_AlreadyCalc=score_AlreadyCalc;
	}

	public ArrayList<Cluster> emailsClustering() {
		for (Email e : test_emails) {
			if (emails_visited.get(e.getId()))
				continue;
			emails_visited.put(e.getId(), true);
			ArrayList<Email> neighborhood = getNeighborhood(e);
			clusters.add(clusterCreation(e, neighborhood));
		}

		/*for(ArrayList<Email> cluster : clusters){
			System.out.println("\n**************"); 
			for(Email e : cluster)
				System.out.println(e.getThreadid()+"\t"+e.getId()+"\t"+e.getSubject()+"\t"+e.getTimestamp()); 
		}*/
		 
		return clusters;
	}

	private ArrayList<Email> getNeighborhood(Email e) {
		ArrayList<Email> neighborhood = new ArrayList<>();
		Map<Integer, Double> e_simmap = new HashMap<Integer, Double>();
		int ide=e.getId();
		for (int i = 0; i < test_emails.size(); i++) {			
			Email ei = test_emails.get(i);
			int ide1=ei.getId();
			Double sim=score_AlreadyCalc.get(ide, ide1);
			if(sim==null){
				sim=wekamodel.classifyEmailPair(e, ei, test_emails)[1];
				score_AlreadyCalc.put(ide, ide1, sim);
			}
			e_simmap.put(ei.getId(), sim);			
		}
		ValueComparatorInt bvc = new ValueComparatorInt(e_simmap);
		Map<Integer, Double> sortedPrediction = new TreeMap<Integer, Double>(bvc);
		sortedPrediction.putAll(e_simmap);
		for (Entry<Integer, Double> entry : sortedPrediction.entrySet()) {
			if (entry.getValue() <= threshold)
				break;
			for (Email ei : test_emails) {
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

	public void setThreshold(double threshold){
		this.threshold=threshold;
	}
	
	public void resetClusters(){
		this.emails_visited = new HashMap<>();
		this.emails_clusterized = new HashMap<>();
		for (Email e : test_emails) {
			emails_visited.put(e.getId(), false);
			emails_clusterized.put(e.getId(), false);
		}
		clusters = new ArrayList<>();
	}
	

	public Classifier getClassifierModel(){
		return this.wekamodel.getClassifier();
	}
	
	public void setClassifier(Classifier classifier){
		this.wekamodel.setClassifier(classifier);
	}
}
