package processing.text.enron.filters;

/**
 * Filter that drops any word belonging to a given list of <i>stop words</i>.
 * @author giacomo
 */
public class PunctuationRemover implements IFilter {

	@SuppressWarnings("unused")
	private String[] punctuation= {"\\.","\\,","\\:","\\;","\\!","\\|","\\'","\\<","\\>","\\=","\\#","\\_","nbsp","\\?","\\*","\\&","\\-","\\+","\\(","\\)","\\\"","\\%","\\/","\\[","\\]","\\{","\\}"};

	public PunctuationRemover() {
	}

	@Override
	public String [] Filter(String word) {
		String newWord="";
		for(int i=0;i<word.length();i++){
			int ascii=(int)word.charAt(i);
			if(ascii<65 || (ascii>90 && ascii<97) || ascii>122){
				//word.replace(word.charAt(i), ' ');
				newWord+=" ";
			}
			else
				newWord+=word.charAt(i);
		}
		
		/*for(String p : punctuation)
			word=word.replaceAll(p, " ");
		word=word.replaceAll("\\s+"," ");*/
		String [] words=newWord.split(" ");
		return words;			
	}
}
