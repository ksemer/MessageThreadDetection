package processing.text.deeplearning;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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

public class DLClassification{
	
	
	//private ArrayList<Email> train_emails;	
	//private ArrayList<Email> test_emails;
	private MLP mlp;
	private ArrayList<String> features;
	private EmailSimGlobalCloseness esimClosenesses;
	private String DATASET;
	private RelatednessCalculator wordnet;
	
	public DLClassification(ArrayList<String> features, String DATASET, RelatednessCalculator wordnet) {
		this.features=features;	
		this.esimClosenesses=new EmailSimGlobalCloseness(DATASET);
		this.DATASET=DATASET;
		this.wordnet=wordnet;
	}
	
	public void buildClassifier(ArrayList<Email> train, int numInstSameThread,int numInstDifferentThread,
			int n_hidden, double learning_rate, int n_epochs, String hidden_activation,  long randomSeed){
		try{
			System.out.print("training set creation...");
			Instances traininst=WekaUtil.creteWekaEmailRandomPairsInstances(train, features, numInstSameThread, numInstDifferentThread, randomSeed, esimClosenesses, wordnet);
			PrintWriter pw=new PrintWriter(DATASET+"_train.arff");
			pw.print(traininst);		
			pw.close();
			
			traininst.deleteStringAttributes();
			traininst.setClass(traininst.attribute(traininst.numAttributes()-1));
			System.out.println("training the classifier...\n");
			
			double[][] train_X = new double[traininst.numInstances()][traininst.numAttributes()-1];
			int[][] train_Y = new int[traininst.numInstances()][2];
			for(int i=0;i<traininst.numInstances();i++){
				train_X[i]=WekaUtil.convertInstanceToFeaturesVector(traininst.instance(i));
				if(traininst.instance(i).classValue()>0){
					train_Y[i][0]=0;
					train_Y[i][1]=1;
				}
				else{
					train_Y[i][0]=1;
					train_Y[i][1]=0;
				}
			}
			
			Random rng = new Random(randomSeed);
			int train_N = train_X.length;
	        int n_in = train_X[0].length;
	        int n_out = train_Y[0].length;

	        // construct MLP
	        this.mlp = new MLP(train_N, n_in, n_hidden, n_out, null, rng);
	        // train
	        for(int epoch=0; epoch<n_epochs; epoch++) {
	        	mlp.train(train_X, train_Y, learning_rate);
	        }
	        
			
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
			double [] testInst=WekaUtil.convertInstanceToFeaturesVector(inst.firstInstance());
			mlp.predict(testInst, pred);
		}catch(Exception e){
			e.printStackTrace();
		}
		return pred;
	}
		
	
	public MLP getClassifier(){
		return this.mlp;
	}
	
	public void setClassifier(MLP classifier){
		this.mlp=classifier;
	}

}
