package crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.math.Vector;

import twitter4j.FilterQuery;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import crawler.util.*;

public class CETRe {
	public static SimpleDateFormat fm = new SimpleDateFormat("MM|dd-HH:mm");
	private String fileFolder = "KeyWord", fileName;
	private int trackThrsd;
	public static TreeMap<String,Integer> TFFreq;
	
	private TwitterStream twitterStream;
	private static DataStorage dataStore;
	private BufferedWriter out; 
	private FilterQuery query = new FilterQuery();
	
	public static DataStream dataStream;
	public static TimeCal Timer;
	public static boolean expire;
	public static boolean finishStore = false;	
	
	public TwitterCrawler() {
		
		String appendix = getFilenameApd();
		fileName = "./"+fileFolder+"/"+appendix;
		Settings.tableName = appendix+"TF";
		trackThrsd = Settings.keywordMax - Settings.baseKeywords.length;
		
		checkFolder();
		Settings.queryKeywords = Settings.baseKeywords;
		writeKeywordFile(Settings.baseKeywords,false);
		writeKeywordFile(Settings.blackList,true); //blacklist writing

		estabTwitterConn();
		try {startCollection();} 
	       catch (SQLException e) {e.printStackTrace();}
	}
	
	private void estabTwitterConn() {
		OAuthUser OA = new OAuthUser();
    	query.track(Settings.baseKeywords);
    	twitterStream = new TwitterStreamFactory(
        		OA.build(Settings.ConsumerKey.get(0),Settings.ConsumerSecret.get(0), 
        				Settings.AccessToken.get(0),Settings.AccessSecret.get(0))).getInstance();

		dataStream = new DataStream();
		twitterStream.addListener(dataStream);
		twitterStream.filter(query);
	}

	public static String getFilenameApd() {
		String date = fm.format(new Date());
		date = "T"+date.replace("-", "");
		date = date.replace(":", "");
		date = date.replace("|", "");
		return date;
	}

	private void startCollection() throws SQLException {
		Timer = new TimeCal(Settings.timer);
		Timer.start();
		
		dataStore = new DataStorage();
		dataStore.initialTable();
		
		dataStore.start();
				
		while(true){
			System.out.print("");
			if(TwitterCrawler.finishStore){
				TwitterCrawler.finishStore = false;
				
				TFFreq = new TreeMap<String,Integer>(Settings.TFHashtagFreq);
				
				checkBlackList();
				checkMinFreq();
				
				ArrayList<Entry<String,Integer>> list = sortByValue(TFFreq);
				/*for(Entry<String, Integer>   entry   :   list){
					String hash = entry.getKey();
					int freq = entry.getValue();
					for(int i = 0; i<Settings.baseKeywords.length; i++){
						if(hash.replace("#", "").equals(Settings.baseKeywords[i].toLowerCase())){
							list.remove(hash);
							System.out.print("{REMOVED}");
						}
					}
					System.out.println("Hashtag <"+hash+"> got frequency "+freq);
				}*/
				
				TFFreq = getTopN(list);
				String topNList = "";
				for(String keyword:TFFreq.keySet().toArray(new String[TFFreq.size()]))	topNList += keyword+", ";
				System.out.printf("************ %3d ************ Top-N Keyword List: %s\n", TFFreq.size(), topNList);
				
				//if using grow ratio to get top N
				//ArrayList<Entry<String,Integer>> listRatio = sortByValue_growRatio();
				//TFFreq = getTopN_growRatio(listRatio);
				
				/*//print sample table
				Iterator<Entry<String, double[]>> iter= TFFreq_sample.entrySet().iterator(); 
				while(iter.hasNext()){
					Entry<String, double[]> ent = iter.next();
					String hashtag = ent.getKey();
					double[] freq = ent.getValue();
					
					String s="";
					for(double d:freq)	s+=d+", ";
					System.out.println("hashtag {"+hashtag+"} got freq {"+s+"}");
				}*/
				putLastKeywords();
							
				Settings.queryKeywords = distFilter();
				
				String queryList = "";
				for(String keyword:Settings.queryKeywords)	queryList += keyword+", ";
				System.out.printf("************ %3d ************ Queried Keyword List: %s\n", Settings.queryKeywords.length, queryList);				
				
				writeKeywordFile(Settings.queryKeywords,false);
				
				query.track(Settings.queryKeywords);
		    	twitterStream.filter(query);
		    	
		    	for(String hash:Settings.queryKeywords){
		    		if(Settings.TFHashtagFreq.containsKey(hash)){
		    			Settings.TFHashtagFreq_last.put(hash, Settings.TFHashtagFreq.get(hash));
		    		}
		    	}
		    	Timer.restart();
		    	Settings.TFHashtagFreq.clear();
		    	TFFreq.clear();
			}
		}
	}
	
	private void putLastKeywords() {
		//??????not sure to keep this!!!
		//check if there are keywords in last time frame but not in this time frame 
		//(given the condition that there is space remaining)
		int count = 0;
		String temp = "";
		if(TFFreq.size()<Settings.keywordMax){
			for(String lastKey : Settings.TFHashtagFreq_last.keySet().toArray(new String[Settings.TFHashtagFreq_last.size()])){
				if(!TFFreq.keySet().contains(lastKey)){
					TFFreq.put(lastKey,Settings.TFHashtagFreq.get(lastKey));
					count++;
					temp+=lastKey+", ";
				}
			}
		}
		System.out.printf("************ %3d ************ Added Last Frame Keywords: %s\n",count,temp);
		Settings.TFHashtagFreq_last.clear();
		
	}

	private String[] distFilter() {
		DataSearch keywordsFilter = new DataSearch(TFFreq.keySet().toArray(new String[TFFreq.size()])); //pass potential keywords
		keywordsFilter.collectNewTweets();
		keywordsFilter.calculateTFIDF();
		
		ArrayList<String> keywordsFreqName = new ArrayList<String>();//
		ArrayList<Vector> keywordsFreq = new ArrayList<Vector>(); //list of keywords vector
		ArrayList<String> keywords = new ArrayList<String>(); //list of keywords

		for(String basehash:Settings.baseKeywords){
			keywordsFreqName.add(basehash);//
			keywordsFreq.add(keywordsFilter.getSeedVect(basehash));
			keywords.add(basehash);
		}
		
		int count = 0;
		while(!keywordsFreq.isEmpty()){
			String seq1Name = keywordsFreqName.get(0);//
			Vector seq1 = keywordsFreq.get(0);	
			Vector seq2;
			Iterator<Entry<String, Integer>> iter= TFFreq.entrySet().iterator(); 
			while(iter.hasNext()){
				Entry<String, Integer> ent = iter.next();
				String hashtag = ent.getKey();
				seq2 = keywordsFilter.getSeedVect(hashtag);
				
				//System.out.print("seq 1 ["+seq1Name+"] count ["+TFFreq.get(seq1Name)+"]: ");//
				//System.out.println(seq1.asFormatString());
				//System.out.print("seq 2 ["+hashtag+"] count ["+TFFreq.get(hashtag)+"]: ");
				if (seq1 != null && seq2 != null){
					//System.out.println(seq2.asFormatString());
					CosineDistanceMeasure cos = new CosineDistanceMeasure();
					double distVal = cos.distance(seq1,seq2);
					//System.out.println("***********["+hashtag+"] v.s ["+seq1Name+"]: "+distVal+"***********");
					
					//check whether one of the two words is baseline criteria
					boolean inBase = false;
					for(String basehash:Settings.baseKeywords){
						if(hashtag.replace("#", "").equals(basehash.toLowerCase()) || hashtag.equals(basehash)
								||keywords.get(count).replace("#", "").equals(basehash.toLowerCase()) || keywords.get(count).equals(basehash)){
							inBase = true;
							break;
						}
					}
					
					//base 0.8, others 0.5
					if(inBase){
						if(distVal < 0.8 && distVal >0.00001 && !keywords.contains(hashtag)){
							keywordsFreqName.add(hashtag);//
							keywordsFreq.add(seq2);
							keywords.add(hashtag);
							System.out.println("BASEL***********["+hashtag+"] v.s ["+seq1Name+"]: "+distVal+"***********");
						}
					}else{
						if(distVal < 0.5 && distVal >0.00001 && !keywords.contains(hashtag)){
							keywordsFreqName.add(hashtag);//
							keywordsFreq.add(seq2);
							keywords.add(hashtag);
							System.out.println("OTHER***********["+hashtag+"] v.s ["+seq1Name+"]: "+distVal+"***********");
						}
					}
				}
			}
			keywordsFreqName.remove(0);//
			keywordsFreq.remove(0);
			count++;
		}
		
		return keywords.toArray(new String[keywords.size()]);
	}

	
	/*private TreeMap<String, Integer> getTopN_growRatio(ArrayList<Entry<String, Integer>> listRatio) {
		TreeMap<String, Integer> keywords = new TreeMap<String, Integer>();

		//baseline criteria
		for(int i = 0; i<Settings.baseKeywords.length; i++){
			keywords.put(Settings.baseKeywords[i], 10000);
		}
		
		//top list
		int count = 1;
		for(Entry<String, Integer>   entry   :   listRatio){
			String hash = entry.getKey();
			int freq = TFFreq.get(hash);
			boolean inBase = false;
			for(String basehash:Settings.baseKeywords){
				if(hash.replace("#", "").equals(basehash.toLowerCase())){
					inBase = true;
					break;
				}
			}
			if(!inBase){
				keywords.put(hash, freq);
				count ++;
			}
			if(count>trackThrsd) break;
		}
		
		//check if there are keywords in last time frame but not in this time frame 
		//(given the condition that there is space remaining)
		ArrayList<Entry<String,Integer>> listOri = sortByValue(Settings.TFHashtagFreq_last);
		int remain = Settings.keywordMax - keywords.size();
		if(remain > 0){
			for(Entry<String, Integer>   entry   :   listOri){
				String hash = entry.getKey();
				int freq = entry.getValue();
				if(!keywords.containsKey(hash)){
					keywords.put(hash, freq);
					remain--;
					if(remain == 0) break;
				}
			}
		}
		
		//kept for using growing ratio approach
		if(Settings.TFHashtagFreq_last.size()!=0){
			Settings.TFHashtagFreq_last.clear();
		}
		Settings.TFHashtagFreq_last = new TreeMap<String,Integer>(keywords);
		return keywords;
	}*/
	
	private TreeMap<String, Integer> getTopN(ArrayList<Entry<String, Integer>> list) {
		TreeMap<String, Integer> keywords = new TreeMap<String, Integer>();

		//baseline criteria
		for(int i = 0; i<Settings.baseKeywords.length; i++){
			keywords.put(Settings.baseKeywords[i], 10000);
		}
		
		//top list
		int count = 1;
		for(Entry<String, Integer>   entry   :   list){
			String hash = entry.getKey();
			int freq = entry.getValue();
			boolean inBase = false;
			for(String basehash:Settings.baseKeywords){
				if(hash.replace("#", "").equals(basehash.toLowerCase())){
					inBase = true;
					break;
				}
			}
			if(!inBase){
				keywords.put(hash, freq);
				count ++;
			}
			if(count>trackThrsd) break;
		}
		
		/*//check if there are keywords in last time frame but not in this time frame 
		//(given the condition that there is space remaining)
		if(keywords.size()<Settings.keywordMax){
			for(String lastKey : Settings.TFHashtagFreq_last.keySet().toArray(new String[Settings.TFHashtagFreq_last.size()])){
				if(!keywords.containsKey(lastKey))
					keywords.put(lastKey, Settings.TFHashtagFreq_last.get(lastKey));
			}
		}
		
		//kept for using growing ratio approach
		if(Settings.TFHashtagFreq_last.size()!=0){
			Settings.TFHashtagFreq_last.clear();
		}
		Settings.TFHashtagFreq_last = new TreeMap<String,Integer>(keywords);*/
		return keywords;
	}

	private ArrayList<Entry<String, Integer>> sortByValue(TreeMap<String, Integer> keywords){
		ArrayList<Entry<String,Integer>> list = new ArrayList<Entry<String,Integer>>(keywords.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>(){  
			@Override
			public int compare(Entry<String, Integer> o1,	Entry<String, Integer> o2) {
				return (o2.getValue() - o1.getValue()); 
			}  
		});
		return list; 
	}

	private void checkMinFreq() {
		String minFreqInTFList= "";
		int num = 0;

		TreeMap<String, Integer> TFFreqTemp = new TreeMap<String, Integer>(TFFreq);
		Iterator<Entry<String, Integer>> iter= TFFreqTemp.entrySet().iterator(); 
		while(iter.hasNext()){
			Entry<String, Integer> entry = iter.next();
			String hash = entry.getKey();
			int freq = entry.getValue();
			if(freq <= Settings.minNewFreq){
				TFFreq.remove(hash);
				//System.out.println(hash+":"+freq);
				minFreqInTFList += hash+", ";
				num ++;
			}
		}
		System.out.printf("************ %3d ********** Removed LowFreq Words: %s\n",num,minFreqInTFList);	
	}

	private void checkBlackList() {
		try{			
			BufferedReader in = new BufferedReader(new FileReader(fileName+"BlackList.txt"));
			String temp = in.readLine();
			String blackInTFList= "";
			int num = 0;
			if(temp!=null) {
				String[] blackList =  temp.split(", ");
				for(String blackWord : blackList) {
					if (TFFreq.containsKey(blackWord)) {
						TFFreq.remove(blackWord);
						blackInTFList += blackWord+", ";
						num ++;
					}
				}
				System.out.printf("************ %3d ********** Removed Black Words: %s\n",num,blackInTFList);
				/*out = new BufferedWriter(new FileWriter(fileName+"BlackWordList.txt",true));
				out.append(fm.format(new Date())+temp+"\n");
				out.close();*/
			}			
			in.close();
    	}catch (IOException e){e.printStackTrace();}
		
	}

	private void writeKeywordFile(String[] keywords, boolean black) {
		String temp = (black? "": ": ");
		for (String s: keywords) { temp += s+", "; }
		try {
			if(!black){
				out = new BufferedWriter(new FileWriter(fileName+"KeywordsList.txt",true));
				out.append(fm.format(new Date())+temp+"\n");
			}else{
				out = new BufferedWriter(new FileWriter(fileName+"BlackList.txt"));
				out.append(temp);
			}
			
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void checkFolder() {
		File KeywordFolder;
		boolean stateKeywordFolder;

		KeywordFolder = new File("./KeyWord");
		stateKeywordFolder = KeywordFolder.exists();

		if(stateKeywordFolder == false ){
			System.out.println("The 'KeyWord' folder do not exist,trying to create one...");
			stateKeywordFolder = KeywordFolder.mkdir();
			if( stateKeywordFolder == false ){
				System.out.println("Unable to create the folder,please check disk ...");
				System.exit(1);
			}
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		interactInput();
		new TwitterCrawler();
	}

	 private static void interactInput(){
		 for(int i = 0; i<Settings.ConsumerKeyArray.length;i++){
			 Settings.ConsumerKey.add(Settings.ConsumerKeyArray[i]);
			 Settings.ConsumerSecret.add(Settings.ConsumerSecretArray[i]);
			 Settings.AccessToken.add(Settings.AccessTokenArray[i]);
			 Settings.AccessSecret.add(Settings.AccessSecretArray[i]);
		 }
		 
		 try {															  
			 System.out.printf("********************DEFAULT VALUE******************\n");
			 for(int i = 0; i<Settings.baseKeywords.length;i++){
				 System.out.printf("************ Query %1d: %15s *************\n", i+1,Settings.baseKeywords[i]);
			 }
			 System.out.printf("****************** Timer: %1d mins ******************\n",Settings.timer/60/1000);
			 System.out.printf("***************************************************\n");
			
			 System.out.print("Please enter the query terms(using comma to differentiate, press enter for default): ");
			 BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));    	
			 String temp = bufferRead.readLine();
			 if (!temp.equals("")){
				 Settings.baseKeywords = temp.split(",");
			 }else{
				 System.out.println("Default value used!");
			 } 
        	
			 System.out.print("Please enter the time interval in minutes(press enter for default): ");
			 bufferRead = new BufferedReader(new InputStreamReader(System.in));    	
			 temp = bufferRead.readLine();
			 if (!temp.equals("")){
				 double inNum = Double.parseDouble(temp);
				 Settings.timer = (long) (inNum*60*1000);
			 }else{
				 System.out.println("Default value used!");
			 }
        	
			 System.out.printf("*******************ACTUAL VALUE********************\n");
			 for(int i = 0; i<Settings.baseKeywords.length;i++){
				 System.out.printf("************ Query %2d: %15s ************\n", i+1,Settings.baseKeywords[i]);
			 }
			 System.out.printf("**************** Timer: %2.1f mins ******************\n",(double)Settings.timer/60/1000);
			 System.out.printf("***************************************************\n");
			
		 } catch (IOException ioe) {
			 System.out.println("IO error!");
			 System.exit(1);
		}
	}
}
