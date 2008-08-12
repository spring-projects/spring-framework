/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel;

import junit.framework.TestCase;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;

/**
 * Tests the evaluation of real expressions in a real context.
 * 
 * @author Andy Clement
 */
@SuppressWarnings("unused")
public class PerformanceTests extends TestCase {

	public static final int ITERATIONS = 1000;
	public static final boolean report = true;

	private static SpelExpressionParser parser = new SpelExpressionParser();
	private static EvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();;

	public void testPerformanceOfSimpleAccess() throws Exception {
		long starttime = 0;
		long endtime = 0;

		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("getPlaceOfBirth().getCity()");
			if (expr == null)
				fail("Parser returned null for expression");
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;

		Expression expr = parser.parseExpression("getPlaceOfBirth().getCity()");
		if (expr == null)
			fail("Parser returned null for expression");
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;
		if (reuseTime > freshParseTime) {
			System.out.println("Fresh parse every time, ITERATIONS iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression, ITERATIONS iterations = " + reuseTime + "ms");
			fail("Should have been quicker to reuse!");
		}
	}

	/**
	 * Testing that using a resolver/executor split for constructor invocation (ie. just doing the reflection once to
	 * find the constructor then executing it over and over) is faster than redoing the reflection and execution every
	 * time.
	 * 
	 * MacBook speeds: 4-Aug-08 <br>
	 * Fresh parse every time, ITERATIONS iterations = 373ms <br>
	 * Reuse SpelExpression, ITERATIONS iterations = 1ms <br>
	 * Reuse SpelExpression (caching off), ITERATIONS iterations = 188ms <br>
	 */
	public void testConstructorResolverExecutorBenefit01() throws Exception {
		long starttime = 0;
		long endtime = 0;

		// warmup
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("new Integer(5)");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}

		// ITERATIONS calls, parsing fresh each time
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("new Integer(5)");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		Expression expr =  parser.parseExpression("new Integer(5)");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		try {
			ConstructorReference.useCaching = false;
			starttime = System.currentTimeMillis();
			for (int i = 0; i < ITERATIONS; i++) {
				Object value = expr.getValue(eContext);
			}
		} finally {
			ConstructorReference.useCaching = true;
		}
		endtime = System.currentTimeMillis();
		long cachingOffReuseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		expr =  parser.parseExpression("new Integer(5)");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;

		if (report) {
			System.out.println("Timings for constructor execution 'new Integer(5)'");
			System.out.println("Fresh parse every time, " + ITERATIONS + " iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression (caching off), " + ITERATIONS + " iterations = "
					+ cachingOffReuseTime + "ms");
			System.out.println("Reuse SpelExpression, " + ITERATIONS + " iterations = " + reuseTime + "ms");
		}
		if (reuseTime > freshParseTime) {
			fail("Should have been quicker to reuse a parsed expression!");
		}
		if (reuseTime > cachingOffReuseTime) {
			fail("Should have been quicker to reuse cached!");
		}
	}

	/**
	 * Testing that using a resolver/executor split for property access is faster than redoing the reflection and
	 * execution every time.
	 * 
	 * MacBook speeds: <br>
	 */
	public void testPropertyResolverExecutorBenefit_Reading() throws Exception {
		long starttime = 0;
		long endtime = 0;

		// warmup
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr =  parser.parseExpression("getPlaceOfBirth().city");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}

		// ITERATIONS calls, parsing fresh each time
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr =  parser.parseExpression("getPlaceOfBirth().city");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		Expression expr =  parser.parseExpression("getPlaceOfBirth().city");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		try {
			PropertyOrFieldReference.useCaching = false;
			starttime = System.currentTimeMillis();
			for (int i = 0; i < ITERATIONS; i++) {
				Object value = expr.getValue(eContext);
			}
		} finally {
			PropertyOrFieldReference.useCaching = true;
		}
		endtime = System.currentTimeMillis();
		long cachingOffReuseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		expr =  parser.parseExpression("getPlaceOfBirth().city");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;
		if (report) {
			System.out.println("Timings for property reader execution 'getPlaceOfBirth().city'");
			System.out.println("Fresh parse every time, " + ITERATIONS + " iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression (caching off), " + ITERATIONS + " iterations = "
					+ cachingOffReuseTime + "ms");
			System.out.println("Reuse SpelExpression, " + ITERATIONS + " iterations = " + reuseTime + "ms");
		}
		if (reuseTime > freshParseTime) {
			fail("Should have been quicker to reuse a parsed expression!");
		}
		if (reuseTime > cachingOffReuseTime) {
			fail("Should have been quicker to reuse cached!");
		}
	}

	/**
	 * Testing that using a resolver/executor split for property writing is faster than redoing the reflection and
	 * execution every time.
	 * 
	 * MacBook speeds: <br>
	 */
	public void testPropertyResolverExecutorBenefit_Writing() throws Exception {
		long starttime = 0;
		long endtime = 0;

		// warmup
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr =  parser.parseExpression("randomField='Andy'");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}

		// ITERATIONS calls, parsing fresh each time
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("randomField='Andy'");
			if (expr == null) {
				fail("Parser returned null for expression");
			}
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		Expression expr = parser.parseExpression("randomField='Andy'");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		try {
			PropertyOrFieldReference.useCaching = false;
			starttime = System.currentTimeMillis();
			for (int i = 0; i < ITERATIONS; i++) {
				Object value = expr.getValue(eContext);
			}
		} finally {
			PropertyOrFieldReference.useCaching = true;
		}
		endtime = System.currentTimeMillis();
		long cachingOffReuseTime = endtime - starttime;

		// ITERATIONS calls, parsing once and using cached executor
		expr = parser.parseExpression("randomField='Andy'");
		if (expr == null) {
			fail("Parser returned null for expression");
		}
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Object value = expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;
		if (report) {
			System.out.println("Timings for property writing execution 'randomField='Andy''");
			System.out.println("Fresh parse every time, " + ITERATIONS + " iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression (caching off), " + ITERATIONS + " iterations = "
					+ cachingOffReuseTime + "ms");
			System.out.println("Reuse SpelExpression, " + ITERATIONS + " iterations = " + reuseTime + "ms");
		}
		if (reuseTime > freshParseTime) {
			fail("Should have been quicker to reuse a parsed expression!");
		}
		if (reuseTime > cachingOffReuseTime) {
			fail("Should have been quicker to reuse cached!");
		}
	}

}
