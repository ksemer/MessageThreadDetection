package processing.text.enron.model;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Email {
	
	private int id;
	private long senderid;
	private ArrayList<Long> receiversid;
	private String subject;
	private String text;
	private Timestamp timestamp;
	private ArrayList<Word> bow;
	private Document alchemy_document;
	private Document alchemy_document_onlycleanedcontet;
	/*private Document alchemy_keywords;
	private Document alchemy_concepts;
	private Document alchemy_entities;
	private Document alchemy_sentiment;*/
	private int numWords;
	private ArrayList<Integer> contentHashCode;
	private String cleanContent;
	private String cleanQuotes;
	private int threadid;
	
	
	public Email() {
		this.subject="";
		this.text="";
		this.receiversid=new ArrayList<>();
		this.setNumWords(0);
		bow=new ArrayList<>();
	}

	public Email(long senderid,String subject, String text) {
		this.senderid=senderid;
		this.subject=subject;
		this.text=text;
		this.receiversid=new ArrayList<>();
		this.setNumWords(0);
		this.bow=new ArrayList<>();
	}
	
	public Email(int id, long senderid,String subject, String text) {
		this.id=id;
		this.senderid=senderid;
		this.subject=subject;
		this.text=text;
		this.receiversid=new ArrayList<>();
		this.setNumWords(0);
		bow=new ArrayList<>();
	}

	public Email(int id, int threadid, long senderid,String subject, String text) {
		this.id=id;
		this.threadid=threadid;
		this.senderid=senderid;
		this.subject=subject;
		this.text=text;
		this.receiversid=new ArrayList<>();
		this.setNumWords(0);
		bow=new ArrayList<>();
	}

	public long getSender() {
		return senderid;
	}

	public void setSender(long senderid) {
		this.senderid = senderid;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ArrayList<Long> getReceiversid() {
		return receiversid;
	}

	public void setReceiversid(ArrayList<Long> receiversid) {
		this.receiversid = receiversid;
	}
	
	public void insertReceiver(long userid){
		if(!receiversid.contains(userid))
			receiversid.add(userid);
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public ArrayList<Word> getBow() {
		return bow;
	}

	public void setBow(ArrayList<Word> bow) {
		this.bow = bow;
	}
	
	public void insertWord(String word, boolean inSubject){
		for(Word w : bow){
			if(w.word.equals(word)){
				w.incOccurence();
				return;
			}
		}
		this.bow.add(new Word(word,inSubject));
	}
	
	public void insertWord(String word, int occurrence, double idf, boolean inSubject){
		for(Word w : bow){
			if(w.word.equals(word)){
				for(int i=0;i<occurrence;i++)
					w.incOccurence();
				return;
			}
		}
		this.bow.add(new Word(word, occurrence, 0, 0, idf, inSubject));
	}

	/*public Document getAlchemy_keywords() {
		return alchemy_keywords;
	}

	public void setAlchemy_keywords(Document alchemy_keywords) {
		this.alchemy_keywords = alchemy_keywords;
	}

	public Document getAlchemy_entities() {
		return alchemy_entities;
	}

	public void setAlchemy_entities(Document alchemy_entities) {
		this.alchemy_entities = alchemy_entities;
	}

	public Document getAlchemy_sentiment() {
		return alchemy_sentiment;
	}

	public void setAlchemy_sentiment(Document alchemy_sentiment) {
		this.alchemy_sentiment = alchemy_sentiment;
	}

	public Document getAlchemy_concepts() {
		return alchemy_concepts;
	}

	public void setAlchemy_concepts(Document alchemy_concepts) {
		this.alchemy_concepts = alchemy_concepts;
	}*/
	

	public int getNumWords() {
		return numWords;
	}

	public void setNumWords(int numWords) {
		this.numWords = numWords;
	}
	
	public void incNumWords() {
		this.numWords++;
	}

	public ArrayList<Integer> getContentHashCode() {
		return contentHashCode;
	}

	public void setContentHashCode(ArrayList<Integer> contentHashCode) {
		this.contentHashCode = contentHashCode;
	}

	public String getCleanContent() {
		return cleanContent;
	}

	public void setCleanContent(String cleanContent) {
		this.cleanContent = cleanContent;
	}

	public String getCleanQuotes() {
		return cleanQuotes;
	}

	public void setCleanQuotes(String cleanQuotes) {
		this.cleanQuotes = cleanQuotes;
	}
	
	public ArrayList<Long> getIvolvedUsers(){
		ArrayList<Long> u=new ArrayList<>(this.getReceiversid());
		u.add(this.getSender());
		return u;
	}

	public Document getAlchemy_document() {
		return alchemy_document;
	}

	public void setAlchemy_document(Document alchemy_document) {
		this.alchemy_document = alchemy_document;
	}

	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) text, 1->(double) relevance, 2->(int) count, 3->(String) type
	 */
	public ArrayList<Object[]> getAlchemy_entities_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("entity");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[4];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				key[2]=Integer.valueOf(element.getElementsByTagName("count").item(0).getTextContent());
				key[3]=element.getElementsByTagName("type").item(0).getTextContent();
				list.add(key);
			}
		}
		return list;
	}

	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) keyword, 1->(double) relevance
	 */
	public ArrayList<Object[]> getAlchemy_keywords_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("keyword");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[2];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				list.add(key);
			}
		}
		return list;
	}
	
	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) keyword, 1->(double) relevance
	 */
	public ArrayList<Object[]> getAlchemy_concepts_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("concept");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {					 
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[2];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				list.add(key);
			}
		}
		return list;
	}
	

	public ArrayList<String> getAlchemy_concepts_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("concept");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}				
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}

	public ArrayList<String> getAlchemy_keywords_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("keyword");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {		
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}						
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}
	
	public ArrayList<String> getAlchemy_entities_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document.getElementsByTagName("entity");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {			
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}					
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}
	
	public double getAlchemy_SentimentScore(){
		
		NodeList keyList = this.alchemy_document.getElementsByTagName("docSentiment");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				return Double.valueOf(element.getElementsByTagName("score").item(0).getTextContent());
			}
		}
		return 0;
	}

	public int getThreadid() {
		return threadid;
	}

	public void setThreadid(int threadid) {
		this.threadid = threadid;
	}

	public Document getAlchemy_document_onlycleanedcontet() {
		return alchemy_document_onlycleanedcontet;
	}

	public void setAlchemy_document_onlycleanedcontet(Document alchemy_document_onlycleanedcontet) {
		this.alchemy_document_onlycleanedcontet = alchemy_document_onlycleanedcontet;
	}
	
	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) text, 1->(double) relevance, 2->(int) count, 3->(String) type
	 */
	public ArrayList<Object[]> getAlchemy_onlycleanedcontet_entities_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("entity");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[4];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				key[2]=Integer.valueOf(element.getElementsByTagName("count").item(0).getTextContent());
				key[3]=element.getElementsByTagName("type").item(0).getTextContent();
				list.add(key);
			}
		}
		return list;
	}

	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) keyword, 1->(double) relevance
	 */
	public ArrayList<Object[]> getAlchemy_onlycleanedcontet_keywords_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("keyword");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[2];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				list.add(key);
			}
		}
		return list;
	}
	
	/**
	 * 
	 * @return Arraylist of object, each one contains: 0->(String) keyword, 1->(double) relevance
	 */
	public ArrayList<Object[]> getAlchemy_onlycleanedcontet_concepts_List(){
		ArrayList<Object[]> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("concept");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {					 
				Element element = (Element) node;
				if (!element.getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				Object [] key=new Object[2];
				key[0]=element.getElementsByTagName("text").item(0).getTextContent();
				key[1]=Double.valueOf(element.getElementsByTagName("relevance").item(0).getTextContent());
				list.add(key);
			}
		}
		return list;
	}
	

	public ArrayList<String> getAlchemy_onlycleanedcontet_concepts_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("concept");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}				
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}

	public ArrayList<String> getAlchemy_onlycleanedcontet_keywords_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("keyword");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {		
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}						
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}
	
	public ArrayList<String> getAlchemy_onlycleanedcontet_entities_onlyStringList(){
		ArrayList<String> list=new ArrayList<>();
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("entity");			
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {			
				if (!((Element) node).getParentNode().getParentNode().getNodeName().equals("results")) {
				    continue;
				}					
				list.add(((Element) node).getElementsByTagName("text").item(0).getTextContent());
			}
		}
		return list;
	}
	
	public double getAlchemy_onlycleanedcontet_SentimentScore(){
		
		NodeList keyList = this.alchemy_document_onlycleanedcontet.getElementsByTagName("docSentiment");				
		for (int t = 0; t < keyList.getLength(); t++) {
			Node node = keyList.item(t);							 				 
			if (node.getNodeType() == Node.ELEMENT_NODE) {				
				Element element = (Element) node;
				if (!element.getParentNode().getNodeName().equals("results")) {
				    continue;
				}
				return Double.valueOf(element.getElementsByTagName("score").item(0).getTextContent());
			}
		}
		return 0;
	}

}
