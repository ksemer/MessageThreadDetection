package processing.text.enron.filters.en;

import java.io.*;

import processing.text.enron.filters.IFilter;
import processing.text.enron.filters.snowball.SnowballStemmer;

/**
 * Filter which turns words into their stems using the Porter Stemmer.
 * 
 * @author Giacomo
 */
public class SnowballStemmerImpl implements IFilter {

	private SnowballStemmer stemmer;

	public SnowballStemmerImpl(String language) {
		try{
			Class stemClass = Class.forName("processing.text.enron.filters.snowball.ext." +
					language + "Stemmer");
			stemmer = (SnowballStemmer) stemClass.newInstance();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	@Override
	public String[] Filter(String word) {
		stemmer.setCurrent(word);
		stemmer.stem();
		String w[] = { stemmer.getCurrent() };
		return w;
	}

}
