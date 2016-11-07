package processing.text.enron.textproc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import processing.text.enron.model.Email;
import utils.Util;

public class DBInserts {

	static final String USER = "giacomo";
	static final String PASS = "12345";
	static Connection connect = null;
	static PreparedStatement preparedStatement;

	public DBInserts(String DB_URL) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(DB_URL, USER, PASS);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void closeConnection() {
		try {
			connect.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeEmailsCleanedContent(ArrayList<Email> emails) {
		int i = 0;
		try {
			for (Email e : emails) {
				String query = "update email_messages set clean_text = '" + e.getCleanContent().replaceAll("'", "''")
						+ "' , " + "quotation_text = '" + e.getCleanQuotes().replaceAll("'", "''")
						+ "' where email_id = " + e.getId() + ";";
				PreparedStatement preparedStmt = connect.prepareStatement(query);
				preparedStmt.executeUpdate();
				i++;
				System.out.println("scrivo nel db " + i);
			}
		} catch (Exception ex) {
			System.out.println("Error: " + ex.toString());
		}

	}

	public void writeEmailsAlchemyInformation(ArrayList<Email> emails) {
		try {
			int i = 0;
			try {
				for (Email e : emails) {
					String query = "update email_messages set alchemy = '"
							+ Util.getStringFromDocument(e.getAlchemy_document()).replaceAll("'", "''")
							+ "' where email_id = " + e.getId() + ";";
					PreparedStatement preparedStmt = connect.prepareStatement(query);
					preparedStmt.executeUpdate();
					i++;
					if (i % 100 == 0)
						System.out.println("scrivo nel db " + i);
				}
			} catch (Exception ex) {
				System.out.println("Error: " + ex.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeSingleEmailAlchemyInformation(Email e) {
		try {
			try {
				String query = "update email_messages set alchemy = '"
						+ Util.getStringFromDocument(e.getAlchemy_document()).replaceAll("'", "''")
						+ "' where email_id = " + e.getId() + ";";
				PreparedStatement preparedStmt = connect.prepareStatement(query);
				preparedStmt.executeUpdate();
			} catch (Exception ex) {
				System.out.println("Error: " + ex.toString());
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}
	
	
	public void writeEmailsAlchemyInformation_onlyCleanContent(ArrayList<Email> emails) {
		try {
			int i = 0;
			try {
				for (Email e : emails) {
					String query = "update email_messages set alchemy_clean_text = '" + Util
							.getStringFromDocument(e.getAlchemy_document_onlycleanedcontet()).replaceAll("'", "''")
							+ "' where email_id = " + e.getId() + ";";
					PreparedStatement preparedStmt = connect.prepareStatement(query);
					preparedStmt.executeUpdate();
					i++;
					if (i % 100 == 0)
						System.out.println("scrivo nel db " + i);
				}
			} catch (Exception ex) {
				System.out.println("Error: " + ex.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}