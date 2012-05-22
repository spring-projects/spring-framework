/*
 * Copyright 2002-2012 the original author or authors.
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

import junit.framework.Assert;
import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

///CLOVER:OFF

/**
 * Tests the evaluation of real expressions in a real context.
 *
 * @author Andy Clement
 */
public class PerformanceTests {

	public static final int ITERATIONS = 10000;
	public static final boolean report = true;

	private static ExpressionParser parser = new SpelExpressionParser();
	private static EvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();

	private static final boolean DEBUG = false;

	@Test
	public void testPerformanceOfPropertyAccess() throws Exception {
		long starttime = 0;
		long endtime = 0;

		// warmup
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("placeOfBirth.city");
			if (expr == null) {
				Assert.fail("Parser returned null for expression");
			}
			expr.getValue(eContext);
		}

		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("placeOfBirth.city");
			if (expr == null) {
				Assert.fail("Parser returned null for expression");
			}
			expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;
		if (DEBUG) {
			System.out.println("PropertyAccess: Time for parsing and evaluation x 10000: "+freshParseTime+"ms");
		}

		Expression expr = parser.parseExpression("placeOfBirth.city");
		if (expr == null) {
			Assert.fail("Parser returned null for expression");
		}
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;
		if (DEBUG) {
			System.out.println("PropertyAccess: Time for just evaluation x 10000: "+reuseTime+"ms");
		}
		if (reuseTime > freshParseTime) {
			System.out.println("Fresh parse every time, ITERATIONS iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression, ITERATIONS iterations = " + reuseTime + "ms");
			Assert.fail("Should have been quicker to reuse!");
		}
	}

	public void testPerformanceOfMethodAccess() throws Exception {
		long starttime = 0;
		long endtime = 0;

		// warmup
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("getPlaceOfBirth().getCity()");
			if (expr == null) {
				Assert.fail("Parser returned null for expression");
			}
			expr.getValue(eContext);
		}

		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			Expression expr = parser.parseExpression("getPlaceOfBirth().getCity()");
			if (expr == null) {
				Assert.fail("Parser returned null for expression");
			}
			expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long freshParseTime = endtime - starttime;
		if (DEBUG) {
			System.out.println("MethodExpression: Time for parsing and evaluation x 10000: "+freshParseTime+"ms");
		}

		Expression expr = parser.parseExpression("getPlaceOfBirth().getCity()");
		if (expr == null) {
			Assert.fail("Parser returned null for expression");
		}
		starttime = System.currentTimeMillis();
		for (int i = 0; i < ITERATIONS; i++) {
			expr.getValue(eContext);
		}
		endtime = System.currentTimeMillis();
		long reuseTime = endtime - starttime;
		if (DEBUG) {
			System.out.println("MethodExpression: Time for just evaluation x 10000: "+reuseTime+"ms");
		}

		if (reuseTime > freshParseTime) {
			System.out.println("Fresh parse every time, ITERATIONS iterations = " + freshParseTime + "ms");
			System.out.println("Reuse SpelExpression, ITERATIONS iterations = " + reuseTime + "ms");
			Assert.fail("Should have been quicker to reuse!");
		}
	}

}
