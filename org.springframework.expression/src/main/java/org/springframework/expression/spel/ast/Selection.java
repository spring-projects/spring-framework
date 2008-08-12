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
 * Represents selection over a map or collection. For example: {1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'} returns
 * [2, 4, 6, 8, 10]
 * 
 * Basically a subset of the input data is returned based on the evaluation of the expression supplied as selection
 * criteria.
 * 
 * @author Andy Clement
 */
public class Selection extends SpelNode {

	public final static int ALL = 0; // ?{}
	public final static int FIRST = 1; // ^{}
	public final static int LAST = 2; // ${}

	private final int variant;

	public Selection(Token payload, int variant) {
		super(payload);
		this.variant = variant;
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object operand = state.getActiveContextObject();
		SpelNode selectionCriteria = getChild(0);
		if (operand instanceof Map) {
			Map<?, ?> mapdata = (Map<?, ?>) operand;
			List<Object> result = new ArrayList<Object>();
			for (Object k : mapdata.keySet()) {
				try {
					Object kvpair = new KeyValuePair(k, mapdata.get(k));
					state.pushActiveContextObject(kvpair);
					Object o = selectionCriteria.getValue(state);
					if (o instanceof Boolean) {
						if (((Boolean) o).booleanValue() == true) {
							if (variant == FIRST)
								return kvpair;
							result.add(kvpair);
						}
					} else {
						throw new SpelException(selectionCriteria.getCharPositionInLine(),
								SpelMessages.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);// ,selectionCriteria.stringifyAST());
					}
				} finally {
					state.popActiveContextObject();
				}
				if ((variant == FIRST || variant == LAST) && result.size() == 0) {
					return null;
				}
				if (variant == LAST) {
					return result.get(result.size() - 1);
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
					Object o = selectionCriteria.getValue(state);
					if (o instanceof Boolean) {
						if (((Boolean) o).booleanValue() == true) {
							if (variant == FIRST)
								return element;
							result.add(element);
						}
					} else {
						throw new SpelException(selectionCriteria.getCharPositionInLine(),
								SpelMessages.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);// ,selectionCriteria.stringifyAST());
					}
					idx++;
				} finally {
					state.exitScope();
					state.popActiveContextObject();
				}
			}
			if ((variant == FIRST || variant == LAST) && result.size() == 0) {
				return null;
			}
			if (variant == LAST) {
				return result.get(result.size() - 1);
			}
			return result;
		} else {
			throw new SpelException(getCharPositionInLine(), SpelMessages.INVALID_TYPE_FOR_SELECTION,
					(operand == null ? "null" : operand.getClass().getName()));
		}
	}

	@Override
	public String toStringAST() {
		StringBuffer sb = new StringBuffer();
		switch (variant) {
		case ALL:
			sb.append("?{");
			break;
		case FIRST:
			sb.append("^{");
			break;
		case LAST:
			sb.append("${");
			break;
		}
		return sb.append(getChild(0).toStringAST()).append("}").toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

}
