package construction.enron;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class getEmails {
	private static String mailDir = "C:\\Users\\IBM_ADMIN\\Desktop\\DATASET\\maildir\\";
	private static String from = null;
	private static String[] to = null;
	private static String[] cc = null;
	private static String[] bcc = null;
	private static Set<String> emails = new HashSet<>();

	public static void main(String[] args) throws IOException {
		
		// for java 8
//		Files.walk(Paths.get(mailDir)).forEach(filePath -> {
		
		File dir = new File(mailDir);
		List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);		

		for (File file : files) {
			String line = null;

			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(new FileInputStream(file.getCanonicalPath()), "UTF8"));

				while ((line = br.readLine()) != null) {
					if (from == null)
						from = Mail.getFrom(line);

					if (to == null)
						to = Mail.getTo(line);

					if (cc == null)
						cc = Mail.getCC(line);

					if (bcc == null)
						bcc = Mail.getBCC(line);

					if (line.contains("X-"))
						break;
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (from != null && to != null) {
				if (!emails.contains(from))
					emails.add(from);

				for (int i = 0; i < to.length; i++)
					if (!emails.contains(to[i]))
						emails.add(to[i]);

				if (cc != null)
					for (int i = 0; i < cc.length; i++)
						if (!emails.contains(cc[i]))
							emails.add(cc[i]);

				if (bcc != null)
					for (int i = 0; i < bcc.length; i++)
						if (!emails.contains(bcc[i]))
							emails.add(bcc[i]);
			}
			from = null;
			to = null;
			bcc = null;
			cc = null;
		}
		
		FileWriter writer = new FileWriter("emails");
		
		for (String email : emails)
			if (!email.isEmpty())
				writer.write(email.toLowerCase() + "\n");
		
		writer.close();
	}
}