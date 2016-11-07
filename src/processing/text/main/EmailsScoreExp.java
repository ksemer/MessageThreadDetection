package processing.text.main;

//STEP 1. Import required packages
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.TextPreprocessing;
import utils.Util;

public class EmailsScoreExp {

	static final String DATASET = "bc3";
	static final String DB_URL = "jdbc:mysql://localhost:3306/" + DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";

	public static void main(String[] args) {
		new EmailsScoreExp();
	}

	public EmailsScoreExp() {
		Connection conn = null;
		Statement stmt = null;
		String language="english";
		String wordnetDir="/home/giacomo/Programs/WordNet-3.0/dict";
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			// String sql = "SELECT * FROM email_messages WHERE subject LIKE
			// '%Hey!%' limit 1000";
			String sql = "SELECT *" + " FROM email_messages e, communications c WHERE "// e.subject
																						// LIKE
																						// '%Hey!%'
																						// AND"
					+ " e.email_id = c.email_id  LIMIT 500000";

			ResultSet rs = stmt.executeQuery(sql);
			// ArrayList<Email> emails=new ArrayList<>();
			// ArrayList<Integer> emails_IDs=new ArrayList<>();
			HashMap<Integer, Email> emails_map = new HashMap<>();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			while (rs.next()) {
				int id = rs.getInt("email_id");
				Email e = emails_map.get(id);
				if (e == null) {
					int senderid = rs.getInt("sender_id");
					int thireadid = rs.getInt("thread_id");
					String text = rs.getString("text");
					String clean_text = rs.getString("clean_text");
					String quotation_text = rs.getString("quotation_text");
					String alchemy_doc = rs.getString("alchemy");
					String subject = rs.getString("subject");
					subject = subject.toLowerCase();
					subject = subject.replaceAll("re:", "");
					subject = subject.replaceAll("fw:", "");
					subject = subject.replaceAll("fwd:", "");

					e = new Email(id, thireadid, senderid, subject, text);
					e.insertReceiver(rs.getInt("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					e.setCleanContent(clean_text);
					e.setCleanQuotes(quotation_text);
					e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
				} else
					e.insertReceiver(rs.getInt("recipient_id"));
				emails_map.put(id, e);
			}
			rs.close();
			stmt.close();
			ArrayList<Email> emails = new ArrayList<>();
			for (Email e : emails_map.values())
				emails.add(e);

			System.out.println("BoWs Creation");
			emails = TextPreprocessing.createBowAndWeight(emails, true, true, language, 2, DATASET);

			//open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);

			int idTarget = 10;
			Email e = emails.get(idTarget);
			System.out.println("***** TARGET EMAIL:\n***** THREAD ID: " + e.getThreadid());
			System.out.println("***** subject: " + e.getSubject());
			System.out.println("***** keywords: " + Arrays.toString(e.getAlchemy_keywords_onlyStringList().toArray()));
			System.out.println("***** entities: " + Arrays.toString(e.getAlchemy_entities_onlyStringList().toArray()));
			System.out.println("***** concepts: " + Arrays.toString(e.getAlchemy_concepts_onlyStringList().toArray()));

			HashMap<Integer, Double> text_sim_map = new HashMap<>();
			EmailsComparator comparator=new EmailsComparator(new EmailSimGlobalCloseness(DATASET), emails, wordnet);
			for (int i = 0; i < emails.size(); i++) {
				if (i == idTarget) {
					continue;
				}
				Email ei = emails.get(i);
				// System.out.println("text: \n"+ ei.getText());
				double simcos_text = comparator.compareContent_CosineSimilarity(e, ei);
				double simcos_subj = comparator.compareContent_SubjectCosineSimilarity(e, ei);
				double jacc_subj = comparator.compareContent_SubjectJaccardSimilarity(e, ei);
				double simcos_concepts = comparator.compareContent_AlchemyConceptsCosineSimilairty(e, ei);
				double simcos_keywords = comparator.compareContent_AlchemykeywordCosineSimilairty(e, ei);
				double simcos_entities = comparator.compareContent_AlchemyEntitiesCosineSimilairty(e, ei);

				double jacc_users = comparator.comparePeople_UsersJaccardSimilarity(e, ei);

				double time_dist = comparator.compareTime_logDistInDays(e, ei);
				text_sim_map.put(i,
						(1 + simcos_text + simcos_subj + simcos_concepts + simcos_entities + simcos_keywords)
								* (1 + time_dist) * (1 + jacc_users));
			}

			System.out.println("\nscore\tthreadID\temailID\tsubject");
			ValueComparatorInt bvc = new ValueComparatorInt(text_sim_map);
			Map<Integer, Double> sortedPrediction = new TreeMap<Integer, Double>(bvc);
			sortedPrediction.putAll(text_sim_map);
			for (Entry<Integer, Double> entry : sortedPrediction.entrySet()) {
				if (entry.getValue() <= 1.2)
					break;
				Email email = emails.get(entry.getKey());
				System.out.println(Util.truncate(entry.getValue()) + "\t" + email.getThreadid() + "\t" + email.getId()
						+ "\t" + email.getSubject());
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try
		} // end try
	}

	public Email readEmailConnections(Email email, Connection conn) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM communications WHERE email_id=" + email.getId();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				email.insertReceiver(rs.getInt("recipient_id"));
				email.setSender(rs.getInt("sender_id"));
				email.setTimestamp(rs.getTimestamp("time"));
			}

			rs.close();
			stmt.close();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		}
		return email;
	}

}