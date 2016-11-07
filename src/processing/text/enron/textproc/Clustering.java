package processing.text.enron.textproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;

public abstract class Clustering {
	
	protected ArrayList<Cluster> clusters;
	
	public abstract ArrayList<Cluster> emailsClustering();
	
	public double evaluatePurity(){
		int purity_count=0, n=0;;
		for(Cluster cluster : clusters){
			HashMap<Integer, Double> cluster_map=new HashMap<>();
			for(Email e : cluster.getEmails()){
				n++;
				Double count=cluster_map.get(e.getThreadid());
				if(count==null)
					count=0.0;
				count++;
				cluster_map.put(e.getThreadid(),count);
			}
			ValueComparatorInt bvc =  new ValueComparatorInt(cluster_map);
	    	Map<Integer,Double> sortedMap = new TreeMap<Integer,Double>(bvc);
	    	sortedMap.putAll(cluster_map);
	    	int thread_id_dominant=0;
	    	for(Entry<Integer,Double> entry : sortedMap.entrySet()){
	    		thread_id_dominant=entry.getKey();
	    		break;
	    	}
	    	for(Email e : cluster.getEmails()){
	    		if(e.getThreadid()==thread_id_dominant)
	    			purity_count++;
	    	}
		}
		return (purity_count*1.0/n);
	}
	
	public int[] evaluateTPFNFPTN(){
		int[] measures=new int[4];
		int tp_fp=0, tp=0;
		int tn=0, fn=0;
		for(int i=0;i<clusters.size();i++){
			Cluster cluster=clusters.get(i);	
			tp_fp+=cluster.numEmails()*(cluster.numEmails()-1)/2;
			HashMap<Integer, Double> cluster_map=new HashMap<>();
			for(Email e : cluster.getEmails()){
				Double count=cluster_map.get(e.getThreadid());
				if(count==null)
					count=0.0;
				count++;
				cluster_map.put(e.getThreadid(),count);
			}
			for(Entry<Integer,Double> entry : cluster_map.entrySet()){
				if(entry.getValue()==1)
					continue;
				tp+=entry.getValue()*(entry.getValue()-1)/2;
			}
			
			for(Email ec : cluster.getEmails()){
				for(int j=0;j<clusters.size();j++){
					if(i==j)
						continue;
					for(Email e : clusters.get(j).getEmails()){
						if(ec.getThreadid()!=e.getThreadid())
							tn++;
						else 
							fn++;
					}
				}
			}		
		}
		int fp=tp_fp-tp;		
		tn=tn/2;
		fn=fn/2;
		measures[0]=tp;
		measures[1]=fn;
		measures[2]=fp;
		measures[3]=tn;
		return measures;	
		
	}
	
	public void setClusters(ArrayList<Cluster> clusters){
		this.clusters=clusters;
	}
	
	public ArrayList<Cluster> getClusters(){
		return this.clusters;
	}

}
