package processing.text.enron.textproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import processing.text.enron.filters.WordFilters;
import processing.text.enron.model.Email;
import processing.text.enron.model.Word;

public class TextPreprocessing {

	public static ArrayList<Email> createBow(ArrayList<Email> emails, boolean withQuotation, boolean doStemming, String language) {
		WordFilters filters = new WordFilters(doStemming, language);
		if (withQuotation) {
			for (Email e : emails) {
				for (String word : e.getSubject().split(" ")) {
					String[] filterWords = filters.applyfilters(word);
					for (String w : filterWords) {
						if ((w != null) || (w != "")) {
							e.insertWord(w, true);
						}
					}
				}
				for (String word : e.getText().split(" ")) {
					String[] filterWords = filters.applyfilters(word);
					for (String w : filterWords) {
						if ((w != null) || (w != "")) {
							e.insertWord(w, false);
						}
					}
				}
			}
		} else {
			// TODO if we want to NON consider the quotations
		}
		return emails;
	}

	public static ArrayList<Email> createBowAndWeight(ArrayList<Email> emails, boolean withQuotation,
			boolean doStemming,String language, int minDocFreqPerWord, String dataset_name) {
		WordFilters filters = new WordFilters(doStemming,language);
		HashMap<String, Integer> dfMap = new HashMap<>();
		if (withQuotation) {
			for (Email e : emails) {
				Set<String> emailsWords = new HashSet<>();
				for (String word : e.getSubject().split(" ")) {
					String[] filterWords = filters.applyfilters(word);
					for (String w : filterWords) {
						if ((w != null) || (w != "")) {
							e.insertWord(w, true);
							e.incNumWords();
							emailsWords.add(w);
						}
					}
				}
				for (String word : e.getText().split(" ")) {
					String[] filterWords = filters.applyfilters(word);
					for (String w : filterWords) {
						if ((w != null) || (w != "")) {
							e.insertWord(w, false);
							e.incNumWords();
							emailsWords.add(w);
						}
					}
				}
				for (String w : emailsWords) {
					Integer count = dfMap.get(w);
					if (count == null)
						count = 0;
					dfMap.put(w, (count + 1));
				}
			}

			// delete all the word with DF<min
			for (Iterator<Entry<String, Integer>> it = dfMap.entrySet().iterator(); it.hasNext();) {
				Entry<String, Integer> entry = it.next();
				if (entry.getValue() < minDocFreqPerWord) {
					it.remove();
					// System.out.println("remove: "+entry.getKey()+ "
					// "+entry.getValue());
				}
			}

			for (Email e : emails) {
				// TF and IDF calculation
				double sumWeight = 0;
				Iterator<Word> it = e.getBow().iterator();
				while (it.hasNext()) {
					Word w = it.next();
					if (!dfMap.containsKey(w.getWord())) {
						it.remove();
						// System.out.println("remove from doc: "+w.getWord());
					} else {
						w.setIdf(emails.size() * 1.0 / dfMap.get(w.getWord()));
						w.setTf(1 + Math.log10(w.getOccurence()));
						double weight = w.getTf() * Math.log(w.getIdf());
						w.setWeight(weight);
						sumWeight += weight * weight;
					}
				}

				// NORMALIZATION
				for (Word w : e.getBow()) {
					w.setWeight(w.getWeight() / Math.sqrt(sumWeight));
				}

			}

			/*
			 * try{ PrintWriter pw=new PrintWriter(dataset_name+"_idfMap.txt");
			 * for(Entry<String, Integer> e: dfMap.entrySet())
			 * pw.println(e.getKey()+"\t"+(emails.size()*1.0/e.getValue()));
			 * pw.close();
			 * 
			 * }catch(Exception e){ e.printStackTrace(); }
			 */

		} else {
			// TODO if we want to NON consider the quotations
		}
		return emails;
	}

}
