/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.stream.IntStream;

import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the {@link SpelCompiler}.
 *
 * @author Sam Brannen
 * @since 5.1.14
 */
public class SpelCompilerTests {

	@Test // gh-24357
	public void expressionCompilesWhenMethodComesFromPublicInterface() {
		SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		SpelExpressionParser parser = new SpelExpressionParser(config);

		OrderedComponent component = new OrderedComponent();
		Expression expression = parser.parseExpression("order");

		// Evaluate the expression multiple times to ensure that it gets compiled.
		IntStream.rangeClosed(1, 5).forEach(i -> assertEquals(42, expression.getValue(component)));
	}


	static class OrderedComponent implements Ordered {

		@Override
		public int getOrder() {
			return 42;
		}
	}

}
