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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * PlaceHolder nodes are created for tokens that come through from the grammar purely to give us additional positional
 * information for messages/etc.
 * 
 * @author Andy Clement
 * 
 */
public class Placeholder extends SpelNode {

	public Placeholder(Token payload) {
		super(payload);
	}

	@Override
	public String getValue(ExpressionState state) throws SpelException {
		throw new SpelException(getCharPositionInLine(), SpelMessages.PLACEHOLDER_SHOULD_NEVER_BE_EVALUATED);
	}

	@Override
	public String toStringAST() {
		return getText();
	}

}
