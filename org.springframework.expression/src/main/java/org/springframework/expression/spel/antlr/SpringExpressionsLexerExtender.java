/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel.antlr;

import org.antlr.runtime.RecognitionException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.WrappedSpelException;
import org.springframework.expression.spel.generated.SpringExpressionsLexer;

/**
 * @author Andy Clement
 * @since 3.0
 */
class SpringExpressionsLexerExtender extends SpringExpressionsLexer {

	/**
	 * recover() attempts to provide better error messages once something has gone wrong. It then throws a
	 * InternalELException (has to be this unchecked exception as the exception must flow through Antlr lexer methods
	 * that do not have declared exceptions). The InternalELException will be caught at the top level and altered to
	 * include context (line,column) information before being rethrown.<br>
	 * 
	 * This error analysis code is in recover() rather than reportError() because reportError() isn't always called by
	 * the lexer and there is no way to add the calls to it by editing the .g file.
	 */
	@Override
	public void recover(RecognitionException re) {
		// TODO recovery needs an overhaul once the expression language syntax is agreed
		
		// List<?> rules = getRuleInvocationStack(re, SpringExpressionsLexer.class.getName());
		// String failedRule = (String) rules.get(rules.size() - 1);
		// System.out.println("DBG: lexer rule " + failedRule);
		// need a concrete example of error recovery in here please! then i can delete the below
		// if (re instanceof NoViableAltException) {
		// NoViableAltException nvae = (NoViableAltException) re;
		// // example error data: { "abc": def }
		// if (failedRule.equals("mTokens") && Character.isLetter((char) (nvae.getUnexpectedType()))) {
		// logger.error(ParserMessage.ERROR_STRINGS_MUST_BE_QUOTED, re.line, re.charPositionInLine);
		// }
		//
		// } else if (re instanceof MismatchedRangeException) {
		// // MismatchedRangeException mre = (MismatchedRangeException) re;
		// // example error data: [ 123e ]
		// if (failedRule.equals("mDIGIT") && rules.size() > 3 && ((String) rules.get(rules.size() -
		// 3)).equals("mExponent")) {
		// logger.error(ParserMessage.ERROR_INVALID_EXPONENT, re.line, re.charPositionInLine);
		// }
		// } else if (re instanceof MismatchedTokenException) {
		// MismatchedTokenException mte = (MismatchedTokenException) re;
		// logger.error(ParserMessage.ERROR_MISMATCHED_CHARACTER, mte.charPositionInLine, mte.charPositionInLine,
		// getCharErrorDisplay(mte.expecting), getCharErrorDisplay(mte.c));
		// }
		SpelException realException = new SpelException(re, SpelMessages.RECOGNITION_ERROR, re.toString());
		throw new WrappedSpelException(realException);
	}

	@Override
	public void reportError(RecognitionException re) {
		// Do not report anything. If better messages could be reported they will have been reported
		// by the recover() method above.
	}

//	private String getTokenForId(int id) {
//		if (id == -1)
//			return "EOF";
//		return getTokenNames()[id];
//	}

}
