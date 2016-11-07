package processing.text.enron.model;

import java.util.ArrayList;

public class User {
	
	private int id;
	private String name;
	private String position;
	private ArrayList<String> emails;
	
	public User() {
		emails=new ArrayList<>();
	}
	
	public User(int id, String name){
		this.name=name;
		this.id=id;
		emails=new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public ArrayList<String> getEmails() {
		return emails;
	}

	public void setEmails(ArrayList<String> emails) {
		this.emails = emails;
	}
	
	public void insertEmail(String email){
		this.emails.add(email);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
