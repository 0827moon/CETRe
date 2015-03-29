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

package crawler.util.analyzer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

@SuppressWarnings("deprecation")
public class TwitterAttri extends TokenFilter{
	public static final String URL = "url";
	public static final String MENTION = "@mention";
	public static final String HASHTAG = "hashtag";
	public static final String PONCTUATIONS = "pounctuation";
	public static final String REPEAT = "repeat";
	
	private TermAttribute termAttr;
	private TypeAttribute typeAttr;

	public TwitterAttri(TokenStream input) {
		super(input);
		termAttr = addAttribute(TermAttribute.class);
		typeAttr = addAttribute(TypeAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if(!input.incrementToken())
		return false;
		
		String newWord = removePunctuation(termAttr.term());
		newWord = removeRepeat(newWord);
		
		termAttr.setTermBuffer(newWord);
		return true;
	}

	private String removeRepeat(String token) {
		String newToken = token.replaceAll("(.)\\1{2,}", "$1");
		if(!newToken.equals(token))	typeAttr.setType(REPEAT);
		return newToken;
	}

	private String removePunctuation(String token) {
		if(token.startsWith("http://")) {
			typeAttr.setType(URL);
			return token;
		}
		
		char[] temp = token.toCharArray();
		if(temp[0] == '@') {
			typeAttr.setType(MENTION);
			return token;
		}
		if(temp[0] == '#') {
			typeAttr.setType(HASHTAG);
			return token;
		}
		
		for (int i=0;i<temp.length;i++) {
			if(!Character.isLetterOrDigit(temp[i])){
				if(i!=temp.length-1){
					typeAttr.setType(PONCTUATIONS);
					return token;
				}else return token.replace(""+temp[i], "");
			}
			else if(i == temp.length-1) return token; 
			else continue;
		}
		
		return token;
	}
}
