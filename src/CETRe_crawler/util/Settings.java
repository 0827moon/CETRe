package crawler.util;

import java.util.ArrayList;
import java.util.TreeMap;

public class Settings {
	public static String tableName;
	
	public static String databaseName = "...";
	public static final String HOSTNAME = "...";
	public static final String PWD = "...";
	public static final String USER = "...";
	
	public static boolean limitTime = false;
	public static long timeLimit = 999999999;
	public static final int keywordMax = 400;
	public static final int batchSize = 3000;
	public static int minNewFreq = 3;
	public static long timer = 10*60*1000;
	public static long pid = 1;
	
	public static int searchTweetNo = 100;
	
	public static String[] baseKeywords = {"..."};
	public static String[] blackList = {"#bbc", "#bbc2013", "#cnn", "#news", "#twitter", "#socialmedia", "#rt","#teamfollowback","#follow","#followback","#instantfollowback","#autofollow"};
	public static String[] queryKeywords;
	
	public static TreeMap<String,Integer> KeywordsFreq = new TreeMap<String,Integer>();	//all keywords accumulative freq
	public static TreeMap<String,Integer> TFHashtagFreq = new TreeMap<String,Integer>();	//time frame hashtags freq
	public static TreeMap<String,Integer> TFHashtagFreq_last = new TreeMap<String,Integer>();	//last time frame hashtags freq

	
	public static ArrayList<String> ConsumerKey = new ArrayList<String>();
	public static ArrayList<String> ConsumerSecret = new ArrayList<String>();
	public static ArrayList<String> AccessToken = new ArrayList<String>();
	public static ArrayList<String> AccessSecret = new ArrayList<String>();
	
	public final static String[] ConsumerKeyArray = {"",
													 ""
													 };
	public final static String[] ConsumerSecretArray = {"",
		 												""
													 	};
	public final static String[] AccessTokenArray = {"",
		 											 ""
													 };
	public final static String[] AccessSecretArray = {"",
													  ""
													 };
	
}
