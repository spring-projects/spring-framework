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
