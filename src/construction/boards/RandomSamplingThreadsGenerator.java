package construction.boards;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class RandomSamplingThreadsGenerator {

	public static void main(String[] args) {
		String mainFolderPath="/home/domeniconi/Desktop/dataset/boards.ie/";
		int [] years= new int[]{2006,2007};
		long seedrandom=1;
		int numThreads=1000;
		int minPost=3;
		int maxPost=40;
		String outFile="boards_threadSampling_2006-2007_3-40-1000.txt";
		
		ArrayList<ArrayList<File>> allThreads=new ArrayList<>();
		
		for(int year : years){
			File folder = new File(mainFolderPath+year+"/threads/000/");
			java.io.File[] listOfFolders = folder.listFiles();
			for (File folderThread : listOfFolders) {
				java.io.File[] fileList = folderThread.listFiles();
				for (File file : fileList) {	
			    	if(!file.getName().contains("page")){
			    		ArrayList<File> threadPages=new ArrayList<>();
			    		for (File f : fileList) {
			    			if(f.getName().split("%26page")[0].equals(file.getName())){
			    				threadPages.add(f);				    
			    			}
			    		}
				    	allThreads.add(threadPages);
				    	//if(threadPages.size()>1)
						//	System.out.println(Arrays.toString(threadPages.toArray()));
			    	}	

			    }
			}
		}
		
		System.out.println("# total threads: "+allThreads.size());
		
		
		Random random=new Random(seedrandom);
		Collections.shuffle(allThreads,random);
		
		try{
			ArrayList<Integer> threads=new ArrayList<>();

			PrintWriter pw=new PrintWriter(outFile);
			int cont=0, i=0, sumSize=0;
			while(cont<numThreads){
				ArrayList<String> posts=new ArrayList<>();
				int threadid=-1;
				
				
				ArrayList<File> threadFiles=allThreads.get(i);
				//System.out.println(Arrays.toString(threadFiles.toArray()));
				
				boolean breakReadThread=false;
				
				for(File threadFile : threadFiles){
					FileInputStream fstream = new FileInputStream(threadFile);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String line=br.readLine();
					while(line!=null){
						if(line.contains("<sioc:Post rdf:about=")){
							int postid=Integer.valueOf(line.split("p=")[1].split("\"")[0]);
							
							//check if the post file exists
							boolean postExists=false;
							String postFile="http%3A%2F%2Fboards.ie%2Fvbulletin%2Fsioc.php%3Fsioc_type%3Dpost%26sioc_id%3D"+postid;
							
							File postfolder = new File(threadFile.getParentFile().getParent().replace("threads", "posts")+"/");
							java.io.File[] listOfFolders = postfolder.listFiles();
							for (File folderPost : listOfFolders) {
								File f=new File(folderPost+"/"+postFile);
								if(f.exists()){
									posts.add(f.getAbsolutePath().replaceAll("/home/domeniconi/Desktop/dataset/boards.ie/", ""));
									postExists=true;
									break;
								}
									
							}
							if(!postExists){
								posts=new ArrayList<>();
								//System.out.println("post missing, discard thread: "+ threadid);
								breakReadThread=true;
								break;
							}
						}
						else if(line.contains("<sioc:Thread rdf:about=")){
							threadid=Integer.valueOf(line.split("t=")[2].split("\">")[0]);				
						}
						line=br.readLine();
					}
					br.close();
					in.close();
					fstream.close();
					
					if(breakReadThread)
						break;
				}
				
				
				if(posts.size()<=maxPost && posts.size()>=minPost && threadid>0){
					pw.print(threadFiles.get(threadFiles.size()-1).getAbsolutePath().replaceAll("/home/domeniconi/Desktop/dataset/boards.ie/", ""));
					//pw.print(threadFile.getParentFile());
					for(String p : posts)
						pw.print(" "+p);
					pw.println();
					cont++;
					sumSize+=posts.size();
					threads.add(threadid);
					System.out.println(i+"/"+cont+"\tadd thread: "+threadid+ " #posts: "+posts.size());
				}
				else{
					System.out.println(i+"/"+cont+"\tdiscard thread: "+threadid+ " #posts: "+posts.size());
				}				
				i++;		
			}
			pw.close();
			System.out.println("#threads: "+threads.size()+"\tavg post per thread: "+(sumSize*1.0/threads.size()));
		}catch(Exception e){
			e.printStackTrace();
		}
		
		

	}
	
	

}
