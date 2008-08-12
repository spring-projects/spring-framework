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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.internal.KeyValuePair;

/**
 * Represents projection, where a given operation is performed on all elements in some input sequence, returning 
 * a new sequence of the same size. For example:
 * "{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}" returns "[n, y, n, y, n, y, n, y, n, y]"
 * 
 * @author Andy Clement
 * 
 */
public class Projection extends SpelNode {

	public Projection(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object operand = state.getActiveContextObject();

		// When the input is a map, we push a special context object on the stack
		// before calling the specified operation. This special context object
		// has two fields 'key' and 'value' that refer to the map entries key
		// and value, and they can be referenced in the operation
		// eg. {'a':'y','b':'n'}.!{value=='y'?key:null}" == ['a', null]
		if (operand instanceof Map) {
			Map<?, ?> mapdata = (Map<?, ?>) operand;
			List<Object> result = new ArrayList<Object>();
			for (Object k : mapdata.keySet()) {
				try {
					state.pushActiveContextObject(new KeyValuePair(k, mapdata.get(k)));
					result.add(getChild(0).getValue(state));
				} finally {
					state.popActiveContextObject();
				}
			}
			return result;
		} else if (operand instanceof Collection) {
			List<Object> data = new ArrayList<Object>();
			data.addAll((Collection<?>) operand);
			List<Object> result = new ArrayList<Object>();
			int idx = 0;
			for (Object element : data) {
				try {
					state.pushActiveContextObject(element);
					state.enterScope("index", idx);
					result.add(getChild(0).getValue(state));
				} finally {
					state.exitScope();
					state.popActiveContextObject();
				}
				idx++;
			}
			return result;
		} else {
			throw new SpelException(SpelMessages.PROJECTION_NOT_SUPPORTED_ON_TYPE, operand.getClass().getName());
		}
	}

	@Override
	public String toStringAST() {
		StringBuffer sb = new StringBuffer();
		return sb.append("!{").append(getChild(0).toStringAST()).append("}").toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
