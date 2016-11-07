package processing.text.main;

//STEP 1. Import required packages
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import processing.text.enron.textproc.DBInserts;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.RetrieveEmailQuotes;
import processing.text.enron.textproc.TextPreprocessing;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;

public class EmailsReader {

	static final String DATASET = "facebook";
	static final String DB_URL = "jdbc:mysql://localhost:3306/" + DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";

	public static void main(String[] args) {
		new EmailsReader();
	}

	public EmailsReader() {
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

			while (rs.next()) {
				int id = rs.getInt("email_id");
				Email e = emails_map.get(id);
				if (e == null) {
					long senderid = rs.getLong("sender_id");
					String text = rs.getString("text");
					//String clean_text = rs.getString("clean_text");
					//String quotation_text = rs.getString("quotation_text");
					//String alchemy_doc = rs.getString("alchemy");
					String subject = "";/*rs.getString("subject");
					subject = subject.toLowerCase();
					subject = subject.replaceAll("re:", "");
					subject = subject.replaceAll("fw:", "");
					subject = subject.replaceAll("fwd:", "");*/

					e = new Email(id, senderid, subject, text);
					e.insertReceiver(rs.getLong("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					//e.setCleanContent(clean_text);
					//e.setCleanQuotes(quotation_text);
					//e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
				} else
					e.insertReceiver(rs.getLong("recipient_id"));
				emails_map.put(id, e);
			}
			rs.close();
			stmt.close();
			ArrayList<Email> emails = new ArrayList<>();
			for (Email e : emails_map.values())
				emails.add(e);

			/*
			 //* **** CLEANING AND SPLITTIN EMAILS CONTENT ****
			 System.out.println(); 
			 RetrieveEmailQuotes quotesRet=new RetrieveEmailQuotes(emails);
			  quotesRet.createAllEmailSentencesHashCode(); 
			  for(int i=0;i<emails.size();i++){ 
				  if(i%1000==0){ 
					  System.out.println( "cleaning "+i); 
				  } 
				  Email e=emails.get(i);
				  e=quotesRet.splitContentAndQuotes(e);
				  //e=quotesRet.splitContentAndQuotesForBC3Datas(e);
				  System.out.println("\n\nemail id: "+e.getId()+"\ttime: "+e.getTimestamp()+"\tsubject: "+e.getSubject()+"\t");
				  System.out.println("***** clean content: *****\n"+e.getCleanContent());
				  System.out.println("***** clean quotes: *****\n"+ e.getCleanQuotes()); 
			  }
			  System.err.println("WRITE THE CONTENT CLEANED ON THE DB..");
			  DBInserts dbins=new DBInserts(DB_URL);
			  dbins.writeEmailsCleanedContent(emails);
			  System.exit(0);
			 */

			
			 // GET AND STORE ALCHEMY INFO 
			  DBInserts dbins=new DBInserts(DB_URL);
			AlchemyAPI alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key_elizabeth.txt"); 
			 for(int i=0;i<100000;i++){ 
				 if(i%1==0){
					 System.out.println("Querying Alchemy for "+i); 
				} 
				 Email e=emails.get(i); 
				 try{
					 e.setAlchemy_document(alchemyObj.TextGetCombined(e.getSubject()+ "\n"+e.getText())); 
					 dbins.writeSingleEmailAlchemyInformation(e);
				 }catch(Exception exc){ 
					 System.err.println("error for email text: "+e.getSubject()+"\n"+e.getText());
					 exc.printStackTrace();
				} 
			}
			  System.exit(0);
			 

			System.out.println("BoWs");
			emails = TextPreprocessing.createBowAndWeight(emails, true, true, language, 2, DATASET);


			//open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);

			
			Email e = emails.get(4);
			System.out.println("subject: " + e.getSubject());

			System.out.println("keywords: " + Arrays.toString(e.getAlchemy_keywords_onlyStringList().toArray()));
			System.out.println("entities: " + Arrays.toString(e.getAlchemy_entities_onlyStringList().toArray()));
			System.out.println("concepts: " + Arrays.toString(e.getAlchemy_concepts_onlyStringList().toArray()));
			// System.out.println("text: \n"+ e.getText());
			EmailsComparator comparator=new EmailsComparator(new EmailSimGlobalCloseness(DATASET),emails,wordnet);
			for (int i = 0; i < emails.size(); i++) {
				Email ei = emails.get(i);
				System.out.println("\nsubject: " + ei.getSubject());
				// System.out.println("text: \n"+ ei.getText());

				System.out.println("simcos text: " + comparator.compareContent_CosineSimilarity(e, ei));
				System.out.println("simcos subj: " + comparator.compareContent_SubjectCosineSimilarity(e, ei));
				System.out.println("simcos subj: " + comparator.compareContent_SubjectJaccardSimilarity(e, ei));

				System.out.print(
						"simcos concepts: " + comparator.compareContent_AlchemyConceptsCosineSimilairty(e, ei));
				System.out
						.print(", entities: " + comparator.compareContent_AlchemyEntitiesCosineSimilairty(e, ei));
				System.out.println(
						", keywords: " + comparator.compareContent_AlchemykeywordCosineSimilairty(e, ei));
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