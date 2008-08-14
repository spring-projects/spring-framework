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

/**
 * Tests the messages and exceptions that come out for badly formed expressions
 * 
 * @author Andy Clement
 */
public class ParserErrorMessagesTests extends ExpressionTestCase {

	public void testBrokenExpression01() {
		// Expression: 0xCAFEBABE - too big to be processed as an int, needs the L suffix
		parseAndCheckError("0xCAFEBABE", SpelMessages.NOT_AN_INTEGER);
	}

	// parseCheck("true or ");
	// parseCheck("tru or false");
	// parseCheck("1 + ");
	// parseCheck("0xCAFEBABEG");
	// TODO 3 too many close brackets - parser recover
	// public void testExpressionLists07a() { parseCheck("((3;4;)+(5;6;)))","((3;4)
	// + (5;6))");}
	// }
	// ---
}
