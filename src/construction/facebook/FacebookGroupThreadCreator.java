package construction.facebook;

import java.io.PrintWriter;

import facebook4j.Comment;
import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.PagableList;
import facebook4j.Post;
import facebook4j.Reading;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;

public class FacebookGroupThreadCreator {
	
	public static void main(String[] args) {
		new FacebookGroupThreadCreator();
	}
	
	public FacebookGroupThreadCreator() {
		try{
			 // Generate facebook instance.
		    Facebook facebook = new FacebookFactory().getInstance();
		    // Use default values for oauth app id.
		    //facebook.setOAuthAppId("415904031942860", "03d0c1d786a9b62bd90643d49898d2e0");
		    facebook.setOAuthAppId("", "");
		    // Get an access token from: 
		    //https://developers.facebook.com/tools/explorer
		    // Copy and paste it below.
		    String accessTokenString = "CAACEdEose0cBAOPXrEUm1Sz6Iw0JgobllKnk1XlwtyVhSd7QaCWqlnU2w1ZCCF4zlwoophJgVLvG25EpuAThRAlMKKlbTscxuo3gsXtL5zrNw3J4XZCJYOCLtWIYeu5izTMEZBqBstp5R8bsUahlGbOJzMirftTuc8fGMOdka5z7qs3U7V0KdtnNIqdyJfAR04VvnAcXgMPG906BLX2";
		    AccessToken at = new AccessToken(accessTokenString);
		    // Set access token.
		    facebook.setOAuthAccessToken(at);

		    // We're done.
		    // Access group feeds.
		    // You can get the group ID from:
		    // https://developers.facebook.com/tools/explorer

		    
		    //String []group=new String[]{"533592236741787","healthcare device"};
		    //String []group=new String[]{"1642872432610530","IBM support"};
		    //String []group=new String[]{"248604091911838","italiani a Dublino_20150828"};
		    String []group=new String[]{"848992498510493","ireland support android box_20150903"};
		    //String []group=new String[]{"Americansforhealthcare","_20150901"};
		    //String []group=new String[]{"280705392045589","Health and fitness matter"};  chiuso
		    //String []group=new String[]{"875376439186308","IBM GBS AND GTS 2015 PLACED STUDENTS"}; chiuso
		    //String []group=new String[]{"WHO","World Health Organization_20150903"};
		    //String []group=new String[]{"healthychoice","20150901"};
		   
		    
		    ResponseList<Post> feeds = facebook.getGroupFeed(group[0],
		            new Reading().limit(4000));
		    
		    PrintWriter pw=new PrintWriter("facebook_group_"+group[0]+"_"+group[1]+".xml");
		    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		    		+ "<!DOCTYPE root>\n"
		    		+ "<root>");
	        // For all 25 feeds...
	        for (int i = 0; i < feeds.size(); i++) {
	        	
	            // Get post.
	            Post post = feeds.get(i);
	            // Get (string) message.
	            
	            String message = post.getMessage();
	          
	            // Get more stuff...
	            PagableList<Comment> comments = post.getComments();
	            
	            if(message==null || comments.size()<3)
	            	continue;
	            
	            message=message.replaceAll("\n", " ");
	            
	        	pw.println("<thread id=\""+i+"\">");

	            
	            String date = post.getCreatedTime().toString();
	            System.out.println("\n\ndate: "+date);
	            String name = post.getFrom().getName();
	            String userid = post.getFrom().getId();
	            System.out.println("from: "+name);
	            String id = post.getId();
	            System.out.println("id: "+id);
	            System.out.println("link: "+post.getLink());
	            // Print out the message.
	            System.out.println("message:"+message);
	            
	            
	        	pw.println("\t<post>");
	        	pw.println("\t\t<id>"+id+"</id>");
	        	pw.println("\t\t<date>"+date+"</date>");
	        	pw.println("\t\t<from_name>"+name+"</from_name>");
	        	pw.println("\t\t<from_id>"+userid+"</from_id>");
	        	
	        	if(post.getLink()!=null)
	        		pw.println("\t\t<link>"+post.getLink()+"</link>");
	        	//else
	        	//	pw.println("\t\t<link></link>");
	        		
	        	pw.println("\t\t<likecount>"+post.getLikes().size()+"</likecount>");
	        	pw.println("\t\t<text>"+message+"</text>");
	        	pw.println("\t\t</post>");
	        	
	        	
	            
	            System.out.println();
	            for(Comment c : comments){
	            	if(c.getMessage()==null || c.getMessage().length()<2)
	            		continue;
	            	System.out.println("\t"+c.getCreatedTime().toString()+"-"+c.getFrom().getName()+": "+c.getMessage().replaceAll("\n", " "));
	            	
	            	pw.println("\t<post>");
		        	pw.println("\t\t<id>"+c.getId()+"</id>");
		        	pw.println("\t\t<date>"+c.getCreatedTime().toString().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</date>");
		        	pw.println("\t\t<from_name>"+ c.getFrom().getName().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</from_name>");
		        	pw.println("\t\t<from_id>"+c.getFrom().getId().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</from_id>");
		        	//pw.println("\t\t<link></link>");
		        	pw.println("\t\t<likecount>"+c.getLikeCount()+"</likecount>");
		        	pw.println("\t\t<text>"+c.getMessage().replaceAll("\n", " ").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</text>");
		        	pw.println("\t</post>");
	            }

	        	pw.println("</thread>");
	        }      
        	pw.println("</root>");   
        	pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
}
