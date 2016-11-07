package processing.graph;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Message Object
 * @author ksemertz
 */
public class Message {
	private final int id;
	private final int sender_id;
	private List<Integer> recipients_id;
	private final Timestamp time;
	
	/**
	 * Constructor
	 * @param id
	 * @param sender_id 
	 * @param time
	 */
	public Message(int id, int sender_id, Timestamp time) {
		this.id = id;
		this.sender_id = sender_id;
		this.time = time;
		this.recipients_id = new ArrayList<>();
	}
	
	/******** Access methods ********/
	public int getID() { return id; }
	public int getSenderID() { return sender_id; }
	public Timestamp getTime() { return time; }
	public List<Integer> getRecipientsID() { return recipients_id; }

	public void addRecipient(int recipient_id) { recipients_id.add(recipient_id); }
}