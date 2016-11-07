package processing.text.enron.filters;

import java.util.ArrayList;
import java.util.Arrays;

import processing.text.enron.filters.en.SnowballStemmerImpl;


public class WordFilters {
	
	private ArrayList<IFilter> filters;
	
	public WordFilters(boolean doStemming, String language) {
		filters=new ArrayList<IFilter>();
		filters.add(new PunctuationRemover());
		filters.add(new NumberRemover());
		filters.add(new Casefolder());
		//filters.add(new PluralToSingularFilter(wordNetDir));	
		filters.add(new StopWordsRemover("stopwords"));
		if(doStemming){
			filters.add(new SnowballStemmerImpl(language));
			//filters.add(new PorterStemmer());
			
		}
	}
	
	public String[] applyfilters(String word){
		ArrayList<String> wordsList=new ArrayList<String>();
		wordsList.add(word);
		for(IFilter f : filters){		
			ArrayList<String> wordsListTemp=new ArrayList<String>();
			for(String w : wordsList){
				String[] wlist=f.Filter(w);
				if(wlist!=null)
					wordsListTemp.addAll(Arrays.asList(f.Filter(w)));
			}
			wordsList=new ArrayList<String>();
			wordsList.addAll(wordsListTemp);
		}
		String [] w={};
		return wordsList.toArray(w);
	}

}
