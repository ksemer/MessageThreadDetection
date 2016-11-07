package construction.enron;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class Delete {
	static final String DB_URL = "jdbc:mysql://localhost:3306/enron_clean";
	static final String USER = "";
	static final String PASS = "";

	public static void main(String[] args) {

		ArrayList<Integer> ids = new ArrayList<Integer>();
		try {
			FileInputStream fstream = new FileInputStream("id_toremove");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			while (line != null) {
				ids.add(Integer.valueOf(line));
				line = br.readLine();
			}
			br.close();
			in.close();
			fstream.close();

			Connection conn = null;

			Class.forName("com.mysql.jdbc.Driver");

			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);

			int d = 0;
			for (int id : ids) {
				d++;
				if (d % 100 == 0)
					System.out.println((d++) + "\tdelete " + id);
				try {
					String SQL = "DELETE FROM connections WHERE email_id = ? ";
					PreparedStatement pstmt = null;
					pstmt = conn.prepareStatement(SQL);
					pstmt.setInt(1, id);
					pstmt.executeUpdate();

				} catch (Exception e) {
					// Handle errors for Class.forName
					e.printStackTrace();
				}
			}

			conn.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}