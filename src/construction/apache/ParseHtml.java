package construction.apache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import processing.text.enron.model.Email;

public class ParseHtml {

	public static void main(String[] args) {
		new ParseHtml();
	}
	
	private static final String DB_URL = "jdbc:mysql://localhost:3306/apache";
	private static final String USER = "";
	private static final String PASS = "";

	private static Connection connect = null;
	private static PreparedStatement preparedStatement = null;
	
	public ParseHtml() {
		try{
			
			HashMap<String, Integer> userToID=new HashMap<>();
			HashMap<String, Integer> subjectToID=new HashMap<>();
			int userid=0,threadid=0,emailid=0;
			ArrayList<Email> emails=new ArrayList<>();
			HashMap<Integer, ArrayList<Email>> threads=new HashMap<>();
			
			PrintWriter pw=new PrintWriter("apache_messages_links.txt");
			//String dirToSave="/home/giacomo/Desktop/apache dataset";
			String [] months=new String[]{"201108","201109","201110","201111","201112","201201","201202","201203"};
			for(String month : months){
				for(int page=0;page<25;page++){
					System.out.println("LINK OF PAGE: "+month+".mbox/date?"+page);
					List<String> links=this.getLinksOnPage("http://mail-archives.apache.org/mod_mbox/tomcat-dev/"+month+".mbox/date?"+page);
					for(String s : links){
						if(s.contains(month) && s.contains("%")){
							//pw.println(s);
							//System.out.println(s);	
							
							URL url = new URL(s);
					        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
					        String line;
					        String from="",subject="";
					        String text="",clean="",quotes="";
					        while ((line = in.readLine()) != null){
					            //System.out.println(line);
					            if(line.contains("<tr class=\"from\">")){
					            	line = in.readLine();
					            	line = in.readLine();
					            	from=line.split(">")[1].split("<")[0];					            	
					            }else if(line.contains("<tr class=\"subject\">")){
					            	line = in.readLine();
					            	line = in.readLine();
					            	subject=line.split(">")[1].split("<")[0];					            	
					            }else if(line.contains("<tr class=\"date\">")){
					            	line = in.readLine();
					            	line = in.readLine();					            						            	
					            }else if(line.contains("<tr class=\"contents\">")){
					            	line = in.readLine();
					            	while(!line.contains("</pre>")){
					            		if(!line.equals("To unsubscribe, e-mail: dev-unsubscribe@tomcat.apache.org") && !line.equals("For additional commands, e-mail: dev-help@tomcat.apache.org")){
					            			text+=line+"\n";
					            			if(line.length()>4){
					            				if(line.substring(0, 4).equals("&gt;") || line.substring(0, 1).equals(">") )
					            					quotes+=line+"\n";
					            				else
					            					clean+=line+"\n";
					            			}
				            				else
				            					clean+=line+"\n";						            		
					            		}					            		
					            		line = in.readLine();
					            		if(line==null)
					            			break;
					            	}
					            }
					            
					            
					        }
					        in.close();
					        //System.out.println("from: "+from+" date: "+date+" subject: "+subject);
					        //System.out.println("clean: "+clean+" \nquotes: "+quotes);
					        
					        String subjectTrimmed=subject=subject.toLowerCase();       
					        subjectTrimmed=subjectTrimmed.replaceAll("re:", "");
					        subjectTrimmed=subjectTrimmed.replaceAll("fw:", "");
					        subjectTrimmed=subjectTrimmed.replaceAll("fwd:", "");
					        subjectTrimmed=subjectTrimmed.trim();
					        					        
					        Integer tID=subjectToID.get(subjectTrimmed);
					        if(tID==null){
					        	tID=threadid;
					        	subjectToID.put(subjectTrimmed, tID);
					        	threadid++;
					        }
					        
					        Integer uID=userToID.get(from);
					        if(uID==null){
					        	uID=userid;
					        	userToID.put(from, uID);
					        	userid++;
					        }
					        
					        Email e=new Email(emailid, tID, uID, subject, text);
					        e.setCleanContent(clean);
					        e.setCleanQuotes(quotes);
					        emailid++;
					        
					        emails.add(e);
					        ArrayList<Email> t=threads.get(tID);
					        if(t==null){
					        	t=new ArrayList<>();
					        }
					        t.add(e);
					        threads.put(tID, t);
					        
					        System.out.println("t: "+tID+" e: "+emailid+ " u: "+uID);
					        
							//System.exit(0);
						}
						
					}
				}
			}
			pw.close();

			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			
			for(Entry<Integer,ArrayList<Email>> t : threads.entrySet()){
				if(t.getValue().size()>3){
					System.out.println("write on DB t:"+t.getKey()+"\t#emails: "+t.getValue().size());
					for(Email e : t.getValue()){
				        this.addMessage(e.getId(), e.getThreadid(), e.getTimestamp(), (int)e.getSender(), null, e.getSubject(), e.getText(), e.getCleanQuotes(), e.getCleanContent());
				    }
				}
			}
			
			connect.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	private void addMessage(int msgID, int threadID, Timestamp dateTimestamp, int from, Set<Integer> to, String subject, String allText, String quote, String clean) throws java.text.ParseException {
		try {
			preparedStatement = connect.prepareStatement("insert into email_messages (email_id, sender_id, thread_id, subject, text, clean_text, quotation_text) values (?, ?, ?, ?, ?, ?, ?)");
			preparedStatement.setInt(1, msgID);
			preparedStatement.setInt(2, from);
			preparedStatement.setInt(3, threadID);
			preparedStatement.setString(4, subject);
			preparedStatement.setString(5, allText);
			preparedStatement.setString(6, clean);
			preparedStatement.setString(7, quote);
			preparedStatement.executeUpdate();    
			
			this.addConnection(from, msgID, dateTimestamp);						
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}
	
	private void addConnection(int from, int emailID, Timestamp dateTimestamp) throws java.text.ParseException {	
		try {
			preparedStatement = connect.prepareStatement("insert into communications (sender_id, email_id, time) values (?, ?, ?)");
			preparedStatement.setInt(1, from);
			//preparedStatement.setInt(2, to);
			preparedStatement.setInt(2, emailID);
			preparedStatement.setTimestamp(3,  dateTimestamp);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private List<String> getLinksOnPage(final String url) {
		try{
			 final Parser htmlParser = new Parser(url);
			    final List<String> result = new LinkedList<>();

			    try {
			        final NodeList tagNodeList = htmlParser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
			        for (int j = 0; j < tagNodeList.size(); j++) {
			            final LinkTag loopLink = (LinkTag) tagNodeList.elementAt(j);
			            final String loopLinkStr = loopLink.getLink();
			            result.add(loopLinkStr);
			        }
			    } catch (ParserException e) {
			        e.printStackTrace(); // TODO handle error
			    }
			    return result;
		}catch(Exception e){
			e.printStackTrace();
		}

	    return null;

	}
}
