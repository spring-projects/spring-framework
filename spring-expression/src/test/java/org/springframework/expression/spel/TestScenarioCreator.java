/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.GregorianCalendar;

import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;
import org.springframework.expression.spel.testresources.PlaceOfBirth;

/**
 * Builds an evaluation context for test expressions.
 * Features of the test evaluation context are:
 * <ul>
 * <li>The root context object is an Inventor instance {@link Inventor}
 * </ul>
 */
class TestScenarioCreator {

	public static StandardEvaluationContext getTestEvaluationContext() {
		StandardEvaluationContext testContext = new StandardEvaluationContext();
		setupRootContextObject(testContext);
		populateVariables(testContext);
		populateFunctions(testContext);
		return testContext;
	}

	/**
	 * Register some Java reflect methods as well known functions that can be called from an expression.
	 * @param testContext the test evaluation context
	 */
	private static void populateFunctions(StandardEvaluationContext testContext) {
		try {
			testContext.registerFunction("isEven",
					TestScenarioCreator.class.getDeclaredMethod("isEven", Integer.TYPE));
			testContext.registerFunction("reverseInt",
					TestScenarioCreator.class.getDeclaredMethod("reverseInt", Integer.TYPE, Integer.TYPE, Integer.TYPE));
			testContext.registerFunction("reverseString",
					TestScenarioCreator.class.getDeclaredMethod("reverseString", String.class));
			testContext.registerFunction("varargsFunction",
					TestScenarioCreator.class.getDeclaredMethod("varargsFunction", String[].class));
			testContext.registerFunction("varargsFunction2",
					TestScenarioCreator.class.getDeclaredMethod("varargsFunction2", Integer.TYPE, String[].class));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Register some variables that can be referenced from the tests
	 * @param testContext the test evaluation context
	 */
	private static void populateVariables(StandardEvaluationContext testContext) {
		testContext.setVariable("answer", 42);
	}

	/**
	 * Create the root context object, an Inventor instance. Non-qualified property
	 * and method references will be resolved against this context object.
	 * @param testContext the evaluation context in which to set the root object
	 */
	private static void setupRootContextObject(StandardEvaluationContext testContext) {
		GregorianCalendar c = new GregorianCalendar();
		c.set(1856, 7, 9);
		Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");
		tesla.setPlaceOfBirth(new PlaceOfBirth("SmilJan"));
		tesla.setInventions(new String[] { "Telephone repeater", "Rotating magnetic field principle",
				"Polyphase alternating-current system", "Induction motor", "Alternating-current power transmission",
				"Tesla coil transformer", "Wireless communication", "Radio", "Fluorescent lights" });
		testContext.setRootObject(tesla);
	}


	// These methods are registered in the test context and therefore accessible through function calls
	// in test expressions

	public static String isEven(int i) {
		if ((i % 2) == 0) {
			return "y";
		}
		return "n";
	}

	public static int[] reverseInt(int i, int j, int k) {
		return new int[] { k, j, i };
	}

	public static String reverseString(String input) {
		StringBuilder backwards = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			backwards.append(input.charAt(input.length() - 1 - i));
		}
		return backwards.toString();
	}

	public static String varargsFunction(String... strings) {
		return Arrays.toString(strings);
	}

	public static String varargsFunction2(int i, String... strings) {
		return String.valueOf(i) + "-" + Arrays.toString(strings);
	}

}
