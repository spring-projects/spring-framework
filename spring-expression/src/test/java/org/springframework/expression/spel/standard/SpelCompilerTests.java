/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilationCoverageTests;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

/**
 * Tests for the {@link SpelCompiler}.
 *
 * @author Sam Brannen
 * @author Andy Clement
 * @since 5.1.14
 */
class SpelCompilerTests {

	@Test  // gh-24357
	void expressionCompilesWhenMethodComesFromPublicInterface() {
		SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		SpelExpressionParser parser = new SpelExpressionParser(config);

		OrderedComponent component = new OrderedComponent();
		Expression expression = parser.parseExpression("order");

		// Evaluate the expression multiple times to ensure that it gets compiled.
		IntStream.rangeClosed(1, 5).forEach(i -> assertThat(expression.getValue(component)).isEqualTo(42));
	}

	@Test  // gh-25706
	void defaultMethodInvocation() {
		SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		SpelExpressionParser parser = new SpelExpressionParser(config);

		StandardEvaluationContext context = new StandardEvaluationContext();
		Item item = new Item();
		context.setRootObject(item);

		Expression expression = parser.parseExpression("#root.isEditable2()");
		assertThat(SpelCompiler.compile(expression)).isFalse();
		assertThat(expression.getValue(context)).isEqualTo(false);
		assertThat(SpelCompiler.compile(expression)).isTrue();
		SpelCompilationCoverageTests.assertIsCompiled(expression);
		assertThat(expression.getValue(context)).isEqualTo(false);

		context.setVariable("user", new User());
		expression = parser.parseExpression("#root.isEditable(#user)");
		assertThat(SpelCompiler.compile(expression)).isFalse();
		assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
		assertThat(SpelCompiler.compile(expression)).isTrue();
		SpelCompilationCoverageTests.assertIsCompiled(expression);
		assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
	}

	@Test  // gh-28043
	void changingRegisteredVariableTypeDoesNotResultInFailureInMixedMode() {
		SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.MIXED, null);
		SpelExpressionParser parser = new SpelExpressionParser(config);
		Expression sharedExpression = parser.parseExpression("#bean.value");
		StandardEvaluationContext context = new StandardEvaluationContext();

		Object[] beans = new Object[] {new Bean1(), new Bean2(), new Bean3(), new Bean4()};

		IntStream.rangeClosed(1, 1_000_000).parallel().forEach(count -> {
			context.setVariable("bean", beans[count % 4]);
			assertThat(sharedExpression.getValue(context)).asString().startsWith("1");
		});
	}


	static class OrderedComponent implements Ordered {

		@Override
		public int getOrder() {
			return 42;
		}
	}


	public static class User {

		boolean isAdmin() {
			return true;
		}
	}


	public static class Item implements Editable {

		// some fields
		private String someField = "";

		// some getters and setters

		@Override
		public boolean hasSomeProperty() {
			return someField != null;
		}
	}


	public interface Editable {

		default boolean isEditable(User user) {
			return user.isAdmin() && hasSomeProperty();
		}

		default boolean isEditable2() {
			return false;
		}

		boolean hasSomeProperty();
	}

	public static class Bean1 {
		public String getValue() {
			return "11";
		}
	}

	public static class Bean2 {
		public Integer getValue() {
			return 111;
		}
	}

	public static class Bean3 {
		public Float getValue() {
			return 1.23f;
		}
	}

	public static class Bean4 {
		public Character getValue() {
			return '1';
		}
	}

}
