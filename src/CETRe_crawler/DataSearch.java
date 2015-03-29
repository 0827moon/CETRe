/* Copyright [2014] [Xinyue Wang]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package crawler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.mahout.math.Vector;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import crawler.util.OAuthUser;
import crawler.util.Settings;
import crawler.util.analyzer.TwitterAnalyzer;

public class DataSearch{
	private TextTFIDF cluster = new TextTFIDF();
	private TreeMap<String,Vector> points = new TreeMap<String,Vector>();
	private ArrayList<String> keywords = new ArrayList<String> ();
	private TreeMap<String,String> hashtext = new TreeMap<String,String>();
	private Twitter twitter;
	private OAuthUser OA;
	
	public DataSearch(String[] keywords) {
		this.keywords.addAll(Arrays.asList(keywords));
		
		OA = new OAuthUser();
		twitter = new TwitterFactory(
        		OA.build(Settings.ConsumerKey.get(0),Settings.ConsumerSecret.get(0),
        				Settings.AccessToken.get(0),Settings.AccessSecret.get(0))).getInstance();
	}
	
	public void collectNewTweets() {
		
		while(keywords.size()>0){
			String hash = keywords.get(0);
			Query query = new Query(hash);
			query.setCount(Settings.searchTweetNo);
			QueryResult result;
			
			try {
				result = twitter.search(query);
				List<Status> tweets = result.getTweets();
				if(tweets.size() == 0)	TwitterCrawler.TFFreq.remove(hash);
				for (Status status : tweets) {
					//System.out.println("{"+hash+"}["+status.getCreatedAt().toString()+"]- [USER: " + status.getUser().getScreenName() + "] - " + status.getText());
					String temp;
					if(trash(status.getText())){
						if(Settings.limitTime){
							long currentTime = System.currentTimeMillis();
							long tweetTime = status.getCreatedAt().getTime();
							
							if(currentTime-tweetTime>Settings.timeLimit) continue;
						}
						if(hashtext.containsKey(hash))	temp = hashtext.get(hash)+status.getText();
						else temp = status.getText();
						hashtext.put(hash, temp);
					}
				}
				//System.out.println("{"+hash+"} with "+tweets.size()+"tweets");
				keywords.remove(0);
			} catch (TwitterException e) {
				if(e.getErrorCode()==88){
					System.out.println("Using other keys???");
					
					String ConsumerKey_temp = Settings.ConsumerKey.get(0);
					String ConsumerSecret_temp = Settings.ConsumerSecret.get(0);
					String AccessToken_temp = Settings.AccessToken.get(0);
					String AccessSecret_temp = Settings.AccessSecret.get(0);
					
					Settings.ConsumerKey.remove(ConsumerKey_temp);
					Settings.ConsumerSecret.remove(ConsumerSecret_temp);
					Settings.AccessToken.remove(AccessToken_temp);
					Settings.AccessSecret.remove(AccessSecret_temp);
					
					twitter = new TwitterFactory(
			        		OA.build(Settings.ConsumerKey.get(0),Settings.ConsumerSecret.get(0),
			        				Settings.AccessToken.get(0),Settings.AccessSecret.get(0))).getInstance();
					
					Settings.ConsumerKey.add(Settings.ConsumerKey.size(), ConsumerKey_temp);
					Settings.ConsumerSecret.add(Settings.ConsumerSecret.size(), ConsumerSecret_temp);
					Settings.AccessToken.add(Settings.AccessToken.size(), AccessToken_temp);
					Settings.AccessSecret.add(Settings.AccessSecret.size(), AccessSecret_temp);
					
					System.out.println("**************************************************************");
					System.out.println("["+ConsumerKey_temp+"] to ["+Settings.ConsumerKey.get(0)+"]");
					System.out.println("["+ConsumerSecret_temp+"] to ["+Settings.ConsumerSecret.get(0)+"]");
					System.out.println("["+AccessToken_temp+"] to ["+Settings.AccessToken.get(0)+"]");
					System.out.println("["+AccessSecret_temp+"] to ["+Settings.AccessSecret.get(0)+"]");
					System.out.println("**************************************************************");
					
					continue;
				}
				e.printStackTrace();
			}
		}
	
		
		
	}

	//check if the tweet carry the hashtag
	private boolean trash(String tweet) {
		Analyzer analyzer = new TwitterAnalyzer();
		
		StringReader in = new StringReader(tweet);
		TokenStream ts = analyzer.tokenStream("body", in);
		TermAttribute termAtt = ts.addAttribute(TermAttribute.class);
		TypeAttribute typeAttr = ts.addAttribute(TypeAttribute.class);
		int totalCount = 0, hashCount = 0;
		//Vector v = new SequentialAccessSparseVector(100);                   
		try {
			while (ts.incrementToken()) {
			  char[] termBuffer = termAtt.termBuffer();
			  int termLen = termAtt.termLength();      

			  //System.out.println(w);
			  if(typeAttr.type().equals("hashtag")) hashCount++;
			  totalCount++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(totalCount-hashCount<=1)	return true;
		else return false;
	}

	public void calculateTFIDF() {		
		//write sequence file
		cluster.initial();
		for(String hash:hashtext.keySet()){
			cluster.putData(hash, hashtext.get(hash));
		}
		cluster.closeWrite();
		
		//calculateTFIDF
		cluster.calculate();
		points = cluster.getPointsVectors();
	}

	public Vector getSeedVect(String hashtag) {
		Vector seq = points.get(hashtag);
		return seq;
	}
}
