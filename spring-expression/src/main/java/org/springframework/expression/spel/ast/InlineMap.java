/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * Represent a map in an expression, for example, '{name:'foo',age:12}'.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author Harry Yang
 * @author Semyon Danilov
 * @since 4.1
 */
public class InlineMap extends SpelNodeImpl {

	private final @Nullable TypedValue constant;


	public InlineMap(int startPos, int endPos, SpelNodeImpl... args) {
		super(startPos, endPos, args);
		this.constant = computeConstantValue();
	}


	/**
	 * If all the components of the map are constants, or lists/maps that themselves
	 * contain constants, then a constant map can be built to represent this node.
	 * <p>This will speed up later getValue calls and reduce the amount of garbage
	 * created.
	 */
	private @Nullable TypedValue computeConstantValue() {
		for (int c = 0, max = getChildCount(); c < max; c++) {
			SpelNode child = getChild(c);
			if (!(child instanceof Literal)) {
				if (child instanceof InlineList inlineList) {
					if (!inlineList.isConstant()) {
						return null;
					}
				}
				else if (child instanceof InlineMap inlineMap) {
					if (!inlineMap.isConstant()) {
						return null;
					}
				}
				else if (!(c % 2 == 0 && child instanceof PropertyOrFieldReference)) {
					if (!(child instanceof OpMinus opMinus) || !opMinus.isNegativeNumberLiteral()) {
						return null;
					}
				}
			}
		}

		Map<Object, Object> constantMap = new LinkedHashMap<>();
		int childCount = getChildCount();
		ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());
		for (int c = 0; c < childCount; c++) {
			SpelNode keyChild = getChild(c++);
			Object key;
			if (keyChild instanceof Literal literal) {
				key = literal.getLiteralValue().getValue();
			}
			else if (keyChild instanceof PropertyOrFieldReference propertyOrFieldReference) {
				key = propertyOrFieldReference.getName();
			}
			else if (keyChild instanceof OpMinus) {
				key = keyChild.getValue(expressionState);
			}
			else {
				return null;
			}

			SpelNode valueChild = getChild(c);
			Object value = null;
			if (valueChild instanceof Literal literal) {
				value = literal.getLiteralValue().getValue();
			}
			else if (valueChild instanceof InlineList inlineList) {
				value = inlineList.getConstantValue();
			}
			else if (valueChild instanceof InlineMap inlineMap) {
				value = inlineMap.getConstantValue();
			}
			else if (valueChild instanceof OpMinus) {
				value = valueChild.getValue(expressionState);
			}
			constantMap.put(key, value);
		}
		return new TypedValue(Collections.unmodifiableMap(constantMap));
	}

	@Override
	public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
		if (this.constant != null) {
			return this.constant;
		}
		else {
			Map<Object, Object> returnValue = new LinkedHashMap<>();
			int childcount = getChildCount();
			for (int c = 0; c < childcount; c++) {
				SpelNode keyChild = getChild(c++);
				Object key = null;
				if (keyChild instanceof PropertyOrFieldReference reference) {
					key = reference.getName();
				}
				else {
					key = keyChild.getValue(expressionState);
				}
				Object value = getChild(c).getValue(expressionState);
				returnValue.put(key, value);
			}
			return new TypedValue(returnValue);
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("{");
		for (int c = 0; c < getChildCount(); c++) {
			if (c > 0) {
				sb.append(',');
			}
			sb.append(getChild(c++).toStringAST());
			sb.append(':');
			sb.append(getChild(c).toStringAST());
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Return whether this map is a constant value.
	 */
	public boolean isConstant() {
		return this.constant != null;
	}

	@SuppressWarnings("unchecked")
	public @Nullable Map<Object, Object> getConstantValue() {
		Assert.state(this.constant != null, "No constant");
		return (Map<Object, Object>) this.constant.getValue();
	}

}
