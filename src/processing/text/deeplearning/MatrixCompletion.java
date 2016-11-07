package processing.text.deeplearning;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import processing.text.enron.model.ValueComparator;
import processing.text.enron.model.ValueComparatorStringArray;



public class MatrixCompletion {
	private static int maxPreds=100000;
	
	
	public static HashMap<String, Double> convertPredsMatrixToPredMap(double[][] test_X,double[][] test_Pred,ArrayList<String> geneList, ArrayList<Integer> termList){
		HashMap<String, Double> preds_map=new HashMap<>();
		for(int i=0;i<test_X.length;i++){
			for(int j=0;j<test_X[0].length;j++){
	  			if(test_X[i][j]==0){
	  				preds_map.put(geneList.get(i)+"\t"+termList.get(j), test_Pred[i][j]);
	         	}
			}
		}
		return preds_map;
	}	
	
	public static void writePredsToFile_Old(HashMap<String[], Double> preds, String fileName){
		try{
			ValueComparatorStringArray bvc =  new ValueComparatorStringArray(preds);
        	Map<String[],Double> sortedPreds = new TreeMap<String[],Double>(bvc);
        	sortedPreds.putAll(preds);
			
			PrintWriter pw=new PrintWriter(fileName);
			int k=0;
	        for (Entry<String[], Double> e : sortedPreds.entrySet()){
	        	pw.println(e.getKey()[0]+"\t"+e.getKey()[1]+"\t"+e.getValue());
	        	if(k++>100000 || e.getValue()<=0.01)
	        		break;
	        }
			pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	

	public static HashMap<String, Double> makePreds_MLP(double [][] train_X, int[][] train_Y,double[][] test_X, int[][] test_Y,
			int n_hidden, double learning_rate, int n_epochs, String hidden_cativation, long randomSeed, 
			double minScore, ArrayList<String> geneList, ArrayList<Integer> termList){
		Random rng = new Random(randomSeed);

        int train_N = train_X.length;
        int test_N = test_X.length;
        int n_in = train_X[0].length;
        int n_out = test_X[0].length;

        // construct MLP
        MLP classifier = new MLP(train_N, n_in, n_hidden, n_out, hidden_cativation, rng);
        
        System.out.print("Train MLP... |");
        // train
        int nMarks=(n_epochs>20?20:n_epochs),perc=0;
        for(int epoch=0; epoch<n_epochs; epoch++) {
        	if(epoch>(n_epochs*1.0/nMarks*perc)){
				perc++;
				System.out.print(((int)(perc*100/nMarks))+"%");
			}
            classifier.train(train_X, train_Y, learning_rate);
        }
        System.out.println("|");

        // test data
        double[] genePred = new double[n_out];
        HashMap<String, Double> preds=new HashMap<>();
        
    	int predFatte=0, predCorrette=0;
    	System.out.print("Test MLP... |");
        //test
        perc=0;
        for(int i=0; i<test_N; i++) { 
        	if(i>(test_N*1.0/nMarks*perc)){
				perc++;
				System.out.print(((int)(perc*100/nMarks))+"%");
			}
         	classifier.predict(test_X[i], genePred);
         	for(int j=0;j<n_out;j++){
         		if(test_X[i][j]==0 && genePred[j]>minScore){
         			preds.put(geneList.get(i)+"\t"+termList.get(j), genePred[j]);
             	}
         	}
        }    
        System.out.println("| #preds: "+preds.size() );
        return preds;
    }
	
	public static double[][] makePreds_DropoutMLP(double [][] train_X, int[][] train_Y,double[][] test_X, int[][] test_Y,
			double learning_rate, int n_epochs, 
			boolean dropout, double p_dropout, long randomSeed){
		Random rng = new Random(randomSeed);

        int train_N = train_X.length;
        int test_N = test_X.length;
        int n_in = train_X[0].length;
        int n_out = test_X[0].length;
        int[] hidden_layer_sizes=new int[]{n_out,n_out};

        // construct Dropout
        Dropout classifier = new Dropout(train_N, n_in, hidden_layer_sizes, n_out, rng, "ReLU");

        // train
        classifier.train(n_epochs, train_X, train_Y, dropout, p_dropout, learning_rate);

        // pretest
        if(dropout) classifier.pretest(p_dropout);

        double[][] test_Pred = new double[test_N][n_out];

        // test
        for(int i=0; i<test_N; i++) {
         	classifier.predict(test_X[i], test_Pred[i]);
        }      
        return test_Pred;
    }
		
	public static double[][] makePreds_DBN(int [][] train_X, int[][] train_Y,int[][] test_X, int[][] test_Y,
			int[] hidden_layer_sizes, int k,  double pre_learning_rate, double finetune_learning_rate, 
			int pre_n_epochs, int finetune_n_epochs, long randomSeed){
		Random rng = new Random(randomSeed);

		int train_N = train_X.length;
        int test_N = test_X.length;
        int n_in = train_X[0].length;
        int n_out = test_X[0].length;

        int n_layers = hidden_layer_sizes.length;

        /*double pretrain_lr = 0.1;
        int pretraining_epochs = 1000;
        int k = 1;
        double finetune_lr = 0.1;
        int finetune_epochs = 500;*/

        System.out.println("Train DBN...");
        // construct DNN.DBN
        DBN dbn = new DBN(train_N, n_in, hidden_layer_sizes, n_out, n_layers, rng);

        // pretrain
        dbn.pretrain(train_X, pre_learning_rate, k, pre_n_epochs);

        // finetune
        dbn.finetune(train_X, train_Y, finetune_learning_rate, finetune_n_epochs);
       
        // test data
        double[][] test_Pred = new double[test_N][n_out];             
        for(int i=0; i<test_N; i++) {
        	dbn.predict(test_X[i], test_Pred[i]);
        }      
        return test_Pred;
    }
	
	public static double[][] makePreds_SDA(int [][] train_X, int[][] train_Y,int[][] test_X, int[][] test_Y,
			int[] hidden_layer_sizes, int k,  double pre_learning_rate, double finetune_learning_rate, 
			int pre_n_epochs, int finetune_n_epochs, double corruption_level, long randomSeed){
		Random rng = new Random(randomSeed);

		int train_N = train_X.length;
        int test_N = test_X.length;
        int n_in = train_X[0].length;
        int n_out = test_X[0].length;

        int n_layers = hidden_layer_sizes.length;

        /*double pretrain_lr = 0.1;
        double corruption_level = 0.3;
        int pretraining_epochs = 1000;
        double finetune_lr = 0.1;
        int finetune_epochs = 500;
        int n_ins = 28;
        int n_outs = 28;
        int[] hidden_layer_sizes = {15, 15};*/

      
        // construct SdA
        SdA sda = new SdA(train_N, n_in, hidden_layer_sizes, n_out, n_layers, rng);

        // pretrain
        sda.pretrain(train_X, pre_learning_rate, corruption_level, pre_n_epochs);

        // finetune
        sda.finetune(train_X, train_Y, finetune_learning_rate, finetune_n_epochs);

        double[][] test_Pred = new double[test_N][n_out];
        // test
        for(int i=0; i<test_N; i++) {
            sda.predict(test_X[i], test_Pred[i]);
        }      
        return test_Pred;
    }
	
	
	public static HashMap<String, Double> makePreds_DA(int [][] train_X,int[][] test_X, int[][] test_Y,
			int n_hidden, double corruption_level, double learning_rate, int n_epochs, long randomSeed,
			double minScore, ArrayList<String> geneList, ArrayList<Integer> termList){
    	Random rng = new Random(randomSeed);

    	int train_N = train_X.length;
		int test_N = test_X.length;
		int n_visible = train_X[0].length;
	
	  	dA da = new dA(train_N, n_visible, n_hidden, null, null, null, rng);
	
	  	System.out.print("Train DA... |");
        // train
        int nMarks=(n_epochs>20?20:n_epochs),perc=0;
        for(int epoch=0; epoch<n_epochs; epoch++) {
        	if(epoch>(n_epochs*1.0/nMarks*perc)){
				perc++;
				System.out.print(((int)(perc*100/nMarks))+"%");
			}
			for(int i=0; i<train_N; i++) {
				da.train(train_X[i], learning_rate, corruption_level);
			}
		}
        System.out.println("|");

        // test data
        double[] genePred = new double[n_visible];
        HashMap<String, Double> preds=new HashMap<>();
        
    	int predFatte=0, predCorrette=0;
    	System.out.print("Test DA... |");
    	//test
        perc=0;
        for(int i=0; i<test_N; i++) { 
        	if(i>(test_N*1.0/nMarks*perc)){
				perc++;
				System.out.print(((int)(perc*100/nMarks))+"%");
			}
	  		da.reconstruct(test_X[i], genePred);
	  		for(int j=0;j<n_visible;j++){
         		if(test_X[i][j]==0 && genePred[j]>minScore){
         			preds.put(geneList.get(i)+"\t"+termList.get(j), genePred[j]);
             	}
         	}
        }            
        System.out.print("| #totpreds: "+preds.size() );
        ValueComparator bvc =  new ValueComparator(preds);
    	Map<String,Double> sortedPreds = new TreeMap<String,Double>(bvc);
    	sortedPreds.putAll(preds);	
    	preds=new HashMap<>();
		int k=0;
        for (Entry<String, Double> e : sortedPreds.entrySet()){
        	preds.put(e.getKey(), e.getValue());
        	if((++k)>maxPreds)
        		break;
        }

        System.out.println(" -> #preds: "+preds.size() );
        return preds;
    }
	
	public static double[][] makePreds_RBM(int [][] train_X,int[][] test_X, int[][] test_Y,
			int n_hidden, int k, double learning_rate, int n_epochs, long randomSeed){
    	Random rng = new Random(randomSeed);

    	int train_N = train_X.length;
        int test_N = test_X.length;
        int n_visible = train_X[0].length;
        
        RBM rbm = new RBM(train_N, n_visible, n_hidden, null, null, null, rng);

        // train
        for(int epoch=0; epoch<n_epochs; epoch++) {
            for(int i=0; i<train_N; i++) {
                rbm.contrastive_divergence(train_X[i], learning_rate, k);
            }
        }

        // test data
        double[][] test_Pred = new double[test_N][n_visible];
        		 
		int predFatte=0, predCorrette=0;
	  	for(int i=0; i<test_N; i++) {
        	rbm.reconstruct(test_X[i], test_Pred[i]);
	  	}
		return test_Pred;
    }
	

}
