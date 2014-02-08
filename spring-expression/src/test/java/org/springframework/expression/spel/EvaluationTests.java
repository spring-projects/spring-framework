/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.expression.spel.testresources.TestPerson;

/**
 * Tests the evaluation of real expressions in a real context.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class EvaluationTests extends AbstractExpressionTests {

	@Test
	public void testCreateListsOnAttemptToIndexNull01() throws EvaluationException, ParseException {
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = parser.parseExpression("list[0]");
		TestClass testClass = new TestClass();
		Object o = null;
		o = expression.getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		o = parser.parseExpression("list[3]").getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		assertEquals(4, testClass.list.size());
		try {
			o = parser.parseExpression("list2[3]").getValue(new StandardEvaluationContext(testClass));
			fail();
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			// success!
		}
		o = parser.parseExpression("foo[3]").getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		assertEquals(4, testClass.getFoo().size());
	}

	@Test(expected = SpelEvaluationException.class)
	public void testCreateMapsOnAttemptToIndexNull01() throws Exception {
		TestClass testClass = new TestClass();
		StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Object o = null;
		o = parser.parseExpression("map['a']").getValue(ctx);
		assertNull(o);
		o = parser.parseExpression("map").getValue(ctx);
		assertNotNull(o);

		o = parser.parseExpression("map2['a']").getValue(ctx);
		// map2 should be null, there is no setter
	}

	// wibble2 should be null (cannot be initialized dynamically), there is no setter
	@Test(expected = SpelEvaluationException.class)
	public void testCreateObjectsOnAttemptToReferenceNull() throws Exception {
		TestClass testClass = new TestClass();
		StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Object o = null;
		o = parser.parseExpression("wibble.bar").getValue(ctx);
		assertEquals("hello", o);
		o = parser.parseExpression("wibble").getValue(ctx);
		assertNotNull(o);

		o = parser.parseExpression("wibble2.bar").getValue(ctx);
	}


	@SuppressWarnings("rawtypes")
	static class TestClass {
		public Foo wibble;
		private Foo wibble2;
		public Map map;
		public Map<String, Integer> mapStringToInteger;
		public List<String> list;
		public List list2;
		private Map map2;
		private List<String> foo;

		public Map getMap2() { return this.map2; }
		public Foo getWibble2() { return this.wibble2; }
		public List<String> getFoo() { return this.foo; }
		public void setFoo(List<String> newfoo) { this.foo = newfoo; }
	}

	public static class Foo {
		public Foo() {}
		public String bar = "hello";
	}


	@Test
	public void testElvis01() {
		evaluate("'Andy'?:'Dave'", "Andy", String.class);
		evaluate("null?:'Dave'", "Dave", String.class);
	}

	@Test
	public void testSafeNavigation() {
		evaluate("null?.null?.null", null, null);
	}

	@Test
	public void testRelOperatorGT01() {
		evaluate("3 > 6", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorLT01() {
		evaluate("3 < 6", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorLE01() {
		evaluate("3 <= 6", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorGE01() {
		evaluate("3 >= 6", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorGE02() {
		evaluate("3 >= 3", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof01() {
		evaluate("'xyz' instanceof T(int)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof04() {
		evaluate("null instanceof T(String)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof05() {
		evaluate("null instanceof T(Integer)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof06() {
		evaluateAndCheckError("'A' instanceof null", SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND, 15, "null");
	}

	@Test
	public void testRelOperatorsMatches01() {
		evaluate("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsMatches02() {
		evaluate("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorsMatches03() {
		evaluateAndCheckError("null matches '^.*$'", SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, 0, null);
	}

	@Test
	public void testRelOperatorsMatches04() {
		evaluateAndCheckError("'abc' matches null", SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, 14, null);
	}

	@Test
	public void testRelOperatorsMatches05() {
		evaluate("27 matches '^.*2.*$'", true, Boolean.class); // conversion int>string
	}

	// mixing operators
	@Test
	public void testMixingOperators01() {
		evaluate("true and 5>3", "true", Boolean.class);
	}

	// property access
	@Test
	public void testPropertyField01() {
		evaluate("name", "Nikola Tesla", String.class, false);
		// not writable because (1) name is private (2) there is no setter, only a getter
		evaluateAndCheckError("madeup", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 0, "madeup",
			"org.springframework.expression.spel.testresources.Inventor");
	}

	@Test
	public void testPropertyField02_SPR7100() {
		evaluate("_name", "Nikola Tesla", String.class);
		evaluate("_name_", "Nikola Tesla", String.class);
	}

	@Test
	public void testRogueTrailingDotCausesNPE_SPR6866() {
		try {
			new SpelExpressionParser().parseExpression("placeOfBirth.foo.");
			fail("Should have failed to parse");
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.OOD, spe.getMessageCode());
			assertEquals(16, spe.getPosition());
		}
	}

	// nested properties
	@Test
	public void testPropertiesNested01() {
		evaluate("placeOfBirth.city", "SmilJan", String.class, true);
	}

	@Test
	public void testPropertiesNested02() {
		evaluate("placeOfBirth.doubleIt(12)", "24", Integer.class);
	}

	@Test
	public void testPropertiesNested03() throws ParseException {
		try {
			new SpelExpressionParser().parseRaw("placeOfBirth.23");
			fail();
		} catch (SpelParseException spe) {
			assertEquals(spe.getMessageCode(), SpelMessage.UNEXPECTED_DATA_AFTER_DOT);
			assertEquals("23", spe.getInserts()[0]);
		}
	}

	// methods
	@Test
	public void testMethods01() {
		evaluate("echo(12)", "12", String.class);
	}

	@Test
	public void testMethods02() {
		evaluate("echo(name)", "Nikola Tesla", String.class);
	}

	// constructors
	@Test
	public void testConstructorInvocation01() {
		evaluate("new String('hello')", "hello", String.class);
	}

	@Test
	public void testConstructorInvocation05() {
		evaluate("new java.lang.String('foobar')", "foobar", String.class);
	}

	@Test
	public void testConstructorInvocation06() throws Exception {
		// repeated evaluation to drive use of cached executor
		SpelExpression expr = (SpelExpression) parser.parseExpression("new String('wibble')");
		String newString = expr.getValue(String.class);
		assertEquals("wibble", newString);
		newString = expr.getValue(String.class);
		assertEquals("wibble", newString);

		// not writable
		assertFalse(expr.isWritable(new StandardEvaluationContext()));

		// ast
		assertEquals("new String('wibble')", expr.toStringAST());
	}

	// unary expressions
	@Test
	public void testUnaryMinus01() {
		evaluate("-5", "-5", Integer.class);
	}

	@Test
	public void testUnaryPlus01() {
		evaluate("+5", "5", Integer.class);
	}

	@Test
	public void testUnaryNot01() {
		evaluate("!true", "false", Boolean.class);
	}

	@Test
	public void testUnaryNot02() {
		evaluate("!false", "true", Boolean.class);
	}

	@Test(expected = EvaluationException.class)
	public void testUnaryNotWithNullValue() {
		parser.parseExpression("!null").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testAndWithNullValueOnLeft() {
		parser.parseExpression("null and true").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testAndWithNullValueOnRight() {
		parser.parseExpression("true and null").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testOrWithNullValueOnLeft() {
		parser.parseExpression("null or false").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testOrWithNullValueOnRight() {
		parser.parseExpression("false or null").getValue();
	}

	// assignment
	@Test
	public void testAssignmentToVariables01() {
		evaluate("#var1='value1'", "value1", String.class);
	}

	@Test
	public void testTernaryOperator01() {
		evaluate("2>4?1:2", 2, Integer.class);
	}

	@Test
	public void testTernaryOperator02() {
		evaluate("'abc'=='abc'?1:2", 1, Integer.class);
	}

	@Test
	public void testTernaryOperator03() {
		// cannot convert String to boolean
		evaluateAndCheckError("'hello'?1:2", SpelMessage.TYPE_CONVERSION_ERROR);
	}

	@Test
	public void testTernaryOperator04() throws Exception {
		Expression expr = parser.parseExpression("1>2?3:4");
		assertFalse(expr.isWritable(eContext));
	}

	@Test
	public void testTernaryOperator05() {
		evaluate("1>2?#var=4:#var=5", 5, Integer.class);
		evaluate("3?:#var=5", 3, Integer.class);
		evaluate("null?:#var=5", 5, Integer.class);
		evaluate("2>4?(3>2?true:false):(5<3?true:false)", false, Boolean.class);
	}

	@Test(expected = EvaluationException.class)
	public void testTernaryOperatorWithNullValue() {
		parser.parseExpression("null ? 0 : 1").getValue();
	}

	@Test
	public void methodCallWithRootReferenceThroughParameter() {
		evaluate("placeOfBirth.doubleIt(inventions.length)", 18, Integer.class);
	}

	@Test
	public void ctorCallWithRootReferenceThroughParameter() {
		evaluate("new org.springframework.expression.spel.testresources.PlaceOfBirth(inventions[0].toString()).city",
			"Telephone repeater", String.class);
	}

	@Test
	public void fnCallWithRootReferenceThroughParameter() {
		evaluate("#reverseInt(inventions.length, inventions.length, inventions.length)", "int[3]{9,9,9}", int[].class);
	}

	@Test
	public void methodCallWithRootReferenceThroughParameterThatIsAFunctionCall() {
		evaluate("placeOfBirth.doubleIt(#reverseInt(inventions.length,2,3)[2])", 18, Integer.class);
	}

	@Test
	public void testIndexer03() {
		evaluate("'christian'[8]", "n", String.class);
	}

	@Test
	public void testIndexerError() {
		evaluateAndCheckError("new org.springframework.expression.spel.testresources.Inventor().inventions[1]",
			SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
	}

	@Test
	public void testStaticRef02() {
		evaluate("T(java.awt.Color).green.getRGB()!=0", "true", Boolean.class);
	}

	// variables and functions
	@Test
	public void testVariableAccess01() {
		evaluate("#answer", "42", Integer.class, true);
	}

	@Test
	public void testFunctionAccess01() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
	}

	@Test
	public void testFunctionAccess02() {
		evaluate("#reverseString('hello')", "olleh", String.class);
	}

	// type references
	@Test
	public void testTypeReferences01() {
		evaluate("T(java.lang.String)", "class java.lang.String", Class.class);
	}

	@Test
	public void testTypeReferencesAndQualifiedIdentifierCaching() throws Exception {
		SpelExpression expr = (SpelExpression) parser.parseExpression("T(java.lang.String)");
		assertFalse(expr.isWritable(new StandardEvaluationContext()));
		assertEquals("T(java.lang.String)", expr.toStringAST());
		assertEquals(String.class, expr.getValue(Class.class));
		// use cached QualifiedIdentifier:
		assertEquals("T(java.lang.String)", expr.toStringAST());
		assertEquals(String.class, expr.getValue(Class.class));
	}

	@Test
	public void testTypeReferencesPrimitive() {
		evaluate("T(int)", "int", Class.class);
		evaluate("T(byte)", "byte", Class.class);
		evaluate("T(char)", "char", Class.class);
		evaluate("T(boolean)", "boolean", Class.class);
		evaluate("T(long)", "long", Class.class);
		evaluate("T(short)", "short", Class.class);
		evaluate("T(double)", "double", Class.class);
		evaluate("T(float)", "float", Class.class);
	}

	@Test
	public void testTypeReferences02() {
		evaluate("T(String)", "class java.lang.String", Class.class);
	}

	@Test
	public void testStringType() {
		evaluateAndAskForReturnType("getPlaceOfBirth().getCity()", "SmilJan", String.class);
	}

	@Test
	public void testNumbers01() {
		evaluateAndAskForReturnType("3*4+5", 17, Integer.class);
		evaluateAndAskForReturnType("3*4+5", 17L, Long.class);
		evaluateAndAskForReturnType("65", 'A', Character.class);
		evaluateAndAskForReturnType("3*4+5", (short) 17, Short.class);
		evaluateAndAskForReturnType("3*4+5", "17", String.class);
	}

	@Test
	public void testAdvancedNumerics() throws Exception {
		int twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Integer.class);
		assertEquals(24, twentyFour);
		double one = parser.parseExpression("8.0 / 5e0 % 2").getValue(Double.class);
		assertEquals(1.6d, one, 0);
		int o = parser.parseExpression("8.0 / 5e0 % 2").getValue(Integer.class);
		assertEquals(1, o);
		int sixteen = parser.parseExpression("-2 ^ 4").getValue(Integer.class);
		assertEquals(16, sixteen);
		int minusFortyFive = parser.parseExpression("1+2-3*8^2/2/2").getValue(Integer.class);
		assertEquals(-45, minusFortyFive);
	}

	@Test
	public void testComparison() throws Exception {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		boolean trueValue = parser.parseExpression("T(java.util.Date) == Birthdate.Class").getValue(context,
			Boolean.class);
		assertTrue(trueValue);
	}

	@Test
	public void testResolvingList() throws Exception {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		try {
			assertFalse(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
			fail("should have failed to find List");
		} catch (EvaluationException ee) {
			// success - List not found
		}
		((StandardTypeLocator) context.getTypeLocator()).registerImport("java.util");
		assertTrue(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
	}

	@Test
	public void testResolvingString() throws Exception {
		Class<?> stringClass = parser.parseExpression("T(String)").getValue(Class.class);
		assertEquals(String.class, stringClass);
	}

	/**
	 * SPR-6984: attempting to index a collection on write using an index that
	 * doesn't currently exist in the collection (address.crossStreets[0] below)
	 */
	@Test
	public void initializingCollectionElementsOnWrite() throws Exception {
		TestPerson person = new TestPerson();
		EvaluationContext context = new StandardEvaluationContext(person);
		SpelParserConfiguration config = new SpelParserConfiguration(true, true);
		ExpressionParser parser = new SpelExpressionParser(config);
		Expression expression = parser.parseExpression("name");
		expression.setValue(context, "Oleg");
		assertEquals("Oleg", person.getName());

		expression = parser.parseExpression("address.street");
		expression.setValue(context, "123 High St");
		assertEquals("123 High St", person.getAddress().getStreet());

		expression = parser.parseExpression("address.crossStreets[0]");
		expression.setValue(context, "Blah");
		assertEquals("Blah", person.getAddress().getCrossStreets().get(0));

		expression = parser.parseExpression("address.crossStreets[3]");
		expression.setValue(context, "Wibble");
		assertEquals("Blah", person.getAddress().getCrossStreets().get(0));
		assertEquals("Wibble", person.getAddress().getCrossStreets().get(3));
	}

	/**
	 * Verifies behavior requested in SPR-9613.
	 */
	@Test
	public void caseInsensitiveNullLiterals() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp;

		exp = parser.parseExpression("null");
		assertNull(exp.getValue());

		exp = parser.parseExpression("NULL");
		assertNull(exp.getValue());

		exp = parser.parseExpression("NuLl");
		assertNull(exp.getValue());
	}

	/**
	 * Verifies behavior requested in SPR-9621.
	 */
	@Test
	public void customMethodFilter() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();

		// Register a custom MethodResolver...
		List<MethodResolver> customResolvers = new ArrayList<MethodResolver>();
		customResolvers.add(new CustomMethodResolver());
		context.setMethodResolvers(customResolvers);

		// or simply...
		// context.setMethodResolvers(new ArrayList<MethodResolver>());

		// Register a custom MethodFilter...
		MethodFilter filter = new CustomMethodFilter();
		try {
			context.registerMethodFilter(String.class, filter);
			fail("should have failed");
		} catch (IllegalStateException ise) {
			assertEquals(
					"Method filter cannot be set as the reflective method resolver is not in use",
					ise.getMessage());
		}
	}

	static class CustomMethodResolver implements MethodResolver {

		@Override
		public MethodExecutor resolve(EvaluationContext context,
				Object targetObject, String name,
				List<TypeDescriptor> argumentTypes) throws AccessException {
			return null;
		}
	}

	static class CustomMethodFilter implements MethodFilter {

		@Override
		public List<Method> filter(List<Method> methods) {
			return null;
		}

	}

	// increment/decrement operators - SPR-9751

	static class Spr9751 {
		public String type = "hello";
		public BigDecimal bd = new BigDecimal("2");
		public double ddd = 2.0d;
		public float fff = 3.0f;
		public long lll = 66666L;
		public int iii = 42;
		public short sss = (short)15;
		public Spr9751_2 foo = new Spr9751_2();

		public void m() {}

		public int[] intArray = new int[]{1,2,3,4,5};
		public int index1 = 2;

		public Integer[] integerArray;
		public int index2 = 2;

		public List<String> listOfStrings;
		public int index3 = 0;

		public Spr9751() {
			integerArray = new Integer[5];
			integerArray[0] = 1;
			integerArray[1] = 2;
			integerArray[2] = 3;
			integerArray[3] = 4;
			integerArray[4] = 5;
			listOfStrings = new ArrayList<String>();
			listOfStrings.add("abc");
		}

		public static boolean isEven(int i) {
			return (i%2)==0;
		}
	}

	static class Spr9751_2 {
		public int iii = 99;
	}

	/**
	 * This test is checking that with the changes for 9751 that the refactoring in Indexer is
	 * coping correctly for references beyond collection boundaries.
	 */
	@Test
	public void collectionGrowingViaIndexer() {
		Spr9751 instance = new Spr9751();

		// Add a new element to the list
		StandardEvaluationContext ctx = new StandardEvaluationContext(instance);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e =  parser.parseExpression("listOfStrings[++index3]='def'");
		e.getValue(ctx);
		assertEquals(2,instance.listOfStrings.size());
		assertEquals("def",instance.listOfStrings.get(1));

		// Check reference beyond end of collection
		ctx = new StandardEvaluationContext(instance);
		parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		e =  parser.parseExpression("listOfStrings[0]");
		String value = e.getValue(ctx,String.class);
		assertEquals("abc",value);
		e =  parser.parseExpression("listOfStrings[1]");
		value = e.getValue(ctx,String.class);
		assertEquals("def",value);
		e =  parser.parseExpression("listOfStrings[2]");
		value = e.getValue(ctx,String.class);
		assertEquals("",value);

		// Now turn off growing and reference off the end
		ctx = new StandardEvaluationContext(instance);
		parser = new SpelExpressionParser(new SpelParserConfiguration(false, false));
		e =  parser.parseExpression("listOfStrings[3]");
		try {
			e.getValue(ctx,String.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS,see.getMessageCode());
		}
	}

	@Test
	public void limitCollectionGrowing() throws Exception {
		TestClass instance = new TestClass();
		StandardEvaluationContext ctx = new StandardEvaluationContext(instance);
		SpelExpressionParser parser = new SpelExpressionParser( new SpelParserConfiguration(true, true, 3));
		Expression expression = parser.parseExpression("foo[2]");
		expression.setValue(ctx, "2");
		assertThat(instance.getFoo().size(), equalTo(3));
		expression = parser.parseExpression("foo[3]");
		try {
			expression.setValue(ctx, "3");
		} catch(SpelEvaluationException see) {
			assertEquals(SpelMessage.UNABLE_TO_GROW_COLLECTION, see.getMessageCode());
			assertThat(instance.getFoo().size(), equalTo(3));
		}
	}

	// For now I am making #this not assignable
	@Test
	public void increment01root() {
		Integer i = 42;
		StandardEvaluationContext ctx = new StandardEvaluationContext(i);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e =  parser.parseExpression("#this++");
		assertEquals(42,i.intValue());
		try {
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
	}

	@Test
	public void increment02postfix() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		// BigDecimal
		e = parser.parseExpression("bd++");
		assertTrue(new BigDecimal("2").equals(helper.bd));
		BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
		assertTrue(new BigDecimal("2").equals(return_bd));
		assertTrue(new BigDecimal("3").equals(helper.bd));

		// double
		e = parser.parseExpression("ddd++");
		assertEquals(2.0d,helper.ddd,0d);
		double return_ddd = e.getValue(ctx,Double.TYPE);
		assertEquals(2.0d,return_ddd,0d);
		assertEquals(3.0d,helper.ddd,0d);

		// float
		e = parser.parseExpression("fff++");
		assertEquals(3.0f,helper.fff,0d);
		float return_fff = e.getValue(ctx,Float.TYPE);
		assertEquals(3.0f,return_fff,0d);
		assertEquals(4.0f,helper.fff,0d);

		// long
		e = parser.parseExpression("lll++");
		assertEquals(66666L,helper.lll);
		long return_lll = e.getValue(ctx,Long.TYPE);
		assertEquals(66666L,return_lll);
		assertEquals(66667L,helper.lll);

		// int
		e = parser.parseExpression("iii++");
		assertEquals(42,helper.iii);
		int return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,return_iii);
		assertEquals(43,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(43,return_iii);
		assertEquals(44,helper.iii);

		// short
		e = parser.parseExpression("sss++");
		assertEquals(15,helper.sss);
		short return_sss = e.getValue(ctx,Short.TYPE);
		assertEquals(15,return_sss);
		assertEquals(16,helper.sss);
	}

	@Test
	public void increment02prefix() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;


		// BigDecimal
		e = parser.parseExpression("++bd");
		assertTrue(new BigDecimal("2").equals(helper.bd));
		BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
		assertTrue(new BigDecimal("3").equals(return_bd));
		assertTrue(new BigDecimal("3").equals(helper.bd));

		// double
		e = parser.parseExpression("++ddd");
		assertEquals(2.0d,helper.ddd,0d);
		double return_ddd = e.getValue(ctx,Double.TYPE);
		assertEquals(3.0d,return_ddd,0d);
		assertEquals(3.0d,helper.ddd,0d);

		// float
		e = parser.parseExpression("++fff");
		assertEquals(3.0f,helper.fff,0d);
		float return_fff = e.getValue(ctx,Float.TYPE);
		assertEquals(4.0f,return_fff,0d);
		assertEquals(4.0f,helper.fff,0d);

		// long
		e = parser.parseExpression("++lll");
		assertEquals(66666L,helper.lll);
		long return_lll = e.getValue(ctx,Long.TYPE);
		assertEquals(66667L,return_lll);
		assertEquals(66667L,helper.lll);

		// int
		e = parser.parseExpression("++iii");
		assertEquals(42,helper.iii);
		int return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(43,return_iii);
		assertEquals(43,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(44,return_iii);
		assertEquals(44,helper.iii);

		// short
		e = parser.parseExpression("++sss");
		assertEquals(15,helper.sss);
		int return_sss = (Integer)e.getValue(ctx);
		assertEquals(16,return_sss);
		assertEquals(16,helper.sss);
	}

	@Test
	public void increment03() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		e = parser.parseExpression("m()++");
		try {
			e.getValue(ctx,Double.TYPE);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.OPERAND_NOT_INCREMENTABLE,see.getMessageCode());
		}

		e = parser.parseExpression("++m()");
		try {
			e.getValue(ctx,Double.TYPE);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.OPERAND_NOT_INCREMENTABLE,see.getMessageCode());
		}
	}


	@Test
	public void increment04() {
		Integer i = 42;
		StandardEvaluationContext ctx = new StandardEvaluationContext(i);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		try {
			Expression e =  parser.parseExpression("++1");
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
		try {
			Expression e =  parser.parseExpression("1++");
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
	}
	@Test
	public void decrement01root() {
		Integer i = 42;
		StandardEvaluationContext ctx = new StandardEvaluationContext(i);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e =  parser.parseExpression("#this--");
		assertEquals(42,i.intValue());
		try {
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
	}

	@Test
	public void decrement02postfix() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		// BigDecimal
		e = parser.parseExpression("bd--");
		assertTrue(new BigDecimal("2").equals(helper.bd));
		BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
		assertTrue(new BigDecimal("2").equals(return_bd));
		assertTrue(new BigDecimal("1").equals(helper.bd));

		// double
		e = parser.parseExpression("ddd--");
		assertEquals(2.0d,helper.ddd,0d);
		double return_ddd = e.getValue(ctx,Double.TYPE);
		assertEquals(2.0d,return_ddd,0d);
		assertEquals(1.0d,helper.ddd,0d);

		// float
		e = parser.parseExpression("fff--");
		assertEquals(3.0f,helper.fff,0d);
		float return_fff = e.getValue(ctx,Float.TYPE);
		assertEquals(3.0f,return_fff,0d);
		assertEquals(2.0f,helper.fff,0d);

		// long
		e = parser.parseExpression("lll--");
		assertEquals(66666L,helper.lll);
		long return_lll = e.getValue(ctx,Long.TYPE);
		assertEquals(66666L,return_lll);
		assertEquals(66665L,helper.lll);

		// int
		e = parser.parseExpression("iii--");
		assertEquals(42,helper.iii);
		int return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,return_iii);
		assertEquals(41,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(41,return_iii);
		assertEquals(40,helper.iii);

		// short
		e = parser.parseExpression("sss--");
		assertEquals(15,helper.sss);
		short return_sss = e.getValue(ctx,Short.TYPE);
		assertEquals(15,return_sss);
		assertEquals(14,helper.sss);
	}

	@Test
	public void decrement02prefix() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		// BigDecimal
		e = parser.parseExpression("--bd");
		assertTrue(new BigDecimal("2").equals(helper.bd));
		BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
		assertTrue(new BigDecimal("1").equals(return_bd));
		assertTrue(new BigDecimal("1").equals(helper.bd));

		// double
		e = parser.parseExpression("--ddd");
		assertEquals(2.0d,helper.ddd,0d);
		double return_ddd = e.getValue(ctx,Double.TYPE);
		assertEquals(1.0d,return_ddd,0d);
		assertEquals(1.0d,helper.ddd,0d);

		// float
		e = parser.parseExpression("--fff");
		assertEquals(3.0f,helper.fff,0d);
		float return_fff = e.getValue(ctx,Float.TYPE);
		assertEquals(2.0f,return_fff,0d);
		assertEquals(2.0f,helper.fff,0d);

		// long
		e = parser.parseExpression("--lll");
		assertEquals(66666L,helper.lll);
		long return_lll = e.getValue(ctx,Long.TYPE);
		assertEquals(66665L,return_lll);
		assertEquals(66665L,helper.lll);

		// int
		e = parser.parseExpression("--iii");
		assertEquals(42,helper.iii);
		int return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(41,return_iii);
		assertEquals(41,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(40,return_iii);
		assertEquals(40,helper.iii);

		// short
		e = parser.parseExpression("--sss");
		assertEquals(15,helper.sss);
		int return_sss = (Integer)e.getValue(ctx);
		assertEquals(14,return_sss);
		assertEquals(14,helper.sss);
	}

	@Test
	public void decrement03() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		e = parser.parseExpression("m()--");
		try {
			e.getValue(ctx,Double.TYPE);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.OPERAND_NOT_DECREMENTABLE,see.getMessageCode());
		}

		e = parser.parseExpression("--m()");
		try {
			e.getValue(ctx,Double.TYPE);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.OPERAND_NOT_DECREMENTABLE,see.getMessageCode());
		}
	}


	@Test
	public void decrement04() {
		Integer i = 42;
		StandardEvaluationContext ctx = new StandardEvaluationContext(i);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		try {
			Expression e =  parser.parseExpression("--1");
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
		try {
			Expression e =  parser.parseExpression("1--");
			e.getValue(ctx,Integer.class);
			fail();
		} catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NOT_ASSIGNABLE,see.getMessageCode());
		}
	}

	@Test
	public void incdecTogether() {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		// index1 is 2 at the start - the 'intArray[#root.index1++]' should not be evaluated twice!
		// intArray[2] is 3
		e = parser.parseExpression("intArray[#root.index1++]++");
		e.getValue(ctx,Integer.class);
		assertEquals(3,helper.index1);
		assertEquals(4,helper.intArray[2]);

		// index1 is 3 intArray[3] is 4
		e =  parser.parseExpression("intArray[#root.index1++]--");
		assertEquals(4,e.getValue(ctx,Integer.class).intValue());
		assertEquals(4,helper.index1);
		assertEquals(3,helper.intArray[3]);

		// index1 is 4, intArray[3] is 3
		e =  parser.parseExpression("intArray[--#root.index1]++");
		assertEquals(3,e.getValue(ctx,Integer.class).intValue());
		assertEquals(3,helper.index1);
		assertEquals(4,helper.intArray[3]);
	}




	private void expectFail(ExpressionParser parser, EvaluationContext eContext, String expressionString, SpelMessage messageCode) {
		try {
			Expression e = parser.parseExpression(expressionString);
			 SpelUtilities.printAbstractSyntaxTree(System.out, e);
			e.getValue(eContext);
			fail();
		} catch (SpelEvaluationException see) {
			see.printStackTrace();
			assertEquals(messageCode,see.getMessageCode());
		}
	}

	private void expectFailNotAssignable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
		expectFail(parser,eContext,expressionString,SpelMessage.NOT_ASSIGNABLE);
	}

	private void expectFailSetValueNotSupported(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
		expectFail(parser,eContext,expressionString,SpelMessage.SETVALUE_NOT_SUPPORTED);
	}

	private void expectFailNotIncrementable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
		expectFail(parser,eContext,expressionString,SpelMessage.OPERAND_NOT_INCREMENTABLE);
	}

	private void expectFailNotDecrementable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
		expectFail(parser,eContext,expressionString,SpelMessage.OPERAND_NOT_DECREMENTABLE);
	}

	// Verify how all the nodes behave with assignment (++, --, =)
	@Test
	public void incrementAllNodeTypes() throws SecurityException, NoSuchMethodException {
		Spr9751 helper = new Spr9751();
		StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression e = null;

		// BooleanLiteral
		expectFailNotAssignable(parser, ctx, "true++");
		expectFailNotAssignable(parser, ctx, "--false");
		expectFailSetValueNotSupported(parser, ctx, "true=false");

		// IntLiteral
		expectFailNotAssignable(parser, ctx, "12++");
		expectFailNotAssignable(parser, ctx, "--1222");
		expectFailSetValueNotSupported(parser, ctx, "12=16");

		// LongLiteral
		expectFailNotAssignable(parser, ctx, "1.0d++");
		expectFailNotAssignable(parser, ctx, "--3.4d");
		expectFailSetValueNotSupported(parser, ctx, "1.0d=3.2d");

		// NullLiteral
		expectFailNotAssignable(parser, ctx, "null++");
		expectFailNotAssignable(parser, ctx, "--null");
		expectFailSetValueNotSupported(parser, ctx, "null=null");
		expectFailSetValueNotSupported(parser, ctx, "null=123");

		// OpAnd
		expectFailNotAssignable(parser, ctx, "(true && false)++");
		expectFailNotAssignable(parser, ctx, "--(false AND true)");
		expectFailSetValueNotSupported(parser, ctx, "(true && false)=(false && true)");

		// OpDivide
		expectFailNotAssignable(parser, ctx, "(3/4)++");
		expectFailNotAssignable(parser, ctx, "--(2/5)");
		expectFailSetValueNotSupported(parser, ctx, "(1/2)=(3/4)");

		// OpEq
		expectFailNotAssignable(parser, ctx, "(3==4)++");
		expectFailNotAssignable(parser, ctx, "--(2==5)");
		expectFailSetValueNotSupported(parser, ctx, "(1==2)=(3==4)");

		// OpGE
		expectFailNotAssignable(parser, ctx, "(3>=4)++");
		expectFailNotAssignable(parser, ctx, "--(2>=5)");
		expectFailSetValueNotSupported(parser, ctx, "(1>=2)=(3>=4)");

		// OpGT
		expectFailNotAssignable(parser, ctx, "(3>4)++");
		expectFailNotAssignable(parser, ctx, "--(2>5)");
		expectFailSetValueNotSupported(parser, ctx, "(1>2)=(3>4)");

		// OpLE
		expectFailNotAssignable(parser, ctx, "(3<=4)++");
		expectFailNotAssignable(parser, ctx, "--(2<=5)");
		expectFailSetValueNotSupported(parser, ctx, "(1<=2)=(3<=4)");

		// OpLT
		expectFailNotAssignable(parser, ctx, "(3<4)++");
		expectFailNotAssignable(parser, ctx, "--(2<5)");
		expectFailSetValueNotSupported(parser, ctx, "(1<2)=(3<4)");

		// OpMinus
		expectFailNotAssignable(parser, ctx, "(3-4)++");
		expectFailNotAssignable(parser, ctx, "--(2-5)");
		expectFailSetValueNotSupported(parser, ctx, "(1-2)=(3-4)");

		// OpModulus
		expectFailNotAssignable(parser, ctx, "(3%4)++");
		expectFailNotAssignable(parser, ctx, "--(2%5)");
		expectFailSetValueNotSupported(parser, ctx, "(1%2)=(3%4)");

		// OpMultiply
		expectFailNotAssignable(parser, ctx, "(3*4)++");
		expectFailNotAssignable(parser, ctx, "--(2*5)");
		expectFailSetValueNotSupported(parser, ctx, "(1*2)=(3*4)");

		// OpNE
		expectFailNotAssignable(parser, ctx, "(3!=4)++");
		expectFailNotAssignable(parser, ctx, "--(2!=5)");
		expectFailSetValueNotSupported(parser, ctx, "(1!=2)=(3!=4)");

		// OpOr
		expectFailNotAssignable(parser, ctx, "(true || false)++");
		expectFailNotAssignable(parser, ctx, "--(false OR true)");
		expectFailSetValueNotSupported(parser, ctx, "(true || false)=(false OR true)");

		// OpPlus
		expectFailNotAssignable(parser, ctx, "(3+4)++");
		expectFailNotAssignable(parser, ctx, "--(2+5)");
		expectFailSetValueNotSupported(parser, ctx, "(1+2)=(3+4)");

		// RealLiteral
		expectFailNotAssignable(parser, ctx, "1.0d++");
		expectFailNotAssignable(parser, ctx, "--2.0d");
		expectFailSetValueNotSupported(parser, ctx, "(1.0d)=(3.0d)");
		expectFailNotAssignable(parser, ctx, "1.0f++");
		expectFailNotAssignable(parser, ctx, "--2.0f");
		expectFailSetValueNotSupported(parser, ctx, "(1.0f)=(3.0f)");

		// StringLiteral
		expectFailNotAssignable(parser, ctx, "'abc'++");
		expectFailNotAssignable(parser, ctx, "--'def'");
		expectFailSetValueNotSupported(parser, ctx, "'abc'='def'");

		// Ternary
		expectFailNotAssignable(parser, ctx, "(true?true:false)++");
		expectFailNotAssignable(parser, ctx, "--(true?true:false)");
		expectFailSetValueNotSupported(parser, ctx, "(true?true:false)=(true?true:false)");

		// TypeReference
		expectFailNotAssignable(parser, ctx, "T(String)++");
		expectFailNotAssignable(parser, ctx, "--T(Integer)");
		expectFailSetValueNotSupported(parser, ctx, "T(String)=T(Integer)");

		// OperatorBetween
		expectFailNotAssignable(parser, ctx, "(3 between {1,5})++");
		expectFailNotAssignable(parser, ctx, "--(3 between {1,5})");
		expectFailSetValueNotSupported(parser, ctx, "(3 between {1,5})=(3 between {1,5})");

		// OperatorInstanceOf
		expectFailNotAssignable(parser, ctx, "(type instanceof T(String))++");
		expectFailNotAssignable(parser, ctx, "--(type instanceof T(String))");
		expectFailSetValueNotSupported(parser, ctx, "(type instanceof T(String))=(type instanceof T(String))");

		// Elvis
		expectFailNotAssignable(parser, ctx, "(true?:false)++");
		expectFailNotAssignable(parser, ctx, "--(true?:false)");
		expectFailSetValueNotSupported(parser, ctx, "(true?:false)=(true?:false)");

		// OpInc
		expectFailNotAssignable(parser, ctx, "(iii++)++");
		expectFailNotAssignable(parser, ctx, "--(++iii)");
		expectFailSetValueNotSupported(parser, ctx, "(iii++)=(++iii)");

		// OpDec
		expectFailNotAssignable(parser, ctx, "(iii--)++");
		expectFailNotAssignable(parser, ctx, "--(--iii)");
		expectFailSetValueNotSupported(parser, ctx, "(iii--)=(--iii)");

		// OperatorNot
		expectFailNotAssignable(parser, ctx, "(!true)++");
		expectFailNotAssignable(parser, ctx, "--(!false)");
		expectFailSetValueNotSupported(parser, ctx, "(!true)=(!false)");

		// OperatorPower
		expectFailNotAssignable(parser, ctx, "(iii^2)++");
		expectFailNotAssignable(parser, ctx, "--(iii^2)");
		expectFailSetValueNotSupported(parser, ctx, "(iii^2)=(iii^3)");

		// Assign
		// iii=42
		e = parser.parseExpression("iii=iii++");
		assertEquals(42,helper.iii);
		int return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,helper.iii);
		assertEquals(42,return_iii);

		// Identifier
		e = parser.parseExpression("iii++");
		assertEquals(42,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,return_iii);
		assertEquals(43,helper.iii);

		e = parser.parseExpression("--iii");
		assertEquals(43,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,return_iii);
		assertEquals(42,helper.iii);

		e = parser.parseExpression("iii=99");
		assertEquals(42,helper.iii);
		return_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(99,return_iii);
		assertEquals(99,helper.iii);

		// CompoundExpression
		// foo.iii == 99
		e = parser.parseExpression("foo.iii++");
		assertEquals(99,helper.foo.iii);
		int return_foo_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(99,return_foo_iii);
		assertEquals(100,helper.foo.iii);

		e = parser.parseExpression("--foo.iii");
		assertEquals(100,helper.foo.iii);
		return_foo_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(99,return_foo_iii);
		assertEquals(99,helper.foo.iii);

		e = parser.parseExpression("foo.iii=999");
		assertEquals(99,helper.foo.iii);
		return_foo_iii = e.getValue(ctx,Integer.TYPE);
		assertEquals(999,return_foo_iii);
		assertEquals(999,helper.foo.iii);

		// ConstructorReference
		expectFailNotAssignable(parser, ctx, "(new String('abc'))++");
		expectFailNotAssignable(parser, ctx, "--(new String('abc'))");
		expectFailSetValueNotSupported(parser, ctx, "(new String('abc'))=(new String('abc'))");

		// MethodReference
		expectFailNotIncrementable(parser, ctx, "m()++");
		expectFailNotDecrementable(parser, ctx, "--m()");
		expectFailSetValueNotSupported(parser, ctx, "m()=m()");

		// OperatorMatches
		expectFailNotAssignable(parser, ctx, "('abc' matches '^a..')++");
		expectFailNotAssignable(parser, ctx, "--('abc' matches '^a..')");
		expectFailSetValueNotSupported(parser, ctx, "('abc' matches '^a..')=('abc' matches '^a..')");

		// Selection
		ctx.registerFunction("isEven", Spr9751.class.getDeclaredMethod("isEven", Integer.TYPE));

		expectFailNotIncrementable(parser, ctx, "({1,2,3}.?[#isEven(#this)])++");
		expectFailNotDecrementable(parser, ctx, "--({1,2,3}.?[#isEven(#this)])");
		expectFailNotAssignable(parser, ctx, "({1,2,3}.?[#isEven(#this)])=({1,2,3}.?[#isEven(#this)])");

		// slightly diff here because return value isn't a list, it is a single entity
		expectFailNotAssignable(parser, ctx, "({1,2,3}.^[#isEven(#this)])++");
		expectFailNotAssignable(parser, ctx, "--({1,2,3}.^[#isEven(#this)])");
		expectFailNotAssignable(parser, ctx, "({1,2,3}.^[#isEven(#this)])=({1,2,3}.^[#isEven(#this)])");

		expectFailNotAssignable(parser, ctx, "({1,2,3}.$[#isEven(#this)])++");
		expectFailNotAssignable(parser, ctx, "--({1,2,3}.$[#isEven(#this)])");
		expectFailNotAssignable(parser, ctx, "({1,2,3}.$[#isEven(#this)])=({1,2,3}.$[#isEven(#this)])");

		// FunctionReference
		expectFailNotAssignable(parser, ctx, "#isEven(3)++");
		expectFailNotAssignable(parser, ctx, "--#isEven(4)");
		expectFailSetValueNotSupported(parser, ctx, "#isEven(3)=#isEven(5)");

		// VariableReference
		ctx.setVariable("wibble", "hello world");
		expectFailNotIncrementable(parser, ctx, "#wibble++");
		expectFailNotDecrementable(parser, ctx, "--#wibble");
		e = parser.parseExpression("#wibble=#wibble+#wibble");
		String s = e.getValue(ctx,String.class);
		assertEquals("hello worldhello world",s);
		assertEquals("hello worldhello world",ctx.lookupVariable("wibble"));

		ctx.setVariable("wobble", 3);
		e = parser.parseExpression("#wobble++");
		assertEquals(3,((Integer)ctx.lookupVariable("wobble")).intValue());
		int r = e.getValue(ctx,Integer.TYPE);
		assertEquals(3,r);
		assertEquals(4,((Integer)ctx.lookupVariable("wobble")).intValue());

		e = parser.parseExpression("--#wobble");
		assertEquals(4,((Integer)ctx.lookupVariable("wobble")).intValue());
		r = e.getValue(ctx,Integer.TYPE);
		assertEquals(3,r);
		assertEquals(3,((Integer)ctx.lookupVariable("wobble")).intValue());

		e = parser.parseExpression("#wobble=34");
		assertEquals(3,((Integer)ctx.lookupVariable("wobble")).intValue());
		r = e.getValue(ctx,Integer.TYPE);
		assertEquals(34,r);
		assertEquals(34,((Integer)ctx.lookupVariable("wobble")).intValue());

		// Projection
		expectFailNotIncrementable(parser, ctx, "({1,2,3}.![#isEven(#this)])++"); // projection would be {false,true,false}
		expectFailNotDecrementable(parser, ctx, "--({1,2,3}.![#isEven(#this)])"); // projection would be {false,true,false}
		expectFailNotAssignable(parser, ctx, "({1,2,3}.![#isEven(#this)])=({1,2,3}.![#isEven(#this)])");

		// InlineList
		expectFailNotAssignable(parser, ctx, "({1,2,3})++");
		expectFailNotAssignable(parser, ctx, "--({1,2,3})");
		expectFailSetValueNotSupported(parser, ctx, "({1,2,3})=({1,2,3})");

		// BeanReference
		ctx.setBeanResolver(new MyBeanResolver());
		expectFailNotAssignable(parser, ctx, "@foo++");
		expectFailNotAssignable(parser, ctx, "--@foo");
		expectFailSetValueNotSupported(parser, ctx, "@foo=@bar");

		// PropertyOrFieldReference
		helper.iii = 42;
		e = parser.parseExpression("iii++");
		assertEquals(42,helper.iii);
		r = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,r);
		assertEquals(43,helper.iii);

		e = parser.parseExpression("--iii");
		assertEquals(43,helper.iii);
		r = e.getValue(ctx,Integer.TYPE);
		assertEquals(42,r);
		assertEquals(42,helper.iii);

		e = parser.parseExpression("iii=100");
		assertEquals(42,helper.iii);
		r = e.getValue(ctx,Integer.TYPE);
		assertEquals(100,r);
		assertEquals(100,helper.iii);

	}

	static class MyBeanResolver implements BeanResolver {

		@Override
		public Object resolve(EvaluationContext context, String beanName)
				throws AccessException {
			if (beanName.equals("foo") || beanName.equals("bar")) {
				return new Spr9751_2();
			}
			throw new AccessException("not heard of "+beanName);
		}

	}


}
