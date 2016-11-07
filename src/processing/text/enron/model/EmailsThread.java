package processing.text.enron.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class EmailsThread {
	
	private String subject;
	private ArrayList<Email> emails;
	private ArrayList<Integer> involvedUsers;
	
	public EmailsThread(String subject) {
		this.subject=subject;
		this.emails=new ArrayList<>();
		this.involvedUsers=new ArrayList<>();
	}

	public ArrayList<Email> getEmails() {
		return emails;
	}

	public void setEmails(ArrayList<Email> emails) {
		this.emails = emails;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public void insertEmail(Email e){
		emails.add(e);
	}

	public ArrayList<Integer> getInvolvedUsers() {
		return involvedUsers;
	}

	public void setInvolvedUsers(ArrayList<Integer> involvedUsers) {
		this.involvedUsers = involvedUsers;
	}
	
	public void addInvolvedUser(int userid){
		if(!involvedUsers.contains(userid))
			involvedUsers.add(userid);
	}
	
	public boolean isAtLeastOneUsersInvolved(ArrayList<Integer> users){
		for(int u : users){
			if(involvedUsers.contains(u))
				return true;
		}		
		return false;
	}
	
	public void orderEmailsByTime(){
		Collections.sort(emails, new EmailTimeComparator());
	}
	

}

class EmailTimeComparator implements Comparator<Email> {
    @Override
    public int compare(Email o1, Email o2) {
        return o1.getTimestamp().compareTo(o2.getTimestamp());
    }
}