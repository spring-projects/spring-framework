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
import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.ParseException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.FormatHelper;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.ReflectionHelper.ArgsMatchKind;

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
	
	public void testTypedValue() {
		TypedValue tValue = new TypedValue("hello");
		assertEquals(String.class,tValue.getTypeDescriptor().getType());
		assertEquals("TypedValue: hello of type java.lang.String",tValue.toString());
	}
	
	public void testReflectionHelperCompareArguments_ExactMatching() {
		StandardTypeConverter typeConverter = new StandardTypeConverter();
		
		// Calling foo(String) with (String) is exact match
		checkMatch(new Class[]{String.class},new Class[]{String.class},typeConverter,ArgsMatchKind.EXACT);
		
		// Calling foo(String,Integer) with (String,Integer) is exact match
		checkMatch(new Class[]{String.class,Integer.class},new Class[]{String.class,Integer.class},typeConverter,ArgsMatchKind.EXACT);
	}
	
	public void testReflectionHelperCompareArguments_Varargs_ExactMatching() {
		StandardTypeConverter tc = new StandardTypeConverter();
		
		// Calling foo(String) with (String) is exact match
		checkMatch(new Class[]{String.class},new Class[]{String.class},tc,ArgsMatchKind.EXACT);
	}
	
	public void testReflectionHelperCompareArguments_CloseMatching() {
		StandardTypeConverter typeConverter = new StandardTypeConverter();
		
		// Calling foo(List) with (ArrayList) is close match (no conversion required)
		checkMatch(new Class[]{ArrayList.class},new Class[]{List.class},typeConverter,ArgsMatchKind.CLOSE);
		
		// Passing (Sub,String) on call to foo(Super,String) is close match
		checkMatch(new Class[]{Sub.class,String.class},new Class[]{Super.class,String.class},typeConverter,ArgsMatchKind.CLOSE);
		
		// Passing (String,Sub) on call to foo(String,Super) is close match
		checkMatch(new Class[]{String.class,Sub.class},new Class[]{String.class,Super.class},typeConverter,ArgsMatchKind.CLOSE);
	}
	
	public void testReflectionHelperCompareArguments_RequiresConversionMatching() {
		StandardTypeConverter typeConverter = new StandardTypeConverter();
		
		// Calling foo(String,int) with (String,Integer) requires boxing conversion of argument one
		checkMatch(new Class[]{String.class,Integer.TYPE},new Class[]{String.class,Integer.class},typeConverter,ArgsMatchKind.REQUIRES_CONVERSION,1);

		// Passing (int,String) on call to foo(Integer,String) requires boxing conversion of argument zero
		checkMatch(new Class[]{Integer.TYPE,String.class},new Class[]{Integer.class, String.class},typeConverter,ArgsMatchKind.REQUIRES_CONVERSION,0);
		
		// Passing (int,Sub) on call to foo(Integer,Super) requires boxing conversion of argument zero
		checkMatch(new Class[]{Integer.TYPE,Sub.class},new Class[]{Integer.class, Super.class},typeConverter,ArgsMatchKind.REQUIRES_CONVERSION,0);
		
		// Passing (int,Sub,boolean) on call to foo(Integer,Super,Boolean) requires boxing conversion of arguments zero and two
		checkMatch(new Class[]{Integer.TYPE,Sub.class,Boolean.TYPE},new Class[]{Integer.class, Super.class,Boolean.class},typeConverter,ArgsMatchKind.REQUIRES_CONVERSION,0,2);
	}

	public void testReflectionHelperCompareArguments_NotAMatch() {
		StandardTypeConverter typeConverter = new StandardTypeConverter();
		
		// Passing (Super,String) on call to foo(Sub,String) is not a match
		checkMatch(new Class[]{Super.class,String.class},new Class[]{Sub.class,String.class},typeConverter,null);
	}
	
	static class Super {
	}
	
	static class Sub extends Super {
	}
	
	// ---
	
	/**
	 * Used to validate the match returned from a compareArguments call.
	 */
	private void checkMatch(Class[] inputTypes, Class[] expectedTypes, StandardTypeConverter typeConverter,ArgsMatchKind expectedMatchKind,int... argsForConversion) {
		ReflectionHelper.ArgumentsMatchInfo matchInfo = ReflectionHelper.compareArguments(expectedTypes, inputTypes, typeConverter);
		if (expectedMatchKind==null) {
			assertNull("Did not expect them to match in any way", matchInfo);
		} else {
			assertNotNull("Should not be a null match", matchInfo);
		}

		if (expectedMatchKind==ArgsMatchKind.EXACT) {
			assertTrue(matchInfo.isExactMatch());
			assertNull(matchInfo.argsRequiringConversion);		
		} else if (expectedMatchKind==ArgsMatchKind.CLOSE) {
			assertTrue(matchInfo.isCloseMatch());
			assertNull(matchInfo.argsRequiringConversion);		
		} else if (expectedMatchKind==ArgsMatchKind.REQUIRES_CONVERSION) {
			assertTrue("expected to be a match requiring conversion, but was "+matchInfo,matchInfo.isMatchRequiringConversion());
			if (argsForConversion==null) {
				fail("there are arguments that need conversion");
			}
			assertEquals("The array of args that need conversion is different length to that expected",argsForConversion.length, matchInfo.argsRequiringConversion.length);
			for (int a=0;a<argsForConversion.length;a++) {
				assertEquals(argsForConversion[a],matchInfo.argsRequiringConversion[a]);
			}
		}
	}
}
