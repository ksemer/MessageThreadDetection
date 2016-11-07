package processing.text.main;
import java.io.PrintWriter;
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
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import processing.text.deeplearning.MLP;
import processing.text.enron.model.Cluster;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;
import processing.text.enron.textproc.AgglomerativeClustering;
import processing.text.enron.textproc.AgglomerativeClusteringSupervised;
import processing.text.enron.textproc.AgglomerativeClusteringSupervisedDeepLearning;
import processing.text.enron.textproc.Clustering;
import processing.text.enron.textproc.DBInserts;
import processing.text.enron.textproc.DBSCANSupervised;
import processing.text.enron.textproc.DBScan;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.TextPreprocessing;
import processing.text.enron.textproc.WekaClassification;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;
import weka.classifiers.Classifier;

public class SupervisedDeepLearningThreadReconstruction {

	static final String DATASET="bc3";
	static final String DB_URL = "jdbc:mysql://shadow.cs.uoi.gr:3306/"+DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";
	
	public static void main(String[] args) {
		new SupervisedDeepLearningThreadReconstruction();	
	}
   
	public SupervisedDeepLearningThreadReconstruction() {	
		//**** PARAMETER
		boolean considerSubject=false;
		boolean considerQuotations=true;
		boolean useAlchemy=true;
		boolean onlyEnglish=true;
		 ArrayList<String> features=new ArrayList<>();
		features.add("compareContent_CosineSimilarity");
		features.add("compareContent_SubjectCosineSimilarity");
		///features.add("compareContent_SubjectJaccardSimilarity");
		features.add("compareContent_AlchemykeywordCosineSimilairty");
		features.add("compareContent_AlchemyEntitiesCosineSimilairty");
		features.add("compareContent_AlchemyConceptsCosineSimilairty");
		//features.add("compareContent_AlchemykeywordWordnetSimilarity");
		//features.add("compareContent_AlchemyEntitiesWordnetSimilarity");
		//features.add("compareContent_AlchemyConceptsWordnetSimilarity");
		
		
		features.add("compareTime_logDistInDays");
		features.add("compareTime_logDistInHours");
		features.add("comparePeople_UsersJaccardSimilarity");
		features.add("comparePeople_UsersClosenessSimilarity");
		//features.add("comparePeople_AVGPairsOfUsersConditionalProbability");

		//String classifierName="weka.classifiers.trees.J48";
		//String classifierName="weka.classifiers.trees.RandomForest:100";
		//String classifierName="weka.classifiers.functions.LibSVM";
		//String classifierName="weka.classifiers.functions.SMO";
		//String classifierName="weka.classifiers.functions.Logistic";
		//String classifierName="weka.classifiers.lazy.IBk:100";
		int n_hidden=50;
		int n_epochs=500;
		double learning_rate=0.5;
		String hidden_activation="tanh";
		String language="english";
		int numInstSameThread=5;
		int numInstDifferentThread=5;		
		long seedrandom=1;
		int crossvalidation=5;
		long maxTimeSpanInDays=30;
		double startTH=1.0,stopTH=0,stepTH=0.02;
		String expComment="";
		String clusteringMethod="agglomerative";//dbscan or agglomerative
		String wordnetSimMethod="wup";//lin wup jiang hirst leocock
		

		String expDescr="DATASET: "+DATASET+" clustering: "+clusteringMethod+" consider[Subject: "+considerSubject+", Quotations: "+considerQuotations+", Alchemy: "+useAlchemy+"]"+
				" feat: "+Arrays.toString(features.toArray())+" classifier: MLP deep learning"+
				" traininst: "+numInstSameThread+"-"+numInstDifferentThread+" tcrossfold: "+crossvalidation+
				" maxTimeSpan: "+maxTimeSpanInDays+" seedrandom: "+seedrandom+
				" MLP params: n_hidden: "+n_hidden+" n_epochs: "+n_epochs+" learning_rate:"+learning_rate+" activation: "+hidden_activation;
		String expDescrShort=DATASET+" "+clusteringMethod+" "+considerSubject+" "+considerQuotations+" "+useAlchemy+
				" numfeat: "+features.size()+" MLP"+
				" "+numInstSameThread+"-"+numInstDifferentThread+" "+crossvalidation+
				" "+maxTimeSpanInDays+" "+seedrandom+
				" "+n_hidden+"  "+n_epochs+" "+learning_rate+" "+hidden_activation;

		
		
		String agglomerativeDistanceCalculation="average-link";
		 //**** END P{ARAMETER		
		
		Connection conn = null;
		Statement stmt = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("Connecting to database..."+DATASET);
			conn = DriverManager.getConnection(DB_URL,USER,PASS);	 
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			//String sql = "SELECT * FROM email_messages WHERE subject LIKE '%Hey!%' limit 1000";
			String sql =  "SELECT *"
			+ " FROM email_messages e, communications c WHERE "//e.subject LIKE '%Hey!%' AND"
			+ " e.email_id = c.email_id ";
			if(onlyEnglish)
				sql+=" AND e.alchemy IS NOT NULL ";
			//+ "AND thread_id <2054890523 LIMIT 5000000";
			  
			  
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
					String subject="";
					if(considerSubject){
						subject=rs.getString("subject");
						subject=subject.toLowerCase();       
						subject=subject.replaceAll("re:", "");
						subject=subject.replaceAll("fw:", "");
						subject=subject.replaceAll("fwd:", "");		
					}					
					
					
					e=new Email(id,thireadid, senderid, subject, text);
					e.insertReceiver(rs.getLong("recipient_id"));
					e.setTimestamp(rs.getTimestamp("time"));
					e.setCleanContent(clean_text);
					e.setCleanQuotes(quotation_text);
					
					if(useAlchemy){
						String alchemy_doc= rs.getString("alchemy");
						e.setAlchemy_document(Util.getDocumentFromXmlString(alchemy_doc));
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
					e.insertReceiver(rs.getLong("recipient_id"));		 
				emails_map.put(id,e);	         
			}
			
			rs.close();
			stmt.close();
			ArrayList<Email> emails=new ArrayList<>();
			ArrayList<Integer> threadIDs=new ArrayList<>();
			ArrayList<Long> users=new ArrayList<>();
			
			for(Email e : emails_map.values()){
				emails.add(e);	      			
				if(!threadIDs.contains(e.getThreadid()))
					threadIDs.add(e.getThreadid());
				for(long u : e.getIvolvedUsers()){
					if(!users.contains(u))
						users.add(u);
				}
			}
			
			System.out.println("#emails: "+emails.size()+"\t#threads: "+threadIDs.size()+"\t#users: "+users.size());
			
		    System.out.println("BoWs Creation...");
			emails=TextPreprocessing.createBowAndWeight(emails, true, true, language, 2, DATASET);
			
			//open  WORDNET
			ILexicalDatabase wordnetdb = new NictWordNet();
		    RelatednessCalculator wordnet = null;
		    if(wordnetSimMethod.equals("wup"))
		    	wordnet= new WuPalmer(wordnetdb);
		    else if(wordnetSimMethod.equals("lin"))
	    		wordnet= new Lin(wordnetdb);
			//RiWordNet wordnet = new RiWordNet(wordnetDir);
		    
	
			Random random=new Random(seedrandom);
			Collections.shuffle(threadIDs,random);
			int splitSet=threadIDs.size()/crossvalidation;
			
			
			double[] best = new double[6];
			
			Table<Integer, Integer, Double> score_AlreadyCalc=HashBasedTable.create();
			ArrayList<Cluster>[] clustersFolds=new ArrayList[crossvalidation];
			MLP [] classifiers=new MLP[crossvalidation];
			
			PrintWriter pwResuts=new PrintWriter("results/"+expDescrShort+".txt");
			pwResuts.println(expDescr);
			pwResuts.println("th\tpurity\tp\tr\tf1\t#clustersPred\t#clustersCorrect");
			
			
			for (double th = startTH; th >=stopTH; th -= stepTH) {
				ArrayList<Cluster> clustersAll=new ArrayList<>();
				int sumTP=0,sumTN=0,sumFP=0,sumFN=0;
				double sumPurity=0, sumP=0, sumR=0, sumF1=0;
				for(int c=0;c<crossvalidation;c++){
					ArrayList<Email> train=new ArrayList<>();
					ArrayList<Email> test=new ArrayList<>();
	
					ArrayList<Integer> threadIDs_train=new ArrayList<>();
					ArrayList<Integer> threadIDs_test=new ArrayList<>();
					
					for(Email e : emails){
						int t=threadIDs.indexOf(e.getThreadid());
						if(t>=(splitSet*c) && t<(splitSet*c+splitSet)){
							test.add(e);
							if(!threadIDs_test.contains(e.getThreadid()))
								threadIDs_test.add(e.getThreadid());
						}else{
							train.add(e);			
							if(!threadIDs_train.contains(e.getThreadid()))
								threadIDs_train.add(e.getThreadid());
						}
					}
					//System.out.println("\n\t*** TH:"+th+" - split: "+c+" trainThreadID: "+Arrays.toString(threadIDs_train.toArray())+" testThreadID: "+Arrays.toString(threadIDs_test.toArray()));
					
					
					//Clustering clusterModel=new DBSCANSupervised(train, test, th, classifierName, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET);
					Clustering clusterModel=null;
					if(classifiers[c]==null){
						if(clusteringMethod.equals("agglomerative")){
							clusterModel=new AgglomerativeClusteringSupervisedDeepLearning(train, test, th, numInstSameThread, numInstDifferentThread, features,seedrandom, DATASET, agglomerativeDistanceCalculation,maxTimeSpanInDays,wordnet,score_AlreadyCalc,  expComment,
									n_hidden,n_epochs,learning_rate,hidden_activation);
							classifiers[c]=((AgglomerativeClusteringSupervisedDeepLearning)clusterModel).getClassifierModel();
						}
					}else{
						if(clusteringMethod.equals("agglomerative")){
							clusterModel=new AgglomerativeClusteringSupervisedDeepLearning(test, th, classifiers[c], features, DATASET, agglomerativeDistanceCalculation,maxTimeSpanInDays,wordnet,score_AlreadyCalc,  expComment);
							clusterModel.setClusters(clustersFolds[c]);
						}
					}
					
					//System.out.println("MAP SIZE: "+score_AlreadyCalc.size());
					long timeStart=System.currentTimeMillis();
					//System.out.print("\tstart clusterization");
					clusterModel.emailsClustering();
					//System.out.println("\tended in :"+((System.currentTimeMillis()-timeStart))+" mSec");
					
					clustersAll.addAll(clusterModel.getClusters());
					clustersFolds[c]=clusterModel.getClusters();
					
					
					double purity=Util.truncate(clusterModel.evaluatePurity());			  	
				  	int [] meas=clusterModel.evaluateTPFNFPTN();
				  	double p=meas[0]*1.0/(meas[0]+meas[2]);
				  	double r=meas[0]*1.0/(meas[0]+meas[1]);
				  	
				  	/*System.out.print("\tFOLD: "+(c+1)+"\tpurity: "+purity);
				  	System.out.print("\tp: "+p);
				  	System.out.print("\tr: "+r);
				  	System.out.print("\tf1: "+((2*p*r)/(p+r)));
					System.out.println("\t#clusters: "+clusterModel.getClusters().size()+" correct("+threadIDs_test.size()+")");
	*/
				  	sumTP+=meas[0];
				  	sumFN+=meas[1];
				  	sumFP+=meas[2];
				  	sumTN+=meas[3];
				  	sumPurity+=purity;
				  	sumP+=p;
				  	sumR+=r;
				  	sumF1+=(2*p*r)/(p+r);
				  	
				}
				
			
				Clustering cFinal=new Clustering() {
				@Override
				public ArrayList<Cluster> emailsClustering() {
						// TODO Auto-generated method stub
						return null;
					}
				};
				cFinal.setClusters(clustersAll);
				double purity=Util.truncate(cFinal.evaluatePurity());			  	
			  	int [] meas=cFinal.evaluateTPFNFPTN();
			  	double p=meas[0]*1.0/(meas[0]+meas[2]);
			  	double r=meas[0]*1.0/(meas[0]+meas[1]);
			  	double f1=(2.0*(p*r)/(p+r));
			  	/*
			  	System.out.println("\n***** micro AVG on ALL clusters ***");
			  	System.out.print("purity: "+purity);
			  	System.out.print("\tp: "+p);
			  	System.out.print("\tr: "+r);
			  	System.out.println("\tf1: "+((2*p*r)/(p+r)));
			  	
		
			  	System.out.println("\n***** Macro AVG on ALL scores ***");
			  	System.out.print("purity: "+sumPurity/crossvalidation);
			  	System.out.print("\tp: "+(sumP/crossvalidation));
			  	System.out.print("\tr: "+(sumR/crossvalidation));
			  	System.out.println("\tf1: "+((2*(sumP/crossvalidation)*(sumR/crossvalidation))/((sumP/crossvalidation)+(sumR/crossvalidation))));
			*/
			  	
				System.out.print("th "+Util.truncate(th)+" PUR: "+purity);
				System.out.print("\tp: "+Util.truncate(p));
				System.out.print("\tr: "+Util.truncate(r));
				System.out.print("\tf1: "+Util.truncate(f1));
				System.out.println("\t#clusters: "+cFinal.getClusters().size()+" correct("+threadIDs.size()+")");

				pwResuts.print(Util.truncate(th)+"\t"+purity);
				pwResuts.print("\t"+Util.truncate(p));
				pwResuts.print("\t"+Util.truncate(r));
				pwResuts.print("\t"+Util.truncate(f1));
				pwResuts.println("\t"+cFinal.getClusters().size()+"\t"+threadIDs.size());
				
				if (f1 > best[0]) {
					best[0] = Util.truncate(f1);
					best[1] = Util.truncate(p);
					best[2] = Util.truncate(r);
					best[3] = purity;
					best[4] = Util.truncate(th);
					best[5] = cFinal.getClusters().size();
				}
				
			}
			
			System.out.print("*******\n\n"+expDescr);
					
			System.out.print("\nth-best: " + best[4] + " \npurity: " + best[3]);
			System.out.print("\tp: " + best[1]);
			System.out.print("\tr: " + best[2]);
			System.out.print("\tf1: " + best[0]);
			System.out.println("\t#clusters: "+best[5]+" correct("+threadIDs.size()+")");
			
			pwResuts.print("\n\nBEST\n"+best[4]+"\t"+best[3]);
			pwResuts.print("\t"+best[1]);
			pwResuts.print("\t"+best[2]);
			pwResuts.print("\t"+best[0]);
			pwResuts.println("\t"+best[5]+"\t"+threadIDs.size());	
							
			pwResuts.close();
			
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