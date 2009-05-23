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

package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Represents projection, where a given operation is performed on all elements in some input sequence, returning 
 * a new sequence of the same size. For example:
 * "{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}" returns "[n, y, n, y, n, y, n, y, n, y]"
 * 
 * @author Andy Clement
 * 
 */
public class Projection extends SpelNodeImpl {

	public Projection(Token payload) {
		super(payload);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue op = state.getActiveContextObject();

		Object operand = op.getValue();
		// TypeDescriptor operandTypeDescriptor = op.getTypeDescriptor();
		
		// When the input is a map, we push a special context object on the stack
		// before calling the specified operation. This special context object
		// has two fields 'key' and 'value' that refer to the map entries key
		// and value, and they can be referenced in the operation
		// eg. {'a':'y','b':'n'}.!{value=='y'?key:null}" == ['a', null]
		if (operand instanceof Map) {
			Map<?, ?> mapdata = (Map<?, ?>) operand;
			List<Object> result = new ArrayList<Object>();
			for (Map.Entry entry : mapdata.entrySet()) {
				try {
					state.pushActiveContextObject(new TypedValue(entry,TypeDescriptor.valueOf(Map.Entry.class)));
					result.add(getChild(0).getValueInternal(state).getValue());
				} finally {
					state.popActiveContextObject();
				}
			}
			return new TypedValue(result,TypeDescriptor.valueOf(List.class)); // TODO unable to build correct type descriptor
		} else if (operand instanceof List) {
			List<Object> data = new ArrayList<Object>();
			data.addAll((Collection<?>) operand);
			List<Object> result = new ArrayList<Object>();
			int idx = 0;
			for (Object element : data) {
				try {
					state.pushActiveContextObject(new TypedValue(element,TypeDescriptor.valueOf(op.getTypeDescriptor().getType())));
					state.enterScope("index", idx);
					result.add(getChild(0).getValueInternal(state).getValue());
				} finally {
					state.exitScope();
					state.popActiveContextObject();
				}
				idx++;
			}
			return new TypedValue(result,op.getTypeDescriptor());
		} else {
			throw new SpelException(SpelMessages.PROJECTION_NOT_SUPPORTED_ON_TYPE, operand.getClass().getName());
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		return sb.append("![").append(getChild(0).toStringAST()).append("]").toString();
	}

}
