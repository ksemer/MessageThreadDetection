package construction.boards;

import java.util.Date;
import java.util.Set;

public class Message { 
	private int msgID, from;
	private Date date; 
	private Set<Integer> to;
	private String subject, allText, quote, clean;
	
	public Message(int msgID, Date date, int from, Set<Integer> to, String subject, String allText,
			String quote, String clean) {
		this.msgID = msgID;
		this.date = date;
		this.to = to;
		this.subject = subject;
		this.clean = clean;
		this.quote = quote;
		this.allText = allText;
		this.from = from;
	}

	public Date getTimestamp() {
		return date;
	}

	public String getCleanContent() {
		return clean;
	}

	public Set<Integer> getTo() {
		return to;
	}

	public int getSender() {
		return from;
	}

	public int getId() {
		return msgID;
	}

	public String getText() {
		return allText;
	}

	public String getCleanQuotes() {
		return quote;
	}

	public String getSubject() {
		return subject;
	}
}