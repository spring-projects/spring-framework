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
import org.springframework.expression.spel.ExpressionState;

/**
 * This is used for preserving positional information from the input expression.
 * 
 * @author Andy Clement
 * 
 */
public class Dot extends SpelNode {
	// TODO Keep Dot for the positional information or remove it?

	public Dot(Token payload) {
		super(payload);
	}

	@Override
	public String toStringAST() {
		return ".";
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		// This makes Dot a do-nothing operation, but this is not free in terms of computation
		return state.getActiveContextObject();
	}

}
