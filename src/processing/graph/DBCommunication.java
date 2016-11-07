package processing.graph;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import processing.graph.Edge;
import processing.graph.Graph;
import processing.graph.Message;
import processing.graph.Node;

public class DBCommunication {
	/***********************************************/
	// JDBC driver name, database URL, user, pass
	private String DB_URL = "jdbc:mysql://localhost:3306/";
	private final String USER = "";
	private final String PASS = "";
	
	private Connection conn = null;
	private Statement stmt = null;
	
	// Graph instance
	private Graph g;
	// threadID -> messageIDS
	public Map<Integer, Set<Integer>> mapMessagesToThread;
	// messageID -> threadID
	public Map<Integer, Integer> mapThreadToMessage;
	// threadID-> node ids
	public Map<Integer, Set<Integer>> mapNodesToThread;
	// nodeID -> threadIDS
	private static Map<Integer, Set<Integer>> mapThreadsToNode;
	
	// graph direction
	private boolean DIRECTED = true;
	
	// for debugging purpose
	private final boolean DEBUG = false;
	/***********************************************/
	
	/*************** Access methods ***************/
	public Graph getGraph() { return g; }
	public Map<Integer, Set<Integer>> getMessagesToThread() { return mapMessagesToThread; }
	public Map<Integer, Integer> getThreadToMessage() { return mapThreadToMessage; }
	public Map<Integer, Set<Integer>> getNodesToThread() { return mapNodesToThread; }
	public Map<Integer, Set<Integer>> getThreadsToNode() { return mapThreadsToNode; }

	/**
	 * Constructor
	 * @param db_name
	 * @param directed
	 */
	public DBCommunication(String db_name, boolean directed) {
		DB_URL+= db_name;
		DIRECTED = directed;
		
		// create graph instance
		g = new Graph(db_name);
	
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			
			// get connections
			getConnections();
			
			// get threads
			getThreadsFromEmails();
						
			conn.close();
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
			}
		}
		// sort messages in edges by time
		g.sortTimes();
		
		if (DEBUG) {
			for (Node n : g.getNodes()) {
				for (Edge e : n.getAdjacency()) {
					for (Message m : e.getMessages()) {
						Timestamp t = m.getTime();
						System.out.println(t);
						DateTime dateTime = new DateTime(t.getTime());
						System.out.print(dateTime.getYear());
						System.out.print("-" + dateTime.getMonthOfYear());
						System.out.print("-" + dateTime.getDayOfMonth());
						System.out.println("\t" + dateTime.getDayOfYear());
						System.out.println("--------------");
					}
					System.out.println("/////////////");
				}
				System.out.println("**************");
			}
		}
	}

	/**
	 * Get threads from DB and map messages and nodes to a thread
	 * @throws SQLException
	 */
	private void getThreadsFromEmails() throws SQLException {
		System.out.println("Creating statement for threads from emails...");
		stmt = conn.createStatement();
		String sql = "SELECT email_id, sender_id, thread_id FROM email_messages";
		ResultSet rs = stmt.executeQuery(sql);
		int email_id, thread_id;

		mapMessagesToThread = new HashMap<>();
		mapThreadToMessage = new HashMap<>();
		mapNodesToThread = new HashMap<>();
		mapThreadsToNode = new HashMap<>();

		while (rs.next()) {
			// Retrieve by column name
			email_id = rs.getInt("email_id");
			thread_id = rs.getInt("thread_id");
			
			// for null value in thread_id column
			if (rs.wasNull())
				continue;
			
			addInMap(mapMessagesToThread, thread_id, email_id, -1);
			mapThreadToMessage.put(email_id, thread_id);

			if (DEBUG) {
				// Display values
				System.out.println("Sender: " + email_id);
				System.out.println("Recipient: " + thread_id);
				System.out.println("--------");
			}
		}
		// clean up
		rs.close();
		stmt.close();
				
		stmt = conn.createStatement();
		sql = "SELECT * FROM communications";
	    rs = stmt.executeQuery(sql);
	    int sender_id, recipient_id;
	    boolean hasRecipient = true;

	      
	    while (rs.next()){
	    	hasRecipient = true;
	    	
	    	//Retrieve by column name
	    	sender_id  = rs.getInt("sender_id");
	    	email_id = rs.getInt("email_id");
	    	recipient_id = rs.getInt("recipient_id");
	    	
	    	if (rs.wasNull()) {
	    		hasRecipient = false;
	    		recipient_id = -1;
	    	}
	    	
			// for null value in thread_id column in 
	    	// the previous sql iteration
	    	if (!mapThreadToMessage.containsKey(email_id))
	    		continue;
	    	
	    	//FIXME
			if (g.getName().contains("enron") && (sender_id > 148 || recipient_id > 148))
	    		continue;

	    	// get thread_id
			thread_id = mapThreadToMessage.get(email_id);
			
			addInMap(mapNodesToThread, thread_id, sender_id, recipient_id);	    	
	    	addInMap(mapThreadsToNode, sender_id, thread_id, -1);
	    	
	    	if (hasRecipient)
	    		addInMap(mapThreadsToNode, recipient_id, thread_id, -1);
	    }
	    // clean up
	    rs.close();
	    stmt.close();
	}
	
	/**
	 * Adds in given map in given key the value, value1
	 * @param map
	 * @param key
	 * @param value
	 * @param value1
	 */
	public void addInMap(Map<Integer, Set<Integer>> map, int key, int value, int value1) {
		Set<Integer> set = map.get(key);
    	
    	if (set == null) {
    		set = new HashSet<>();
    		map.put(key, set);
    	}
    	
    	set.add(value);
    	
    	// when value1 is not needed
    	if (value1 != -1)
    		set.add(value1);
	}

	/**
	 * Reads connection tables and creates the version graph
	 * @throws SQLException
	 */
	private void getConnections() throws SQLException {
	      System.out.println("Creating statement for connections...");
	      stmt = conn.createStatement();
	      String sql = "SELECT * FROM communications";
	      ResultSet rs = stmt.executeQuery(sql);
	      int sender_id, recipient_id, email_id;
	      Timestamp timestamp;
	      Map<Integer, Message> messages = new HashMap<>();
	      Message m;
	      boolean hasRecipient = true;

	      while(rs.next()){
	    	  hasRecipient = true;
	    	  
	         //Retrieve by column name
	         sender_id  = rs.getInt("sender_id");
	         email_id = rs.getInt("email_id");
	         recipient_id = rs.getInt("recipient_id");
	         
	         if (rs.wasNull())
	        	 hasRecipient = false;
	         
	         timestamp = rs.getTimestamp("time");
	         
	         // create nodes
	         g.getCreateNode(sender_id);
	         
	         if (hasRecipient)
	        	 g.getCreateNode(recipient_id);
	         
	         m = messages.get(email_id);
	         
	         if (m == null) {
	        	 m = new Message(email_id, sender_id, timestamp);
	        	 messages.put(email_id, m);
	         }
	         
	         if (hasRecipient)	         
	        	 m.addRecipient(recipient_id);
	         
	         if (hasRecipient) {
	        	 // create edge
	        	 g.addEdge(sender_id, recipient_id, m);
	         
	        	 if (!DIRECTED)
	        		 g.addEdge(recipient_id, sender_id, m);
	         } else {
	        	 g.getNode(sender_id).getMessages().add(m);
	         }

	         if (DEBUG) {
		         // Display values
		         System.out.println("Sender: " + sender_id);
		         System.out.println("Recipient: " + recipient_id);
		         System.out.println("Timestamp: " + timestamp);
		         System.out.println("--------");
	         }
	      }
	      // clean up
	      rs.close();
	      stmt.close();		
	}
}