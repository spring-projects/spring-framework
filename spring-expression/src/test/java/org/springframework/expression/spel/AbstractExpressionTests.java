/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import java.util.Arrays;
import java.util.List;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Common superclass for expression tests.
 *
 * @author Andy Clement
 */
public abstract class AbstractExpressionTests {

	protected static final boolean DEBUG = false;

	protected static final boolean SHOULD_BE_WRITABLE = true;

	protected static final boolean SHOULD_NOT_BE_WRITABLE = false;


	protected final SpelExpressionParser parser = new SpelExpressionParser();

	protected final StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();


	/**
	 * Evaluate an expression and check that the actual result matches the
	 * expectedValue and the class of the result matches the expectedResultType.
	 * @param expression the expression to evaluate
	 * @param expectedValue the expected result for evaluating the expression
	 * @param expectedResultType the expected class of the evaluation result
	 */
	public void evaluate(String expression, Object expectedValue, Class<?> expectedResultType) {
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}

		Object value = expr.getValue(context);

		// Check the return value
		if (value == null) {
			if (expectedValue == null) {
				return;  // no point doing other checks
			}
			assertThat(expectedValue).as("Expression returned null value, but expected '" + expectedValue + "'").isNull();
		}

		Class<?> resultType = value.getClass();
		assertThat(resultType).as("Type of the actual result was not as expected.  Expected '" + expectedResultType +
				"' but result was of type '" + resultType + "'").isEqualTo(expectedResultType);

		if (expectedValue instanceof String) {
			assertThat(AbstractExpressionTests.stringValueOf(value)).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		}
		else {
			assertThat(value).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		}
	}

	public void evaluateAndAskForReturnType(String expression, Object expectedValue, Class<?> expectedResultType) {
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}

		Object value = expr.getValue(context, expectedResultType);
		if (value == null) {
			if (expectedValue == null) {
				return;  // no point doing other checks
			}
			assertThat(expectedValue).as("Expression returned null value, but expected '" + expectedValue + "'").isNull();
		}

		Class<?> resultType = value.getClass();
		assertThat(resultType).as("Type of the actual result was not as expected.  Expected '" + expectedResultType +
				"' but result was of type '" + resultType + "'").isEqualTo(expectedResultType);
		assertThat(value).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
	}

	/**
	 * Evaluate an expression and check that the actual result matches the
	 * expectedValue and the class of the result matches the expectedResultType.
	 * This method can also check if the expression is writable (for example,
	 * it is a variable or property reference).
	 * @param expression the expression to evaluate
	 * @param expectedValue the expected result for evaluating the expression
	 * @param expectedResultType the expected class of the evaluation result
	 * @param shouldBeWritable should the parsed expression be writable?
	 */
	public void evaluate(String expression, Object expectedValue, Class<?> expectedResultType, boolean shouldBeWritable) {
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}
		Object value = expr.getValue(context);
		if (value == null) {
			if (expectedValue == null) {
				return;  // no point doing other checks
			}
			assertThat(expectedValue).as("Expression returned null value, but expected '" + expectedValue + "'").isNull();
		}
		Class<?> resultType = value.getClass();
		if (expectedValue instanceof String) {
			assertThat(AbstractExpressionTests.stringValueOf(value)).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		}
		else {
			assertThat(value).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		}
		assertThat(resultType).as("Type of the result was not as expected.  Expected '" + expectedResultType +
				"' but result was of type '" + resultType + "'").isEqualTo(expectedResultType);

		assertThat(expr.isWritable(context)).as("isWritable").isEqualTo(shouldBeWritable);
	}

	/**
	 * Evaluate the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param expression the expression to evaluate
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void evaluateAndCheckError(String expression, SpelMessage expectedMessage, Object... otherProperties) {
		evaluateAndCheckError(expression, null, expectedMessage, otherProperties);
	}

	/**
	 * Evaluate the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param expression the expression to evaluate
	 * @param expectedReturnType ask the expression return value to be of this type if possible
	 * ({@code null} indicates don't ask for conversion)
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void evaluateAndCheckError(String expression, Class<?> expectedReturnType, SpelMessage expectedMessage,
			Object... otherProperties) {

		evaluateAndCheckError(this.parser, expression, expectedReturnType, expectedMessage, otherProperties);
	}

	/**
	 * Evaluate the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param evaluationContext the evaluation context to use
	 * @param expression the expression to evaluate
	 * @param expectedReturnType ask the expression return value to be of this type if possible
	 * ({@code null} indicates don't ask for conversion)
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void evaluateAndCheckError(EvaluationContext evaluationContext, String expression,
			Class<?> expectedReturnType, SpelMessage expectedMessage, Object... otherProperties) {

		evaluateAndCheckError(this.parser, evaluationContext, expression, expectedReturnType, expectedMessage, otherProperties);
	}

	/**
	 * Evaluate the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param parser the expression parser to use
	 * @param expression the expression to evaluate
	 * @param expectedReturnType ask the expression return value to be of this type if possible
	 * ({@code null} indicates don't ask for conversion)
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void evaluateAndCheckError(ExpressionParser parser, String expression, Class<?> expectedReturnType, SpelMessage expectedMessage,
			Object... otherProperties) {

		evaluateAndCheckError(parser, this.context, expression, expectedReturnType, expectedMessage, otherProperties);
	}

	/**
	 * Evaluate the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param parser the expression parser to use
	 * @param evaluationContext the evaluation context to use
	 * @param expression the expression to evaluate
	 * @param expectedReturnType ask the expression return value to be of this type if possible
	 * ({@code null} indicates don't ask for conversion)
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void evaluateAndCheckError(ExpressionParser parser, EvaluationContext evaluationContext,
			String expression, Class<?> expectedReturnType, SpelMessage expectedMessage, Object... otherProperties) {

		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() -> {
			Expression expr = parser.parseExpression(expression);
			assertThat(expr).as("expression").isNotNull();
			if (expectedReturnType != null) {
				expr.getValue(evaluationContext, expectedReturnType);
			}
			else {
				expr.getValue(evaluationContext);
			}
		}).satisfies(ex -> {
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			if (!ObjectUtils.isEmpty(otherProperties)) {
				// first one is expected position of the error within the string
				int pos = (Integer) otherProperties[0];
				assertThat(ex.getPosition()).as("position").isEqualTo(pos);
				if (otherProperties.length > 1) {
					// Check inserts match
					Object[] inserts = ex.getInserts();
					assertThat(inserts).as("inserts").hasSizeGreaterThanOrEqualTo(otherProperties.length - 1);
					Object[] expectedInserts = new Object[inserts.length];
					System.arraycopy(otherProperties, 1, expectedInserts, 0, expectedInserts.length);
					assertThat(inserts).as("inserts").containsExactly(expectedInserts);
				}
			}
		});
	}

	/**
	 * Evaluate the specified expression against the supplied root object and
	 * verify that the evaluation performs exactly the expected number of tracked
	 * operations.
	 * <p>The expression is first evaluated with a maximum number of operations
	 * of {@code expectedOperations + 1}, which must succeed, and then with a
	 * maximum of {@code expectedOperations}, which must fail.
	 * @param expression the expression to evaluate
	 * @param rootObject the root object against which to evaluate the expression
	 * @param expectedOperations the expected number of tracked operations
	 */
	protected void assertMaxOperations(String expression, Object rootObject, int expectedOperations) {
		EvaluationContext evaluationContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();

		Expression expr1 = createParser(expectedOperations + 1).parseExpression(expression);
		expr1.getValue(evaluationContext, rootObject);

		Expression expr2 = createParser(expectedOperations).parseExpression(expression);
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expr2.getValue(evaluationContext, rootObject))
				.satisfies(ex -> {
					assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.MAX_OPERATIONS_EXCEEDED);
					assertThat(ex.getInserts()).as("inserts").containsExactly(expectedOperations);
				});
	}

	private static ExpressionParser createParser(int maxOperations) {
		return new SpelExpressionParser(SpelParserConfiguration.builder()
				.compilerMode(SpelCompilerMode.OFF)
				.maximumOperations(maxOperations)
				.build());
	}

	/**
	 * Parse the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 * @param expression the expression to evaluate
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void parseAndCheckError(String expression, SpelMessage expectedMessage, Object... otherProperties) {
		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			Expression expr = parser.parseExpression(expression);
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, expr);
			}
		}).satisfies(ex -> {
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			if (otherProperties != null && otherProperties.length != 0) {
				// first one is expected position of the error within the string
				int pos = (Integer) otherProperties[0];
				assertThat(ex.getPosition()).as("reported position").isEqualTo(pos);
				if (otherProperties.length > 1) {
					// Check inserts match
					Object[] inserts = ex.getInserts();
					assertThat(inserts).as("inserts").hasSizeGreaterThanOrEqualTo(otherProperties.length - 1);
					Object[] expectedInserts = new Object[inserts.length];
					System.arraycopy(otherProperties, 1, expectedInserts, 0, expectedInserts.length);
					assertThat(inserts).as("inserts").containsExactly(expectedInserts);
				}
			}
		});
	}


	protected static String stringValueOf(Object value) {
		return stringValueOf(value, false);
	}

	/**
	 * Produce a nice string representation of the input object.
	 * @param value object to be formatted
	 * @return a nice string
	 */
	protected static String stringValueOf(Object value, boolean isNested) {
		// do something nice for arrays
		if (value == null) {
			return "null";
		}
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			if (value.getClass().componentType().isPrimitive()) {
				Class<?> primitiveType = value.getClass().componentType();
				if (primitiveType == int.class) {
					int[] l = (int[]) value;
					sb.append("int[").append(l.length).append("]{");
					for (int j = 0; j < l.length; j++) {
						if (j > 0) {
							sb.append(',');
						}
						sb.append(stringValueOf(l[j]));
					}
					sb.append('}');
				}
				else if (primitiveType == long.class) {
					long[] l = (long[]) value;
					sb.append("long[").append(l.length).append("]{");
					for (int j = 0; j < l.length; j++) {
						if (j > 0) {
							sb.append(',');
						}
						sb.append(stringValueOf(l[j]));
					}
					sb.append('}');
				}
				else {
					throw new RuntimeException("Please implement support for type " + primitiveType.getName() +
							" in ExpressionTestCase.stringValueOf()");
				}
			}
			else if (value.getClass().componentType().isArray()) {
				List<Object> l = Arrays.asList((Object[]) value);
				if (!isNested) {
					sb.append(value.getClass().componentType().getName());
				}
				sb.append('[').append(l.size()).append("]{");
				int i = 0;
				for (Object object : l) {
					if (i > 0) {
						sb.append(',');
					}
					i++;
					sb.append(stringValueOf(object, true));
				}
				sb.append('}');
			}
			else {
				List<Object> l = Arrays.asList((Object[]) value);
				if (!isNested) {
					sb.append(value.getClass().componentType().getName());
				}
				sb.append('[').append(l.size()).append("]{");
				int i = 0;
				for (Object object : l) {
					if (i > 0) {
						sb.append(',');
					}
					i++;
					sb.append(stringValueOf(object));
				}
				sb.append('}');
			}
			return sb.toString();
		}
		else {
			return value.toString();
		}
	}

}
