Content-based Event Tweet Retrieval (CETRe)
=====

This Event Tweet Retrieval tool is based on the content similarity between the Hashtags' Tf-idf(Term frequency - inverse document frequence).

See the following paper: 
Xinyue Wang, [Laurissa Tokarchuk]( http://www.eecs.qmul.ac.uk/~laurissa), and [Stefan Poslad](http://www.eecs.qmul.ac.uk/~stefan) (2014). Identifying Relevant Event Content for Real-time Event Detection. To be appeared in Proceedings of the 2014 IEEE/ACM International Conference on Advances in Social Networks Analysis and Mining (ASONAM '14).

Motivation
----------
A Twitter crawler is software that filters a large number of twitter data, tweets, to select the ones of interest, i.e., that match a set of hashtags and key terms as search criteria. One important application is to use the Twitter crawler to detect unplanned events or trends of mass interest. However, the majority of Twitter crawlers use a set of predefined keywords that is often highly subjective and can easily lead to incomplete tweets being selected and retrieved. This is often because related terms which are note predefined, but which become key terms and hashtags, cannot be used as the (predefined) search criteria. Even, given expert knowledge, new keywords and specialised hashtags often arise in the midst of such events. Another issue is that in order to identify events and trends, we need to analyse a large collection tweets, however,  free access to Twitter data is rate limited so that we can typically only access 1% of the available tweets.  The consequence of this is that the effect of the set of limited key search terms is greatly aggravated and this means that we are far less likely to reliably detect unforeseen events or trends.

We have developed software (in Java) to automatically to generate a better, more comprehensive set of search terms based upon correlating the traffic patterns of new key words against predefined words, without requiring manual modification of the search terms. We validated our proposed method over the 2012 and 2014 Olympics, 2013 Glastonbury (UK) music festival, MH370 missing plane, and other events. This approach produces higher volume of relevant additional traffic for the event of interest and in real-time.

Run the code
------------
###Dependencies
In order to run the program, the computer must have the following tools/jars installed
  + [Java](https://www.java.com/en/download/chrome.jsp?locale-=en)
  + [MySQL](http://dev.mysql.com/downloads/)
  + [MySQL jdbc](http://dev.mysql.com/downloads/connector/j/)
  + [Twitter4j 3.0.5](http://twitter4j.org/archive/twitter4j-3.0.5.zip)
  + [Mahout 0.7](http://archive.apache.org/dist/mahout/0.7/)
  + [Hadoop 0.20.2]
  + [Lucene 3.6.2]

###Accounts
Please change them in the CETRe/util/Settings.java
- Twitter account: the number of accounts needed depends on the time interval (10 min interval will need about 6 accounts, and the smaller the interval the more the accounts are needed, the following parameters are in the format [Twitter/MySQL parameters -> variable name in the code])
  + Comsumer Key -> ConsumerKey
  + Comsumer Secret -> ConsumerSecret
  + Access Token -> AccessToken
  + AccessSecret -> AccessSecret
- MySQL database
  + host name -> HOSTNAME
  + user name -> USER
  + password -> PWD
  + database name -> databaseName

###Input parameters
Parameters are initialized in the crawler/util/Settings.java
- command line changeable
  + initial keywords -> baseKeywords
  + time frame -> timer
  + sample time slot -> sample
  + if only retrieve tweets through Search API for a limit time ago -> limitTime (set true) & timeLimit (the time period)
- text file changable
  + blacklist: the blacklist can be modified during the crawling, but must follow the format like "#keys"
- others: please see in the file

###Outputs
All the outputs are named with a prefix which indicates the running time & date. For example, if the this CETRe cralwer is started at 12:00 30th Jun, the prefix will be "T06301200"...
- Keywords List: a txt file records all the keywords will be generated under KeyWord file with name [prefix]KeywordList.txt
- Black List: a txt file records all the black list keys with name [prefix]BlackList.txt (this can be modified during the crawling)
- MySQL table: a table stores all the collected tweets with name [prefix]COR
  + MySQL table format: 
  
    	pid bigint(50) NOT NULL,
	createdAt text DEFAULT NULL, 
	geoLocationLat double NOT NULL,
	geoLocationLong double NOT NULL,	
	placeInfo text,
	id bigint(50) NOT NULL, 
	tweet longtext CHARACTER SET utf8, 
	source text CHARACTER SET utf8, 
	lang text,
	screenName VARCHAR(150),
	replyTo text,
	rtCount bigint(50), 
	hashtags text, 
	PRIMARY KEY (pid)

###Entrance
The main method is in the file CETRe/CETReCrawler.java

Previous Solutions
-------------
This is a consecutive work of the [Exploiting Hashtags for Adaptive Microblog Crawling](http://dl.acm.org/citation.cfm?id=2492517.2492624). In that previous work, we showed that the static keywords manner of event tweets retrieval risks losing a significant amount of relevant information. We proposed an adaptive crawling model that analyzes the traffic patterns of the hashtags collected from the live stream to update subsequent collection queries. To evaluate it, we first applied the Keyword Adaptive algorithm to a dataset collected during the 2012 London Olympic Games, and we also test it during 2013 Glastonbury music festival in real-time. Our analysis shows that adaptive crawling based on the proposed adaptive crawling based upon hashtag traffic pattern collects a more comprehensive dataset than pre-defined keyword crawling. However, a further investigation on the collected datasets shows that this approach can’t properly adapt under large-scale global events and sometimes introducing too much noise on topic finding. As a result, we proposed this CETRe to overcome the aforementioned issues.

Pros and Cons
-------------
This CETRe tool tries to identify new keywords that talk about the event of interest. The performance is much more stable than the [Adaptive Crawling based upon hashtag traffic pattern]( https://github.com/0827moon/Adaptive-Crawler). Although, it can lead to new noisy terms being generated which would otherwise worsen the detection of related tweets and the overall performance is improved according to multiple experiments on different kinds of events. Additional, events with few extra new hashtags are not the target application as the emerging hashtags are essential for the algorithm to adapt. New keywords that were identified during an event, always represents sub-event topics. To summarise, the extra benefit of this tool is that it can detect the emerging trending event topics in real-time, with good precision (>80%) and an acceptable recall (>50%).
