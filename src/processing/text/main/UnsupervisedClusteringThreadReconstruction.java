package processing.text.main;

//STEP 1. Import required packages
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import processing.text.enron.model.ValueComparatorInt;
import processing.text.enron.textproc.AgglomerativeClustering;
import processing.text.enron.textproc.Clustering;
import processing.text.enron.textproc.DBScan;
import processing.text.enron.textproc.EmailsComparator;
import processing.text.enron.textproc.TextPreprocessing;
import utils.Util;
import utils.alchemyapi.api.AlchemyAPI;

public class UnsupervisedClusteringThreadReconstruction {

	static final String DATASET = "facebook";
	static final String DB_URL = "jdbc:mysql://localhost:3306/" + DATASET;
	static final String USER = "giacomo";
	static final String PASS = "12345";
	private Table<Integer, Integer, Double> score_dim_map;;


	public static void main(String[] args) {
		new UnsupervisedClusteringThreadReconstruction();
	}

	public UnsupervisedClusteringThreadReconstruction() {
		//**** PARAMETER
		boolean considerSubject=false;
		boolean considerQuotations=true;
		boolean useAlchemy=true;
		boolean onlyEnglish=true;
		 ArrayList<String> features=new ArrayList<>();
		features.add("compareContent_CosineSimilarity");
		//features.add("compareContent_SubjectCosineSimilarity");
		///features.add("compareContent_SubjectJaccardSimilarity");
		features.add("compareContent_AlchemykeywordCosineSimilairty");
		features.add("compareContent_AlchemyEntitiesCosineSimilairty");
		features.add("compareContent_AlchemyConceptsCosineSimilairty");
		///features.add("compareContent_AlchemykeywordWordnetSimilarity");
		//features.add("compareContent_AlchemyEntitiesWordnetSimilarity");
		//features.add("compareContent_AlchemyConceptsWordnetSimilarity");*/
		
		features.add("compareTime_logDistInDays");
		//features.add("compareTime_logDistInHours");
		features.add("comparePeople_UsersJaccardSimilarity");
		//features.add("comparePeople_UsersClosenessSimilarity");
		//features.add("comparePeople_AVGPairsOfUsersConditionalProbability");

		String language="english";
		String wordnetDir="/home/domeniconi/Programmi/WordNet-3.0/dict";
		int numInstSameThread=10;
		int numInstDifferentThread=20;		
		long seedrandom=1;
		int crossvalidation=3;
		long maxTimeSpanInDays=30;
		double startTH=25.0,stopTH=1,stepTH=1;
		String expComment="";
		String wordnetSimMethod="wup";//lin wup jiang hirst leocock
		//String clusteringMethod="dbscan";//dbscan or agglomerative

		String expDescr="DATASET: "+DATASET+" consider[Subject: "+considerSubject+", Quotations: "+considerQuotations+", Alchemy: "+useAlchemy+"]"+
				" feat: "+Arrays.toString(features.toArray())+
				" traininst: "+numInstSameThread+"-"+numInstDifferentThread+" tcrossfold: "+crossvalidation+
				" maxTimeSpan: "+maxTimeSpanInDays+" seedrandom: "+seedrandom;
		String expDescrShort=DATASET+" "+considerSubject+" "+considerQuotations+" "+useAlchemy+
				" numfeat: "+features.size()+
				" "+numInstSameThread+"-"+numInstDifferentThread+" "+crossvalidation+
				" "+maxTimeSpanInDays+" "+seedrandom;
		
		
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
			
			for(Email e : emails_map.values()){
				emails.add(e);	      			
				if(!threadIDs.contains(e.getThreadid()))
					threadIDs.add(e.getThreadid());
			}
			
			System.out.println("#emails: "+emails.size());
			
			/*DBInserts dbins=new DBInserts(DB_URL);
			dbins.writeEmailsAlchemyInformation_onlyCleanContent(emails);*/
			
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
	

			EmailsComparator comparator=new EmailsComparator(null,emails, wordnet);
			score_dim_map = HashBasedTable.create();

			System.out.println("score calculation...");
			//Preparation for java reflection
			Class[] type = { EmailSimGlobalCloseness.class, ArrayList.class, RelatednessCalculator.class };
			Class cls_comparator = Class.forName("processing.text.enron.textproc.EmailsComparator");
			Constructor cons = cls_comparator.getConstructor(type);			
			Object[] const_param = {null, emails, wordnet};
			Object obj_comparator = cons.newInstance(const_param);
			Class[] param = new Class[2];	
			param[0] = Email.class;
			param[1] = Email.class;
			Object[] arguments = new Email[2];	
			int size=emails.size();
			for (int i = 0; i < size-1; i++) {
				if((int)(i%(size/20))==0)
					System.out.print((int)(i*5/(size/20))+"% ");
				Email ei = emails.get(i);
				HashMap<Integer, Double> ei_map = new HashMap<>();

				for (int j = i; j < size; j++) {
					Email ej = emails.get(j);
					if(ei.getId()==ej.getId())
						continue;

					// System.out.println("text: \n"+ ei.getText());
					
					int id1 = ei.getId();
					int id2 = ej.getId();
										
					arguments[0]=ei;
					arguments[1]=ej;
					
					double score=1.0;
					for(String feat : features){						
						Method method = cls_comparator.getDeclaredMethod(feat, param);
						Double val=(Double)method.invoke(obj_comparator, arguments);
						if(val!=null)
							score*=(1+val);
					}

					this.score_dim_map.put(id1, id2, score);
					this.score_dim_map.put(id2, id1, score);
				}
			}
			System.out.println();
			
			//if(clusteringMethod.equals("dbscan")){
				
				//DBASCAN
				PrintWriter pwResuts=new PrintWriter("results/unsupervised/DBSCAN_"+expDescrShort+".txt");
				pwResuts.println(expDescr);
				pwResuts.println("th\tpurity\tp\tr\tf1\t#clustersPred\t#clustersCorrect");
				
				double[] best = new double[6];
				for (double th = startTH; th >=stopTH; th -= stepTH) {
					DBScan dbscan = new DBScan(emails, score_dim_map, th);
					dbscan.emailsClustering();
					double purity = Util.truncate(dbscan.evaluatePurity());
					int[] meas = dbscan.evaluateTPFNFPTN();
					double p = meas[0] * 1.0 / (meas[0] + meas[2]);
					double r = meas[0] * 1.0 / (meas[0] + meas[1]);
					double f1=(2.0*(p*r)/(p+r));
					 System.out.print("\tth "+Util.truncate(th)+" \tPUR:"+purity);
					 System.out.print("\tp: "+Util.truncate(p));
					 System.out.print("\tr: "+Util.truncate(r));
					 System.out.println("\tf1: "+Util.truncate((2.0*(p*r)/(p+r))));
					if (Util.truncate(f1) > best[0]) {
						best[0] = Util.truncate(f1);
						best[1] = +Util.truncate(p);
						best[2] = +Util.truncate(r);
						best[3] = purity;
						best[4] = Util.truncate(th);
						best[5] = dbscan.getClusters().size();
					}
					pwResuts.print(Util.truncate(th)+"\t"+purity);
					pwResuts.print("\t"+Util.truncate(p));
					pwResuts.print("\t"+Util.truncate(r));
					pwResuts.print("\t"+Util.truncate(f1));
					pwResuts.println("\t"+dbscan.getClusters().size()+"\t"+threadIDs.size());
					pwResuts.flush();
				}
				System.out.print("\n\nDBSCAN\nth-best: " + best[4] + " \npurity: " + best[3]);
				System.out.print("\tp: " + best[1]);
				System.out.print("\tr: " + best[2]);
				System.out.println("\tf1: " + best[0]);
				
				
				pwResuts.print("\n\nBEST\n"+best[4]+"\t"+best[3]);
				pwResuts.print("\t"+best[1]);
				pwResuts.print("\t"+best[2]);
				pwResuts.print("\t"+best[0]);
				pwResuts.println("\t"+best[5]+"\t"+threadIDs.size());	
								
				pwResuts.close();
				
			//}
			System.out.println("\n\n");
			//else if(clusteringMethod.equals("agglomerative")){
				//AGGLOMERATIVE 
				pwResuts=new PrintWriter("results/unsupervised/AGGL_"+expDescrShort+".txt");
				pwResuts.println(expDescr);
				pwResuts.println("th\tpurity\tp\tr\tf1\t#clustersPred\t#clustersCorrect");
			
				best = new double[6];
				for (double th = startTH; th >=stopTH; th -= stepTH) {
					Clustering clusteringModel=new AgglomerativeClustering(emails, score_dim_map, th, "average-link", comparator, maxTimeSpanInDays);
					//Clustering clusteringModel = new DBScan(emails, score_dim_map[i], th);
					clusteringModel.emailsClustering();
					double purity = Util.truncate(clusteringModel.evaluatePurity());
					int[] meas = clusteringModel.evaluateTPFNFPTN();
					double p = meas[0] * 1.0 / (meas[0] + meas[2]);
					double r = meas[0] * 1.0 / (meas[0] + meas[1]);
					double f1=(2.0*(p*r)/(p+r));
					 System.out.print("\tth "+Util.truncate(th)+" \tPUR:"+purity);
					 System.out.print("\tp: "+Util.truncate(p));
					 System.out.print("\tr: "+Util.truncate(r));
					 System.out.println("\tf1: "+Util.truncate((2.0*(p*r)/(p+r))));
					if (Util.truncate(f1) > best[0]) {
						best[0] = Util.truncate(f1);
						best[1] = +Util.truncate(p);
						best[2] = +Util.truncate(r);
						best[3] = purity;
						best[4] = Util.truncate(th);
						best[5] = clusteringModel.getClusters().size();
					}
					pwResuts.print(Util.truncate(th)+"\t"+purity);
					pwResuts.print("\t"+Util.truncate(p));
					pwResuts.print("\t"+Util.truncate(r));
					pwResuts.print("\t"+Util.truncate(f1));
					pwResuts.println("\t"+clusteringModel.getClusters().size()+"\t"+threadIDs.size());
					pwResuts.flush();
				}
				System.out.print("\n\nAGGLOMERATIVE\nth-best: " + best[4] + " \npurity: " + best[3]);
				System.out.print("\tp: " + best[1]);
				System.out.print("\tr: " + best[2]);
				System.out.println("\tf1: " + best[0]);
				
				pwResuts.print("\n\nBEST\n"+best[4]+"\t"+best[3]);
				pwResuts.print("\t"+best[1]);
				pwResuts.print("\t"+best[2]);
				pwResuts.print("\t"+best[0]);
				pwResuts.println("\t"+best[5]+"\t"+threadIDs.size());	
								
				pwResuts.close();
			//}
		
	
		} // end try
		 catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			
		}
		
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