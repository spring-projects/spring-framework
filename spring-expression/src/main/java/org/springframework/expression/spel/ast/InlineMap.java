/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.expression.spel.support.SimpleEvaluationContext;
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

	private final boolean isConstant;

	private volatile @Nullable TypedValue constant;


	public InlineMap(int startPos, int endPos, SpelNodeImpl... args) {
		super(startPos, endPos, args);
		this.isConstant = determineIfConstant();
	}


	/**
	 * Determine whether this map is structurally eligible to be a constant
	 * value: whether all of its components are themselves constants or lists/maps
	 * that contain only constants.
	 * <p>The actual constant value is created lazily on the first call to
	 * {@link #getValueInternal(ExpressionState)}.
	 */
	private boolean determineIfConstant() {
		int childCount = getChildCount();
		for (int c = 0; c < childCount; c++) {
			SpelNode child = getChild(c);
			if (child instanceof Literal) {
				continue;
			}
			if (child instanceof InlineList inlineList && inlineList.isConstant()) {
				continue;
			}
			if (child instanceof InlineMap inlineMap && inlineMap.isConstant()) {
				continue;
			}
			if (c % 2 == 0 && child instanceof PropertyOrFieldReference) {
				continue;
			}
			if (child instanceof OpMinus opMinus && opMinus.isNegativeNumberLiteral()) {
				continue;
			}
			return false;
		}
		return true;
	}

	@Override
	public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
		TypedValue result = this.constant;
		if (result != null) {
			return result;
		}
		result = createMap(expressionState);
		if (this.isConstant) {
			this.constant = result;
		}
		return result;
	}

	private TypedValue createMap(ExpressionState expressionState) throws EvaluationException {
		expressionState.trackOperation();
		Map<Object, Object> map = new LinkedHashMap<>();
		int childCount = getChildCount();
		for (int c = 0; c < childCount; c++) {
			SpelNode keyChild = getChild(c++);
			Object key;
			if (keyChild instanceof PropertyOrFieldReference reference) {
				key = reference.getName();
			}
			else {
				key = keyChild.getValue(expressionState);
			}
			Object value = getChild(c).getValue(expressionState);
			expressionState.trackOperation();
			map.put(key, value);
		}
		return new TypedValue(this.isConstant ? Collections.unmodifiableMap(map) : map);
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
	 * Return whether this map is structurally a constant value.
	 * <p>Note that the resulting constant value is created lazily on the
	 * first call to {@link #getValueInternal(ExpressionState)} or
	 * {@link #getConstantValue()}.
	 */
	public boolean isConstant() {
		return this.isConstant;
	}

	/**
	 * Return the cached constant {@link Map} value for this inline map,
	 * lazily creating it on first access.
	 * @see #isConstant()
	 * @deprecated as of Spring Framework 6.2.19; this method was only intended for
	 * testing purposes and will be removed in a future version of the framework
	 */
	@SuppressWarnings("unchecked")
	@Deprecated(since = "6.2.19")
	public @Nullable Map<Object, Object> getConstantValue() {
		Assert.state(this.isConstant, "Not a constant");
		TypedValue result = this.constant;
		if (result == null) {
			result = createMap(new ExpressionState(SimpleEvaluationContext.forReadOnlyDataBinding().build()));
			this.constant = result;
		}
		return (Map<Object, Object>) result.getValue();
	}

}
