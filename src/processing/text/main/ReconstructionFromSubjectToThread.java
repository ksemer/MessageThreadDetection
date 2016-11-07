package processing.text.main;
import java.lang.reflect.Method;
//STEP 1. Import required packages
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;
import processing.text.enron.textproc.AgglomerativeClustering;
import processing.text.enron.textproc.AgglomerativeClusteringSupervised;
import processing.text.enron.textproc.Clustering;
import processing.text.enron.textproc.DBInserts;
import processing.text.enron.textproc.DBSCANSupervised;
import processing.text.enron.textproc.DBScan;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.TextPreprocessing;
import processing.text.enron.textproc.WekaClassification;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;
import weka.core.json.sym;

public class ReconstructionFromSubjectToThread {

	static final String DATASET="bc3";
	static final String DB_URL = "jdbc:mysql://localhost:3306/"+DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";
	
	public static void main(String[] args) {
		new ReconstructionFromSubjectToThread();	
	}
   
	public ReconstructionFromSubjectToThread() {		
		//**** PARAMETER
		boolean considerSubject=true;
		boolean considerQuotations=true;
		 ArrayList<String> features=new ArrayList<>();
		features.add("compareContent_CosineSimilarity");
		features.add("compareContent_SubjectCosineSimilarity");
		//features.add("compareContent_SubjectJaccardSimilarity");
		//features.add("compareContent_AlchemykeywordCosineSimilairty");
		//features.add("compareContent_AlchemyEntitiesCosineSimilairty");
		//features.add("compareContent_AlchemyConceptsCosineSimilairty");
		features.add("compareTime_logDistInDays");
		features.add("comparePeople_UsersJaccardSimilarity");
		features.add("comparePeople_UsersClosenessSimilarity");
		//features.add("comparePeople_AVGPairsOfUsersConditionalProbability");

		//String classifierName="weka.classifiers.trees.J48";
		String classifierName="weka.classifiers.trees.RandomForest";
		//String classifierName="weka.classifiers.functions.LibSVM";
		//String classifierName="weka.classifiers.functions.SMO";
		String language="italian";
		String wordnetDir="/home/giacomo/Programs/WordNet-3.0/dict";
		int numInstSameThread=10;
		int numInstDifferentThread=30;		
		long seedrandom=1;
		long maxTimeSpanInDays=30;
		String agglomerativeDistanceCalculation="average-link";
		 //**** END P{ARAMETER		
		
		Connection conn = null;
		Statement stmt = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL,USER,PASS);	 
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			//String sql = "SELECT * FROM email_messages WHERE subject LIKE '%Hey!%' limit 1000";
			String sql =  "SELECT *"
			+ " FROM email_messages e, communications c WHERE "//e.subject LIKE '%Hey!%' AND"
			+ " e.email_id = c.email_id  LIMIT 50000";
			  
			  
			ResultSet rs = stmt.executeQuery(sql);
			//ArrayList<Email> emails=new ArrayList<>();
			//ArrayList<Integer> emails_IDs=new ArrayList<>();
			HashMap<Integer, Email> emails_map=new HashMap<>();
			  
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			AlchemyAPI alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key_elizabeth.txt");
			
			while(rs.next()){
				int id  = rs.getInt("email_id");
				Email e=emails_map.get(id);
				if(e==null){
					int senderid  = rs.getInt("sender_id");
					int thireadid  = rs.getInt("thread_id");
						//int thireadid=0;
					String text = rs.getString("text");
					String clean_text= rs.getString("clean_text");
					String quotation_text= rs.getString("quotation_text");
					String alchemy_doc= rs.getString("alchemy");
					String subject=rs.getString("subject");
					subject=subject.toLowerCase();       
					subject=subject.replaceAll("re:", "");
					subject=subject.replaceAll("fw:", "");
					subject=subject.replaceAll("fwd:", "");					
					
					
					e=new Email(id,thireadid, senderid, subject, text);
					e.insertReceiver(rs.getInt("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					e.setCleanContent(clean_text);
					e.setCleanQuotes(quotation_text);
					e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
				
					if(!considerSubject){
						e.setSubject("");
					}
					if(!considerQuotations){
						e.setText(e.getCleanContent());				
						try{
							//System.out.println("alchemy query: "+e.getId());
							e.setAlchemy_document_onlycleanedcontet(alchemyObj.TextGetCombined(e.getCleanContent()));
						}catch(Exception exc){
							System.err.println("error for email text: "+e.getCleanContent());
							exc.printStackTrace();
						}				
						e.setAlchemy_document(e.getAlchemy_document_onlycleanedcontet());
					}
				}
				else
					e.insertReceiver(rs.getInt("recipient_id"));		 
				emails_map.put(id,e);	         
			}
			rs.close();
			stmt.close();
			ArrayList<Email> emails=new ArrayList<>();
			ArrayList<Integer> threadIDs=new ArrayList<>();
			
			for(Email e : emails_map.values()){
				emails.add(e);	      			
				if(!threadIDs.contains(e.getThreadid()))
					threadIDs.add(e.getThreadid());
			}
			
			/*DBInserts dbins=new DBInserts(DB_URL);
			dbins.writeEmailsAlchemyInformation_onlyCleanContent(emails);*/
			
		    System.out.println("BoWs Creation...");
			emails=TextPreprocessing.createBowAndWeight(emails, true, true, language, 2, DATASET);

			//open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);

			ArrayList<Email> train=new ArrayList<>();
			ArrayList<Email> test=new ArrayList<>();
	
			HashMap<String, Integer>  threadSubjectToId_train=new HashMap<>();
			ArrayList<Integer> threadIDs_test=new ArrayList<>();
			int cont_t=0;
			for(Email e : emails){
				test.add(e);
				Email train_e=new Email(e.getId(), e.getThreadid(), e.getSender(), e.getSubject(), e.getText());
				train_e.setAlchemy_document(e.getAlchemy_document());
				train_e.setAlchemy_document_onlycleanedcontet(e.getAlchemy_document_onlycleanedcontet());
				train_e.setBow(e.getBow());
				train_e.setCleanContent(e.getCleanContent());
				train_e.setCleanQuotes(e.getCleanQuotes());
				train_e.setTimestamp(e.getTimestamp());
				train_e.setReceiversid(e.getReceiversid());
				if(threadSubjectToId_train.keySet().contains(e.getSubject().trim())){
					train_e.setThreadid(threadSubjectToId_train.get(e.getSubject().trim()));				
					train.add(train_e);
				}
				else{
					threadSubjectToId_train.put(e.getSubject().trim(), cont_t);
					train_e.setThreadid(cont_t);				
					train.add(train_e);
					cont_t++;
				}
			}

			Table<Integer, Integer, Double> score_AlreadyCalc=HashBasedTable.create();
			
			//DBSCANSupervised clusteringModel=new DBSCANSupervised(train, test, dbscanTH, classifierName, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET);
		  	AgglomerativeClusteringSupervised clusteringModel=new AgglomerativeClusteringSupervised(train, test, 0.5, classifierName, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET,agglomerativeDistanceCalculation,maxTimeSpanInDays, wordnet, score_AlreadyCalc, "");
	  		double[] best = new double[5];
	  		
	  		
			for (double th = 0; th < 1; th += 0.05) {
				clusteringModel.setThreshold(th);
				clusteringModel.resetClusters();
				long timeStart=System.currentTimeMillis();
				System.out.print("start clusterization...");
				clusteringModel.emailsClustering();
				System.out.println("ended in :"+((System.currentTimeMillis()-timeStart))+" mSec");
				double purity = Util.truncate(clusteringModel.evaluatePurity());
				int[] meas = clusteringModel.evaluateTPFNFPTN();
				double p = meas[0] * 1.0 / (meas[0] + meas[2]);
				double r = meas[0] * 1.0 / (meas[0] + meas[1]);
				System.out.print("\nth "+th+" \nPUR: "+purity);
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
			System.out.print("\nth-best: " + best[4] + " \npurity: " + best[3]);
			System.out.print("\tp: " + best[1]);
			System.out.print("\tr: " + best[2]);
			System.out.println("\tf1: " + best[0]);
		  	
	
	  		  
		}catch(SQLException se){
			se.printStackTrace();
		}catch(Exception e){
	      e.printStackTrace();
		}finally{
	      try{
	         if(stmt!=null)
	            stmt.close();
	      }catch(SQLException se2){
	      }// nothing we can do
	      try{
	         if(conn!=null)
	            conn.close();
	      }catch(SQLException se){
	         se.printStackTrace();
	      }//end finally try
		}//end try
	}
   
   
}