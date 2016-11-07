package processing.text.enron.filters;


/**
 * Filter that turns any word in lower case form.
 * @author giacomo
 */
public class Casefolder implements IFilter {

	public Casefolder() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String[] Filter(String word) {
		String w[]={word.toLowerCase()};
		return w;
	}

}
