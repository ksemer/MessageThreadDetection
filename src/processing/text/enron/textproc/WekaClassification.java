package processing.text.enron.textproc;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.util.ArrayList;

import edu.cmu.lti.ws4j.RelatednessCalculator;
import processing.graph.measure.EmailSimGlobalCloseness;
import processing.text.enron.model.Email;
import utils.WekaUtil;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

public class WekaClassification{
	
	
	//private ArrayList<Email> train_emails;	
	//private ArrayList<Email> test_emails;
	private Classifier classifier;
	private ArrayList<String> features;
	private EmailSimGlobalCloseness esimClosenesses;
	private String DATASET;
	private RelatednessCalculator wordnet;
	
	public WekaClassification(ArrayList<String> features, String DATASET, RelatednessCalculator wordnet) {
		this.features=features;	
		//this.esimClosenesses=new EmailSimGlobalCloseness(DATASET);
		this.DATASET=DATASET;
		this.wordnet=wordnet;
	}
	
	public void buildClassifier(ArrayList<Email> train, String classifierName, int numInstSameThread,int numInstDifferentThread, long randomSeed){
		try{
			System.out.print("training set creation...");
			Instances traininst=WekaUtil.creteWekaEmailRandomPairsInstances(train, features, numInstSameThread, numInstDifferentThread, randomSeed, esimClosenesses, wordnet);
			PrintWriter pw=new PrintWriter(DATASET+"_train.arff");
			pw.print(traininst);		
			pw.close();
			
			traininst.deleteStringAttributes();
			traininst.setClass(traininst.attribute(traininst.numAttributes()-1));
			System.out.println("training the classifier...\n");
			
			if(classifierName.contains(":")){
				//dei parametri
				
				if(classifierName.contains("RandomForest")){
					classifier=new CostSensitiveClassifier();
					CostMatrix cm=new CostMatrix(2);
					cm.setCell(1, 0, 1.0);
					cm.setCell(0, 1, Double.valueOf(classifierName.split(":")[1]));
					((CostSensitiveClassifier)classifier).setCostMatrix(cm);
					((CostSensitiveClassifier)classifier).setClassifier((Classifier)Class.forName(classifierName.split(":")[0]).newInstance());
				}
				if(classifierName.contains("IBk")){
					classifier=new IBk(Integer.valueOf(classifierName.split(":")[1]));
				}
				
			}else{
				classifier=(Classifier)Class.forName(classifierName).newInstance();
			}			
			
			classifier.buildClassifier(traininst);
			
			//SerializationHelper.write("subjectToThread_withSubjectFeature.model", classifier);
			
			/* // display classifier
			if(classifierName.equals("weka.classifiers.trees.J48")){
				  final javax.swing.JFrame jf = 
					       new javax.swing.JFrame("Weka Classifier Tree Visualizer: J48");
					     jf.setSize(1000,800);
					     jf.getContentPane().setLayout(new BorderLayout());
					     TreeVisualizer tv = new TreeVisualizer(null,
					    		 ((J48)classifier).graph(),
					         new PlaceNode2());
					     jf.getContentPane().add(tv, BorderLayout.CENTER);
					     jf.addWindowListener(new java.awt.event.WindowAdapter() {
					       public void windowClosing(java.awt.event.WindowEvent e) {
					         jf.dispose();
					       }
					     });

					     tv.fitToScreen();
					     jf.setVisible(true);
			}
		   */
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public double [] classifyEmailPair(Email email1, Email email2, ArrayList<Email> emails){
		Instances inst=WekaUtil.creteWekaEmailPairsSingleInstance(email1, email2, features, esimClosenesses, emails, wordnet);
		inst.deleteStringAttributes();
		inst.setClass(inst.attribute(inst.numAttributes()-1));
		double [] pred=new double[]{1,0};
		try{
			pred=classifier.distributionForInstance(inst.firstInstance());
		}catch(Exception e){
			e.printStackTrace();
		}
		return pred;
	}
	
	public void loadClassifierFromFile(String modelFile){
		try{
			this.classifier = (Classifier)SerializationHelper.read(modelFile);			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Classifier getClassifier(){
		return this.classifier;
	}
	
	public void setClassifier(Classifier classifier){
		this.classifier=classifier;
	}

}
