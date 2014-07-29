package crawler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import crawler.util.Settings;
import crawler.util.analyzer.TwitterAnalyzer;

import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

public class DataStream implements StatusListener {
	private ArrayList<Status> statusRepo = new ArrayList<Status>();
	
	public ArrayList<Status> getList() {
		ArrayList<Status> statuses = new ArrayList<Status>(statusRepo);
		statusRepo.clear();
		return statuses;
	}
	
	public int getSize() {
		return statusRepo.size();
	}
	
	private void freqUpdate(Status status) {
		//String hashtags = "";//
		HashtagEntity[] tags = status.getHashtagEntities();
		for (HashtagEntity t: tags) {
			String hashtag = "#"+t.getText().toLowerCase();
			
			int val = 1;
			if (Settings.TFHashtagFreq.containsKey(hashtag)) {
				val += Settings.TFHashtagFreq.get(hashtag);
			}
			Settings.TFHashtagFreq.put(hashtag, val);
			
			//hashtags+=t.getText()+",";//
			
		}
		//System.out.println(status.getCreatedAt()+", [" + hashtags + "] - " + status.getText());//
	}
	
	public void onStatus(Status status) {
		if(!trash(status.getText())){
			statusRepo.add(status);
			freqUpdate(status);
		}			
		/*HashtagEntity[] tags = status.getHashtagEntities();
		String hashtags = "";
		for (HashtagEntity t: tags) hashtags+=t.getText()+",";
		System.out.println(status.getCreatedAt()+", [" + hashtags + "] - " + status.getText());*/
	}

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
	
	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
	    //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
	}
	
	public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
	    //System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
	}
	
	public void onScrubGeo(long userId, long upToStatusId) {
	    //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
	}
	
	public void onException(Exception ex) {
	    ex.printStackTrace();
	}

	@Override
	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
		
	}
}
