
package processing.text.enron.filters.snowball;

import java.lang.reflect.Method;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

public class TestApp {
    private static void usage()
    {
        System.err.println("Usage: TestApp <algorithm> <input file> [-o <output file>]");
    }

    public static void main(String [] arg) throws Throwable {
	
    	String lang="italian";
    	
    	String text="Ragazzi tutto questo che dite è anche sul sito? Perchè sto controllando ora e la tariffa minima per 1 gb e 100 minuti è di 15 euro"
    			+ " Si è incendiato un capannone vicino all'aereoporto ma ora è tutto ok. Sono arrivato 2 ore fa da Roma quindi i voli hanno ripreso normalmente";

	Class stemClass = Class.forName("processing.text.enron.filters.snowball.ext." +
			lang + "Stemmer");
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();

	/*Reader reader;
	reader = new InputStreamReader(new FileInputStream("/home/giacomo/svn/trunk/facebook_group_248604091911838_italiani a Dublino.xml"));
	reader = new BufferedReader(reader);*/

	StringBuffer input = new StringBuffer();

        OutputStream outstream = System.out;
	Writer output = new OutputStreamWriter(outstream);
	output = new BufferedWriter(output);

	int repeat = 1;

	Object [] emptyArgs = new Object[0];
	int character;
	for(String s : text.replaceAll("\n", " ").split(" ")){
	//while ((character = reader.read()) != -1) {
	    //char ch = (char) character;
	    /*if (Character.isWhitespace((char) ch)) {
		if (input.length() > 0) {
		    stemmer.setCurrent(input.toString());
		    for (int i = repeat; i != 0; i--) {
			stemmer.stem();
		    }
		    output.write(stemmer.getCurrent());
		    output.write('\n');
		    input.delete(0, input.length());
		}
	    } else {
		input.append(Character.toLowerCase(ch));
	    }*/
		 stemmer.setCurrent(s);
		    for (int i = repeat; i != 0; i--) {
		    	stemmer.stem();
		    }
		    output.write(stemmer.getCurrent());
		    output.write('\n');
	}
	output.flush();
    }
}
