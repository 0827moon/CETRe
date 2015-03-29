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

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;


public class TwitterFilter extends FilteringTokenFilter {
	//private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	
	public TwitterFilter(boolean enablePositionIncrements, TokenStream input) {
		super(enablePositionIncrements, input);
	}

	@Override
	protected boolean accept() throws IOException {
		String type = typeAtt.type();
		if (type.equals(TwitterAttri.PONCTUATIONS)||type.equals(TwitterAttri.MENTION)||type.equals(TwitterAttri.URL))
			return false;
		else return true;
	}

}
