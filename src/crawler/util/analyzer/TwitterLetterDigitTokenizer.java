package crawler.util.analyzer;

import java.io.Reader;

//import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.LetterTokenizer;
import org.apache.lucene.util.Version;

public class TwitterLetterDigitTokenizer extends LetterTokenizer{
	
	public TwitterLetterDigitTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	protected boolean  isTokenChar(int c) {
		if ((c==(int)'@')||(c==(int)'#')||(c==(int)'/')||(c==(int)':')||(c==(int)'.'))
			return true;
		else return Character.isLetterOrDigit(c);
	}

	
}
