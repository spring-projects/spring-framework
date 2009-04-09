/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.expression.ParseException;
import org.springframework.expression.spel.ast.FormatHelper;

/**
 * Tests for any helper code.
 * 
 * @author Andy Clement
 */
public class HelperTests extends ExpressionTestCase {

	public void testFormatHelperForClassName() {
		assertEquals("java.lang.String",FormatHelper.formatClassNameForMessage(String.class));
		assertEquals("java.lang.String[]",FormatHelper.formatClassNameForMessage(new String[1].getClass()));
		assertEquals("int[]",FormatHelper.formatClassNameForMessage(new int[1].getClass()));
		assertEquals("int[][]",FormatHelper.formatClassNameForMessage(new int[1][2].getClass()));
		assertEquals("null",FormatHelper.formatClassNameForMessage(null));
	}
	
	public void testFormatHelperForMethod() {
		assertEquals("foo(java.lang.String)",FormatHelper.formatMethodForMessage("foo", String.class));
		assertEquals("goo(java.lang.String,int[])",FormatHelper.formatMethodForMessage("goo", String.class,new int[1].getClass()));
		assertEquals("boo()",FormatHelper.formatMethodForMessage("boo"));
	}
	
	public void testUtilities() throws ParseException {
		SpelExpression expr = (SpelExpression)parser.parseExpression("3+4+5+6+7-2");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		SpelUtilities.printAbstractSyntaxTree(ps, expr);
		ps.flush();
		String s = baos.toString();
//		===> Expression '3+4+5+6+7-2' - AST start
//		OperatorMinus  value:(((((3 + 4) + 5) + 6) + 7) - 2)  #children:2
//		  OperatorPlus  value:((((3 + 4) + 5) + 6) + 7)  #children:2
//		    OperatorPlus  value:(((3 + 4) + 5) + 6)  #children:2
//		      OperatorPlus  value:((3 + 4) + 5)  #children:2
//		        OperatorPlus  value:(3 + 4)  #children:2
//		          CompoundExpression  value:3
//		            IntLiteral  value:3
//		          CompoundExpression  value:4
//		            IntLiteral  value:4
//		        CompoundExpression  value:5
//		          IntLiteral  value:5
//		      CompoundExpression  value:6
//		        IntLiteral  value:6
//		    CompoundExpression  value:7
//		      IntLiteral  value:7
//		  CompoundExpression  value:2
//		    IntLiteral  value:2
//		===> Expression '3+4+5+6+7-2' - AST end
		assertTrue(s.indexOf("===> Expression '3+4+5+6+7-2' - AST start")!=-1);
		assertTrue(s.indexOf(" OperatorPlus  value:((((3 + 4) + 5) + 6) + 7)  #children:2")!=-1);
	}
}
