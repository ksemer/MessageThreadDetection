package processing.text.enron.model;

public class Word {

	protected String word;
	protected int occurence;
	protected double weight;
	protected double idf;
	protected double tf;
	protected boolean inSubject;

	public Word() {
	}

	public Word(String word) {
		this.setWord(word);
		this.occurence = 1;
	}

	public Word(String word, boolean inSubject) {
		this.setWord(word);
		this.occurence = 1;
		this.inSubject = inSubject;
	}

	public Word(String word, int occurrence, double tf, double weight, double idf, boolean inSubject) {
		this.setWord(word);
		this.occurence = occurrence;
		this.weight = weight;
		this.tf = tf;
		this.inSubject = inSubject;
		this.idf = idf;
	}

	public synchronized void incOccurence() {
		this.occurence++;
	}

	public synchronized void setWord(String word) {
		this.word = word;
	}

	public synchronized String getWord() {
		return word;
	}

	public synchronized void setWeight(double weight) {
		this.weight = weight;
	}

	public synchronized double getWeight() {
		return weight;
	}

	public void setOccurence(int occurence) {
		this.occurence = occurence;
	}

	public int getOccurence() {
		return occurence;
	}

	public void setInSubject(boolean inSubject) {
		this.inSubject = inSubject;
	}

	public boolean isInSubject() {
		return inSubject;
	}

	public synchronized void setIdf(double idf) {
		this.idf = idf;
	}

	public synchronized double getIdf() {
		return idf;
	}

	public synchronized void setTf(double tf) {
		this.tf = tf;
	}

	public synchronized double getTf() {
		return tf;
	}
}
