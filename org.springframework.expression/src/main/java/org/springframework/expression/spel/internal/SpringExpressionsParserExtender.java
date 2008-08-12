/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel.internal;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.springframework.expression.spel.generated.SpringExpressionsParser;

public class SpringExpressionsParserExtender extends SpringExpressionsParser {

	public SpringExpressionsParserExtender(TokenStream input) {
		super(input);
	}

	/**
	 * This method does not actually recover but can attempt to produce better error messages than a generic Antlr
	 * parser because it knows about grammar specifics. would do
	 */
	@Override
	public void recoverFromMismatchedToken(IntStream input, RecognitionException re, int ttype, BitSet follow)
			throws RecognitionException {
//		CommonTokenStream tokStream = (CommonTokenStream) input;
//		int prevToken = tokStream.LA(-1);
//		int nextToken = tokStream.LA(1);
//		String prevTokenText = tokStream.LT(-1).getText();
//		String expectedToken = getTokenForId(ttype);
		// Use token knowledge to log a more appropriate error:
		// logger.error(ParserMessage.ERROR_NO_LEADING_ZERO, re.line, re.charPositionInLine);
		throw re;
	}

	//    
	// /**
	// * Similar to the BaseRecognizer getErrorMessage() but uses the ParserMessages class to get the text of the
	// message
	// */
	// public void logError(RecognitionException re, String[] tokenNames) {
	// logger.error(ELMessages.RECOGNITION_ERROR, re.line, re.charPositionInLine, re);
	// }

	@Override
	public void recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow)
			throws RecognitionException {
		throw e;
	}

	@Override
	public String getTokenErrorDisplay(Token t) {
		if (t == null) {
			return "<unknown>";
		}
		return super.getTokenErrorDisplay(t);
	}

//	private String getTokenForId(int id) {
//		if (id == -1)
//			return "EOF";
//		return getTokenNames()[id];
//	}

}
