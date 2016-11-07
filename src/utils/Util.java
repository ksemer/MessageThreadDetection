package utils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Util {
	
    public static String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static Document getDocumentFromXmlString(String xml)
    {
    	try{
        	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	InputSource is = new InputSource();
        	is.setCharacterStream(new StringReader(xml));
        	return db.parse(is);
    	}catch(Exception e ){
    		try{
    			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    		}catch(Exception ee ){
    			ee.printStackTrace();
    		}
    	}
    	return null;
    }    
    
    public static double truncate (double x)
    {
        int i=(int)(x*1000);
		return (double)(i/1000.0);
    }
	
	public static String arrayToString(int [] array){
		String s="";
		for(int i=0;i<array.length;i++){
			s+=array[i];
			if(i<array.length-1)
				s+=" | ";
		}
		return s;
	}
	
	public static double log_base( double x, double base )
	{
		return Math.log(x) / Math.log(base);
	}

	public static double log2( double x )
	{
		return log_base(x,2);
	}
	
	public static boolean isInSubDirectory(File dir, File file) {

	    if (file == null)
	        return false;

	    if (file.equals(dir))
	        return true;

	    return isInSubDirectory(dir, file.getParentFile());
	}
}
