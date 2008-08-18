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
	// TODO extract expected insert messages into constants (just in case of changes)?
	// TODO review poor messages, marked // POOR below

	public void testBrokenExpression01() {
		// will not fit into an int, needs L suffix
		parseAndCheckError("0xCAFEBABE", SpelMessages.NOT_AN_INTEGER);
		evaluate("0xCAFEBABEL", 0xCAFEBABEL, Long.class);
	}

	public void testBrokenExpression02() {
		// rogue 'G' on the end
		parseAndCheckError("0xB0BG", SpelMessages.PARSE_PROBLEM, 5, "mismatched input 'G' expecting EOF");
	}

	public void testBrokenExpression03() {
		// too many closing brackets
		parseAndCheckError("((3;4;)+(5;6;)))", SpelMessages.PARSE_PROBLEM, 15, "mismatched input ')' expecting EOF");
		evaluate("((3;4;)+(5;6;))", 10 /* 4+6 */, Integer.class);
	}

	public void testBrokenExpression04() {
		// missing right operand
		parseAndCheckError("true or ", SpelMessages.PARSE_PROBLEM, -1, "no viable alternative at input '<EOF>'"); // POOR
	}

	public void testBrokenExpression05() {
		// missing right operand
		parseAndCheckError("1 + ", SpelMessages.PARSE_PROBLEM, -1, "no viable alternative at input '<EOF>'"); // POOR
	}

	public void testBrokenExpression06() {
		// expression list missing surrounding parentheses
		parseAndCheckError("1;2;3", SpelMessages.PARSE_PROBLEM, 1, "mismatched input ';' expecting EOF"); // POOR
		evaluate("(1;2;3)", 3, Integer.class);
	}

	public void testBrokenExpression07() {
		// T() can only take an identifier (possibly qualified), not a literal
		// message ought to say identifier rather than ID
		parseAndCheckError("null is T('a')", SpelMessages.PARSE_PROBLEM, 10, "mismatched input ''a'' expecting ID"); // POOR
	}

	public void testExpressionLists02a() {
		// either missing semi or rogue 5. RPAREN should at least be ')', and why doesn't it give the other possibles?
		parseAndCheckError("( (3;4)5)", SpelMessages.PARSE_PROBLEM, 7, "mismatched input '5' expecting RPAREN"); // POOR
	}

}
