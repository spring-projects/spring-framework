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

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.springframework.expression.spel.standard.SpelExpressionTestUtils.assertIsCompiled;
import static org.springframework.expression.spel.standard.SpelExpressionTestUtils.assertIsNotCompiled;
import static org.springframework.expression.spel.standard.SpelExpressionTestUtils.getInterpretedCount;

/**
 * Tests for the {@link SpelCompiler}.
 *
 * @author Sam Brannen
 * @author Andy Clement
 * @since 5.1.14
 * @see org.springframework.expression.spel.SpelCompilationCoverageTests
 */
class SpelCompilerTests {

	@Test  // gh-24357
	void expressionCompilesWhenMethodComesFromPublicInterface() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);

		OrderedComponent component = new OrderedComponent();
		Expression expression = parser.parseExpression("order");

		// Evaluate the expression multiple times to ensure that it gets compiled.
		IntStream.rangeClosed(1, 5).forEach(i -> assertThat(expression.getValue(component)).isEqualTo(42));
		assertIsCompiled(expression);
	}

	@Test  // gh-25706
	void defaultMethodInvocation() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);

		StandardEvaluationContext context = new StandardEvaluationContext();
		Item item = new Item();
		context.setRootObject(item);

		Expression expression = parser.parseExpression("#root.isEditable2()");
		assertThat(SpelCompiler.compile(expression)).isFalse();
		assertThat(expression.getValue(context)).isEqualTo(false);
		assertThat(SpelCompiler.compile(expression)).isTrue();
		assertIsCompiled(expression);
		assertThat(expression.getValue(context)).isEqualTo(false);

		context.setVariable("user", new User());
		expression = parser.parseExpression("#root.isEditable(#user)");
		assertThat(SpelCompiler.compile(expression)).isFalse();
		assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
		assertThat(SpelCompiler.compile(expression)).isTrue();
		assertIsCompiled(expression);
		assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
	}

	@Test
	void simpleEvaluationContextBlocksCompilationByDefault() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);
		// "order" resides in the public Ordered interface and is therefore compilable,
		// so any non-compilation is attributable solely to the context's policy.
		Expression expression = parser.parseExpression("order");

		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		assertThat(context.isCompilationSupported()).isFalse();

		// Evaluate the expression multiple times to ensure that it stays in interpreted mode,
		// effectively overriding SpelCompilerMode.IMMEDIATE.
		OrderedComponent component = new OrderedComponent();
		IntStream.rangeClosed(1, 5).forEach(i -> assertThat(expression.getValue(context, component)).isEqualTo(42));
		assertIsNotCompiled(expression);
	}

	@Test
	void simpleEvaluationContextAllowsCompilationWhenSupported() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);
		// "order" resides in the public Ordered interface and is therefore compilable.
		Expression expression = parser.parseExpression("order");

		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withCompilationSupported()
				.build();
		assertThat(context.isCompilationSupported()).isTrue();

		// Two evaluations are enough for IMMEDIATE mode to compile.
		OrderedComponent component = new OrderedComponent();
		IntStream.rangeClosed(1, 2).forEach(i -> assertThat(expression.getValue(context, component)).isEqualTo(42));
		assertIsCompiled(expression);
	}

	@Test
	void simpleEvaluationContextIgnoresPrecompiledExpressionByDefault() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);
		// "order" resides in the public Ordered interface and is therefore compilable.
		Expression expression = parser.parseExpression("order");

		EvaluationContext standardContext = new StandardEvaluationContext();
		assertThat(standardContext.isCompilationSupported()).isTrue();
		OrderedComponent component = new OrderedComponent();
		IntStream.rangeClosed(1, 2).forEach(i -> assertThat(expression.getValue(standardContext, component)).isEqualTo(42));
		assertIsCompiled(expression);

		// Switch to a SimpleEvaluationContext without opting into compilation — should
		// fall back to interpreted evaluation even though compiledAst is non-null.
		EvaluationContext simpleContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		assertThat(simpleContext.isCompilationSupported()).isFalse();

		// Record interpretedCount before the simpleContext evaluation.
		// checkCompile() — which increments interpretedCount as its very first action —
		// is only reachable from the interpreted path. If the compiled path were taken
		// instead, interpretedCount would not change.
		int interpretedCountBefore = getInterpretedCount(expression);
		assertThat(expression.getValue(simpleContext, component)).isEqualTo(42);
		assertThat(getInterpretedCount(expression)).isEqualTo(interpretedCountBefore + 1);

		// compiledAst is still set: the compiled expression was not cleared, rather merely ignored.
		assertIsCompiled(expression);
	}

	/**
	 * Verify that the four implicit {@link EvaluationContext} {@code getValue()} variants
	 * in {@link SpelExpression} honor a {@link SimpleEvaluationContext} set as the default
	 * context: compilation must be blocked even under {@link SpelCompilerMode#IMMEDIATE}.
	 */
	@Test
	void simpleEvaluationContextSetAsDefaultBlocksCompilationForImplicitContextVariants() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);
		// "order" resides in the public Ordered interface and is therefore compilable,
		// so any non-compilation is attributable solely to the context's policy.
		SpelExpression expression = parser.parseRaw("order");

		OrderedComponent component = new OrderedComponent();
		SimpleEvaluationContext simpleContext = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withRootObject(component)
				.build();
		assertThat(simpleContext.isCompilationSupported()).isFalse();
		expression.setEvaluationContext(simpleContext);

		// Evaluate the expression multiple times using all four implicit context
		// variants to ensure that they stay in interpreted mode.
		for (int i = 0; i < 5; i++) {
			assertThat(expression.getValue()).isEqualTo(42);
			assertIsNotCompiled(expression);

			assertThat(expression.getValue(Integer.class)).isEqualTo(42);
			assertIsNotCompiled(expression);

			assertThat(expression.getValue(component)).isEqualTo(42);
			assertIsNotCompiled(expression);

			assertThat(expression.getValue(component, Integer.class)).isEqualTo(42);
			assertIsNotCompiled(expression);
		}
	}

	/**
	 * Verify that the four implicit {@link EvaluationContext} {@code getValue()} variants
	 * in {@link SpelExpression} ignore a previously compiled expression when the default
	 * context is a {@link SimpleEvaluationContext} (where {@code isCompilationSupported()}
	 * returns {@code false}).
	 */
	@Test
	void simpleEvaluationContextSetAsDefaultIgnoresPrecompiledExpressionForImplicitContextVariants() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.IMMEDIATE).build();
		SpelExpressionParser parser = new SpelExpressionParser(config);
		// "order" resides in the public Ordered interface and is therefore compilable.
		SpelExpression expression = parser.parseRaw("order");

		// Compile the expression via StandardEvaluationContext.
		StandardEvaluationContext standardContext = new StandardEvaluationContext();
		assertThat(standardContext.isCompilationSupported()).isTrue();
		OrderedComponent component = new OrderedComponent();
		IntStream.rangeClosed(1, 2).forEach(i ->
				assertThat(expression.getValue(standardContext, component, Integer.class)).isEqualTo(42));
		assertIsCompiled(expression);

		// Switch to a SimpleEvaluationContext set as the default context — the precompiled
		// expression should be ignored for all four implicit context getValue() variants.
		SimpleEvaluationContext simpleContext = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withRootObject(component)
				.build();
		assertThat(simpleContext.isCompilationSupported()).isFalse();
		expression.setEvaluationContext(simpleContext);

		// Record interpretedCount before the simpleContext evaluations.
		// checkCompile() — which increments interpretedCount as its very first action —
		// is only reachable from the interpreted path. If the compiled path were taken
		// instead, interpretedCount would not change.
		int interpretedCountBefore = getInterpretedCount(expression);
		assertThat(expression.getValue()).isEqualTo(42);
		assertThat(getInterpretedCount(expression)).isEqualTo(interpretedCountBefore + 1);

		interpretedCountBefore = getInterpretedCount(expression);
		assertThat(expression.getValue(Integer.class)).isEqualTo(42);
		assertThat(getInterpretedCount(expression)).isEqualTo(interpretedCountBefore + 1);

		interpretedCountBefore = getInterpretedCount(expression);
		assertThat(expression.getValue(component)).isEqualTo(42);
		assertThat(getInterpretedCount(expression)).isEqualTo(interpretedCountBefore + 1);

		interpretedCountBefore = getInterpretedCount(expression);
		assertThat(expression.getValue(component, Integer.class)).isEqualTo(42);
		assertThat(getInterpretedCount(expression)).isEqualTo(interpretedCountBefore + 1);

		// compiledAst is still set: the compiled expression was not cleared, rather merely ignored.
		assertIsCompiled(expression);
	}

	@Test  // gh-28043
	void changingRegisteredVariableTypeDoesNotResultInFailureInMixedMode() {
		SpelParserConfiguration config = SpelParserConfiguration.builder().compilerMode(SpelCompilerMode.MIXED).build();
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
