package processing.text.enron.filters;

/**
 * Filter that drops any word belonging to a given list of <i>stop words</i>.
 * @author giacomo
 */
public class NumberRemover implements IFilter {

	private String[] number= {"1","2","3","4","5","6","7","8","9","0"};

	public NumberRemover() {
	}

	@Override
	public String[] Filter(String word) {
		for(String n : number)
			word=word.replaceAll(n, " ");
		word=word.replaceAll("\\s+"," ");
		String [] words=word.split(" ");
		return words;		
	}
	
	
}
