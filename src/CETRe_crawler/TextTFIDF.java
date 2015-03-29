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
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;

import crawler.util.analyzer.TwitterAnalyzer;

public class TextTFIDF {
    private static String dir;// = "./documentData";
    private String inputDir;// = dir+"/sequenceFile";
    private String outputDir;// = dir+"/allTextClusters";
    private static SequenceFile.Writer writer;
    
    public void initial(){
    	Configuration conf = new Configuration();
	    FileSystem fs;
	    String appendix = TwitterCrawler.getFilenameApd();
	    dir = "./documentData/"+appendix;
		inputDir = dir+"/sequenceFile";
		outputDir = dir+"/allTextClusters";
		    
		
		try {
			HadoopUtil.delete(conf, new Path(inputDir));
			fs = FileSystem.get(conf);
			
			//write the key/value pair into sequence format
		    //Create the named file
		    writer = new SequenceFile.Writer(fs, conf,
		        new Path(inputDir, "allText.seq"), Text.class, Text.class);
		} catch (IOException e) {
			e.printStackTrace();
		}    
	}
    
    public void	putData(String hash, String data){
	    try {
	    	 writer.append(new Text(hash), new Text(data));
	         /*for (Document d : Database) {
	 	      writer.append(new Text(d.getID()), new Text(d.contents()));
	 	    }*/
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void closeWrite() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void calculate(){
		int minSupport = 5;
	    int minDf = 1;
	    int maxDFPercent = 100; //remove top 1-X% high frequency words
	    int maxNGramSize = 1;
	    int minLLRValue = 1;
	    int reduceTasks = 1;
	    int chunkSize = 200;
	    int norm = -1; //no normalisation
	    boolean sequentialAccessOutput = true;
	    
	    Configuration conf = new Configuration();
	    //FileSystem fs = FileSystem.get(conf);
	    
	    try {
			HadoopUtil.delete(conf, new Path(outputDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    Path tokenizedPath = new Path(outputDir,
	        DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
	    TwitterAnalyzer analyzer = new TwitterAnalyzer();
	    try {
			DocumentProcessor.tokenizeDocuments(new Path(inputDir), analyzer.getClass()
			    .asSubclass(Analyzer.class), tokenizedPath, conf);
			
			 DictionaryVectorizer.createTermFrequencyVectors(tokenizedPath,
			  	      new Path(outputDir), DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER,
			  	      conf, minSupport, maxNGramSize, minLLRValue, norm, true, reduceTasks,
			  	      chunkSize, sequentialAccessOutput, false);
			    
		    TFIDFConverter.processTfIdf(
		  	      new Path(outputDir , DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER), new Path(outputDir), conf, 
		  	      TFIDFConverter.calculateDF(new Path(outputDir ,DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER),
		  	      new Path(outputDir), conf, chunkSize),
		  	      minDf, maxDFPercent, norm, true, sequentialAccessOutput, 
		  	      false, reduceTasks);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	    analyzer.close();
	    
	   
	}

	public TreeMap<String, Vector> getPointsVectors() {
		TreeMap<String, Vector> points = new TreeMap<String, Vector>();
		
		Configuration conf = new Configuration();
		FileSystem fs;
		try {
			fs = FileSystem.get(conf);
			Path path = new Path(outputDir+"/tfidf-vectors/part-r-00000");
			SequenceFile.Reader readerTFIDF = new SequenceFile.Reader(fs, path, conf);
			Text keyTFIDF = new Text();
			VectorWritable valueTFIDF = new VectorWritable();
			while (readerTFIDF.next(keyTFIDF, valueTFIDF)) {
				Vector point = valueTFIDF.get();
				points.put(keyTFIDF.toString(), point);
			}
			readerTFIDF.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return points;
	}

	
}
