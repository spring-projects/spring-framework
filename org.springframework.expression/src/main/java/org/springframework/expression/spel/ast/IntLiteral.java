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
package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;

public class IntLiteral extends Literal {

	private static String[] suffixes = {"UL" , "LU" , "ul" , "lu" , "uL" , "lU" , "U" , "L" , "u" , "l" };
	
	private Integer value;

	public IntLiteral(Token payload) {
		super(payload);
		// TODO properly support longs and unsigned numbers
		String toParse = payload.getText();
		try {
			value = Integer.parseInt(toParse);
		} catch (NumberFormatException nfe) {
			for (int i=0;i<suffixes.length;i++) {
				if (toParse.endsWith(suffixes[i])) {
					value = Integer.parseInt(toParse.substring(0,toParse.length()-suffixes[i].length()));
					return;
				}
			}
			throw nfe;
		}
	}
	

	public IntLiteral(Token payload, int radix) {
		super(payload);
		value = Integer.parseInt(payload.getText().substring(2), radix);
	}

	@Override
	public Integer getLiteralValue() {
		return value;
	}

}
