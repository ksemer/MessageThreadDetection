package construction.enron;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * @author ksemer
 */
public class Enron {

	private static String mailDir = "C:\\Users\\IBM_ADMIN\\Desktop\\DATASET\\maildir\\";
	public static String emailsInfo = "emails_of_users_reverse.txt",
			usersInfo = "emails_of_users_new.txt";
	private static Graph g;
	private static Node n1, n2;
	private static Date d = null;
	private static String date = null, from = null, subject = null, body = "";
	private static String[] to = null, cc = null, bcc = null;
	private static boolean flag = false;
	public static Connection connect = null;
	public static Statement statement = null;
	public static PreparedStatement preparedStatement = null;
	public static ResultSet resultSet = null;
	private static int emailID = -1, duplicateEmails = 0;
	private static Table<Node, Node, Set<String>> pair = HashBasedTable.create();
	private static Table<Integer, String, Set<String>> messagesDBInfo = HashBasedTable.create();
	private static Set<String> set;

	public Enron () throws IOException, SQLException, ClassNotFoundException {
		g = new Graph();
		
		Class.forName("com.mysql.jdbc.Driver");
		connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/enron_new", "ksemer", "10101990");
		final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z (z)");		
		
		File dir = new File(mailDir);
		List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);		

		for (File file : files) {
			String line = null;

			BufferedReader br = null;

			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file.getCanonicalPath()), "UTF8"));

				while ((line = br.readLine()) != null) {
					if (flag) {
						if (line.contains("X-") || line.isEmpty() || !line.matches(".*\\w.*"))
							continue;
						body += line + "\n";
					}

					if (!flag) {
						if (date == null)
							date = Mail.getDate(line);

						if (from == null)
							from = Mail.getFrom(line);

						if (subject == null)
							subject = Mail.getSubject(line);

						if (to == null)
							to = Mail.getTo(line);

						if (cc == null)
							cc = Mail.getCC(line);

						if (bcc == null)
							bcc = Mail.getBCC(line);

						if (line.contains("X-"))
							flag = true;
					}
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				d = formatter.parse(date);
				flag = false;
				// System.out.println(formatter.format(d));

				if (from != null && to != null) {
					n1 = g.getCreateNode(from);

					for (int i = 0; i < to.length; i++) {
						try {
							n2 = g.getCreateNode(to[i]);
							updateStructure();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					if (cc != null) {
						for (int i = 0; i < cc.length; i++) {
							try {
								n2 = g.getCreateNode(cc[i]);
								updateStructure();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					if (bcc != null) {
						for (int i = 0; i < bcc.length; i++) {
							try {
								n2 = g.getCreateNode(bcc[i]);
								updateStructure();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} finally {
				from = null;
				to = null;
				bcc = null;
				subject = null;
				cc = null;
				flag = false;
				date = null;
				body = "";
			}
		}
			
		
		close();

		FileWriter w = new FileWriter("graph", false);
		System.out.println("Duplicate Emails: " + duplicateEmails);
		
		for (Node n : g.getNodes())
			for (int adj : n.getAdjacency())
				w.write(n.getID() + "\t" + adj + "\n");
		w.close();
	}

	/**
	 * Insert email into DB
	 */
	private static void addMailInDB() {
		try {
			emailID++;
			preparedStatement = connect.prepareStatement("insert into email_messages values (?, ?, ?, ?)");
			preparedStatement.setInt(1, emailID);
			preparedStatement.setInt(2, n1.getID());
			preparedStatement.setString(3, subject);
			preparedStatement.setString(4, body);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update Structure by adding neighbors
	 * and keeping the pair of nodes updated
	 */
	private static void updateStructure() {
		if (pair.get(n2, n1) == null || !pair.get(n2, n1).contains(body)){
			n1.getAdjacency().add(n2.getID());
			
			if (!flag) {
				// used to check for dublicate emails				
				if (messagesDBInfo.get(n1.getID(), subject) == null) {
					addMailInDB();
					set = new HashSet<>();
					set.add(body);
					messagesDBInfo.put(n1.getID(), subject, set);
				}
				else if (!(set = messagesDBInfo.get(n1.getID(), subject)).contains(body)) {
					addMailInDB();
					set.add(body);
				}
			}
		
			if ((set = pair.get(n1, n2)) == null) {
				set = new HashSet<>();
				pair.put(n1, n2, set);
			}
			
			if (!pair.get(n1, n2).contains(body))
				try {
					preparedStatement = Enron.connect.prepareStatement("insert into communications values (?, ?, ?, ?)");
					preparedStatement.setInt(1, n1.getID());
					preparedStatement.setInt(2, n2.getID());
					preparedStatement.setInt(3, emailID);
					preparedStatement.setTimestamp(4, new Timestamp(d.getTime()));
					preparedStatement.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}

			set.add(body);	
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
	 
	 public static void main(String[] args) throws ClassNotFoundException, IOException, SQLException {
		 new Enron();
	 }
}