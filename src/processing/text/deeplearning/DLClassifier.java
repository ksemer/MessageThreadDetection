package processing.text.deeplearning;

import java.util.Random;

public interface DLClassifier {
	
	public void train(double[][] train_X, int[][] train_Y, double lr, int n_epochs);
	
	public void pretrain(int[][] train_X, double lr, int k, int epochs);
	
	public void predict(double[] x, double[] y);

}
