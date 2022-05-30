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

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test construction of arrays.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class ArrayConstructorTests extends AbstractExpressionTests {

	@Test
	void conversion() {
		evaluate("new String[]{1,2,3}[0]", "1", String.class);
		evaluate("new int[]{'123'}[0]", 123, Integer.class);
	}

	@Test
	void primitiveTypeArrayConstructors() {
		evaluateArrayBuildingExpression("new int[]{}", "{}");
		evaluateArrayBuildingExpression("new int[]{1,2,3,4}", "{1, 2, 3, 4}");
		evaluateArrayBuildingExpression("new boolean[]{true,false,true}", "{true, false, true}");
		evaluateArrayBuildingExpression("new char[]{'a','b','c'}", "{'a', 'b', 'c'}");
		evaluateArrayBuildingExpression("new long[]{1,2,3,4,5}", "{1, 2, 3, 4, 5}");
		evaluateArrayBuildingExpression("new short[]{2,3,4,5,6}", "{2, 3, 4, 5, 6}");
		evaluateArrayBuildingExpression("new double[]{1d,2d,3d,4d}", "{1.0, 2.0, 3.0, 4.0}");
		evaluateArrayBuildingExpression("new float[]{1f,2f,3f,4f}", "{1.0, 2.0, 3.0, 4.0}");
		evaluateArrayBuildingExpression("new byte[]{1,2,3,4}", "{1, 2, 3, 4}");

		evaluate("new int[]{}.length", "0", Integer.class);
	}

	@Test
	void primitiveTypeArrayConstructorsElements() {
		evaluate("new int[]{1,2,3,4}[0]", 1, Integer.class);
		evaluate("new boolean[]{true,false,true}[0]", true, Boolean.class);
		evaluate("new char[]{'a','b','c'}[0]", 'a', Character.class);
		evaluate("new long[]{1,2,3,4,5}[0]", 1L, Long.class);
		evaluate("new short[]{2,3,4,5,6}[0]", (short) 2, Short.class);
		evaluate("new double[]{1d,2d,3d,4d}[0]", (double) 1, Double.class);
		evaluate("new float[]{1f,2f,3f,4f}[0]", (float) 1, Float.class);
		evaluate("new byte[]{1,2,3,4}[0]", (byte) 1, Byte.class);
		evaluate("new String(new char[]{'h','e','l','l','o'})", "hello", String.class);
	}

	@Test
	void errorCases() {
		evaluateAndCheckError("new int[]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new String[]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new int[3][]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new int[][1]", SpelMessage.MISSING_ARRAY_DIMENSION);

		evaluateAndCheckError("new char[7]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);
		evaluateAndCheckError("new char[3]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);

		evaluateAndCheckError("new int[][]{{1,2},{3,4}}", SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);

		evaluateAndCheckError("new char[2]{'hello','world'}", SpelMessage.TYPE_CONVERSION_ERROR);
		// Could conceivably be a SpelMessage.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, but it appears
		// that SpelMessage.INCORRECT_ELEMENT_TYPE_FOR_ARRAY is not actually (no longer?) used
		// in the code base.
		evaluateAndCheckError("new Integer[3]{'3','ghi','5'}", SpelMessage.TYPE_CONVERSION_ERROR);

		evaluateAndCheckError("new String('a','c','d')", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);
		// Root cause: java.lang.OutOfMemoryError: Requested array size exceeds VM limit
		evaluateAndCheckError("new java.util.ArrayList(T(java.lang.Integer).MAX_VALUE)", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);

		int threshold = 256 * 1024; // ConstructorReference.MAX_ARRAY_ELEMENTS
		evaluateAndCheckError("new int[T(java.lang.Integer).MAX_VALUE]", SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, 0, threshold);
		evaluateAndCheckError("new int[1024 * 1024][1024 * 1024]", SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, 0, threshold);
	}

	@Test
	void typeArrayConstructors() {
		evaluate("new String[]{'a','b','c','d'}[1]", "b", String.class);
		evaluateAndCheckError("new String[]{'a','b','c','d'}.size()", SpelMessage.METHOD_NOT_FOUND, 30, "size()",
			"java.lang.String[]");
		evaluate("new String[]{'a','b','c','d'}.length", 4, Integer.class);
	}

	@Test
	void basicArray() {
		evaluate("new String[3]", "java.lang.String[3]{null,null,null}", String[].class);
	}

	@Test
	void multiDimensionalArrays() {
		evaluate("new String[2][2]", "[Ljava.lang.String;[2]{[2]{null,null},[2]{null,null}}", String[][].class);
		evaluate("new String[3][2][1]",
			"[[Ljava.lang.String;[3]{[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}}}",
			String[][][].class);
	}

	private void evaluateArrayBuildingExpression(String expression, String expectedToString) {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression e = parser.parseExpression(expression);
		Object array = e.getValue();
		assertThat(array).isNotNull();
		assertThat(array.getClass().isArray()).isTrue();
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo(expectedToString);
	}

}
