package processing.text.main;

//STEP 1. Import required packages
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.textproc.AgglomerativeClusteringSupervised;
import processing.text.enron.textproc.Clustering;
import processing.text.enron.textproc.TextPreprocessing;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;
import weka.classifiers.Classifier;

public class InterdatasetThreadReconstruction {

	static final String DATASET_SOURCE="apache";
	static final String DATASET_TARGET="bc3";
	static final String DB_URL_SOURCE = "jdbc:mysql://localhost:3306/"+DATASET_SOURCE;
	static final String DB_URL_TARGET = "jdbc:mysql://localhost:3306/"+DATASET_TARGET;
	static final String USER = "giacomo";
	static final String PASS = "12345";
	
	public static void main(String[] args) {
		new InterdatasetThreadReconstruction();	
	}
 
	public InterdatasetThreadReconstruction() {	
		//**** PARAMETER
		boolean considerSubject=false;
		boolean considerQuotations=true;
		boolean useAlchemy=true;
		 ArrayList<String> features=new ArrayList<>();
		features.add("compareContent_CosineSimilarity");
		//features.add("compareContent_SubjectCosineSimilarity");
		//features.add("compareContent_SubjectJaccardSimilarity");
		features.add("compareContent_AlchemykeywordCosineSimilairty");
		features.add("compareContent_AlchemyEntitiesCosineSimilairty");
		features.add("compareContent_AlchemyConceptsCosineSimilairty");
		///features.add("compareContent_AlchemykeywordWordnetSimilarity");
		//features.add("compareContent_AlchemyEntitiesWordnetSimilarity");
		//features.add("compareContent_AlchemyConceptsWordnetSimilarity");
		
		features.add("compareTime_logDistInDays");
		//features.add("compareTime_logDistInHours");
		features.add("comparePeople_UsersJaccardSimilarity");
		//features.add("comparePeople_UsersClosenessSimilarity");
		//features.add("comparePeople_AVGPairsOfUsersConditionalProbability");

		//String classifierName="weka.classifiers.trees.J48";
		String classifierName="weka.classifiers.trees.RandomForest";
		//String classifierName="weka.classifiers.functions.LibSVM";
		//String classifierName="weka.classifiers.functions.SMO";
		String language="english";
		String wordnetDir="/home/giacomo/Programs/WordNet-3.0/dict";
		int numInstSameThread=10;
		int numInstDifferentThread=20;		
		long seedrandom=1;
		long maxTimeSpanInDays=30;
		double startTH=0.95,stopTH=0.05,stepTH=0.05;
		String expComment="";

		String agglomerativeDistanceCalculation="average-link";
		 //**** END P{ARAMETER		
		
		Connection conn = null;
		Statement stmt = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to source database..."+DATASET_SOURCE);
			conn = DriverManager.getConnection(DB_URL_SOURCE,USER,PASS);	 
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			String sql =  "SELECT *"
			+ " FROM email_messages e, communications c WHERE "
			+ " e.email_id = c.email_id ";
			  			  
			ResultSet rs = stmt.executeQuery(sql);
			//ArrayList<Email> emails=new ArrayList<>();
			//ArrayList<Integer> emails_IDs=new ArrayList<>();
			HashMap<Integer, Email> emails_map=new HashMap<>();
			  			
			AlchemyAPI alchemyObj=null;
			if(useAlchemy)
				alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key_elizabeth.txt");
			
			while(rs.next()){
				int id  = rs.getInt("email_id");
				Email e=emails_map.get(id);
				if(e==null){
					long senderid  = rs.getLong("sender_id");
					int thireadid  = rs.getInt("thread_id");
					String text = rs.getString("text");
					String clean_text= rs.getString("clean_text");
					String quotation_text= rs.getString("quotation_text");
					//String subject="";
					String subject=rs.getString("subject");
					subject=subject.toLowerCase();       
					subject=subject.replaceAll("re:", "");
					subject=subject.replaceAll("fw:", "");
					subject=subject.replaceAll("fwd:", "");			
					
					
					e=new Email(id,thireadid, senderid, subject, text);
					e.insertReceiver(rs.getLong("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					e.setCleanContent(clean_text);
					e.setCleanQuotes(quotation_text);
					
					if(useAlchemy){
						String alchemy_doc= rs.getString("alchemy");
						e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
					}
					
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
			conn.close();
			ArrayList<Email> source_emails=new ArrayList<>();
			ArrayList<Integer> source_threadIDs=new ArrayList<>();
			
			for(Email e : emails_map.values()){
				source_emails.add(e);	      			
				if(!source_threadIDs.contains(e.getThreadid()))
					source_threadIDs.add(e.getThreadid());
			}

			
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to target database..."+DATASET_TARGET);
			conn = DriverManager.getConnection(DB_URL_TARGET,USER,PASS);	 
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			sql =  "SELECT *"
			+ " FROM email_messages e, communications c WHERE "
			+ " e.email_id = c.email_id ";
				  			  
			rs = stmt.executeQuery(sql);
			emails_map=new HashMap<>();
			
			while(rs.next()){
				int id  = rs.getInt("email_id");
				Email e=emails_map.get(id);
				if(e==null){
					long senderid  = rs.getLong("sender_id");
					int thireadid  = rs.getInt("thread_id");
					String text = rs.getString("text");
					String clean_text= rs.getString("clean_text");
					String quotation_text= rs.getString("quotation_text");
					//String subject="";
					String subject=rs.getString("subject");
					subject=subject.toLowerCase();       
					subject=subject.replaceAll("re:", "");
					subject=subject.replaceAll("fw:", "");
					subject=subject.replaceAll("fwd:", "");				
					
					
					e=new Email(id,thireadid, senderid, subject, text);
					e.insertReceiver(rs.getLong("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					e.setCleanContent(clean_text);
					e.setCleanQuotes(quotation_text);
					
					if(useAlchemy){
						String alchemy_doc= rs.getString("alchemy");
						e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
					}
					
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
			ArrayList<Email> target_emails=new ArrayList<>();
			ArrayList<Integer> target_threadIDs=new ArrayList<>();
			
			for(Email e : emails_map.values()){
				target_emails.add(e);	      			
				if(!target_threadIDs.contains(e.getThreadid()))
					target_threadIDs.add(e.getThreadid());
			}
			
			
			
			/*DBInserts dbins=new DBInserts(DB_URL);
			dbins.writeEmailsAlchemyInformation_onlyCleanContent(emails);*/
			
		    System.out.println("BoWs Creation...");
		    source_emails=TextPreprocessing.createBowAndWeight(source_emails, true, true, language, 2, DATASET_SOURCE);
		    target_emails=TextPreprocessing.createBowAndWeight(target_emails, true, true, language, 2, DATASET_TARGET);
			
		  //open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);
			
				
			double[] best = new double[6];
			
			Table<Integer, Integer, Double> score_AlreadyCalc=HashBasedTable.create();
			Classifier classifier=null;
			ArrayList<Cluster> clusters=null;
			
			for (double th = startTH; th >=stopTH; th -= stepTH) {		
				
				//Clustering clusterModel=new DBSCANSupervised(train, test, th, classifierName, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET);
				Clustering clusterModel=null;
				if(classifier==null){
					clusterModel=new AgglomerativeClusteringSupervised(source_emails, target_emails, th, classifierName, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET_TARGET, agglomerativeDistanceCalculation,maxTimeSpanInDays,wordnet,score_AlreadyCalc,  expComment);
					classifier=((AgglomerativeClusteringSupervised)clusterModel).getClassifierModel();
				}else{
					clusterModel=new AgglomerativeClusteringSupervised(target_emails, th, classifier, features, DATASET_TARGET, agglomerativeDistanceCalculation,maxTimeSpanInDays,wordnet,score_AlreadyCalc, expComment);
				}
				if(clusters!=null){
					clusterModel.setClusters(clusters);
				}
				
				//System.out.println("MAP SIZE: "+score_AlreadyCalc.size());
				long timeStart=System.currentTimeMillis();
				System.out.print("\tstart clusterization");
				clusterModel.emailsClustering();
				System.out.println("\tended in :"+((System.currentTimeMillis()-timeStart))+" mSec");
				
				clusters=clusterModel.getClusters();
				
				
				double purity=Util.truncate(clusterModel.evaluatePurity());			  	
			  	int [] meas=clusterModel.evaluateTPFNFPTN();
			  	double p=meas[0]*1.0/(meas[0]+meas[2]);
			  	double r=meas[0]*1.0/(meas[0]+meas[1]);
			  	double f1=(2.0*(p*r)/(p+r));
			  			  	
				System.out.print("\nth "+th+" \nPUR: "+purity);
				System.out.print("\tp: "+Util.truncate(p));
				System.out.print("\tr: "+Util.truncate(r));
				System.out.print("\tf1: "+Util.truncate(f1));
				System.out.println("\t#clusters: "+clusters.size()+" correct("+target_threadIDs.size()+")");
				if (f1 > best[0]) {
					best[0] = Util.truncate(f1);
					best[1] = Util.truncate(p);
					best[2] = Util.truncate(r);
					best[3] = purity;
					best[4] = th;
					best[5] = clusters.size();
				}
			}
			System.out.print("*******\n\ninter-DATASET: "+ DATASET_SOURCE+"-TO-"+DATASET_TARGET);
			System.out.print("\tconsider[Subject: "+considerSubject+", Quotations: "+considerQuotations+", Alchemy: "+useAlchemy+"]");
			System.out.print("\tfeat: "+Arrays.toString(features.toArray())+"\tclassifier: "+classifierName);
			System.out.print("\ttraininst: "+numInstSameThread+"-"+numInstDifferentThread);
			System.out.println("\tmaxTimeSpan: "+maxTimeSpanInDays+"\tseedrandom: "+seedrandom);
					
			System.out.print("\nth-best: " + best[4] + " \npurity: " + best[3]);
			System.out.print("\tp: " + best[1]);
			System.out.print("\tr: " + best[2]);
			System.out.print("\tf1: " + best[0]);
			System.out.println("\t#clusters: "+best[5]+" correct("+target_threadIDs.size()+")");
				
		  		  
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
 
	public Email readEmailConnections(Email email,  Connection conn){
		try{
		  Statement stmt = conn.createStatement();
	      String sql = "SELECT * FROM communications WHERE email_id="+email.getId();
	      ResultSet rs = stmt.executeQuery(sql);
	      while(rs.next()){
	    	  email.insertReceiver(rs.getInt("recipient_id"));
	    	  email.setSender(rs.getInt("sender_id"));
	    	  email.setTimestamp(rs.getTimestamp("time"));
	      }

	      rs.close();
	      stmt.close();
	  }catch(Exception e){
	      //Handle errors for Class.forName
	      e.printStackTrace();
	   }
	  return email;   	  
 }	
 
} 