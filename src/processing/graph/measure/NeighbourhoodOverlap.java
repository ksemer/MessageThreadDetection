package processing.graph.measure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to compute neighborhood overlap
 * for a given dataset
 * @author ksemertz
 */
public class NeighbourhoodOverlap {
	// JDBC driver name, database URL, user, pass
	private String DB_URL = "jdbc:mysql://localhost:3306/";
	private static final String USER = "";
	private static final String PASS = "";
	private Connection conn = null;
	private Statement stmt = null;
	
	private Map<Long, Set<Long>> nodes;
	
	/**
	 * Constructor
	 * @param dataset_name
	 */
	public NeighbourhoodOverlap(String dataset_name) {
		DB_URL+= dataset_name;
		nodes = new HashMap<>();

		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			getConnections();		
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
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get connections
	 * @throws SQLException
	 */
	private void getConnections() throws SQLException {
		stmt = conn.createStatement();
		String sql = "SELECT sender_id, recipient_id FROM communications";
		ResultSet rs = stmt.executeQuery(sql);
		long sender_id, recipient_id;
		Set<Long> adj;

		while (rs.next()) {
			// Retrieve by column name
			sender_id = rs.getInt("sender_id");
			recipient_id = rs.getInt("recipient_id");

			if ((adj = this.nodes.get(sender_id)) != null) {
				adj.add(recipient_id);
			} else {
				adj = new HashSet<>();
				adj.add(recipient_id);
				adj.add(sender_id);
				this.nodes.put(sender_id, adj);
			}
			
			if (this.nodes.get(recipient_id) == null) {
				adj = new HashSet<>();
				adj.add(recipient_id);
				this.nodes.put(recipient_id, adj);
			}
			
		}
		// clean up
		rs.close();
		stmt.close();		
	}
	
	/**
	 * Returns the neighborhood overlap of user1 and user2
	 * @param user1_id
	 * @param user2_id
	 * @return
	 */
	public double getCloseness(long user1_id, long user2_id) {
		Set<Long> user1 = nodes.get(user1_id), user2 = nodes.get(user2_id),
				intersection, union = new HashSet<>();
	
		intersection = new HashSet<>(user1);
		intersection.retainAll(user2);

		union.addAll(user1);
		union.addAll(user2);
		
		return (double) intersection.size() / union.size();
	}
}