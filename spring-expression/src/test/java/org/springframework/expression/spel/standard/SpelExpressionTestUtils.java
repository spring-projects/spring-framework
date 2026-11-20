/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.standard;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.expression.Expression;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests utilities for {@link SpelExpression}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class SpelExpressionTestUtils {

	public static void assertIsCompiled(Expression expression) {
		try {
			Field field = SpelExpression.class.getDeclaredField("compiledAst");
			field.setAccessible(true);
			Object object = field.get(expression);
			assertThat(object).isNotNull();
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	public static void assertIsNotCompiled(Expression expression) {
		try {
			Field field = SpelExpression.class.getDeclaredField("compiledAst");
			field.setAccessible(true);
			Object object = field.get(expression);
			assertThat(object).isNull();
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	/**
	 * Return the current interpreted evaluation count for the given expression.
	 * <p>This counter is incremented exclusively by the interpreted evaluation path
	 * (inside {@code checkCompile()}), so it serves as a reliable witness for
	 * distinguishing interpreted from compiled evaluations in tests.
	 */
	public static int getInterpretedCount(Expression expression) {
		try {
			Field field = SpelExpression.class.getDeclaredField("interpretedCount");
			field.setAccessible(true);
			return ((AtomicInteger) field.get(expression)).get();
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

}
