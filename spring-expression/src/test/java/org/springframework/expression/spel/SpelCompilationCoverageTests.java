/*
 * Copyright 2014 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.*;

/**
 * Checks the behaviour of the SpelCompiler. This should cover compilation all compiled node types.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class SpelCompilationCoverageTests extends AbstractExpressionTests {
	
	private Expression expression;
	private SpelNodeImpl ast;
	
	/*
	 * Further TODOs for compilation:
	 * 
	 * - OpMinus with a single literal operand could be treated as a negative literal. Will save a
	 *   pointless loading of 0 and then a subtract instruction in code gen.
	 * - allow other accessors/resolvers to participate in compilation and create their own code
	 * - A TypeReference followed by (what ends up as) a static method invocation can really skip
	 *   code gen for the TypeReference since once that is used to locate the method it is not
	 *   used again.
	 * - The opEq implementation is quite basic. It will compare numbers of the same type (allowing
	 *   them to be their boxed or unboxed variants) or compare object references. It does not
	 *   compile expressions where numbers are of different types or when objects implement
	 *   Comparable.  
     *
	 * Compiled nodes:
	 * 
	 * TypeReference
	 * OperatorInstanceOf
	 * StringLiteral
	 * NullLiteral
	 * RealLiteral
	 * IntLiteral
	 * LongLiteral
	 * BooleanLiteral
	 * FloatLiteral
	 * OpOr
	 * OpAnd
	 * OperatorNot
	 * Ternary
	 * Elvis
	 * VariableReference
	 * OpLt
	 * OpLe
	 * OpGt
	 * OpGe
	 * OpEq
	 * OpNe
	 * OpPlus
	 * OpMinus
	 * OpMultiply
	 * OpDivide
	 * MethodReference
	 * PropertyOrFieldReference
	 * Indexer
	 * CompoundExpression
	 * ConstructorReference
	 * FunctionReference
	 * 
	 * Not yet compiled (some may never need to be):
	 * Assign
	 * BeanReference
	 * Identifier
	 * InlineList
	 * OpDec
	 * OpBetween
	 * OpMatches
	 * OpPower
	 * OpInc
	 * OpModulus
	 * Projection
	 * QualifiedId
	 * Selection
	 */
	
	@Test
	public void typeReference() throws Exception {
		expression = parse("T(String)");
		assertEquals(String.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(String.class,expression.getValue());
		 
		expression = parse("T(java.io.IOException)");
		assertEquals(IOException.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException.class,expression.getValue());

		expression = parse("T(java.io.IOException[])");
		assertEquals(IOException[].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException[].class,expression.getValue());

		expression = parse("T(int[][])");
		assertEquals(int[][].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(int[][].class,expression.getValue());

		expression = parse("T(int)");
		assertEquals(Integer.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Integer.TYPE,expression.getValue());

		expression = parse("T(byte)");
		assertEquals(Byte.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Byte.TYPE,expression.getValue());

		expression = parse("T(char)");
		assertEquals(Character.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Character.TYPE,expression.getValue());

		expression = parse("T(short)");
		assertEquals(Short.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Short.TYPE,expression.getValue());

		expression = parse("T(long)");
		assertEquals(Long.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Long.TYPE,expression.getValue());

		expression = parse("T(float)");
		assertEquals(Float.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Float.TYPE,expression.getValue());

		expression = parse("T(double)");
		assertEquals(Double.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Double.TYPE,expression.getValue());

		expression = parse("T(boolean)");
		assertEquals(Boolean.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Boolean.TYPE,expression.getValue());
		
		expression = parse("T(Missing)");
		assertGetValueFail(expression);
		assertCantCompile(expression);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void operatorInstanceOf() throws Exception {
		expression = parse("'xyz' instanceof T(String)");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		expression = parse("'xyz' instanceof T(Integer)");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		List<String> list = new ArrayList<String>();
		expression = parse("#root instanceof T(java.util.List)");
		assertEquals(true,expression.getValue(list));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(list));

		List<String>[] arrayOfLists = new List[]{new ArrayList<String>()};
		expression = parse("#root instanceof T(java.util.List[])");
		assertEquals(true,expression.getValue(arrayOfLists));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(arrayOfLists));
		
		int[] intArray = new int[]{1,2,3};
		expression = parse("#root instanceof T(int[])");
		assertEquals(true,expression.getValue(intArray));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(intArray));

		String root = null;
		expression = parse("#root instanceof T(Integer)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		// root still null
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		root = "howdy!";
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(true,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(root));
	}

	@Test
	public void stringLiteral() throws Exception {
		expression = parser.parseExpression("'abcde'");		
		assertEquals("abcde",expression.getValue(new TestClass1(),String.class));
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("abcde",resultC);
		assertEquals("abcde",expression.getValue(String.class));
		assertEquals("abcde",expression.getValue());
		assertEquals("abcde",expression.getValue(new StandardEvaluationContext()));
		expression = parser.parseExpression("\"abcde\"");
		assertCanCompile(expression);
		assertEquals("abcde",expression.getValue(String.class));
	}
	
	@Test
	public void nullLiteral() throws Exception {
		expression = parser.parseExpression("null");
		Object resultI = expression.getValue(new TestClass1(),Object.class);
		assertCanCompile(expression);
		Object resultC = expression.getValue(new TestClass1(),Object.class);
		assertEquals(null,resultI);
		assertEquals(null,resultC);
	}
	
	@Test
	public void realLiteral() throws Exception {
		expression = parser.parseExpression("3.4d");
		double resultI = expression.getValue(new TestClass1(),Double.TYPE);
		assertCanCompile(expression);
		double resultC = expression.getValue(new TestClass1(),Double.TYPE);
		assertEquals(3.4d,resultI,0.1d);
		assertEquals(3.4d,resultC,0.1d);

		assertEquals(3.4d,expression.getValue());
	}

	@Test
	public void intLiteral() throws Exception {
		expression = parser.parseExpression("42");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);

		expression = parser.parseExpression("T(Integer).valueOf(42)");
		expression.getValue(Integer.class);
		assertCanCompile(expression);
		assertEquals(new Integer(42),expression.getValue(null,Integer.class));
		
		// Code gen is different for -1 .. 6 because there are bytecode instructions specifically for those
		// values
		
		// Not an int literal but an opminus with one operand:
//		expression = parser.parseExpression("-1");
//		assertCanCompile(expression);
//		assertEquals(-1,expression.getValue());
		expression = parser.parseExpression("0");
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());
		expression = parser.parseExpression("2");
		assertCanCompile(expression);
		assertEquals(2,expression.getValue());
		expression = parser.parseExpression("7");
		assertCanCompile(expression);
		assertEquals(7,expression.getValue());
	}
	
	@Test
	public void longLiteral() throws Exception {
		expression = parser.parseExpression("99L");
		long resultI = expression.getValue(new TestClass1(),Long.TYPE);
		assertCanCompile(expression);
		long resultC = expression.getValue(new TestClass1(),Long.TYPE);
		assertEquals(99L,resultI);
		assertEquals(99L,resultC);		
	}
		
	@Test
	public void booleanLiteral() throws Exception {
		expression = parser.parseExpression("true");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertTrue(SpelCompiler.compile(expression));
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertTrue(SpelCompiler.compile(expression));
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultC);
	}
	
	@Test
	public void floatLiteral() throws Exception {
		expression = parser.parseExpression("3.4f");
		float resultI = expression.getValue(new TestClass1(),Float.TYPE);
		assertCanCompile(expression);
		float resultC = expression.getValue(new TestClass1(),Float.TYPE);
		assertEquals(3.4f,resultI,0.1f);
		assertEquals(3.4f,resultC,0.1f);

		assertEquals(3.4f,expression.getValue());
	}
	
	@Test
	public void opOr() throws Exception {
		Expression expression = parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);
		
		expression = parser.parseExpression("false or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		TestClass4 tc = new TestClass4();
		expression = parser.parseExpression("getfalse() or gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(tc,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		// Can't compile this as we aren't going down the getfalse() branch in our evaluation
		expression = parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);
		
		expression = parser.parseExpression("getA() or getB()");
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = false;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertTrue(resultI);
		
		boolean b = false;
		expression = parse("#root or #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertFalse((Boolean)resultI2);
		assertFalse((Boolean)expression.getValue(b));
	}
	
	@Test
	public void opAnd() throws Exception {
		Expression expression = parser.parseExpression("false and false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("false and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);
		
		expression = parser.parseExpression("true and false");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("true and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		TestClass4 tc = new TestClass4();

		// Can't compile this as we aren't going down the gettrue() branch in our evaluation
		expression = parser.parseExpression("getfalse() and gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);
		
		expression = parser.parseExpression("getA() and getB()");
		tc.a = false;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = true;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertFalse(resultI);
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertTrue(resultI);
		
		boolean b = true;
		expression = parse("#root and #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertTrue((Boolean)resultI2);
		assertTrue((Boolean)expression.getValue(b));
	}
	
	@Test
	public void operatorNot() throws Exception {
		expression = parse("!true");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		expression = parse("!false");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		boolean b = true;
		expression = parse("!#root");
		assertEquals(false,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(b));

		b = false;
		expression = parse("!#root");
		assertEquals(true,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(b));
	}

	@Test
	public void ternary() throws Exception {
		Expression expression = parser.parseExpression("true?'a':'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		expression = parser.parseExpression("false?'a':'b'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("b",resultI);
		assertEquals("b",resultC);

		expression = parser.parseExpression("false?1:'b'");
		// All literals so we can do this straight away
		assertCanCompile(expression);
		assertEquals("b",expression.getValue());

		boolean root = true;
		expression = parser.parseExpression("(#root and true)?T(Integer).valueOf(1):T(Long).valueOf(3L)");
		assertEquals(1,expression.getValue(root));
		assertCantCompile(expression); // Have not gone down false branch
		root = false;
		assertEquals(3L,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue(root));
		root = true;
		assertEquals(1,expression.getValue(root));		
	}
	
	@Test
	public void elvis() throws Exception {
		Expression expression = parser.parseExpression("'a'?:'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		expression = parser.parseExpression("null?:'a'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		String s = "abc";
		expression = parser.parseExpression("#root?:'b'");
		assertCantCompile(expression);
		resultI = expression.getValue(s,String.class);
		assertEquals("abc",resultI);
		assertCanCompile(expression);
	}
	
	@Test
	public void variableReference_root() throws Exception {
		String s = "hello";
		Expression expression = parser.parseExpression("#root");
		String resultI = expression.getValue(s,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(s,String.class);
		assertEquals(s,resultI);
		assertEquals(s,resultC);		

		expression = parser.parseExpression("#root");
		int i = (Integer)expression.getValue(42);
		assertEquals(42,i);
		assertCanCompile(expression);
		i = (Integer)expression.getValue(42);
		assertEquals(42,i);
	}
	
	public static String concat(String a, String b) {
		return a+b;
	}
	
	@Test
	public void functionReference() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = this.getClass().getDeclaredMethod("concat",String.class,String.class);
		ctx.setVariable("concat",m);
		
		expression = parser.parseExpression("#concat('a','b')");
		assertEquals("ab",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("ab",expression.getValue(ctx));
		
		expression = parser.parseExpression("#concat(#concat('a','b'),'c').charAt(1)");
		assertEquals('b',expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals('b',expression.getValue(ctx));
		
		expression = parser.parseExpression("#concat(#a,#b)");
		ctx.setVariable("a", "foo");
		ctx.setVariable("b", "bar");
		assertEquals("foobar",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("foobar",expression.getValue(ctx));
		ctx.setVariable("b", "boo");
		assertEquals("fooboo",expression.getValue(ctx));
	}
	
	@Test
	public void variableReference_userDefined() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("target", "abc");
		expression = parser.parseExpression("#target");
		assertEquals("abc",expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals("abc",expression.getValue(ctx));	
		ctx.setVariable("target", "123");
		assertEquals("123",expression.getValue(ctx));	
		ctx.setVariable("target", 42);
		try {
			assertEquals(42,expression.getValue(ctx));
			fail();
		} catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	
		ctx.setVariable("target", "abc");
		expression = parser.parseExpression("#target.charAt(0)");
		assertEquals('a',expression.getValue(ctx));
		assertCanCompile(expression);
		assertEquals('a',expression.getValue(ctx));	
		ctx.setVariable("target", "1");
		assertEquals('1',expression.getValue(ctx));	
		ctx.setVariable("target", 42);
		try {
			assertEquals('4',expression.getValue(ctx));
			fail();
		} catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
	}
	
	@Test
	public void opLt() throws Exception {
		expression = parse("3.0d < 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d < 1123.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 < 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2 < 4");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f < 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("1.0f < 5.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("30L < 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L < 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 < 3.0d");
		assertCantCompile(expression);
		
		expression = parse("T(Integer).valueOf(3) < 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) < T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 < T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
	}
	
	@Test
	public void opLe() throws Exception {
		expression = parse("3.0d <= 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d <= 1123.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d <= 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 <= 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2 <= 4");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3 <= 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f <= 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("1.0f <= 5.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2.0f <= 2.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("30L <= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L <= 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 <= 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) <= 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) <= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 <= T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
	}
	
	
	@Test
	public void opGt() throws Exception {
		expression = parse("3.0d > 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d > 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 > 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 > 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("3.0f > 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f > 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("30L > 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L > 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 > 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) > 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Integer).valueOf(3) > T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 > T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}
	
	@Test
	public void opGe() throws Exception {
		expression = parse("3.0d >= 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d >= 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d >= 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 >= 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 >= 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 >= 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f >= 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f >= 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3.0f >= 3.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("40L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L >= 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("30L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 >= 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) >= 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Integer).valueOf(3) >= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 >= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test
	public void opEq() throws Exception {
		
		TestClass7 tc7 = new TestClass7();
		expression = parse("property == 'UK'");
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.property = null;
		assertFalse((Boolean)expression.getValue(tc7));
		assertCanCompile(expression);
		TestClass7.reset();
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.property = "UK";
		assertTrue((Boolean)expression.getValue(tc7));
		TestClass7.reset();
		TestClass7.property = null;
		assertFalse((Boolean)expression.getValue(tc7));
		expression = parse("property == null");
		assertTrue((Boolean)expression.getValue(tc7));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(tc7));
		
		
		expression = parse("3.0d == 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d == 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 == 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 == 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f == 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2.0f == 2.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("30L == 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L == 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		// number types are not the same
		expression = parse("1 == 3.0d");
		assertCantCompile(expression);
		
		Double d = 3.0d;
		expression = parse("#root==3.0d");
		assertTrue((Boolean)expression.getValue(d));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(d));
		
		Integer i = 3;
		expression = parse("#root==3");
		assertTrue((Boolean)expression.getValue(i));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(i));

		Float f = 3.0f;
		expression = parse("#root==3.0f");
		assertTrue((Boolean)expression.getValue(f));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(f));
		
		long l = 300l;
		expression = parse("#root==300l");
		assertTrue((Boolean)expression.getValue(l));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(l));
		
		boolean b = true;
		expression = parse("#root==true");
		assertTrue((Boolean)expression.getValue(b));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(b));

		expression = parse("T(Integer).valueOf(3) == 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Integer).valueOf(3) == T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5 == T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) == 4.0f");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Float).valueOf(3.0f) == T(Float).valueOf(3.0f)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0f == T(Float).valueOf(3.0f)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Long).valueOf(3L) == 4L");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Long).valueOf(3L) == T(Long).valueOf(3L)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5L == T(Long).valueOf(3L)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());	
		
		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("false == true");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}

	@Test
	public void opNe() throws Exception {
		expression = parse("3.0d != 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d != 3446.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 != 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3 != 3");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("3.0f != 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2.0f != 2.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("30L != 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L != 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// not compatible number types
		expression = parse("1 != 3.0d");	
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) != 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("T(Integer).valueOf(3) != T(Integer).valueOf(3)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5 != T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Float).valueOf(3.0f) != 4.0f");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("T(Float).valueOf(3.0f) != T(Float).valueOf(3.0f)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5.0f != T(Float).valueOf(3.0f)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("T(Long).valueOf(3L) != 4L");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("T(Long).valueOf(3L) != T(Long).valueOf(3L)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("5L != T(Long).valueOf(3L)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());	
		
		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("false == true");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Boolean).valueOf('true') == true");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("false == T(Boolean).valueOf('false')");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}
	
	@Test
	public void opPlus() throws Exception {
		expression = parse("2+2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());
		
		expression = parse("2L+2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f+2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d+4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(7.0d,expression.getValue());
		
		expression = parse("+1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());		

		expression = parse("+1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());		

		expression = parse("+1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.5f,expression.getValue());		

		expression = parse("+2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.5d,expression.getValue());	

		expression = parse("+T(Double).valueOf(2.5d)");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.5d,expression.getValue());	
		
		expression = parse("T(Integer).valueOf(2)+6");
		assertEquals(8,expression.getValue());
		assertCanCompile(expression);
		assertEquals(8,expression.getValue());
		
		expression = parse("T(Integer).valueOf(1)+T(Integer).valueOf(3)");
		assertEquals(4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("1+T(Integer).valueOf(3)");
		assertEquals(4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)+6");
		assertEquals(8.0f,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Float).valueOf(2.0f)+T(Float).valueOf(3.0f)");
		assertEquals(5.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5.0f,expression.getValue());

		expression = parse("3L+T(Long).valueOf(4L)");
		assertEquals(7L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(7L,expression.getValue());

		expression = parse("T(Long).valueOf(2L)+6");
		assertEquals(8L,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Long).valueOf(2L)+T(Long).valueOf(3L)");
		assertEquals(5L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5L,expression.getValue());

		expression = parse("1L+T(Long).valueOf(2L)");
		assertEquals(3L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue());
	}

	@Test
	public void opMinus() throws Exception {
		expression = parse("2-2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());
		
		expression = parse("4L-2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("4.0f-2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.0f,expression.getValue());

		expression = parse("3.0d-4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.0d,expression.getValue());
		
		expression = parse("-1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1,expression.getValue());		

		expression = parse("-1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1L,expression.getValue());		

		expression = parse("-1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.5f,expression.getValue());		

		expression = parse("-2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-2.5d,expression.getValue());	
		
		expression = parse("T(Integer).valueOf(2)-6");
		assertEquals(-4,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-4,expression.getValue());
		
		expression = parse("T(Integer).valueOf(1)-T(Integer).valueOf(3)");
		assertEquals(-2,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-2,expression.getValue());

		expression = parse("4-T(Integer).valueOf(3)");
		assertEquals(1,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)-6");
		assertEquals(-4.0f,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Float).valueOf(8.0f)-T(Float).valueOf(3.0f)");
		assertEquals(5.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(5.0f,expression.getValue());

		expression = parse("11L-T(Long).valueOf(4L)");
		assertEquals(7L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(7L,expression.getValue());

		expression = parse("T(Long).valueOf(9L)-6");
		assertEquals(3L,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Long).valueOf(4L)-T(Long).valueOf(3L)");
		assertEquals(1L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("8L-T(Long).valueOf(2L)");
		assertEquals(6L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(6L,expression.getValue());
	}
	
	
	@Test
	public void opMultiply() throws Exception {
		expression = parse("2*2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());
		
		expression = parse("2L*2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f*2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d*4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(12.0d,expression.getValue());

		expression = parse("T(Float).valueOf(2.0f)*6");
		assertEquals(12.0f,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Float).valueOf(8.0f)*T(Float).valueOf(3.0f)");
		assertEquals(24.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(24.0f,expression.getValue());

		expression = parse("11L*T(Long).valueOf(4L)");
		assertEquals(44L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(44L,expression.getValue());

		expression = parse("T(Long).valueOf(9L)*6");
		assertEquals(54L,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Long).valueOf(4L)*T(Long).valueOf(3L)");
		assertEquals(12L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(12L,expression.getValue());

		expression = parse("8L*T(Long).valueOf(2L)");
		assertEquals(16L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(16L,expression.getValue());

		expression = parse("T(Float).valueOf(8.0f)*-T(Float).valueOf(3.0f)");
		assertEquals(-24.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-24.0f,expression.getValue());
	}
	
	@Test
	public void opDivide() throws Exception {
		expression = parse("2/2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());
		
		expression = parse("2L/2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("2.0f/2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.0f,expression.getValue());

		expression = parse("3.0d/4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0.75d,expression.getValue());

		expression = parse("T(Float).valueOf(6.0f)/2");
		assertEquals(3.0f,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Float).valueOf(8.0f)/T(Float).valueOf(2.0f)");
		assertEquals(4.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("12L/T(Long).valueOf(4L)");
		assertEquals(3L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue());

		expression = parse("T(Long).valueOf(44L)/11");
		assertEquals(4L,expression.getValue());
		assertCantCompile(expression);
		
		expression = parse("T(Long).valueOf(4L)/T(Long).valueOf(2L)");
		assertEquals(2L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("8L/T(Long).valueOf(2L)");
		assertEquals(4L,expression.getValue());
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("T(Float).valueOf(8.0f)/-T(Float).valueOf(4.0f)");
		assertEquals(-2.0f,expression.getValue());
		assertCanCompile(expression);
		assertEquals(-2.0f,expression.getValue());
	}
	

	@Test
	public void constructorReference() throws Exception {
		// simple ctor
		expression = parser.parseExpression("new String('123')");
		assertEquals("123",expression.getValue());
		assertCanCompile(expression);
		assertEquals("123",expression.getValue());

		String testclass8 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass8"; 
		// multi arg ctor that includes primitives
		expression = parser.parseExpression("new "+testclass8+"(42,'123',4.0d,true)");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		Object o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		TestClass8 tc8 = (TestClass8)o;
		assertEquals(42,tc8.i);
		assertEquals("123",tc8.s);
		assertEquals(4.0d,tc8.d,0.5d);
		assertEquals(true,tc8.z);
		
		// no-arg ctor
		expression = parser.parseExpression("new "+testclass8+"()");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		
		// pass primitive to reference type ctor
		expression = parser.parseExpression("new "+testclass8+"(42)");
		assertEquals(testclass8,expression.getValue().getClass().getName());
		assertCanCompile(expression);
		o = expression.getValue();
		assertEquals(testclass8,o.getClass().getName());
		tc8 = (TestClass8)o;
		assertEquals(42,tc8.i);

		// private class, can't compile it
		String testclass9 = "org.springframework.expression.spel.SpelCompilationCoverageTests$TestClass9"; 
		expression = parser.parseExpression("new "+testclass9+"(42)");
		assertEquals(testclass9,expression.getValue().getClass().getName());
		assertCantCompile(expression);
	}
	
	@Test
	public void methodReference() throws Exception {
		TestClass5 tc = new TestClass5();
		
		// non-static method, no args, void return
		expression = parser.parseExpression("one()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(1,tc.i);
		tc.reset();
		
		// static method, no args, void return
		expression = parser.parseExpression("two()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(1,TestClass5._i);
		tc.reset();
		
		// non-static method, reference type return
		expression = parser.parseExpression("three()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals("hello",expression.getValue(tc));
		tc.reset();

		// non-static method, primitive type return
		expression = parser.parseExpression("four()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals(3277700L,expression.getValue(tc));
		tc.reset();
		
		// static method, reference type return
		expression = parser.parseExpression("five()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals("hello",expression.getValue(tc));
		tc.reset();

		// static method, primitive type return
		expression = parser.parseExpression("six()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertEquals(3277700L,expression.getValue(tc));
		tc.reset();
		
		// non-static method, one parameter of reference type
		expression = parser.parseExpression("seven(\"foo\")");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		tc.reset();
		
		// static method, one parameter of reference type
		expression = parser.parseExpression("eight(\"bar\")");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals("bar",TestClass5._s);
		tc.reset();
		
		// non-static method, one parameter of primitive type
		expression = parser.parseExpression("nine(231)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(231,tc.i);
		tc.reset();
		
		// static method, one parameter of primitive type
		expression = parser.parseExpression("ten(111)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertEquals(111,TestClass5._i);
		tc.reset();
		
		// non-static method, varargs with reference type
		expression = parser.parseExpression("eleven(\"a\",\"b\",\"c\")");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCantCompile(expression); // Varargs is not yet supported
		
		expression = parser.parseExpression("eleven()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCantCompile(expression); // Varargs is not yet supported
		
		// static method, varargs with primitive type
		expression = parser.parseExpression("twelve(1,2,3)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCantCompile(expression); // Varargs is not yet supported
		
		expression = parser.parseExpression("twelve()");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertCantCompile(expression); // Varargs is not yet supported
		
		// method that gets type converted parameters
		
		// Converting from an int to a string
		expression = parser.parseExpression("seven(123)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		assertCantCompile(expression); // Uncompilable as argument conversion is occurring
		
		Expression expression = parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
		
		// Converting from an int to a Number
		expression = parser.parseExpression("takeNumber(123)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("123",tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("123",tc.s);

		// Passing a subtype
		expression = parser.parseExpression("takeNumber(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42",tc.s);
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertEquals("42",tc.s);

		// Passing a subtype
		expression = parser.parseExpression("takeString(T(Integer).valueOf(42))");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("42",tc.s);
		tc.reset();
		assertCantCompile(expression); // method takes a string and we are passing an Integer
	}
		
	
	@Test 
	public void errorHandling() throws Exception {
		TestClass5 tc = new TestClass5();
		
		// changing target
		
		// from primitive array to reference type array
		int[] is = new int[]{1,2,3};
		String[] strings = new String[]{"a","b","c"};
		expression = parser.parseExpression("[1]");
		assertEquals(2,expression.getValue(is));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(is));
		
		try {
			assertEquals(2,expression.getValue(strings));
			fail();
		} catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
		SpelCompiler.revertToInterpreted(expression);
		assertEquals("b",expression.getValue(strings));
		assertCanCompile(expression);
		assertEquals("b",expression.getValue(strings));
		
		
		tc.field = "foo";
		expression = parser.parseExpression("seven(field)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		assertCanCompile(expression);
		tc.reset();
		tc.field="bar";
		expression.getValue(tc);
		
		// method with changing parameter types (change reference type)
		tc.obj = "foo";
		expression = parser.parseExpression("seven(obj)");
		assertCantCompile(expression);
		expression.getValue(tc);
		assertEquals("foo",tc.s);
		assertCanCompile(expression);
		tc.reset();
		tc.obj=new Integer(42);
		try {
			expression.getValue(tc);
			fail();
		} catch (SpelEvaluationException see) {
			assertTrue(see.getCause() instanceof ClassCastException);
		}
		
		
		// method with changing target
		expression = parser.parseExpression("#root.charAt(0)");
		assertEquals('a',expression.getValue("abc"));
		assertCanCompile(expression);
		try {
			expression.getValue(new Integer(42));
			fail();
		} catch (SpelEvaluationException see) {
			// java.lang.Integer cannot be cast to java.lang.String
			assertTrue(see.getCause() instanceof ClassCastException);
		}		
	}
	
	@Test
	public void methodReference_staticMethod() throws Exception {
		Expression expression = parser.parseExpression("T(Integer).valueOf(42)");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);		
	}
	
	@Test
	public void methodReference_literalArguments_int() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodNoArg() throws Exception {
		Expression expression = parser.parseExpression("toString()");
		String resultI = expression.getValue(42,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(42,String.class);
		assertEquals("42",resultI);
		assertEquals("42",resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodNoArgReturnPrimitive() throws Exception {
		expression = parser.parseExpression("intValue()");
		int resultI = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultI);
		assertCanCompile(expression);
		int resultC = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultC);
	}
	
	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive1() throws Exception {
		Expression expression = parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc",Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc",Integer.TYPE);
		assertEquals(1,resultI);
		assertEquals(1,resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive2() throws Exception {
		expression = parser.parseExpression("charAt(2)");
		char resultI = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultI);
		assertCanCompile(expression);
		char resultC = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultC);
	}


	@Test
	public void compoundExpression() throws Exception {
		Payload payload = new Payload();
		expression = parser.parseExpression("DR[0]");
		assertEquals("instanceof Two",expression.getValue(payload).toString());
		assertCanCompile(expression);
		assertEquals("instanceof Two",expression.getValue(payload).toString());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two",ast.getExitDescriptor());

		expression = parser.parseExpression("holder.three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",ast.getExitDescriptor());

		expression = parser.parseExpression("DR[0]");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Two",expression.getValue(payload).getClass().getName());
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two",getAst().getExitDescriptor());

		expression = parser.parseExpression("DR[0].three");
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		assertCanCompile(expression);
		assertEquals("org.springframework.expression.spel.SpelCompilationCoverageTests$Three",expression.getValue(payload).getClass().getName());
		ast = getAst();
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",ast.getExitDescriptor());

		expression = parser.parseExpression("DR[0].three.four");
		assertEquals(0.04d,expression.getValue(payload));
		assertCanCompile(expression);
		assertEquals(0.04d,expression.getValue(payload));
		assertEquals("D",getAst().getExitDescriptor());
	}
	
	
	@Test
	public void mixingItUp_indexerOpEqTernary() throws Exception {
		Map<String, String> m = new HashMap<String,String>();
		m.put("andy","778");

		expression = parse("['andy']==null?1:2");
		System.out.println(expression.getValue(m));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(m));
		m.remove("andy");
		assertEquals(1,expression.getValue(m));
	}
	
	@Test
	public void propertyReference() throws Exception {
		TestClass6 tc = new TestClass6();
		
		// non static field
		expression = parser.parseExpression("orange");
		assertCantCompile(expression);
		assertEquals("value1",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value1",expression.getValue(tc));
		
		// static field
		expression = parser.parseExpression("apple");
		assertCantCompile(expression);
		assertEquals("value2",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value2",expression.getValue(tc));	
		
		// non static getter
		expression = parser.parseExpression("banana");
		assertCantCompile(expression);
		assertEquals("value3",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value3",expression.getValue(tc));

		// static getter
		expression = parser.parseExpression("plum");
		assertCantCompile(expression);
		assertEquals("value4",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value4",expression.getValue(tc));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void indexer() throws Exception {
		String[] sss = new String[]{"a","b","c"};
		Number[] ns = new Number[]{2,8,9};
		int[] is = new int[]{8,9,10};
		double[] ds = new double[]{3.0d,4.0d,5.0d};
		long[] ls = new long[]{2L,3L,4L};
		short[] ss = new short[]{(short)33,(short)44,(short)55};
		float[] fs = new float[]{6.0f,7.0f,8.0f};
		byte[] bs = new byte[]{(byte)2,(byte)3,(byte)4};
		char[] cs = new char[]{'a','b','c'};
				
		// Access String (reference type) array
		expression = parser.parseExpression("[0]");
		assertEquals("a",expression.getValue(sss));
		assertCanCompile(expression);
		assertEquals("a",expression.getValue(sss));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());

		expression = parser.parseExpression("[1]");
		assertEquals(8,expression.getValue(ns));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(ns));
		assertEquals("Ljava/lang/Number",getAst().getExitDescriptor());
		
		// Access int array
		expression = parser.parseExpression("[2]");
		assertEquals(10,expression.getValue(is));
		assertCanCompile(expression);
		assertEquals(10,expression.getValue(is));
		assertEquals("I",getAst().getExitDescriptor());

		// Access double array
		expression = parser.parseExpression("[1]");
		assertEquals(4.0d,expression.getValue(ds));
		assertCanCompile(expression);
		assertEquals(4.0d,expression.getValue(ds));
		assertEquals("D",getAst().getExitDescriptor());

		// Access long array
		expression = parser.parseExpression("[0]");
		assertEquals(2L,expression.getValue(ls));
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue(ls));
		assertEquals("J",getAst().getExitDescriptor());

		// Access short array
		expression = parser.parseExpression("[2]");
		assertEquals((short)55,expression.getValue(ss));
		assertCanCompile(expression);
		assertEquals((short)55,expression.getValue(ss));
		assertEquals("S",getAst().getExitDescriptor());

		// Access float array
		expression = parser.parseExpression("[0]");
		assertEquals(6.0f,expression.getValue(fs));
		assertCanCompile(expression);
		assertEquals(6.0f,expression.getValue(fs));
		assertEquals("F",getAst().getExitDescriptor());

		// Access byte array
		expression = parser.parseExpression("[2]");
		assertEquals((byte)4,expression.getValue(bs));
		assertCanCompile(expression);
		assertEquals((byte)4,expression.getValue(bs));
		assertEquals("B",getAst().getExitDescriptor());

		// Access char array
		expression = parser.parseExpression("[1]");
		assertEquals('b',expression.getValue(cs));
		assertCanCompile(expression);
		assertEquals('b',expression.getValue(cs));
		assertEquals("C",getAst().getExitDescriptor());
		
		// Collections
		List<String> strings = new ArrayList<String>();
		strings.add("aaa");
		strings.add("bbb");
		strings.add("ccc");
		expression = parser.parseExpression("[1]");
		assertEquals("bbb",expression.getValue(strings));
		assertCanCompile(expression);
		assertEquals("bbb",expression.getValue(strings));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		List<Integer> ints = new ArrayList<Integer>();
		ints.add(123);
		ints.add(456);
		ints.add(789);
		expression = parser.parseExpression("[2]");
		assertEquals(789,expression.getValue(ints));
		assertCanCompile(expression);
		assertEquals(789,expression.getValue(ints));
		assertEquals("Ljava/lang/Integer",getAst().getExitDescriptor());
		
		// Maps
		Map<String,Integer> map1 = new HashMap<String,Integer>();
		map1.put("aaa", 111);
		map1.put("bbb", 222);
		map1.put("ccc", 333);
		expression = parser.parseExpression("['aaa']");
		assertEquals(111,expression.getValue(map1));
		assertCanCompile(expression);
		assertEquals(111,expression.getValue(map1));
		assertEquals("Ljava/lang/Integer",getAst().getExitDescriptor());
		
		// Object
		TestClass6 tc = new TestClass6();
		expression = parser.parseExpression("['orange']");
		assertEquals("value1",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value1",expression.getValue(tc));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("['peach']");
		assertEquals(34L,expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals(34L,expression.getValue(tc));
		assertEquals("J",getAst().getExitDescriptor());

		// getter
		expression = parser.parseExpression("['banana']");
		assertEquals("value3",expression.getValue(tc));
		assertCanCompile(expression);
		assertEquals("value3",expression.getValue(tc));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		// list of arrays
		
		List<String[]> listOfStringArrays = new ArrayList<String[]>();
		listOfStringArrays.add(new String[]{"a","b","c"});
		listOfStringArrays.add(new String[]{"d","e","f"});
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(listOfStringArrays)));
		assertCanCompile(expression);
		assertEquals("d e f",stringify(expression.getValue(listOfStringArrays)));
		assertEquals("[Ljava/lang/String",getAst().getExitDescriptor());

		List<Integer[]> listOfIntegerArrays = new ArrayList<Integer[]>();
		listOfIntegerArrays.add(new Integer[]{1,2,3});
		listOfIntegerArrays.add(new Integer[]{4,5,6});
		expression = parser.parseExpression("[0]");
		assertEquals("1 2 3",stringify(expression.getValue(listOfIntegerArrays)));
		assertCanCompile(expression);
		assertEquals("1 2 3",stringify(expression.getValue(listOfIntegerArrays)));
		assertEquals("[Ljava/lang/Integer",getAst().getExitDescriptor());

		expression = parser.parseExpression("[0][1]");
		assertEquals(2,expression.getValue(listOfIntegerArrays));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(listOfIntegerArrays));
		assertEquals("Ljava/lang/Integer",getAst().getExitDescriptor());
		
		// array of lists
		List<String>[] stringArrayOfLists = new ArrayList[2];
		stringArrayOfLists[0] = new ArrayList<String>();
		stringArrayOfLists[0].add("a");
		stringArrayOfLists[0].add("b");
		stringArrayOfLists[0].add("c");
		stringArrayOfLists[1] = new ArrayList<String>();
		stringArrayOfLists[1].add("d");
		stringArrayOfLists[1].add("e");
		stringArrayOfLists[1].add("f");
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(stringArrayOfLists)));
		assertCanCompile(expression);
		assertEquals("d e f",stringify(expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(stringArrayOfLists)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(stringArrayOfLists)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		// array of arrays
		String[][] referenceTypeArrayOfArrays = new String[][]{new String[]{"a","b","c"},new String[]{"d","e","f"}};
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("[Ljava/lang/String",getAst().getExitDescriptor());
		assertEquals("d e f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("[Ljava/lang/String",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(referenceTypeArrayOfArrays)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		int[][] primitiveTypeArrayOfArrays = new int[][]{new int[]{1,2,3},new int[]{4,5,6}};
		expression = parser.parseExpression("[1]");
		assertEquals("4 5 6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("4 5 6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("[I",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("[1][2]");
		assertEquals("6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertCanCompile(expression);
		assertEquals("6",stringify(expression.getValue(primitiveTypeArrayOfArrays)));
		assertEquals("I",getAst().getExitDescriptor());
		
		// list of lists of reference types
		List<List<String>> listOfListOfStrings = new ArrayList<List<String>>();
		List<String> list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		listOfListOfStrings.add(list);
		list = new ArrayList<String>();
		list.add("d");
		list.add("e");
		list.add("f");
		listOfListOfStrings.add(list);
		
		expression = parser.parseExpression("[1]");
		assertEquals("d e f",stringify(expression.getValue(listOfListOfStrings)));
		assertCanCompile(expression);
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());
		assertEquals("d e f",stringify(expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("[1][2]");
		assertEquals("f",stringify(expression.getValue(listOfListOfStrings)));
		assertCanCompile(expression);
		assertEquals("f",stringify(expression.getValue(listOfListOfStrings)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		// Map of lists
		Map<String,List<String>> mapToLists = new HashMap<String,List<String>>();
		list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		mapToLists.put("foo", list);
		expression = parser.parseExpression("['foo']");
		assertEquals("a b c",stringify(expression.getValue(mapToLists)));
		assertCanCompile(expression);
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());
		assertEquals("a b c",stringify(expression.getValue(mapToLists)));
		assertEquals("Ljava/util/ArrayList",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("['foo'][2]");
		assertEquals("c",stringify(expression.getValue(mapToLists)));
		assertCanCompile(expression);
		assertEquals("c",stringify(expression.getValue(mapToLists)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
		
		// Map to array
		Map<String,int[]> mapToIntArray = new HashMap<String,int[]>();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new CompilableMapAccessor());
		mapToIntArray.put("foo",new int[]{1,2,3});
		expression = parser.parseExpression("['foo']");
		assertEquals("1 2 3",stringify(expression.getValue(mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("[I",getAst().getExitDescriptor());
		assertEquals("1 2 3",stringify(expression.getValue(mapToIntArray)));
		assertEquals("[I",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("['foo'][1]");
		assertEquals(2,expression.getValue(mapToIntArray));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(mapToIntArray));
		
		expression = parser.parseExpression("foo");
		assertEquals("1 2 3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("1 2 3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertEquals("Ljava/lang/Object",getAst().getExitDescriptor());

		expression = parser.parseExpression("foo[1]");
		assertEquals(2,expression.getValue(ctx,mapToIntArray));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(ctx,mapToIntArray));

		expression = parser.parseExpression("['foo'][2]");
		assertEquals("3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertCanCompile(expression);
		assertEquals("3",stringify(expression.getValue(ctx,mapToIntArray)));
		assertEquals("I",getAst().getExitDescriptor());
		
		// Map array
		Map<String,String>[] mapArray = new Map[1];
		mapArray[0] = new HashMap<String,String>();
		mapArray[0].put("key", "value1");
		expression = parser.parseExpression("[0]");
		assertEquals("{key=value1}",stringify(expression.getValue(mapArray)));
		assertCanCompile(expression);
		assertEquals("Ljava/util/Map",getAst().getExitDescriptor());
		assertEquals("{key=value1}",stringify(expression.getValue(mapArray)));
		assertEquals("Ljava/util/Map",getAst().getExitDescriptor());
		
		expression = parser.parseExpression("[0]['key']");
		assertEquals("value1",stringify(expression.getValue(mapArray)));
		assertCanCompile(expression);
		assertEquals("value1",stringify(expression.getValue(mapArray)));
		assertEquals("Ljava/lang/String",getAst().getExitDescriptor());
	}
	
	@Test
	public void mixingItUp_propertyAccessIndexerOpLtTernaryRootNull() throws Exception {
		Payload payload = new Payload();
		
		expression = parser.parseExpression("DR[0].three");
		Object v = expression.getValue(payload);
		assertEquals("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three",getAst().getExitDescriptor());
		
		Expression expression = parser.parseExpression("DR[0].three.four lt 0.1d?#root:null");
		v = expression.getValue(payload);
		
		SpelExpression sExpr = (SpelExpression)expression;
		Ternary ternary = (Ternary)sExpr.getAST();
		OpLT oplt = (OpLT)ternary.getChild(0);
		CompoundExpression cExpr = (CompoundExpression)oplt.getLeftOperand();
		String cExprExitDescriptor = cExpr.getExitDescriptor();
		assertEquals("D",cExprExitDescriptor);
		assertEquals("Z",oplt.getExitDescriptor());
		
		assertCanCompile(expression);
		Object vc = expression.getValue(payload);
		assertEquals(payload,v);
		assertEquals(payload,vc);
		payload.DR[0].three.four = 0.13d;
		vc = expression.getValue(payload);
		assertNull(vc);
	}

	@Test
	public void variantGetter() throws Exception {
		Payload2Holder holder = new Payload2Holder();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MyAccessor());
		expression = parser.parseExpression("payload2.var1");
		Object v = expression.getValue(ctx,holder);
		assertEquals("abc",v);
		
//		// time it interpreted
//		long stime = System.currentTimeMillis();
//		for (int i=0;i<100000;i++) {
//			v = expression.getValue(ctx,holder);
//		}
//		System.out.println((System.currentTimeMillis()-stime));
//
		assertCanCompile(expression);
		v = expression.getValue(ctx,holder);
		assertEquals("abc",v);
//		
//		// time it compiled
//		stime = System.currentTimeMillis();
//		for (int i=0;i<100000;i++) {
//			v = expression.getValue(ctx,holder);
//		}
//		System.out.println((System.currentTimeMillis()-stime));
	}
	
	@Test
	public void compilerWithGenerics_12040() {
		expression = parser.parseExpression("payload!=2");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));
		
		expression = parser.parseExpression("2!=payload");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload!=6L");
		assertTrue(expression.getValue(new GenericMessageTestHelper<Long>(4L),Boolean.class));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper<Long>(6L),Boolean.class));
		
		expression = parser.parseExpression("payload==2");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));
		
		expression = parser.parseExpression("2==payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload==6L");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Long>(4L),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Long>(6L),Boolean.class));

		expression = parser.parseExpression("2==payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper<Integer>(4),Boolean.class));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper<Integer>(2),Boolean.class));

		expression = parser.parseExpression("payload/2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper<Integer>(6)));
		
		expression = parser.parseExpression("100/payload");
		assertEquals(25,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(10,expression.getValue(new GenericMessageTestHelper<Integer>(10)));
		
		expression = parser.parseExpression("payload+2");
		assertEquals(6,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(new GenericMessageTestHelper<Integer>(6)));
		
		expression = parser.parseExpression("100+payload");
		assertEquals(104,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(110,expression.getValue(new GenericMessageTestHelper<Integer>(10)));
		
		expression = parser.parseExpression("payload-2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(4,expression.getValue(new GenericMessageTestHelper<Integer>(6)));
		
		expression = parser.parseExpression("100-payload");
		assertEquals(96,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(90,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload*2");
		assertEquals(8,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(12,expression.getValue(new GenericMessageTestHelper<Integer>(6)));
		
		expression = parser.parseExpression("100*payload");
		assertEquals(400,expression.getValue(new GenericMessageTestHelper<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(1000,expression.getValue(new GenericMessageTestHelper<Integer>(10)));

		expression = parser.parseExpression("payload/2L");
		assertEquals(2L,expression.getValue(new GenericMessageTestHelper<Long>(4L)));
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue(new GenericMessageTestHelper<Long>(6L)));
		
		expression = parser.parseExpression("100L/payload");
		assertEquals(25L,expression.getValue(new GenericMessageTestHelper<Long>(4L)));
		assertCanCompile(expression);
		assertEquals(10L,expression.getValue(new GenericMessageTestHelper<Long>(10L)));

		expression = parser.parseExpression("payload/2f");
		assertEquals(2f,expression.getValue(new GenericMessageTestHelper<Float>(4f)));
		assertCanCompile(expression);
		assertEquals(3f,expression.getValue(new GenericMessageTestHelper<Float>(6f)));
		
		expression = parser.parseExpression("100f/payload");
		assertEquals(25f,expression.getValue(new GenericMessageTestHelper<Float>(4f)));
		assertCanCompile(expression);
		assertEquals(10f,expression.getValue(new GenericMessageTestHelper<Float>(10f)));

		expression = parser.parseExpression("payload/2d");
		assertEquals(2d,expression.getValue(new GenericMessageTestHelper<Double>(4d)));
		assertCanCompile(expression);
		assertEquals(3d,expression.getValue(new GenericMessageTestHelper<Double>(6d)));
		
		expression = parser.parseExpression("100d/payload");
		assertEquals(25d,expression.getValue(new GenericMessageTestHelper<Double>(4d)));
		assertCanCompile(expression);
		assertEquals(10d,expression.getValue(new GenericMessageTestHelper<Double>(10d)));
	}
	
	// The new helper class here uses an upper bound on the generic
	@Test
	public void compilerWithGenerics_12040_2() {
		expression = parser.parseExpression("payload/2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));

		expression = parser.parseExpression("9/payload");
		assertEquals(1,expression.getValue(new GenericMessageTestHelper2<Integer>(9)));
		assertCanCompile(expression);
		assertEquals(3,expression.getValue(new GenericMessageTestHelper2<Integer>(3)));

		expression = parser.parseExpression("payload+2");
		assertEquals(6,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(8,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));
		
		expression = parser.parseExpression("100+payload");
		assertEquals(104,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(110,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));
		
		expression = parser.parseExpression("payload-2");
		assertEquals(2,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(4,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));
		
		expression = parser.parseExpression("100-payload");
		assertEquals(96,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(90,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));

		expression = parser.parseExpression("payload*2");
		assertEquals(8,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(12,expression.getValue(new GenericMessageTestHelper2<Integer>(6)));
		
		expression = parser.parseExpression("100*payload");
		assertEquals(400,expression.getValue(new GenericMessageTestHelper2<Integer>(4)));
		assertCanCompile(expression);
		assertEquals(1000,expression.getValue(new GenericMessageTestHelper2<Integer>(10)));
	}
	
	// The other numeric operators
	@Test
	public void compilerWithGenerics_12040_3() {
		expression = parser.parseExpression("payload >= 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(4),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("2 >= payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(5),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("payload > 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(4),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("2 > payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(5),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));

		expression = parser.parseExpression("payload <=2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("2 <= payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("payload < 2");
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));

		expression = parser.parseExpression("2 < payload");
		assertFalse(expression.getValue(new GenericMessageTestHelper2<Integer>(1),Boolean.TYPE));
		assertCanCompile(expression);
		assertTrue(expression.getValue(new GenericMessageTestHelper2<Integer>(6),Boolean.TYPE));
	}

	// ---

	public static class GenericMessageTestHelper<T> {
		private T payload;
		
		GenericMessageTestHelper(T value) {
			this.payload = value;
		}
		
		public T getPayload() {
			return payload;
		}
	}
	
	// This test helper has a bound on the type variable
	public static class GenericMessageTestHelper2<T extends Number> {
		private T payload;
		
		GenericMessageTestHelper2(T value) {
			this.payload = value;
		}
		
		public T getPayload() {
			return payload;
		}
	}
	
	static class MyAccessor implements CompilablePropertyAccessor {

		private Method method;

		public Class<?>[] getSpecificTargetClasses() {
			return new Class[]{Payload2.class};
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			// target is a Payload2 instance
			return true;
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Payload2 payload2 = (Payload2)target;
			return new TypedValue(payload2.getField(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv,CodeFlow codeflow) {
			if (method == null) {
				try {
					method = Payload2.class.getDeclaredMethod("getField", String.class);
				}
				catch (Exception e) {
				}
			}
			String descriptor = codeflow.lastDescriptor();
			String memberDeclaringClassSlashedDescriptor = method.getDeclaringClass().getName().replace('.','/');
			if (descriptor == null) {
				codeflow.loadTarget(mv);
			}
			if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
				mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor, method.getName(),CodeFlow.createSignatureDescriptor(method),false);
		}
	}


	static class CompilableMapAccessor implements CompilablePropertyAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			return map.containsKey(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Map<?,?> map = (Map<?,?>) target;
			Object value = map.get(name);
			if (value == null && !map.containsKey(name)) {
				throw new MapAccessException(name);
			}
			return new TypedValue(value);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			Map<String,Object> map = (Map<String,Object>) target;
			map.put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] {Map.class};
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow codeflow) {
			String descriptor = codeflow.lastDescriptor();
			if (descriptor == null) {
				codeflow.loadTarget(mv);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get","(Ljava/lang/Object;)Ljava/lang/Object;",true);

//			if (method == null) {
//				try {
//					method = Payload2.class.getDeclaredMethod("getField", String.class);
//				} catch (Exception e) {}
//			}
//			String descriptor = codeflow.lastDescriptor();
//			String memberDeclaringClassSlashedDescriptor = method.getDeclaringClass().getName().replace('.','/');
//			if (descriptor == null) {
//				codeflow.loadTarget(mv);
//			}
//			if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
//				mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
//			}
//			mv.visitLdcInsn(propertyReference.getName());
//			mv.visitMethodInsn(INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor, method.getName(),CodeFlow.createDescriptor(method));
//			   6:	invokeinterface	#6,  2; //InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
		}
	}


	/**
	 * Exception thrown from {@code read} in order to reset a cached
	 * PropertyAccessor, allowing other accessors to have a try.
	 */
	@SuppressWarnings("serial")
	private static class MapAccessException extends AccessException {

		private final String key;

		public MapAccessException(String key) {
			super(null);
			this.key = key;
		}

		@Override
		public String getMessage() {
			return "Map does not contain a value for key '" + this.key + "'";
		}
	}

	
	// helpers

	private SpelNodeImpl getAst() {
		SpelExpression spelExpression = (SpelExpression)expression;
		SpelNode ast = spelExpression.getAST();
		return (SpelNodeImpl)ast;
	}

	private String stringify(Object object) {
		StringBuilder s = new StringBuilder();
		if (object instanceof List) {
			List<?> ls = (List<?>)object;
			for (Object l: ls) {
				s.append(l);
				s.append(" ");
			}
		}
		else if (object instanceof Object[]) {
			Object[] os = (Object[])object;
			for (Object o: os) {
				s.append(o);
				s.append(" ");
			}
		}
		else if (object instanceof int[]) {
			int[] is = (int[])object;
			for (int i: is) {
				s.append(i);
				s.append(" ");
			}
		}
		else {
			s.append(object.toString());
		}
		return s.toString().trim();
	}
	
	private void assertCanCompile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}
	
	private void assertCantCompile(Expression expression) {
		assertFalse(SpelCompiler.compile(expression));
	}
	
	private Expression parse(String expression) {
		return parser.parseExpression(expression);
	}
	
	private void assertGetValueFail(Expression expression) {
		try {
			Object o = expression.getValue();
			fail("Calling getValue on the expression should have failed but returned "+o);
		} catch (Exception ex) {
			// success!
		}
	}
	
	// test classes
		
	public static class Payload {
		Two[] DR = new Two[]{new Two()};
		public Two holder = new Two();
		
		public Two[] getDR() {
			return DR;
		}
	}
	
	public static class Payload2 {
		String var1 = "abc";
		String var2 = "def";
		public Object getField(String name) {
			if (name.equals("var1")) {
				return var1;
			} else if (name.equals("var2")) {
				return var2;
			}
			return null;
		}
	}

	public static class Payload2Holder {
		public Payload2 payload2 = new Payload2();
	}
	
	public static class Two {
		Three three = new Three();
		public Three getThree() {
			return three;
		}
		public String toString() {
			return "instanceof Two";
		}
	}
	
	public static class Three {
		double four = 0.04d;
		public double getFour() {
			return four;
		}
	}

	public static class TestClass1 {
		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";		
	}
	
	public static class TestClass4 {
		public boolean a,b;
		public boolean gettrue() { return true; }
		public boolean getfalse() { return false; }
		public boolean getA() { return a; }
		public boolean getB() { return b; }
	}
	
	public static class TestClass5 {
		public int i = 0;
		public String s = null;
		public static int _i = 0;
		public static String _s = null;
		
		public Object obj = null;
		
		public String field = null;
		
		public void reset() {
			i = 0;
			_i=0;
			s = null;
			_s = null;
			field = null;
		}
		
		public void one() { i = 1; }
		
		public static void two() { _i = 1; }
		
		public String three() { return "hello"; }
		public long four() { return 3277700L; }

		public static String five() { return "hello"; }
		public static long six() { return 3277700L; }
		
		public void seven(String toset) { s = toset; }
//		public void seven(Number n) { s = n.toString(); }
		
		public void takeNumber(Number n) { s = n.toString(); }
		public void takeString(String s) { this.s = s; }
		public static void eight(String toset) { _s = toset; }
		
		public void nine(int toset) { i = toset; }
		public static void ten(int toset) { _i = toset; }
		
		public void eleven(String... vargs) { 
			if (vargs==null) {
				s = "";
			}
			else {
				s = "";
				for (String varg: vargs) {
					s+=varg;
				}
			}
		}

		public void twelve(int... vargs) { 
			if (vargs==null) {
				i = 0;
			}
			else {
				i = 0;
				for (int varg: vargs) {
					i+=varg;
				}
			}
		}
	}
	
	public static class TestClass6 {
		public String orange = "value1";
		public static String apple = "value2";
		
		public long peach = 34L;
		
		public String getBanana() {
			return "value3";
		}

		public static String getPlum() {
			return "value4";
		}
	}
	
	public static class TestClass7 {
		public static String property;
		static {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}
		
		public static void reset() {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}
		
	}

	public static class TestClass8 {
		public int i;
		public String s;
		public double d;
		public boolean z;
		
		public TestClass8(int i, String s, double d, boolean z) {
			this.i = i;
			this.s = s;
			this.d = d;
			this.z = z;
		}
		
		public TestClass8() {
			
		}
		
		public TestClass8(Integer i) {
			this.i = i;
		}
		
		@SuppressWarnings("unused")
		private TestClass8(String a, String b) {
			this.s = a+b;
		}
	}
	
	@SuppressWarnings("unused")
	private static class TestClass9 {
		public TestClass9(int i) {}
	}
	
}
