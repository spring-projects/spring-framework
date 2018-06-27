/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Represents selection over a map or collection.
 * For example: {1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'} returns [2, 4, 6, 8, 10]
 *
 * <p>Basically a subset of the input data is returned based on the
 * evaluation of the expression supplied as selection criteria.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.0
 */
public class Selection extends SpelNodeImpl {

	public static final int ALL = 0; // ?[]

	public static final int FIRST = 1; // ^[]

	public static final int LAST = 2; // $[]

	private final int variant;

	private final boolean nullSafe;


	public Selection(boolean nullSafe, int variant, int pos, SpelNodeImpl expression) {
		super(pos, expression);
		this.nullSafe = nullSafe;
		this.variant = variant;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueRef(state).getValue();
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		TypedValue op = state.getActiveContextObject();
		Object operand = op.getValue();
		SpelNodeImpl selectionCriteria = this.children[0];

		if (operand instanceof Map) {
			Map<?, ?> mapdata = (Map<?, ?>) operand;
			// TODO don't lose generic info for the new map
			Map<Object, Object> result = new HashMap<>();
			Object lastKey = null;

			for (Map.Entry<?, ?> entry : mapdata.entrySet()) {
				try {
					TypedValue kvPair = new TypedValue(entry);
					state.pushActiveContextObject(kvPair);
					state.enterScope();
					Object val = selectionCriteria.getValueInternal(state).getValue();
					if (val instanceof Boolean) {
						if ((Boolean) val) {
							if (this.variant == FIRST) {
								result.put(entry.getKey(), entry.getValue());
								return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
							}
							result.put(entry.getKey(), entry.getValue());
							lastKey = entry.getKey();
						}
					}
					else {
						throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
								SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
					}
				}
				finally {
					state.popActiveContextObject();
					state.exitScope();
				}
			}

			if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
				return new ValueRef.TypedValueHolderValueRef(new TypedValue(null), this);
			}

			if (this.variant == LAST) {
				Map<Object, Object> resultMap = new HashMap<>();
				Object lastValue = result.get(lastKey);
				resultMap.put(lastKey,lastValue);
				return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultMap),this);
			}

			return new ValueRef.TypedValueHolderValueRef(new TypedValue(result),this);
		}

		if (operand instanceof Iterable || ObjectUtils.isArray(operand)) {
			Iterable<?> data = (operand instanceof Iterable ?
					(Iterable<?>) operand : Arrays.asList(ObjectUtils.toObjectArray(operand)));

			List<Object> result = new ArrayList<>();
			int index = 0;
			for (Object element : data) {
				try {
					state.pushActiveContextObject(new TypedValue(element));
					state.enterScope("index", index);
					Object val = selectionCriteria.getValueInternal(state).getValue();
					if (val instanceof Boolean) {
						if ((Boolean) val) {
							if (this.variant == FIRST) {
								return new ValueRef.TypedValueHolderValueRef(new TypedValue(element), this);
							}
							result.add(element);
						}
					}
					else {
						throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
								SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
					}
					index++;
				}
				finally {
					state.exitScope();
					state.popActiveContextObject();
				}
			}

			if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
				return ValueRef.NullValueRef.INSTANCE;
			}

			if (this.variant == LAST) {
				return new ValueRef.TypedValueHolderValueRef(new TypedValue(CollectionUtils.lastElement(result)), this);
			}

			if (operand instanceof Iterable) {
				return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
			}

			Class<?> elementType = null;
			TypeDescriptor typeDesc = op.getTypeDescriptor();
			if (typeDesc != null) {
				TypeDescriptor elementTypeDesc = typeDesc.getElementTypeDescriptor();
				if (elementTypeDesc != null) {
					elementType = ClassUtils.resolvePrimitiveIfNecessary(elementTypeDesc.getType());
				}
			}
			Assert.state(elementType != null, "Unresolvable element type");

			Object resultArray = Array.newInstance(elementType, result.size());
			System.arraycopy(result.toArray(), 0, resultArray, 0, result.size());
			return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultArray), this);
		}

		if (operand == null) {
			if (this.nullSafe) {
				return ValueRef.NullValueRef.INSTANCE;
			}
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION, "null");
		}

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION,
				operand.getClass().getName());
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		switch (this.variant) {
			case ALL:
				sb.append("?[");
				break;
			case FIRST:
				sb.append("^[");
				break;
			case LAST:
				sb.append("$[");
				break;
		}
		return sb.append(getChild(0).toStringAST()).append("]").toString();
	}

}
