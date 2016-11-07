package construction.enron;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class insertClQtext {
	static final String DB_URL = "jdbc:mysql://localhost:3306/enron";
	static final String USER = "";
	static final String PASS = "";
	static String path = "C:/Users/IBM_ADMIN/Desktop/DATASET/clean_text_enron/text/";
	static Connection connect = null;
	static PreparedStatement preparedStatement;

	public static void main(String[] args) throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		try {
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		String clean, quote;

		for (int id = 0; id <= 239710; id++) {
			clean = getText(path + id + "_cleantext.txt");
			quote = getText(path + id + "_quottext.txt");

			String query = "UPDATE email_messages SET clean_text = ?, quotation_text = ? WHERE email_id = ?";
			try {
				preparedStatement = connect.prepareStatement(query);
				if (clean != null)
					preparedStatement.setString(1, clean);
				else
					preparedStatement.setString(1, "");
				if (quote != null)
					preparedStatement.setString(2, quote);
				else
					preparedStatement.setString(2, "");
				preparedStatement.setInt(3, id);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		try {
			preparedStatement.close();
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static String getText(String path) {
		BufferedReader br = null;
		String text = "", line = null;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));

			while ((line = br.readLine()) != null) {
				text += line + "\n";
			}
			br.close();
			
			return text;
		} catch (Exception e) {
			System.out.println(path + " doesn't exist");
		}

		return null;		
	}
}