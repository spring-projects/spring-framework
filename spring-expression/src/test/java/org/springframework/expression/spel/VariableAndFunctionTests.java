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

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the evaluation of expressions that access variables and functions (lambda/java).
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
public class VariableAndFunctionTests extends AbstractExpressionTests {

	@Test
	public void testVariableAccess01() {
		evaluate("#answer", "42", Integer.class, SHOULD_BE_WRITABLE);
		evaluate("#answer / 2", 21, Integer.class, SHOULD_NOT_BE_WRITABLE);
	}

	@Test
	public void testVariableAccess_WellKnownVariables() {
		evaluate("#this.getName()","Nikola Tesla",String.class);
		evaluate("#root.getName()","Nikola Tesla",String.class);
	}

	@Test
	public void testFunctionAccess01() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
		evaluate("#reverseInt('1',2,3)", "int[3]{3,2,1}", int[].class); // requires type conversion of '1' to 1
		evaluateAndCheckError("#reverseInt(1)", SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, 1, 3);
	}

	@Test
	public void testFunctionAccess02() {
		evaluate("#reverseString('hello')", "olleh", String.class);
		evaluate("#reverseString(37)", "73", String.class); // requires type conversion of 37 to '37'
	}

	@Test
	public void testCallVarargsFunction() {
		evaluate("#varargsFunction()", "[]", String.class);
		evaluate("#varargsFunction(new String[0])", "[]", String.class);
		evaluate("#varargsFunction('a')", "[a]", String.class);
		evaluate("#varargsFunction('a','b','c')", "[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsFunction(25)", "[25]", String.class);
		evaluate("#varargsFunction('b',25)", "[b, 25]", String.class);
		// Strings that contain a comma
		evaluate("#varargsFunction('a,b')", "[a,b]", String.class);
		evaluate("#varargsFunction('a', 'x,y', 'd')", "[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsFunction(null)", "[null]", String.class);
		evaluate("#varargsFunction('a',null,'b')", "[a, null, b]", String.class);

		evaluate("#varargsFunction2(9)", "9-[]", String.class);
		evaluate("#varargsFunction2(9, new String[0])", "9-[]", String.class);
		evaluate("#varargsFunction2(9,'a')", "9-[a]", String.class);
		evaluate("#varargsFunction2(9,'a','b','c')", "9-[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsFunction2(9,25)", "9-[25]", String.class);
		evaluate("#varargsFunction2(9,'b',25)", "9-[b, 25]", String.class);
		// Strings that contain a comma:
		evaluate("#varargsFunction2(9, 'a,b')", "9-[a,b]", String.class);
		evaluate("#varargsFunction2(9, 'a', 'x,y', 'd')", "9-[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsFunction2(9,null)", "9-[null]", String.class);
		evaluate("#varargsFunction2(9,'a',null,'b')", "9-[a, null, b]", String.class);
	}

	@Test
	public void testCallingIllegalFunctions() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("notStatic", this.getClass().getMethod("nonStatic"));
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
				parser.parseRaw("#notStatic()").getValue(ctx)).
			satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.FUNCTION_MUST_BE_STATIC));
	}


	// this method is used by the test above
	public void nonStatic() {
	}

}
