package construction.enron;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Insert threads ids after clusterization
 * @author ksemertz
 */
public class insertThreadID {
	static final String DB_URL = "jdbc:mysql://localhost:3306/enron_threads";
	static final String USER = "";
	static final String PASS = "";
	static String path = "C:/Users/IBM_ADMIN/Dropbox/PhD/IBM/ibm stuff/data_files/clustering_emailOf150Enronusers/enron_clusters_atTH_0.6_allTime_onlyEmailWithUserIn150.txt";
	static Connection connect = null;
	static PreparedStatement preparedStatement;

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		Class.forName("com.mysql.jdbc.Driver");
		try {
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null, query;
		int thread_id = -1;
		String[] token;
		
		br.readLine();
		while ((line = br.readLine()) != null) {
			if (line.contains("**************")) {
				thread_id++;
				continue;
			} else if (line.isEmpty())
				continue;
			
			token = line.split("\t");
			query = "UPDATE email_messages SET thread_id = ? WHERE email_id = ?";

			try {
				preparedStatement = connect.prepareStatement(query);
				preparedStatement.setInt(1, thread_id);
				preparedStatement.setString(2, token[0]);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		br.close();

		try {
			preparedStatement.close();
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}