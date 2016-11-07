package construction.boards;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class RedHat {
	private int userID = -1;
	private int threadID = -1;
	private String folder = "2009-January/msg";
	private Map<String, Integer> threads = new HashMap<>();
	private Map<String, Integer> users = new HashMap<>();
	private Map<Integer, List<Message>> threadsMessages = new HashMap<>();
	private static final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");		
	private static final SimpleDateFormat formatter1 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");		

	private static final String DB_URL = "jdbc:mysql://localhost:3306/redhat";
	private static final String USER = "";
	private static final String PASS = "";

	private static Connection connect = null;
	private static PreparedStatement preparedStatement = null;
	
	public RedHat() throws ParseException, ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection(DB_URL, USER, PASS);
		
		getURL();
		
		for(Entry<Integer, List<Message>> t : threadsMessages.entrySet()){
			if (t.getValue().size() > 3)
				for (Message m : t.getValue()) {
					addMessage(m.getId(), t.getKey(), m.getTimestamp(), m.getSender(), m.getTo(), m.getSubject(),
							m.getText(), m.getCleanQuotes(), m.getCleanContent());
				}
		}
	}
	
	int max = 2497;
	
	private void getURL() throws ParseException {
        URL url;
        int l, msgID = -1, from = 0, thread = 0;
        Set<Integer> to;
        String msg, subject, text, allText, quote, clean;
        Date date;
        boolean newMessage = false;
        
        for (int i = 0; i < max; i++) {
	
	        try {
	        	msg = "" + i;
	        	l = msg.length();
	        	
	        	for (int j = l; j< 5; j++) 
	        		msg = "0" + msg;
	        	
	        	quote = "";
	        	subject = "";
	        	text = "";
	        	date = null;
	        	clean = "";
	        	allText = "";
	        	newMessage = false;
	        	to = new HashSet<>();

	            // get URL content
	            String a= "https://www.redhat.com/archives/fedora-devel-list/" + folder + msg + ".html";
	            url = new URL(a);
	            URLConnection conn = url.openConnection();
	
	            // open the stream and put it into BufferedReader
	            BufferedReader br = new BufferedReader(
	                               new InputStreamReader(conn.getInputStream()));
	
	            String inputLine;
	            while ((inputLine = br.readLine()) != null) {
	            	inputLine = inputLine.trim();
	            	
	            	if (inputLine.contains("<!--X-Head-of-Message-->")) {
	            		msgID++;
	            		newMessage = true;
	            	} else if (inputLine.contains("<!--X-Body-of-Message-End-->")) {
						if (date != null)
							threadsMessages.get(thread).add(new Message(msgID, date, from, to, subject, allText, quote, clean));		
	            		break;
	            	} else if (newMessage) {
	            		if (inputLine.contains("<li><em>From</em>:")) {
	            			if (inputLine.contains("&gt"))
	            				inputLine = inputLine.substring(inputLine.indexOf(';') + 1, inputLine.lastIndexOf('&'));
	            			else
	            				inputLine = inputLine.substring(17, inputLine.length() - 5);
	            			from = getUserID(inputLine);
	            		} else if (inputLine.contains("<li><em>To</em>:")) {
	            			if (inputLine.contains("&gt"))
	            				inputLine = inputLine.substring(inputLine.indexOf(';') + 1, inputLine.lastIndexOf('&'));
	            			else
	            				inputLine = inputLine.substring(17, inputLine.length() - 5);							to.add(getUserID(inputLine));
	            		} else if (inputLine.contains("<li><em>Cc</em>:")) {
	            			if (inputLine.contains("&gt"))
	            				inputLine = inputLine.substring(inputLine.indexOf(';') + 1, inputLine.lastIndexOf('&'));
	            			else
	            				inputLine = inputLine.substring(17, inputLine.length() - 5);	               			to.add(getUserID(inputLine));
	            		} else if (inputLine.contains("<li><em>Subject</em>:")) {
	               			subject = inputLine.substring(22, inputLine.length() - 5);
	               			thread = getThreadID(subject);	
	            		} else if (inputLine.contains("<li><em>Date</em>:")) {
	               			inputLine = inputLine.substring(19, inputLine.length() - 5);
	               			try {
	               				date = formatter.parse(inputLine);
	               			} catch (Exception e) {
								try {
									date = formatter1.parse(inputLine);
								} catch (Exception e1) {
									System.out.println(a);
									date = null;
								}
	               			}
	            		} else if (inputLine.contains("<pre>") || inputLine.contains("</pre>") 
	            				|| inputLine.contains("<!--X") || inputLine.contains("ul>")
	            				|| inputLine.contains("<hr>") || inputLine.isEmpty()
	            				|| inputLine.equals("&gt;")) {
	            			continue;
	            		} else if (inputLine.contains("&gt; ")) {
	            			text = inputLine.replaceAll("&gt;", "");
	            			quote+= text;
	            			allText+= text;
	            		} else {
	            			inputLine = inputLine.replaceAll("&gt;", "");
	            			allText+= inputLine;
	            			clean+= inputLine;
	            		}
	            	}
	            }
	            br.close();
	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
			if (i == 2496 && max == 2497) {
				i = 0;
				folder = "2009-February/msg";
				max = 2412;
			}
			
			if (i == 2411 && max == 2412) {
				i = 0;
				folder = "2009-March/msg";
				max = 2187;
			}
			
			if (i == 2186 && max == 2187) {
				i = 0;
				folder = "2009-April/msg";
				max = 2571;
			}
			
			if (i == 2570 && max == 2571) {
				i = 0;
				folder = "2009-May/msg";
				max = 2418;
			}
			
			if (i == 2417 && max == 2418) {
				i = 0;
				folder = "2009-June/msg";
				max = 2478;
			}
        }
	}

	private int getThreadID(String subject) {
		subject = subject.toLowerCase();
		
		if (subject.contains("re: "))
			subject = subject.replace("re: ", "");
		
		if (subject.contains("fwd: ")) {
			subject = subject.replace("[fwd: ", "");
			subject = subject.replace("fwd: ", "");
		}
		
		if (threads.containsKey(subject))
			return threads.get(subject);
		
		threadID++;
		threadsMessages.put(threadID, new ArrayList<Message>());
		threads.put(subject, threadID);
		
		return threadID;
	}
	
	private int getUserID(String user) {
		user = user.toLowerCase();
		
		if (users.containsKey(user))
			return users.get(user);
		
		userID++;
		users.put(user, userID);
		
		return userID;
	}


	private void addMessage(int msgID, int threadID, Date date, int from, Set<Integer> to, String subject, String allText, String quote, String clean) throws ParseException {
		try {
			preparedStatement = connect.prepareStatement("insert into email_messages (email_id, sender_id, thread_id, subject, text, clean_text, quotation_text) values (?, ?, ?, ?, ?, ?, ?)");
			preparedStatement.setInt(1, msgID);
			preparedStatement.setInt(2, from);
			preparedStatement.setInt(3, threadID);
			preparedStatement.setString(4, subject);
			preparedStatement.setString(5, allText);
			preparedStatement.setString(6, clean);
			preparedStatement.setString(7, quote);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		for (int t : to)
			addConnection(from, t, msgID, date);
	}
	
	private void addConnection(int from, int to, int emailID, Date date) throws ParseException {	
		try {
			preparedStatement = connect.prepareStatement("insert into communications (sender_id, recipient_id, email_id, time) values (?, ?, ?, ?)");
			preparedStatement.setInt(1, from);
			preparedStatement.setInt(2, to);
			preparedStatement.setInt(3, emailID);
			preparedStatement.setTimestamp(4,  new Timestamp(date.getTime()));
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) throws ParseException, ClassNotFoundException, SQLException { new RedHat(); }
}