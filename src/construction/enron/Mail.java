package construction.enron;
public class Mail {
	public static String getSubject(String text) {		
		if (text.contains("Subject:")) {
			text = text.replace("Subject: ", "");
			return text;
		}
		return null;
	}

	public static String getDate(String text) {
		if (text.contains("Date:")) {
			text = text.replace("Date: ", "");
			return text;
		}
		return null;
	}
	
	public static String[] getBCC(String text) {
		if (text.startsWith("Bcc:")) {
			text = replace(text);
			text = text.replace("Bcc:", "");
			String[] token;
			
			if (text.contains(";"))
				token = text.split(";");
			else
				token = text.split(",");
			return token;
		}
		return null;		
	}

	public static String[] getCC(String text) {
		if (text.startsWith("Cc:")) {
			text = replace(text);
			text = text.replace("Cc:", "");
			
			String[] token;
			
			if (text.contains(";"))
				token = text.split(";");
			else
				token = text.split(",");
			return token;
		}
		return null;
	}

	public static String[] getTo(String text) {
		if (text.startsWith("To:")) {
			text = replace(text);
			text = text.replace("To:", "");

			String[] token;
			
			if (text.contains(";"))
				token = text.split(";");
			else
				token = text.split(",");
			return token;
		}
		return null;
	}

	public static String getFrom(String text) {
		if (text.startsWith("From:")) {
			text = replace(text);
			text = text.replace("From:", "");
			return text;
		}
		return null;
	}
	
	private static String replace(String text) {
		text= text.replaceAll("e-mail <.","");
		text = text.replaceAll("<.", "");
		text= text.replaceAll("<","");
		text= text.replaceAll(">","");
		text = text.replaceAll("\\s+", "");
		return text;
	}
}