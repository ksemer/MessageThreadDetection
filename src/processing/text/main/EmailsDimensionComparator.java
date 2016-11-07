package processing.text.main;

//STEP 1. Import required packages
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import processing.text.enron.textproc.AgglomerativeClustering;
import processing.text.enron.textproc.Clustering;
import processing.text.enron.textproc.DBScan;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.TextPreprocessing;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;

public class EmailsDimensionComparator {

	static final String DATASET = "bc3";
	static final String DB_URL = "jdbc:mysql://localhost:3306/" + DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";

	private Table<Integer, Integer, Double> time_dim_map = HashBasedTable.create();
	private Table<Integer, Integer, Double> peole_dim_map = HashBasedTable.create();
	private Table<Integer, Integer, Double> text_dim_map = HashBasedTable.create();
	private Table<Integer, Integer, Double>[] score_dim_map = new Table[1];

	/*
	 * private Table<Integer, Integer, Double> score_dim_map2 =
	 * HashBasedTable.create(); private Table<Integer, Integer, Double>
	 * score_dim_map3 = HashBasedTable.create(); private Table<Integer, Integer,
	 * Double> score_dim_map4 = HashBasedTable.create(); private Table<Integer,
	 * Integer, Double> score_dim_map5 = HashBasedTable.create(); private
	 * Table<Integer, Integer, Double> score_dim_map6 = HashBasedTable.create();
	 * private Table<Integer, Integer, Double> score_dim_map7 =
	 * HashBasedTable.create(); private Table<Integer, Integer, Double>
	 * score_dim_map8 = HashBasedTable.create(); private Table<Integer, Integer,
	 * Double> score_dim_map9 = HashBasedTable.create(); private Table<Integer,
	 * Integer, Double> score_dim_map10 = HashBasedTable.create();
	 */

	public static void main(String[] args) {

		EmailsDimensionComparator e=new EmailsDimensionComparator();
	}

	public EmailsDimensionComparator() {
		for (int i = 0; i < score_dim_map.length; i++) {
			score_dim_map[i] = HashBasedTable.create();
		}
		boolean considerSubject = true;
		boolean considerQuotations = true;
		String language="english";
		String wordnetDir="/home/giacomo/Programs/WordNet-3.0/dict";

		Connection conn = null;
		Statement stmt = null;
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

			AlchemyAPI alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key_elizabeth.txt");

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

					if (!considerSubject) {
						e.setSubject("");
					}
					if (!considerQuotations) {
						e.setText(e.getCleanContent());
						try {
							// System.out.println("alchemy query: "+e.getId());
							e.setAlchemy_document_onlycleanedcontet(alchemyObj.TextGetCombined(e.getCleanContent()));
						} catch (Exception exc) {
							System.err.println("error for email text: " + e.getCleanContent());
							exc.printStackTrace();
						}
						e.setAlchemy_document(e.getAlchemy_document_onlycleanedcontet());
					}
				} else
					e.insertReceiver(rs.getInt("recipient_id"));
				emails_map.put(id, e);
			}
			rs.close();
			stmt.close();
			ArrayList<Email> emails = new ArrayList<>();
			for (Email e : emails_map.values())
				emails.add(e);

			/*
			 * DBInserts dbins=new DBInserts(DB_URL);
			 * dbins.writeEmailsAlchemyInformation_onlyCleanContent(emails);
			 */

			System.out.println("BoWs Creation...");
			emails = TextPreprocessing.createBowAndWeight(emails, true, true, language, 2, DATASET);

			//open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);
			
			HashMap<Integer, HashMap<Integer, Double>> score_map = new HashMap<>();
			EmailsComparator comparator=new EmailsComparator(new EmailSimGlobalCloseness(DATASET),emails, wordnet);

			System.out.println("score calculation...");
			for (int i = 0; i < emails.size(); i++) {
				Email ei = emails.get(i);
				HashMap<Integer, Double> ei_map = new HashMap<>();

				for (int j = 0; j < emails.size(); j++) {
					Email ej = emails.get(j);
					if(ei.getId()==ej.getId())
						continue;

					// System.out.println("text: \n"+ ei.getText());
					double simcos_text = comparator.compareContent_CosineSimilarity(ei, ej);
					double simcos_subj = comparator.compareContent_SubjectCosineSimilarity(ei, ej);
					// double
					// jacc_subj=EmailsComparator.compareContent_SubjectJaccardSimilarity(ei,
					// ej);
					double simcos_concepts = comparator.compareContent_AlchemyConceptsCosineSimilairty(ei, ej);
					double simcos_keywords = comparator.compareContent_AlchemykeywordCosineSimilairty(ei, ej);
					double simcos_entities = comparator.compareContent_AlchemyEntitiesCosineSimilairty(ei, ej);
					double jacc_users = comparator.comparePeople_UsersJaccardSimilarity(ei, ej);
					double closeness_users = comparator.comparePeople_UsersClosenessSimilarity(ei, ej);

					double time_dist = comparator.compareTime_logDistInDays(ei, ej);

					int id1 = ei.getId();
					int id2 = ej.getId();
					/*if (id1 > id2) {
						id2 = ei.getId();
						id1 = ej.getId();
					}*/
					//System.out.println(id1+" "+id2);
					this.time_dim_map.put(id1, id2, time_dist);
					this.peole_dim_map.put(id1, id2, jacc_users);
					//this.text_dim_map.put(ei.getId(), ej.getId(),
					//		(simcos_text +simcos_subj+ simcos_concepts + simcos_entities + simcos_keywords));

					//this.score_dim_map[0].put(id1, id2,(1+simcos_text ) * (1 + time_dist)*(1+closeness_users)*(1 + jacc_users));
					//this.score_dim_map[1].put(id1, id2, (1 + simcos_text) * (1 + time_dist)*(1+closeness_users)*(1 + jacc_users));
					this.score_dim_map[0].put(id1, id2, (1 + simcos_text) *(1 + simcos_subj) * (1 + time_dist)*(1 + jacc_users)*(1 + closeness_users)*(1 + simcos_concepts)*(1 + simcos_keywords)*(1 + simcos_entities));
					/*this.score_dim_map[3].put(ei.getId(), ej.getId(), (1 + simcos_entities));
					this.score_dim_map[4].put(ei.getId(), ej.getId(), (1 + simcos_concepts));
					this.score_dim_map[5].put(ei.getId(), ej.getId(), (1 + simcos_keywords));
					this.score_dim_map[6].put(ei.getId(), ej.getId(),
							(1 + simcos_concepts + simcos_entities + simcos_keywords));
					this.score_dim_map[7].put(ei.getId(), ej.getId(), (1 + simcos_text));
					this.score_dim_map[8].put(ei.getId(), ej.getId(), (1 + simcos_subj));
					this.score_dim_map[9].put(ei.getId(), ej.getId(),
							(1 + simcos_text) * (1 + simcos_subj) * (1 + simcos_concepts) * (1 + simcos_entities)
									* (1 + simcos_keywords) * (1 + time_dist) * (1 + jacc_users));
					this.score_dim_map[10].put(ei.getId(), ej.getId(), (1 + simcos_text) * (1 + simcos_subj)
							* (1 + simcos_concepts) * (1 + simcos_entities) * (1 + simcos_keywords));
					this.score_dim_map[11].put(ei.getId(), ej.getId(), (1 + simcos_text) * (1 + time_dist));
					this.score_dim_map[12].put(ei.getId(), ej.getId(), (1 + simcos_subj) * (1 + time_dist)*(1+closeness_users));

					ei_map.put(ej.getId(), (1 + simcos_text + simcos_concepts + simcos_entities + simcos_keywords)
							* (1 + time_dist) * (1 + jacc_users));*/
				}
				score_map.put(ei.getId(), ei_map);

			}

			/*
			//DBASCAN
			for (int i = 0; i < score_dim_map.length; i++) {
				double[] best = new double[5];
				for (double th = 0; th < 20; th += 0.25) {
					DBScan dbscan = new DBScan(emails, score_dim_map[i], th);
					dbscan.emailsClustering();
					double purity = Util.truncate(dbscan.evaluatePurity());
					int[] meas = dbscan.evaluateTPFNFPTN();
					double p = meas[0] * 1.0 / (meas[0] + meas[2]);
					double r = meas[0] * 1.0 / (meas[0] + meas[1]);
					// System.out.print("\ndistmeasure: "+i+"\tth "+th+" \nPUR:
					// "+purity);
					// System.out.print("\tp: "+Util.truncate(p));
					// System.out.print("\tr: "+Util.truncate(r));
					// System.out.println("\tf1:
					// "+Util.truncate((2.0*(p*r)/(p+r))));
					if (Util.truncate(2.0 * (p * r) / (p + r)) > best[0]) {
						best[0] = Util.truncate((2.0 * (p * r) / (p + r)));
						best[1] = +Util.truncate(p);
						best[2] = +Util.truncate(r);
						best[3] = purity;
						best[4] = th;
					}
				}
				System.out.print("\ndistmeasure: " + i + "\nth-best: " + best[4] + " \npurity: " + best[3]);
				System.out.print("\tp: " + best[1]);
				System.out.print("\tr: " + best[2]);
				System.out.println("\tf1: " + best[0]);

			}*/
			
			
			//AGGLOMERATIVE
			long maxTimeSpanInDays=30;
			for (int i = 0; i < score_dim_map.length; i++) {
				double[] best = new double[5];
				for (double th = 0; th < 10; th += 0.05) {
					Clustering clusteringModel=new AgglomerativeClustering(emails, score_dim_map[i], th, "average-link", comparator, maxTimeSpanInDays);
					//Clustering clusteringModel = new DBScan(emails, score_dim_map[i], th);
					clusteringModel.emailsClustering();
					double purity = Util.truncate(clusteringModel.evaluatePurity());
					int[] meas = clusteringModel.evaluateTPFNFPTN();
					double p = meas[0] * 1.0 / (meas[0] + meas[2]);
					double r = meas[0] * 1.0 / (meas[0] + meas[1]);
					System.out.print("\ndistmeasure: "+i+"\tth "+th+" \nPUR: "+purity);
					System.out.print("\tp: "+Util.truncate(p));
					System.out.print("\tr: "+Util.truncate(r));
					System.out.println("\tf1: "+Util.truncate((2.0*(p*r)/(p+r))));
					if (Util.truncate(2.0 * (p * r) / (p + r)) > best[0]) {
						best[0] = Util.truncate((2.0 * (p * r) / (p + r)));
						best[1] = +Util.truncate(p);
						best[2] = +Util.truncate(r);
						best[3] = purity;
						best[4] = th;
					}
				}
				System.out.print("\ndistmeasure: " + i + "\nth-best: " + best[4] + " \npurity: " + best[3]);
				System.out.print("\tp: " + best[1]);
				System.out.print("\tr: " + best[2]);
				System.out.println("\tf1: " + best[0]);

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

	public Table<Integer, Integer, Double> getTime_dim_map() {
		return time_dim_map;
	}

	public void setTime_dim_map(Table<Integer, Integer, Double> time_dim_map) {
		this.time_dim_map = time_dim_map;
	}

	public Table<Integer, Integer, Double> getPeole_dim_map() {
		return peole_dim_map;
	}

	public void setPeole_dim_map(Table<Integer, Integer, Double> peole_dim_map) {
		this.peole_dim_map = peole_dim_map;
	}

	public Table<Integer, Integer, Double>[] getScore_dim_map() {
		return score_dim_map;
	}

	public void setScore_dim_map(Table<Integer, Integer, Double>[] score_dim_map) {
		this.score_dim_map = score_dim_map;
	}

	public Table<Integer, Integer, Double> getText_dim_map() {
		return text_dim_map;
	}

	public void setText_dim_map(Table<Integer, Integer, Double> text_dim_map) {
		this.text_dim_map = text_dim_map;
	}

}