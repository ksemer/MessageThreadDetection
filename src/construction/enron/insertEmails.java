package construction.enron;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class insertEmails {
	static final String DB_URL = "jdbc:mysql://localhost:3306/enron";
	static final String USER = "ksemer";
	static final String PASS = "10101990";
	static String emailsInfo = "emails_of_users_reverse.txt";
	static Connection connect = null;
	static PreparedStatement preparedStatement;

	public static void main(String[] args) {		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(DB_URL, USER, PASS);

			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(emailsInfo), "UTF8"));
			String line = null;
			String[] token = null;
	
			while((line = br.readLine()) != null) {
				token = line.split("\t");
				// for emails that contain symbol '
				token[0] = token[0].replaceAll("'", "''");

				try {
					preparedStatement = connect.prepareStatement(
							"INSERT INTO email_addresses (user_id, email_address) VALUES (" 
									+ Integer.parseInt(token[1]) + 
									", '" + token[0] + "');");
					preparedStatement.executeUpdate();
				} catch (Exception e) {
					System.out.println("Error: " + e.toString());
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}