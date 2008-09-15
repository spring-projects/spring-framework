package org.springframework.expression.common;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;

/**
 * A very simple hardcoded implementation of the Expression interface that represents a string literal. It is used with
 * CompositeStringExpression when representing a template expression which is made up of pieces - some being real
 * expressions to be handled by an EL implementation like Spel, and some being just textual elements.
 * 
 * @author Andy Clement
 * 
 */
public class LiteralExpression implements Expression {

	/**
	 * Fixed literal value of this expression
	 */
	private final String literalValue;

	public LiteralExpression(String literalValue) {
		this.literalValue = literalValue;
	}

	public String getExpressionString() {
		return literalValue;
		// return new StringBuilder().append("'").append(literalValue).append("'").toString();
	}

	public String getValue() throws EvaluationException {
		return literalValue;
	}

	public String getValue(EvaluationContext context) throws EvaluationException {
		return literalValue;
	}

	public Class getValueType(EvaluationContext context) throws EvaluationException {
		return String.class;
	}

	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		throw new EvaluationException(literalValue, "Cannot call setValue() on a LiteralExpression");
	}

	public Object getValue(EvaluationContext context, Class<?> expectedResultType) throws EvaluationException {
		Object value = getValue(context);
		return ExpressionUtils.convert(context, value, expectedResultType);
	}

	public Object getValue(Class<?> expectedResultType) throws EvaluationException {
		Object value = getValue();
		return ExpressionUtils.convert(null, value, expectedResultType);
	}

	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		return false;
	}

	public Class getValueType() throws EvaluationException {
		return String.class;
	}

}
