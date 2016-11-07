package construction.facebook;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MergerXmlGroups {
	
	public static void main(String[] args) {
		
		String fileNew="facebook_group_WHO_World Health Organization_20150903.xml";
		String fileOld="facebook_group_WHO_World Health Organization.xml";
		/*String onlyNewFileName="facebook_group_WHO_World Health Organization_20150903_onlynew.xml";*/
		/*String fileNew="facebook_group_healthychoice_20150901.xml";
		String fileOld="facebook_group_healthychoice_.xml";
		String onlyNewFileName="facebook_group_healthychoice_20150901_onlynew.xml";
		/*String fileNew="facebook_group_Americansforhealthcare__20150901.xml";
		String fileOld="facebook_group_Americansforhealthcare_.xml";
		String onlyNewFileName="facebook_group_Americansforhealthcare__20150901_onlynew.xml";*/

		ArrayList<String> oldIDs=new ArrayList<>();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(fileOld));
		    String line;
		    while ((line = reader.readLine()) != null){
		    	if(line.contains("<id>"))
		    		oldIDs.add(line.split(">")[1].split("<")[0]);
		    }
		    reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		try{			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fileNew);
			doc.getDocumentElement().normalize();
			
			Document docOnlyNew = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = docOnlyNew.createElement("root");   
			docOnlyNew.appendChild(root);   


			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
					
			NodeList tList = doc.getElementsByTagName("thread");			
					
			for (int t = 0; t < tList.getLength(); t++) {
				Node nodethread = tList.item(t);							 				 
				if (nodethread.getNodeType() == Node.ELEMENT_NODE) {					 
					Element elementThread = (Element) nodethread;
					
					NodeList postList = elementThread.getElementsByTagName("post");			
					boolean threadNew=true;
					
					for (int p = 0; p < postList.getLength(); p++) {
						Node nodePost = postList.item(p);							 				 
						if (nodePost.getNodeType() == Node.ELEMENT_NODE) {					 
							Element elementPost = (Element) nodePost;
							
							if(oldIDs.contains(elementPost.getElementsByTagName("id").item(0).getTextContent())){
								threadNew=false;
								break;
							}
						}
					}
					
					if(threadNew){
						Node newNode = docOnlyNew.importNode(nodethread, true);
					    root.appendChild(newNode);	
					}
				}
			}	
			
			TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            aTransformer.setOutputProperty("indent", "yes");

            Source src = new DOMSource(docOnlyNew);
            Result dest = new StreamResult(System.out);
            aTransformer.transform(src, dest);  
			
			/*
            FileInputStream fstream=new FileInputStream(fileOld);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line=br.readLine();
            while(line!=null){
            	if(line.contains("<id>")){
            		
            	}
            	oldIDs.put(Integer.valueOf(line.split(" ")[0]),Double.valueOf(line.split(" ")[1]));
                line=br.readLine();
            }
            br.close();
            in.close();
            fstream.close();
            testPMIDResultMap.put(Integer.valueOf(file.getName().replaceAll(".prd", "")), fMap);*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
