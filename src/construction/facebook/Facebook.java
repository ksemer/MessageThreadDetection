package construction.facebook;

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
import java.util.Date;

public class Facebook {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/facebook1";
	private static final String USER = "";
	private static final String PASS = "";
	private static final String DATASET = "health.xml";
	private static final SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");

	private static Connection connect = null;
	private static Statement statement = null;
	private static PreparedStatement preparedStatement = null;
	private static ResultSet resultSet = null;

	public Facebook() throws ClassNotFoundException, SQLException, IOException, ParseException  {
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection(DB_URL, USER, PASS);

		startParsing();
		close();
	}

	private static void startParsing() throws IOException, ParseException {
		int threadID = -1, postID = -1;
		Date date = null;
		String time = null, fromID = null, text = null;
		
		BufferedReader br = new BufferedReader(new FileReader(DATASET));
		String line = null;
		boolean thread = false, post = false, hatext = false;
		
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (line.contains("<thread")) {
				if (thread)
					thread = false;
				else {
					threadID++;
				}
			} else if (post && !line.contains("</post>")) {
				if (line.contains("<date>")) {
					time = line.substring(6, line.length() - 7);
					date = formatter.parse(time);
				} else if (line.contains("<from_id>")) {
					fromID = line.substring(9, line.length() - 10);
				} else if (hatext) {
					if (line.contains("</text>")) {
						text+= " " + line;
						text = text.substring(6, text.length() - 7);
					} else
						text+= " " + line;
				} else if (line.contains("<text>")) {
//					System.out.println(line);
					if (line.contains("</text>")) {
						text = "";
						text+= line;
						text = text.substring(6, text.length() - 7);
					} else {
						text = "";
						text+= line;
						hatext = true;
					}	
				}
			} else if (line.contains("<post>")) {
				post = true;
				postID++;
			} else if (line.contains("</post>")) {
				addMessage(Long.parseLong(fromID), text, threadID, postID);
				addConnection(Long.parseLong(fromID), date, postID);
				post = false;
				text = "";
				hatext = false;
			}
		}
		br.close();
	}

	private static void addMessage(long l, String text, int threadID, int emailID) {
		try {
			preparedStatement = connect.prepareStatement("insert into email_messages (email_id, sender_id, thread_id, text) values (?, ?, ?, ?)");
			preparedStatement.setInt(1, emailID);
			preparedStatement.setLong(2, l);
			preparedStatement.setInt(3, threadID);
			preparedStatement.setString(4, text);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addConnection(long from, Date time, int emailID) throws ParseException {	
		try {
			preparedStatement = connect.prepareStatement("insert into communications (sender_id, email_id, time) values (?, ?, ?)");
			preparedStatement.setLong(1, from);
			preparedStatement.setInt(2, emailID);
			preparedStatement.setTimestamp(3,  new Timestamp(time.getTime()));
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void close() {
		try {
			if (resultSet != null)
				resultSet.close();

			if (statement != null)
				statement.close();

			if (connect != null)
				connect.close();
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, ParseException {
		new Facebook();
	}
}