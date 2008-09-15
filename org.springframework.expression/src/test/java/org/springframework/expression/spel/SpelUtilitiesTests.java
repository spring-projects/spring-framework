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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 * Tests the SpelUtilities
 * 
 * @author Andy Clement
 */
public class SpelUtilitiesTests extends TestCase {

	public void testPrintAbstractSyntaxTree01() throws Exception {
		SpelExpression sEx = new SpelExpressionParser().parseExpression("1 + 2");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SpelUtilities.printAbstractSyntaxTree(new PrintStream(baos), sEx);
		String theAst = baos.toString();
		System.out.println(theAst);
		String[] expectedLines = new String[] { "===> Expression '1 + 2' - AST start",
				"OperatorPlus  value=+  children=#2", "  CompoundExpression  value=EXPRESSION",
				"    IntLiteral  value=1", "  CompoundExpression  value=EXPRESSION", "    IntLiteral  value=2",
				"===> Expression '1 + 2' - AST end" };
		checkExpected(theAst, expectedLines);
	}

	private static void checkExpected(String theData, String[] expectedLines) {
		String[] theDataSplit = theData.split("\n");
		if (theDataSplit.length != expectedLines.length) {
			System.out.println("TheData:");
			System.out.println(theData);
			System.out.println("ExpectedData:\n" + expectedLines);
			fail("Data incorrect, expected " + expectedLines.length + " but got " + theDataSplit.length + " lines");
		}
		for (int i = 0; i < expectedLines.length; i++) {
			assertEquals("Failure in comparison at line " + i, expectedLines[i], theDataSplit[i]);
		}
	}
}
