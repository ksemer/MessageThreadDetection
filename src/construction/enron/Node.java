package construction.enron;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Set;

public class Node {
	private int id;
	private Set<Integer> adjacency;
	private static PreparedStatement preparedStatement;
	
	public Node(String email){
		this.adjacency = new HashSet<>();
		
		try {
			this.id = Graph.emailID.get(email);

			preparedStatement = Enron.connect.prepareStatement("insert into users values (?, ?, ?)");
			preparedStatement.setInt(1, id);
			
			// name
			preparedStatement.setString(2, Graph.emailName.get(email));
			// occupation
			preparedStatement.setString(3, Graph.emailOccupation.get(email));
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			System.out.println("Error: " + e.toString() + " " + email);
		}
	}
	
	public int getID() {
		return id;
	}

	public Set<Integer> getAdjacency() {
		return adjacency;
	}
}