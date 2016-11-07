package processing.text.enron.textproc;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Table;

import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.Word;

public class AgglomerativeClustering extends Clustering {
	
	private EmailsComparator comparator;
	private ArrayList<Email> emails;
	private Table<Integer, Integer, Double> score_table;
	private String distanceCalculation;
	private double threshold;
	private double lastSimilarity;
	private long maxTimeSpan;
	
	public AgglomerativeClustering(ArrayList<Email> emails, Table<Integer, Integer, Double> score_table, double threshold,String distanceCalculation, EmailsComparator comparator, long maxTimeSpan) {
		clusters = new ArrayList<>();	
		for (Email e : emails) {
			Cluster c=new Cluster();
			c.insertEmail(e);
			clusters.add(c);
		}		
		this.maxTimeSpan=maxTimeSpan;
		this.emails = emails;
		this.score_table = score_table;
		this.distanceCalculation=distanceCalculation;
		this.threshold = threshold;
		this.comparator=comparator;
	}
	
	public ArrayList<Cluster> emailsClustering() {
		this.lastSimilarity=1.0;
		int[] idToMerge=chooseClusterToMerge();
		while(idToMerge!=null){
			Cluster ci=clusters.get(idToMerge[0]);
			Cluster cj=clusters.get(idToMerge[1]);
			Cluster newClusterMerged=new Cluster(ci.getEmails());
			newClusterMerged.insertEmails(cj.getEmails());
			clusters.remove(ci);
			clusters.remove(cj);
			clusters.add(newClusterMerged);
			idToMerge=chooseClusterToMerge();
		}
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
				if(dist>simBest){
					cluster_id[0]=i;
					cluster_id[1]=j;
					simBest=dist;
				}	
				if(dist==simBest && cluster_id!=null){
					if((ci.numEmails()+cj.numEmails())<(clusters.get(cluster_id[0]).numEmails()+clusters.get(cluster_id[1]).numEmails())){
						cluster_id[0]=i;
						cluster_id[1]=j;
					}							
				}
			}	
			if(simBest==lastSimilarity){
				break;
			}
		}	
		if(simBest==threshold)
			return null;
		this.lastSimilarity=simBest;
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
			
			double simcos_text = comparator.compareContent_CosineSimilarity(centroid1, centroid2);
			double simcos_subj = comparator.compareContent_SubjectCosineSimilarity(centroid1, centroid2);
			double jacc_users = comparator.comparePeople_UsersJaccardSimilarity(centroid1, centroid2);
			double closeness_users = comparator.comparePeople_UsersClosenessSimilarity(centroid1, centroid2);
			double time_dist = comparator.compareTime_logDistInDays(centroid1, centroid2);
			return (1 + simcos_text) * (1 + time_dist)*(1 + jacc_users);

		
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
					try{
						double sim=score_table.get(id1, id2);
						similarity+=sim;
						count++;
					}catch(Exception e){
						System.err.println("ERRORE PER ID: "+id1+" "+id2);
						e.printStackTrace();
						System.exit(0);
					}		
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
					try{
						double sim=score_table.get(id1, id2);
						if(sim<similarity){
							similarity=sim;
						}	
					}catch(Exception e){
						System.err.println("ERRORE PER ID: "+id1+" "+id2);
						e.printStackTrace();
						System.exit(0);
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
					try{
						double sim=score_table.get(id1, id2);
						if(sim>similarity){
							similarity=sim;
						}	
					}catch(Exception e){
						System.err.println("ERRORE PER ID: "+id1+" "+id2);
						e.printStackTrace();
						System.exit(0);
					}		
				}
			}
		}		
		return similarity;
	}

}
