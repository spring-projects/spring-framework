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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.MapAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@BenchmarkMode(Mode.Throughput)
public class PropertyReadBenchmark {

	@State(Scope.Benchmark)
	public static class ExpressionState {

		@Param({"single", "multiple"})
		public String accessors;

		public StandardEvaluationContext context;

		public Expression expression;

		@Setup
		public void setUp() {
			this.context = new StandardEvaluationContext();
			if (this.accessors.equals("multiple")) {
				this.context.addPropertyAccessor(new MapAccessor());
			}
			this.expression = new SpelExpressionParser().parseExpression("name");
		}
	}


	@State(Scope.Thread)
	public static class TargetState {

		@Param({"same", "alternating"})
		public String targets;

		private final Object first = new FirstTarget();

		private final Object second = new SecondTarget();

		private boolean next;

		Object nextTarget() {
			if (this.targets.equals("same")) {
				return this.first;
			}
			this.next = !this.next;
			return (this.next ? this.first : this.second);
		}
	}


	@Benchmark
	public Object read(ExpressionState expressionState, TargetState targetState) {
		return expressionState.expression.getValue(expressionState.context, targetState.nextTarget());
	}


	public static class FirstTarget {

		public String getName() {
			return "first";
		}
	}


	public static class SecondTarget {

		public String getName() {
			return "second";
		}
	}
}
