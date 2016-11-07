package processing.text.enron.model;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Cluster {
	
	private int id;
	private ArrayList<Email> emails;
	private Timestamp minTime;
	private Timestamp maxTime;
	//private ArrayList<Integer> involvedUsers;
	
	public Cluster() {
		emails=new ArrayList<>();
		//involvedUsers=new ArrayList<>();
		minTime=new Timestamp(Long.MAX_VALUE);
		maxTime=new Timestamp(Long.MIN_VALUE);
	}
	
	public Cluster(ArrayList<Email> emails) {
		this.emails=new ArrayList<>(emails);
		//involvedUsers=new ArrayList<>();
		minTime=new Timestamp(Long.MAX_VALUE);
		maxTime=new Timestamp(Long.MIN_VALUE);
		for(Email e : emails){
			if(e.getTimestamp().after(maxTime) ){
				maxTime=e.getTimestamp();
			}
			/*if(e.getTimestamp().before(minTime)){
				minTime=e.getTimestamp();
			}
			for(int u : e.getIvolvedUsers()){
				if(!involvedUsers.contains(u))
					involvedUsers.add(u);
			}*/
		}
	}

	public ArrayList<Email> getEmails() {
		return emails;
	}

	public void setEmails(ArrayList<Email> emails) {
		this.emails = emails;
	}
	
	public ArrayList<Long> getIvolvedUsers(){
		ArrayList<Long> users=new ArrayList<>();
		for(Email e : emails){
			for(long u : e.getIvolvedUsers()){
				if(!users.contains(u))
					users.add(u);
			}
		}
		return users;
	}
	
	public void insertEmail(Email e){
		emails.add(e);
		/*for(int u : e.getIvolvedUsers()){
			if(!involvedUsers.contains(u))
				involvedUsers.add(u);
		}*/
		if(e.getTimestamp().after(maxTime) ){
			maxTime=e.getTimestamp();
		}
		if(e.getTimestamp().before(minTime)){
			minTime=e.getTimestamp();
		}
	}
	
	public void insertEmails(ArrayList<Email> e_list){
		for(Email e : e_list){
			emails.add(e);
			/*for(int u : e.getIvolvedUsers()){
				if(!involvedUsers.contains(u))
					involvedUsers.add(u);
			}*/
			if(e.getTimestamp().after(maxTime) ){
				maxTime=e.getTimestamp();
			}
			if(e.getTimestamp().before(minTime)){
				minTime=e.getTimestamp();
			}
		}		
	}
	
	public Timestamp getMinTimestamp() {
		return minTime;
	}

	public void setMinTimestamp(Timestamp minTime) {
		this.minTime = minTime;
	}
	
	public Timestamp getMaxTimestamp() {
		return maxTime;
	}

	public void setMaxTimestamp(Timestamp maxTime) {
		this.maxTime = maxTime;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public int numEmails(){
		return emails.size();
	}

}
