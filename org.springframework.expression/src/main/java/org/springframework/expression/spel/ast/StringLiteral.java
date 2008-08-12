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

public class StringLiteral extends Literal {

	private String value;

	public StringLiteral(Token payload) {
		super(payload);
		value = payload.getText();
		// TODO should these have been skipped being created by the parser rules? or not?
		value = value.substring(1, value.length() - 1);
		value = value.replaceAll("''", "'");
	}

	@Override
	public String getLiteralValue() {
		return value;
	}

	@Override
	public String toString() {
		return new StringBuilder("'").append(getLiteralValue()).append("'").toString();
	}

}
