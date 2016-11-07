package construction.boards;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;

import processing.graph.DBCommunication;
import processing.graph.Graph;
import processing.graph.Message;
import processing.graph.Node;

/**
 * @author ksemer
 */
public class BoardsIE {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/boards.ie_16k";
	private static final String USER = "";
	private static final String PASS = "";
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");		
	private static final boolean parsing = true;
	private static final boolean addQuotes = false;
	private static final boolean timeStats = false;
	
	private static Connection connect = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;
	
	/***********************************************/
	// Graph instance
	private static Graph graph;
	// threadID -> messageIDS
	private static Map<Integer, Set<Integer>> mapMessagesToThread;
	// threadID -> min, max timestamp
	private Map<Integer, List<Timestamp>> threadTimeStats;
	
	// graph direction
	private static boolean DIRECTED = true;
	/***********************************************/

		
	public BoardsIE() throws ClassNotFoundException, SQLException, IOException, NumberFormatException, ParseException {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection(DB_URL, USER, PASS);
		
		if (timeStats)
			threadStats();

		if (parsing)
			startParsing(true);
		else if (addQuotes)
			startParsing(false);
				
		close();
	}
	

	private void threadStats() {
		DBCommunication dbc = new DBCommunication("boards.ie", DIRECTED);
		Set<Integer> nodes, messages_set;
		int thread_id;
		
		// get connections
		graph = dbc.getGraph();
			
		// threadID -> messageIDS
		mapMessagesToThread = dbc.getMessagesToThread();
		
		threadTimeStats = new HashMap<Integer, List<Timestamp>>();
				
		// for each thread get nodes
		for(Map.Entry<Integer, Set<Integer>> mnt : dbc.getNodesToThread().entrySet()) {
			
			thread_id = mnt.getKey();
			nodes = mnt.getValue();
			messages_set = mapMessagesToThread.get(thread_id);
			Set<Message> messages = new HashSet<Message>();
			threadTimeStats.put(thread_id, new ArrayList<Timestamp>());

			// update thread messages structure by adding
			for (int n : nodes) {

				Node node = graph.getNode(n);

				// check if node's messages id belong to the thread
				for (Message m : node.getMessages()) {

					if (messages_set.contains(m.getID()))
						messages.add(m);
				}
			}
			
			Timestamp min, max;
			
			for (Message m : messages) {
				if (threadTimeStats.get(thread_id).isEmpty()) {
					threadTimeStats.get(thread_id).add(m.getTime());
					threadTimeStats.get(thread_id).add(m.getTime());
				} else {
					min = threadTimeStats.get(thread_id).get(0);
					max = threadTimeStats.get(thread_id).get(1);

					if (min.compareTo(m.getTime()) < 0)
						threadTimeStats.get(thread_id).add(0, m.getTime());
					
					if (max.compareTo(m.getTime()) > 0)
						threadTimeStats.get(thread_id).add(1, m.getTime());
				}
			}
		}

	}

	private void addQuotes(int email_id, String text, String quoteID) {
		String quote = null;
		
		try {
			String sql = "SELECT text, clean_text FROM email_messages where email_id = " + quoteID;
			Statement stmt = connect.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {

			 quote = rs.getString("clean_text");
			
			 if (quote == null)
				quote = rs.getString("text");
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			preparedStatement = connect.prepareStatement(
					"UPDATE email_messages SET text = ?, clean_text = ?, quotation_text = ? WHERE email_id = ?");

			preparedStatement.setString(1, quote + "\n" + text);
			preparedStatement.setString(2, text);
			preparedStatement.setString(3, quote);
			preparedStatement.setInt(4, email_id);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void startParsing(boolean flag) throws IOException, NumberFormatException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader("boards_threadSampling_2006-2007_3-40-16000.txt"));
		String line = null;
		String[] token;
		String title = null;
		
		while((line = br.readLine()) != null) {
			token = line.split(" ");
			
			title = getTitle("boards.ie/" + token[0]);

			token[0] = token[0].substring(100, token[0].length());
			
			if (token[0].contains("page"))
				token[0] = token[0].substring(0, 10);
			
			for (int i = 1; i < token.length; i++)
				// token[i] has the url link of post
				readPost("boards.ie/" + token[i], Integer.parseInt(token[0]), title, flag);
		}
		br.close();
	}

	private String getTitle(String thread_path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(thread_path));
		String line = null, title = null;
		boolean first = false;
		
		while((line = br.readLine()) != null) {
			if (line.contains("<dc:title>") && !first) {
				first = true;
				continue;
			} else if (line.contains("<dc:title>") && first) {
				title = line.substring(11, line.length() - 11);
				break;
			}
		}
		br.close();
		return title;
	}

	private void readPost(String path, int threadID, String subject, boolean write) throws IOException, NumberFormatException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null, time = null, user = null, text = null, quote = null, postID = null;
		boolean flag = false;

		while((line = br.readLine()) != null) {

			if (line.contains("sioc:User rdf:about="))
				user = line.substring(64, line.length() - 7);				
			else if (line.contains("dcterms:created>")) {
				time = line.substring(18, line.length() - 19);
				time = time.replace("T", " ");
			} else if (!flag && line.contains("<sioc:content>")) {
				flag = true;
				text="" + line.replace("<sioc:content>", "");
				
				if (text.contains("</sioc:content>"))
					text = text.replace("</sioc:content>", "");
				
				if (text.contains("<sioc:reply_of>"))
					text = text.replace("<sioc:reply_of>", "");
			} 
			else if (line.contains("<sioct:BoardPost"))
				postID = line.substring(70, line.length() - 2);
			else if (flag && line.contains("sioc:Post rdf")) {
				quote = line.substring(66, line.length() - 2);
				break;
			}
			else if (flag && !line.contains("content:encoded")) {
				if (line.contains("</sioc:Post>") || line.contains("</sioct:BoardPost>"))
					break;
				text+= line;
				if (text.contains("</sioc:content>"))
					text = text.replace("</sioc:content>", "");
				
				if (text.contains("<sioc:reply_of>"))
					text = text.replace("<sioc:reply_of>", "");	
			}
		}
		br.close();
		
		if (user == null)
			return;
		
		if (write) {
			addConnection(Integer.parseInt(user), time, Integer.parseInt(postID));
			addMessage(Integer.parseInt(user), time, text, subject, threadID, Integer.parseInt(postID));
		} else {
			if (quote != null)
			addQuotes(Integer.parseInt(postID), text, quote);
		}
	}

	private static void addMessage(int user, String time, String text, String subject, int threadID, int emailID) {
		try {
			preparedStatement = connect.prepareStatement("insert into email_messages (email_id, sender_id, thread_id, subject, text) values (?, ?, ?, ?, ?)");
			preparedStatement.setInt(1, emailID);
			preparedStatement.setInt(2, user);
			preparedStatement.setInt(3, threadID);
			preparedStatement.setString(4, subject);
			preparedStatement.setString(5, text);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addConnection(int from, String time, int emailID) throws ParseException {	
		Date date = formatter.parse(time);
		try {
			preparedStatement = connect.prepareStatement("insert into communications (sender_id, email_id, time) values (?, ?, ?)");
			preparedStatement.setInt(1, from);
			preparedStatement.setInt(2, emailID);
			preparedStatement.setTimestamp(3,  new Timestamp(date.getTime()));
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Close Database
	 */
	 private static void close() {
		 try {
			 if (resultSet != null)
				 resultSet.close();

			 if (statement != null)
				 statement.close();

			 if (connect != null)
				 connect.close();
		 } catch (Exception e) {}
	 }
	 
	 public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, ParseException {
		 new BoardsIE();
	 }
}