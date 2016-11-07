package construction.enron;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Graph {
	private Map<Integer, Node> nodes;
	public static Map<String, Integer> emailID;
	public static Map<String, String> emailName;
	public static Map<String, String> emailOccupation;

	public Graph() throws IOException {
		nodes = new HashMap<>();
		emailID = new HashMap<>();
		emailName = new HashMap<>();
		emailOccupation = new HashMap<>();
		loadEmailsInfo();
	}

	private void loadEmailsInfo() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(Enron.emailsInfo), "UTF8"));
			String line = null;
			String[] token = null;
	
			while((line = br.readLine()) != null) {
				token = line.split("\t");
				emailID.put(token[0], Integer.parseInt(token[1]));
				if (token[2].equals("null"))
					emailName.put(token[0], "");
				else
					emailName.put(token[0], token[2]);
				
				if (token[3].equals("null"))
					emailOccupation.put(token[0], "");
				else
					emailOccupation.put(token[0], token[3]);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Collection<Node> getNodes() { return nodes.values(); }
	public Node getNode(String email) { return nodes.get(email); }
	public int getSize() { return nodes.size(); }

	public Node getCreateNode(String email) {
		// since our file with emails is in lowercase
		email = email.toLowerCase();

		Node n = nodes.get(emailID.get(email));
		
		if (n == null) {
			n = new Node(email);
			nodes.put(emailID.get(email), n);
		}
		
		return n;
	}
}