package org.springframework.expression.common;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;

/**
 * Represents a template expression broken into pieces. Each piece will be an Expression but pure text parts to the
 * template will be represented as LiteralExpression objects. An example of a template expression might be: <code><pre>
 * &quot;Hello ${getName()}&quot;
 * </pre></code> which will be represented as a CompositeStringExpression of two parts. The first part being a
 * LiteralExpression representing 'Hello ' and the second part being a real expression that will call getName() when
 * invoked.
 * 
 * @author Andy Clement
 */
public class CompositeStringExpression implements Expression {

	private final String expressionString;

	/**
	 * The array of expressions that make up the composite expression
	 */
	private final Expression[] expressions;

	public CompositeStringExpression(String expressionString, Expression[] expressions) {
		this.expressionString = expressionString;
		this.expressions = expressions;
	}

	public String getExpressionString() {
		return expressionString;
	}

	public String getValue() throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < expressions.length; i++) {
			// TODO is stringify ok for the non literal components? or should the converters be used? see another
			// case below
			sb.append(expressions[i].getValue());
		}
		return sb.toString();
	}

	public String getValue(EvaluationContext context) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < expressions.length; i++) {
			sb.append(expressions[i].getValue(context));
		}
		return sb.toString();
	}

	public Class getValueType(EvaluationContext context) throws EvaluationException {
		return String.class;
	}

	public Class getValueType() throws EvaluationException {
		return String.class;
	}

	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		throw new EvaluationException(expressionString, "Cannot call setValue() on a composite expression");
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
}
