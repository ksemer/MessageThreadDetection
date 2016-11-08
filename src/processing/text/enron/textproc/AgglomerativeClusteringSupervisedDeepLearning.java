package processing.text.enron.textproc;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.collect.Table;

import edu.cmu.lti.ws4j.RelatednessCalculator;
import processing.text.deeplearning.DLClassification;
import processing.text.deeplearning.MLP;
import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.Word;


public class AgglomerativeClusteringSupervisedDeepLearning extends Clustering {

	/*
	 * private ArrayList<Email> train_emails; private ArrayList<String>
	 * features; private String classifierName; private int numInstSameThread;
	 * private int numInstDifferentThread;
	 */
	private DLClassification dlmodel;
	private ArrayList<Email> test_emails;
	private String distanceCalculation;
	private double threshold;
	private double lastSimilarity;
	private long maxTimeSpan;
	private Table<Integer, Integer, Double> score_AlreadyCalc;
	private String DATASET;
	private String expComment;
	private double nextPrintTH;

	public AgglomerativeClusteringSupervisedDeepLearning(ArrayList<Email> train_emails, ArrayList<Email> test_emails, double threshold,
			int numInstSameThread, int numInstDifferentThread, ArrayList<String> features,
			long randomSeed, String DATASET, String distanceCalculation, long maxTimeSpan, RelatednessCalculator wordnet, Table<Integer, Integer, Double> score_AlreadyCalc, String experimentComment,
			int n_hidden, int n_epochs, double learning_rate,String hidden_activation) {
		clusters = new ArrayList<>();	
		for (Email e : test_emails) {
			Cluster c=new Cluster();
			c.insertEmail(e);
			clusters.add(c);
		}
		this.distanceCalculation=distanceCalculation;
		this.threshold = threshold;
		this.maxTimeSpan=maxTimeSpan;
		this.test_emails=test_emails;
		this.DATASET=DATASET;
		this.expComment=experimentComment;
		dlmodel=new DLClassification(features, DATASET, wordnet);
		dlmodel.buildClassifier(train_emails, numInstSameThread, numInstDifferentThread, n_hidden, learning_rate, n_epochs, hidden_activation, randomSeed);
				
		this.score_AlreadyCalc=score_AlreadyCalc;
		
	}
		
	public AgglomerativeClusteringSupervisedDeepLearning(ArrayList<Email> test_emails, double threshold,
			MLP mlp, ArrayList<String> features,String DATASET, String distanceCalculation, long maxTimeSpan, RelatednessCalculator wordnet, Table<Integer, Integer, Double> score_AlreadyCalc, String experimentComment) {
		clusters = new ArrayList<>();	
		for (Email e : test_emails) {
			Cluster c=new Cluster();
			c.insertEmail(e);
			clusters.add(c);
		}
		this.distanceCalculation=distanceCalculation;
		this.threshold = threshold;
		this.maxTimeSpan=maxTimeSpan;
		this.test_emails=test_emails;
		this.DATASET=DATASET;
		this.expComment=experimentComment;
		this.dlmodel=new DLClassification(features, DATASET, wordnet);		
		this.dlmodel.setClassifier(mlp);

		this.score_AlreadyCalc=score_AlreadyCalc;
	}

	public ArrayList<Cluster> emailsClustering() {
		this.lastSimilarity=1.0;
		nextPrintTH=0.9;
		int[] idToMerge=chooseClusterToMerge();
		while(idToMerge!=null){
			Cluster ci=clusters.get(idToMerge[0]);
			Cluster cj=clusters.get(idToMerge[1]);
			Cluster newClusterMerged=new Cluster(ci.getEmails());
			newClusterMerged.insertEmails(cj.getEmails());
			clusters.remove(ci);
			clusters.remove(cj);
			clusters.add(newClusterMerged);
			/*if(clusters.size()%100==0){
				System.out.println("#clusters: "+clusters.size());
			}*/
			idToMerge=chooseClusterToMerge();
		}

		//this.writeClusters(DATASET+"_clusters_"+expComment+".txt");
				
		return clusters;
	}
	
	private int[] chooseClusterToMerge(){
		int [] cluster_id=new int[2];
		double simBest=threshold;
		for(int i=0;i<clusters.size()-1;i++){
			Cluster ci=clusters.get(i);
			for(int j=i+1;j<clusters.size();j++){
				Cluster cj=clusters.get(j);
				double dist=calcClustersSimilarity(ci,cj);
				if(dist==lastSimilarity){
					cluster_id[0]=i;
					cluster_id[1]=j;
					simBest=dist;
					break;
				}
				if(dist>=simBest){
					if(dist>simBest){
						cluster_id[0]=i;
						cluster_id[1]=j;
						simBest=dist;
					}
					else if(cluster_id!=null){
						if((ci.numEmails()+cj.numEmails())<(clusters.get(cluster_id[0]).numEmails()+clusters.get(cluster_id[1]).numEmails())){
							cluster_id[0]=i;
							cluster_id[1]=j;
						}							
					}
				}	
				
			}	
			if(simBest==lastSimilarity){
				break;
			}
		}	
		if(simBest==threshold)
			return null;
		if(lastSimilarity!=simBest){
			this.lastSimilarity=simBest;
			/*System.out.println(simBest);
			if(simBest<nextPrintTH){
				this.writeClusters(DATASET+"_clusters_atTH_"+nextPrintTH+"_"+expComment+".txt");
				nextPrintTH-=0.1;
			}*/
		}
		return cluster_id;
	}
	
	
	private double calcClustersSimilarity(Cluster c1, Cluster c2){
		double diff1=Math.abs((c1.getMaxTimestamp().getTime()-c2.getMinTimestamp().getTime()*1.0)/(1000*60*60*24));
		double diff2=Math.abs((c1.getMinTimestamp().getTime()-c2.getMaxTimestamp().getTime()*1.0)/(1000*60*60*24));
		if(diff1 > maxTimeSpan || diff2 > maxTimeSpan)
			return 0;
		double similarity=0;
		if(distanceCalculation.equals("centroid")){			
			Email centroid1=new Email();
			long sumTime1=0;
			for(Email e1 : c1.getEmails()){
				for(Word w : e1.getBow())
					centroid1.insertWord(w.getWord(),w.getOccurence(),w.getIdf(),w.isInSubject());
				centroid1.setText(centroid1.getText()+"\n"+e1.getText());
				centroid1.setSubject(centroid1.getSubject()+"\n"+e1.getSubject());
				sumTime1+=e1.getTimestamp().getTime();
				ArrayList<Long> users=centroid1.getIvolvedUsers();
				for(long u : e1.getIvolvedUsers()){
					if(!users.contains(u))
						users.add(u);
				}
			}
			sumTime1/=c1.numEmails();
			centroid1.setTimestamp(new Timestamp(sumTime1));
			
			double sumWeight = 0;
			for(Word w : centroid1.getBow()){
				w.setTf(1 + Math.log10(w.getOccurence()));
				double weight = w.getTf() * Math.log(w.getIdf());
				w.setWeight(weight);
				sumWeight += weight * weight;
			}
			// NORMALIZATION
			for (Word w : centroid1.getBow()) {
				w.setWeight(w.getWeight() / Math.sqrt(sumWeight));
			}
			
			Email centroid2=new Email();
			long sumTime2=0;
			for(Email e2 : c2.getEmails()){
				for(Word w : e2.getBow())
					centroid2.insertWord(w.getWord(),w.getOccurence(),w.getIdf(),w.isInSubject());
				centroid2.setText(centroid2.getText()+"\n"+e2.getText());
				centroid2.setSubject(centroid2.getSubject()+"\n"+e2.getSubject());
				sumTime2+=e2.getTimestamp().getTime();
				ArrayList<Long> users=centroid2.getIvolvedUsers();
				for(long u : e2.getIvolvedUsers()){
					if(!users.contains(u))
						users.add(u);
				}
			}
			sumTime2/=c2.numEmails();
			centroid2.setTimestamp(new Timestamp(sumTime2));
			sumWeight = 0;
			for(Word w : centroid2.getBow()){
				w.setTf(1 + Math.log10(w.getOccurence()));
				double weight = w.getTf() * Math.log(w.getIdf());
				w.setWeight(weight);
				sumWeight += weight * weight;
			}
			// NORMALIZATION
			for (Word w : centroid2.getBow()) {
				w.setWeight(w.getWeight() / Math.sqrt(sumWeight));
			}
			
			similarity=dlmodel.classifyEmailPair(centroid1, centroid2, test_emails)[1];		
		}
		else if(distanceCalculation.equals("average-link")){
			int count=0;
			for(Email e1 : c1.getEmails()){
				for(Email e2 : c2.getEmails()){
					int id1=e1.getId();
					int id2=e2.getId();
					if(id1>id2){
						id1=id2;
						id2=e1.getId();;
					}
					Double sim=score_AlreadyCalc.get(id1, id2);
					if(sim==null){
						sim=dlmodel.classifyEmailPair(e1, e2, test_emails)[1];
						score_AlreadyCalc.put(id1, id2, sim);
					}
					similarity+=sim;
					count++;
				}
			}
			return (similarity/count);
		}
		else if(distanceCalculation.equals("complete-link")){
			similarity=10000000;
			for(Email e1 : c1.getEmails()){
				for(Email e2 : c2.getEmails()){
					int id1=e1.getId();
					int id2=e2.getId();
					if(id1>id2){
						id1=id2;
						id2=e1.getId();;
					}
					Double sim=score_AlreadyCalc.get(id1, id2);
					if(sim==null){
						sim=dlmodel.classifyEmailPair(e1, e2, test_emails)[1];
						score_AlreadyCalc.put(id1, id2, sim);
					}
					if(sim<similarity){
						similarity=sim;
					}							
				}
			}
		}
		else{//if(distanceCalculation.equals("single-link")){
			for(Email e1 : c1.getEmails()){
				for(Email e2 : c2.getEmails()){
					int id1=e1.getId();
					int id2=e2.getId();
					if(id1>id2){
						id1=id2;
						id2=e1.getId();;
					}
					Double sim=score_AlreadyCalc.get(id1, id2);
					if(sim==null){
						sim=dlmodel.classifyEmailPair(e1, e2, test_emails)[1];
						score_AlreadyCalc.put(id1, id2, sim);
					}
					if(sim>similarity){
						similarity=sim;
					}						
				}
			}
		}		
		return similarity;
	}


	public void setThreshold(double threshold){
		this.threshold=threshold;
	}
	
	public void resetClusters(){
		this.lastSimilarity=1.0;
		clusters = new ArrayList<>();	
		for (Email e : test_emails) {
			Cluster c=new Cluster();
			c.insertEmail(e);
			clusters.add(c);
		}
	}
	
	public void writeClusters(String filename){
		try{
			RetrieveEmailQuotes eproc=new RetrieveEmailQuotes(new ArrayList<Email>());
			PrintWriter pw=new PrintWriter(filename);
			System.out.println("email_id\tsubject\tusers\ttimestamp\tcontent(first 4 sentences)"); 
			pw.println("email_id\tsubject\tusers\ttimestamp\tcontent(first 4 sentences)");
			for(Cluster cluster : clusters){
				 System.out.println("\n**************"); 
				 pw.println("\n**************"); 
				 for(Email e : cluster.getEmails()){
					 ArrayList<String> c=eproc.getEmailContentSentences(e);
					 String s="";
					 for(int i=0;i<c.size()&&i<3;i++){
						 s+=c.get(i)+" ";
					 }
					 System.out.println(e.getId()+"\t"+e.getSubject()+"\t"+e.getSender()+"->"+Arrays.toString(e.getReceiversid().toArray())+ "\t"+e.getTimestamp()+"\t"+s); 
					 pw.println(e.getId()+"\t"+e.getSubject()+"\t"+e.getSender()+"->"+Arrays.toString(e.getReceiversid().toArray())+ "\t"+e.getTimestamp()+"\t"+s); 
				 }
			}
			pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public MLP getClassifierModel(){
		return this.dlmodel.getClassifier();
	}
	
	public void setClassifier(MLP classifier){
		this.dlmodel.setClassifier(classifier);
	}
}
