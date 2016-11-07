package processing.text.enron.textproc;

import java.util.ArrayList;

import processing.text.enron.model.Email;

public class RetrieveEmailQuotes {
		
	private ArrayList<Email> emails;
	
	public RetrieveEmailQuotes(ArrayList<Email> emails) {
		this.emails=emails;
	}
	
	public void createAllEmailSentencesHashCode(){
		for(Email e : emails){	
			ArrayList<Integer> hashList=new ArrayList<>();
			for(String s : this.getEmailContentSentences(e))
				hashList.add(s.replaceAll("\\W", "").replaceAll("\\d","").hashCode());	
			e.setContentHashCode(hashList);
		}
	}
	
	public ArrayList<String> getEmailContentSentences(Email e){
		ArrayList<String> sentences=new ArrayList<>();
		String [] eLines=e.getText().split("\n");
		int i=0;
		/*while(!eLines[i].startsWith("X-FileName:"))
			i++;
		i++;*/
		for(;i<eLines.length;i++){
			
			if(eLines[i].equals("-----Original Message-----")){
				i++;
				while(!eLines[i].startsWith(""))
					i++;
				continue;
			}
			if(eLines[i].equals(""))
				continue;
			
			String sentence=eLines[i];
			//control if the line ends with any punctuation
			while(!eLines[i].trim().matches(".*\\p{Punct}") && (i<eLines.length-1)){
				i++;
				if(eLines[i].equals("")){
					break;
				}					
				if(Character.isUpperCase(eLines[i].charAt(0))){
					i--;
					break;
				}				
				sentence+=" "+eLines[i];
			}
						
			sentences.add(sentence);	
			/*if(s.startsWith("Message-ID:") || s.startsWith("Date:") || s.startsWith("From:") || s.startsWith("Subject:")
				|| s.startsWith("Mime-Version:") || s.startsWith("Content-Type:") || s.startsWith("Content-Transfer-Encoding:") 
				|| s.startsWith("X-From:")|| s.startsWith("X-To:")|| s.startsWith("X-cc:")|| s.startsWith("X-bcc:")
				|| s.startsWith("X-Folder")|| s.startsWith("X-Origin:")|| s.startsWith("X-FileName:")|| s.startsWith("X-bcc:")){
				waitForEmails=false
				
			}
			else if(s.startsWith("To:") || s.startsWith("Cc:") || s.startsWith("Bcc:")){
				waitForEmails=false
			}*/
		}
		return sentences;
	}
	
	
	public Email splitContentAndQuotesForBC3Datas(Email email){
		String content="";
		String quotes="";
		String [] eLines=email.getText().split("\n");
		for(String s : eLines){
			if(s.startsWith("&gt;") || s.startsWith(">"))
				quotes+=s+"\n";
			else
				content+=s+"\n";
		}
		email.setCleanContent(content);
		email.setCleanQuotes(quotes);
		return email;
	}
	
	
	public Email splitContentAndQuotes(Email email){
		String content="";
		String quotes="";
		ArrayList<String> emailSentencesToControl=getEmailContentSentences(email);
		for(int i=0;i<emailSentencesToControl.size();i++){
			String sent=emailSentencesToControl.get(i);
			int hash=sent.replaceAll("\\W", "").replaceAll("\\d","").hashCode();
			boolean isQuote=false;
			for(Email e : emails){
				if(e.getId()==email.getId() || e.getTimestamp().after(email.getTimestamp()) 
						|| (!e.getIvolvedUsers().contains(email.getSender())))
					continue;
				/*boolean atLeastOneOverlap=false;
				for(int u : e.getIvolvedUsers()){
					if(e.getIvolvedUsers().contains(u)){
						atLeastOneOverlap=true;
						break;
					}
				}
				if(!atLeastOneOverlap)
					continue;*/
				
				for(int h : e.getContentHashCode()){
					if(hash==h){
						isQuote=true;
						quotes+=sent+"\n";
						break;
					}					
				}	
				if(isQuote)
					break;
			}	
			if(!isQuote)
				content+=sent+"\n";	
		}
		
		
		/*for(Email e : emails){
			if(e.getId()==email.getId() || e.getTimestamp().after(email.getTimestamp()))
				continue;
			
			ArrayList<Integer> quotesFinded=new ArrayList<>();
			for(int i=0;i<emailSentencesToControl.size();i++){
				String sent=emailSentencesToControl.get(i);
				int hash=sent.replaceAll("\\W", "").replaceAll("\\d","").hashCode();
				for(int h : e.getContentHashCode()){
					if(hash==h){
						quotesFinded.add(i);
						quotes+=sent+"\n";
					}
				}
				for(int torem : quotesFinded)
					emailSentencesToControl.remove(torem);
			}
		}
		
		for(int i=0;i<emailSentencesToControl.size();i++){
			text[0]+=emailSentencesToControl.get(i)+"\n";
		}*/
		email.setCleanContent(content);
		email.setCleanQuotes(quotes);
		return email;
	}
	

}
