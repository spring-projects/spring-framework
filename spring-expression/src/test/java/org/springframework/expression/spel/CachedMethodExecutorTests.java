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

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for caching in {@link MethodReference} (SPR-10657).
 *
 * @author Oliver Becker
 */
class CachedMethodExecutorTests {

	private final ExpressionParser parser = new SpelExpressionParser();

	private final StandardEvaluationContext context = new StandardEvaluationContext(new RootObject());


	@Test
	void testCachedExecutionForParameters() {
		Expression expression = this.parser.parseExpression("echo(#var)");

		assertMethodExecution(expression, 42, "int: 42");
		assertMethodExecution(expression, 42, "int: 42");
		assertMethodExecution(expression, "Deep Thought", "String: Deep Thought");
		assertMethodExecution(expression, 42, "int: 42");
	}

	@Test
	void testCachedExecutionForTarget() {
		Expression expression = this.parser.parseExpression("#var.echo(42)");

		assertMethodExecution(expression, new RootObject(), "int: 42");
		assertMethodExecution(expression, new RootObject(), "int: 42");
		assertMethodExecution(expression, new BaseObject(), "String: 42");
		assertMethodExecution(expression, new RootObject(), "int: 42");
	}

	private void assertMethodExecution(Expression expression, Object var, String expected) {
		this.context.setVariable("var", var);
		assertThat(expression.getValue(this.context)).isEqualTo(expected);
	}


	public static class BaseObject {

		public String echo(String value) {
			return "String: " + value;
		}
	}

	public static class RootObject extends BaseObject {

		public String echo(int value) {
			return "int: " + value;
		}
	}

}
