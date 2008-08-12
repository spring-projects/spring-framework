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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

/**
 * Represents the list of arguments supplied to a lambda expression. For an expression "{|x,y| $x > $y ? $x : $y }" the
 * argument list is x,y
 * 
 * @author Andy Clement
 * 
 */
public class ArgList extends SpelNode {

	public ArgList(Token payload) {
		super(payload);
	}

	/**
	 * @return a list of the argument names captured in this ArgList
	 */
	public List<String> getArgumentNames() {
		if (getChildCount() == 0)
			return Collections.emptyList();
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < getChildCount(); i++) {
			result.add(getChild(i).getText());
		}
		return result;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		return sb.toString();
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		throw new SpelException(SpelMessages.ARGLIST_SHOULD_NOT_BE_EVALUATED);
	}

}
