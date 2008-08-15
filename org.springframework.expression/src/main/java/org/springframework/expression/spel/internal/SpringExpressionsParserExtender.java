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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.generated.SpringExpressionsParser;

public class SpringExpressionsParserExtender extends SpringExpressionsParser {

	public SpringExpressionsParserExtender(TokenStream input) {
		super(input);
	}

	/**
	 * Override super type implementation and just include the character position rather than the line number since the
	 * expressions are nearly all going to be just one line.
	 */
	@Override
	public String getErrorHeader(RecognitionException e) {
		StringBuilder retval = new StringBuilder();
		retval.append("(pos ").append(e.charPositionInLine).append("): ");
		return retval.toString();
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		String message = getErrorMessage(e, tokenNames);
		// TODO would something like this be worthwhile to improve messages?
		// if (message.equals("no viable alternative at input '<EOF>'") && !paraphrase.isEmpty()) {
		// // This means we ran out of input building something, that something is named in paraphrase
		// message = "no more input data to process whilst constructing " + paraphrase.peek();
		// }
		SpelException parsingProblem = new SpelException(e.charPositionInLine, e, SpelMessages.PARSE_PROBLEM, message);
		throw new InternalELException(parsingProblem);
	}

	/**
	 * Overridden purely because the base implementation does a System.err.println()
	 */
	@Override
	public void recoverFromMismatchedToken(IntStream input, RecognitionException e, int ttype, BitSet follow)
			throws RecognitionException {
		// if next token is what we are looking for then "delete" this token
		if (input.LA(2) == ttype) {
			reportError(e);
			/*
			 * System.err.println("recoverFromMismatchedToken deleting "+input.LT(1)+ " since "+input.LT(2)+" is what we
			 * want");
			 */
			beginResync();
			input.consume(); // simply delete extra token
			endResync();
			input.consume(); // move past ttype token as if all were ok
			return;
		}
		if (!recoverFromMismatchedElement(input, e, follow)) {
			throw e;
		}
	}

	@Override
	public String getTokenErrorDisplay(Token t) {
		if (t == null) {
			return "<unknown>";
		}
		return super.getTokenErrorDisplay(t);
	}
}
