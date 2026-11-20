/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;

/**
 * Tests the messages and exceptions that come out for badly formed expressions
 *
 * @author Andy Clement
 */
class ParserErrorMessagesTests extends AbstractExpressionTests {

	@Test
	void brokenExpression01() {
		// will not fit into an int, needs L suffix
		parseAndCheckError("0xCAFEBABE", SpelMessage.NOT_AN_INTEGER);
		evaluate("0xCAFEBABEL", 0xCAFEBABEL, Long.class);
		parseAndCheckError("0xCAFEBABECAFEBABEL", SpelMessage.NOT_A_LONG);
	}

	@Test
	void brokenExpression02() {
		// rogue 'G' on the end
		parseAndCheckError("0xB0BG", SpelMessage.MORE_INPUT, 5, "G");
	}

	@Test
	void brokenExpression04() {
		// missing right operand
		parseAndCheckError("true or ", SpelMessage.RIGHT_OPERAND_PROBLEM, 5);
	}

	@Test
	void brokenExpression05() {
		// missing right operand
		parseAndCheckError("1 + ", SpelMessage.RIGHT_OPERAND_PROBLEM, 2);
	}

	@Test
	void brokenExpression07() {
		// T() can only take an identifier (possibly qualified), not a literal
		// message ought to say identifier rather than ID
		parseAndCheckError("null instanceof T('a')", SpelMessage.NOT_EXPECTED_TOKEN, 18,
				"qualified ID","literal_string");
	}

}
