package construction.bc3;

import java.io.File;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author ksemer
 */
public class BC3 {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/bc3";
	private static final String USER = "";
	private static final String PASS = "";
	private static final String DATASET = "corpus.xml";
	private static final boolean DEBUG = false;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");		

	private static Connection connect = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;
	
	private static Map<String, Integer> users = new HashMap<>();
	private static int userID = 0;
	private static int emailID = 0;
	
	public BC3() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection(DB_URL, USER, PASS);

		startParsing();
		addUsersIntoDB();
		close();
	}

	private static void startParsing() {
		int threadID = -1;

		try {
			File fXmlFile = new File(DATASET);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			if (DEBUG)
				System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList nList = doc.getElementsByTagName("thread");

			if (DEBUG)
				System.out.println("----------------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					
					threadID++;
					addThread(threadID, eElement.getElementsByTagName("name").item(0).getTextContent());

					if (DEBUG) {
						System.out.println("thread name : " + eElement.getElementsByTagName("name").item(0).getTextContent());
						System.out.println("listno : " + eElement.getElementsByTagName("listno").item(0).getTextContent());
					}

					NodeList nList1 = eElement.getElementsByTagName("DOC");
					for (int temp1 = 0; temp1 < nList1.getLength(); temp1++) {
						Node nNode1 = nList1.item(temp1);
						
						if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
							Element eElement1 = (Element) nNode1;
							
							NodeList nList2 = eElement1.getElementsByTagName("Text");
							
							for (int temp2 = 0; temp2 < nList2.getLength(); temp2++) {

								Node nNode2 = nList2.item(temp2);
								
								if (nNode2.getNodeType() == Node.ELEMENT_NODE) {
									Element eElement2 = (Element) nNode2;
									addMessage(eElement1, eElement2, threadID);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addMessage(Element eElement, Element eTextElement, int threadID) throws DOMException, ParseException {
		if (DEBUG) {
			System.out.println("Received : " + eElement.getElementsByTagName("Received").item(0).getTextContent());
			System.out.println("From : " + eElement.getElementsByTagName("From").item(0).getTextContent());
			System.out.println("To : " + eElement.getElementsByTagName("To").item(0).getTextContent());
			
			if (eElement.getElementsByTagName("Cc").item(0) != null)
				System.out.println("Cc : " + eElement.getElementsByTagName("Cc").item(0).getTextContent());

			System.out.println("Subject : " + eElement.getElementsByTagName("Subject").item(0).getTextContent());
		}

		String text = "", from, to;
		String[] token;
		String[] token1;
		Date date = formatter.parse(eElement.getElementsByTagName("Received").item(0).getTextContent());
	
		from = eElement.getElementsByTagName("From").item(0).getTextContent();
		
		if (from.contains("<") && from.contains(">")) {
			token = from.split("<");
			token[1] = token[1].replace(">", "");
			token[1] = token[1].replaceAll(" ", "");
			from = token[1];
			
			if (!users.containsKey(from))
				users.put(from, userID++);
		}

		token = eElement.getElementsByTagName("To").item(0).getTextContent().split(",");

		for (int i = 0; i < token.length; i++) {
			if (token[i].contains("<") && token[i].contains(">")) {
				token1 = token[i].split("<");
				token1[1] = token1[1].replace(">", "");
				token1[1] = token1[1].replaceAll(" ", "");
				to = token1[1];
			} else
				to = token[i];
			
			if (!users.containsKey(to))
				users.put(to, userID++);
			
			addConnection(users.get(from), users.get(to), date, emailID);
		}
		
		if (eElement.getElementsByTagName("Cc").item(0) != null) {
			token = eElement.getElementsByTagName("Cc").item(0).getTextContent().split(",");
			
			for (int i = 0; i < token.length; i++) {
				if (token[i].contains("<") && token[i].contains(">")) {
					token1 = token[i].split("<");
					token1[1] = token1[1].replace(">", "");
					token1[1] = token1[1].replaceAll(" ", "");
					to = token1[1];
				} else
					to= token[i];
				
				if (!users.containsKey(to))
					users.put(to, userID++);

				addConnection(users.get(from), users.get(to), date, emailID);
			}
		}
		
		for (int i = 0; i < eTextElement.getElementsByTagName("Sent").getLength(); i++) {
			if (DEBUG)
				System.out.println("Sent : " + eTextElement.getElementsByTagName("Sent").item(i).getTextContent());
			text+= eTextElement.getElementsByTagName("Sent").item(i).getTextContent() + "\n";
		}

		try {
			preparedStatement = connect.prepareStatement("insert into email_messages values (?, ?, ?, ?, ?)");
			preparedStatement.setInt(1, emailID);
			preparedStatement.setInt(2, users.get(from));
			preparedStatement.setString(3, eElement.getElementsByTagName("Subject").item(0).getTextContent());
			preparedStatement.setString(4, text);
			preparedStatement.setInt(5, threadID);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		emailID++;		
	}

	private static void addConnection(int from, int to, Date date, int emailID) {		
		try {
			preparedStatement = connect.prepareStatement("insert into communications values (?, ?, ?, ?)");
			preparedStatement.setInt(1, from);
			preparedStatement.setInt(2, to);
			preparedStatement.setInt(3, emailID);
			preparedStatement.setTimestamp(4, new Timestamp(date.getTime()));
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addThread(int threadID, String name) {
		try {
			preparedStatement = connect.prepareStatement("insert into threads values (?, ?)");
			preparedStatement.setInt(1, threadID);
			preparedStatement.setString(2, name);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private static void addUsersIntoDB() {
		for (String em : users.keySet()) {
			try {
				preparedStatement = connect.prepareStatement("insert into users values (?, ?)");
				preparedStatement.setInt(1, users.get(em));
				preparedStatement.setString(2, em);
				preparedStatement.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		 new BC3();
	 }
}