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
