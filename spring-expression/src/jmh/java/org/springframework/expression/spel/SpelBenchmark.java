/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Benchmarks for parsing and executing SpEL expressions.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class SpelBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkData {

		public ExpressionParser parser = new SpelExpressionParser();

		public EvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();

	}

	@Benchmark
	public Object propertyAccessParseAndExecution(BenchmarkData data) {
		Expression expr = data.parser.parseExpression("placeOfBirth.city");
		return expr.getValue(data.eContext);
	}

	@Benchmark
	public Object methodAccessParseAndExecution(BenchmarkData data) {
		Expression expr = data.parser.parseExpression("getPlaceOfBirth().getCity()");
		return expr.getValue(data.eContext);
	}

	@State(Scope.Benchmark)
	public static class CachingBenchmarkData extends BenchmarkData {

		public Expression propertyExpression;

		public Expression methodExpression;

		public CachingBenchmarkData() {
			this.propertyExpression = this.parser.parseExpression("placeOfBirth.city");
			this.methodExpression = this.parser.parseExpression("getPlaceOfBirth().getCity()");
		}
	}

	@Benchmark
	public Object cachingPropertyAccessParseAndExecution(CachingBenchmarkData data) {
		return data.propertyExpression.getValue(data.eContext);
	}

	@Benchmark
	public Object cachingMethodAccessParseAndExecution(CachingBenchmarkData data) {
		return data.methodExpression.getValue(data.eContext);
	}

	@State(Scope.Benchmark)
	public static class ValueBenchmarkData {

		public EvaluationContext context;

		public Expression expression;

		public ValueBenchmarkData() {
			Map<String, String> map = new HashMap<>();
			map.put("key", "value");
			this.context = new StandardEvaluationContext(map);
			ExpressionParser spelExpressionParser = new SpelExpressionParser();
			this.expression = spelExpressionParser.parseExpression("#root['key']");
		}
	}

	@Benchmark
	public Object getValueFromMap(ValueBenchmarkData data) {
		return data.expression.getValue(data.context);
	}

}
