package processing.text.enron.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;


/**
 * Filter that drops any word belonging to a given list of <i>stop words</i>.
 * @author giacomo
 */
public class StopWordsRemover implements IFilter {

	private Set<String> stopwords;

	public StopWordsRemover() {
		stopwords = new HashSet<String>();
	}

	public StopWordsRemover(File wordsFile) {
		this();
		loadStopWordsPlainList(wordsFile);
	}

	public StopWordsRemover(String wordsFile) {
		this();
		loadStopWordsPlainList(new File(wordsFile));
	}

	public boolean addStopWord(String word) {
		return stopwords.add(word);
	}

	public void loadStopWordsPlainList(URL url) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = in.readLine();
			while (line != null) {
				stopwords.add(line.toLowerCase());
				line = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadStopWordsPlainList(File file) {
		try {
			loadStopWordsPlainList(file.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String[] Filter(String word) {
		
		if(word.length()>3 && word.length()<36){
			if (!stopwords.contains(word)){
				String words[]={word};
				return words;
			}
		}
		return null;
	}
	
	
}
