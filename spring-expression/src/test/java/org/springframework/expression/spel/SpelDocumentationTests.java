/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;
import org.springframework.expression.spel.testresources.PlaceOfBirth;

/**
 * Test the examples specified in the documentation.
 *
 * NOTE: any outgoing changes from this file upon synchronizing with the repo may indicate that
 * you need to update the documentation too !
 *
 * @author Andy Clement
 */
public class SpelDocumentationTests extends ExpressionTestCase {

	static Inventor tesla ;
	static Inventor pupin ;

	static {
		GregorianCalendar c = new GregorianCalendar();
		c.set(1856, 7, 9);
		tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");
		tesla.setPlaceOfBirth(new PlaceOfBirth("SmilJan"));
		tesla.setInventions(new String[] { "Telephone repeater", "Rotating magnetic field principle",
				"Polyphase alternating-current system", "Induction motor", "Alternating-current power transmission",
				"Tesla coil transformer", "Wireless communication", "Radio", "Fluorescent lights" });

		pupin = new Inventor("Pupin", c.getTime(), "Idvor");
		pupin.setPlaceOfBirth(new PlaceOfBirth("Idvor"));

	}

	@SuppressWarnings("rawtypes")
	static class IEEE {
		private String name;


		public Inventor[] Members = new Inventor[1];
		public List Members2 = new ArrayList();
		public Map<String,Object> officers = new HashMap<String,Object>();

		public List<Map<String, Object>> reverse = new ArrayList<Map<String, Object>>();

		@SuppressWarnings("unchecked")
		IEEE() {
			officers.put("president",pupin);
			List linv = new ArrayList();
			linv.add(tesla);
			officers.put("advisors",linv);
			Members2.add(tesla);
			Members2.add(pupin);

			reverse.add(officers);
		}

		public boolean isMember(String name) {
			return true;
		}

		public String getName() { return name; }
		public void setName(String n) { this.name = n; }
	}

	@Test
	public void testMethodInvocation() {
		evaluate("'Hello World'.concat('!')","Hello World!",String.class);
	}

	@Test
	public void testBeanPropertyAccess() {
		evaluate("new String('Hello World'.bytes)","Hello World",String.class);
	}

	@Test
	public void testArrayLengthAccess() {
		evaluate("'Hello World'.bytes.length",11,Integer.class);
	}

	@Test
	public void testRootObject() throws Exception {
		GregorianCalendar c = new GregorianCalendar();
		c.set(1856, 7, 9);

		//  The constructor arguments are name, birthday, and nationaltiy.
		Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");

		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("name");

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(tesla);

		String name = (String) exp.getValue(context);
		Assert.assertEquals("Nikola Tesla",name);
	}

	@Test
	public void testEqualityCheck() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(tesla);

		Expression exp = parser.parseExpression("name == 'Nikola Tesla'");
		boolean isEqual = exp.getValue(context, Boolean.class);  // evaluates to true
		Assert.assertTrue(isEqual);
	}

	// Section 7.4.1

	@Test
	public void testXMLBasedConfig() {
		 evaluate("(T(java.lang.Math).random() * 100.0 )>0",true,Boolean.class);
	}

	// Section 7.5
	@Test
	public void testLiterals() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();

		String helloWorld = (String) parser.parseExpression("'Hello World'").getValue(); // evals to "Hello World"
		Assert.assertEquals("Hello World",helloWorld);

		double avogadrosNumber  = (Double) parser.parseExpression("6.0221415E+23").getValue();
		Assert.assertEquals(6.0221415E+23,avogadrosNumber);

		int maxValue = (Integer) parser.parseExpression("0x7FFFFFFF").getValue();  // evals to 2147483647
		Assert.assertEquals(Integer.MAX_VALUE,maxValue);

		boolean trueValue = (Boolean) parser.parseExpression("true").getValue();
		Assert.assertTrue(trueValue);

		Object nullValue = parser.parseExpression("null").getValue();
		Assert.assertNull(nullValue);
	}

	@Test
	public void testPropertyAccess() throws Exception {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		int year = (Integer) parser.parseExpression("Birthdate.Year + 1900").getValue(context); // 1856
		Assert.assertEquals(1856,year);

		String city = (String) parser.parseExpression("placeOfBirth.City").getValue(context);
		Assert.assertEquals("SmilJan",city);
	}

	@Test
	public void testPropertyNavigation() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();

		// Inventions Array
		StandardEvaluationContext teslaContext = TestScenarioCreator.getTestEvaluationContext();
//		teslaContext.setRootObject(tesla);

		// evaluates to "Induction motor"
		String invention = parser.parseExpression("inventions[3]").getValue(teslaContext, String.class);
		Assert.assertEquals("Induction motor",invention);

		// Members List
		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		IEEE ieee = new IEEE();
		ieee.Members[0]= tesla;
		societyContext.setRootObject(ieee);

		// evaluates to "Nikola Tesla"
		String name = parser.parseExpression("Members[0].Name").getValue(societyContext, String.class);
		Assert.assertEquals("Nikola Tesla",name);

		// List and Array navigation
		// evaluates to "Wireless communication"
		invention = parser.parseExpression("Members[0].Inventions[6]").getValue(societyContext, String.class);
		Assert.assertEquals("Wireless communication",invention);
	}


	@Test
	public void testDictionaryAccess() throws Exception {
		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());
		// Officer's Dictionary
		Inventor pupin = parser.parseExpression("officers['president']").getValue(societyContext, Inventor.class);
		assertThat(pupin.getName(), is("Pupin"));

		// evaluates to "Idvor"
		String city = parser.parseExpression("officers['president'].PlaceOfBirth.city").getValue(societyContext, String.class);
		assertThat(city, is("Idvor"));

		// setting values
		Inventor i = parser.parseExpression("officers['advisors'][0]").getValue(societyContext,Inventor.class);
		Assert.assertEquals("Nikola Tesla",i.getName());

		parser.parseExpression("officers['advisors'][0].PlaceOfBirth.Country").setValue(societyContext, "Croatia");

		Inventor i2 = parser.parseExpression("reverse[0]['advisors'][0]").getValue(societyContext,Inventor.class);
		Assert.assertEquals("Nikola Tesla",i2.getName());

	}

	// 7.5.3

	@Test
	public void testMethodInvocation2() throws Exception {
		// string literal, evaluates to "bc"
		String c = parser.parseExpression("'abc'.substring(1, 3)").getValue(String.class);
		Assert.assertEquals("bc",c);

		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());
		// evaluates to true
		boolean isMember = parser.parseExpression("isMember('Mihajlo Pupin')").getValue(societyContext, Boolean.class);
		Assert.assertTrue(isMember);
	}

	// 7.5.4.1

	@Test
	public void testRelationalOperators() throws Exception {
		boolean result = parser.parseExpression("2 == 2").getValue(Boolean.class);
		Assert.assertTrue(result);
		// evaluates to false
		result = parser.parseExpression("2 < -5.0").getValue(Boolean.class);
		Assert.assertFalse(result);

		// evaluates to true
		result = parser.parseExpression("'black' < 'block'").getValue(Boolean.class);
		Assert.assertTrue(result);
	}

	@Test
	public void testOtherOperators() throws Exception {
		// evaluates to false
		boolean falseValue = parser.parseExpression("'xyz' instanceof T(int)").getValue(Boolean.class);
		Assert.assertFalse(falseValue);

		// evaluates to true
		boolean trueValue = parser.parseExpression("'5.00' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
		Assert.assertTrue(trueValue);

		//evaluates to false
		falseValue = parser.parseExpression("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
		Assert.assertFalse(falseValue);
	}

	// 7.5.4.2

	@Test
	public void testLogicalOperators() throws Exception {

		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());

		// -- AND --

		// evaluates to false
		boolean falseValue = parser.parseExpression("true and false").getValue(Boolean.class);
		Assert.assertFalse(falseValue);
		// evaluates to true
		String expression =  "isMember('Nikola Tesla') and isMember('Mihajlo Pupin')";
		boolean trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);

		// -- OR --

		// evaluates to true
		trueValue = parser.parseExpression("true or false").getValue(Boolean.class);
		Assert.assertTrue(trueValue);

		// evaluates to true
		expression =  "isMember('Nikola Tesla') or isMember('Albert Einstien')";
		trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
		Assert.assertTrue(trueValue);

		// -- NOT --

		// evaluates to false
		falseValue = parser.parseExpression("!true").getValue(Boolean.class);
		Assert.assertFalse(falseValue);


		// -- AND and NOT --
		expression =  "isMember('Nikola Tesla') and !isMember('Mihajlo Pupin')";
		falseValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
		Assert.assertFalse(falseValue);
	}

	// 7.5.4.3

	@Test
	public void testNumericalOperators() throws Exception {
		// Addition
		int two = parser.parseExpression("1 + 1").getValue(Integer.class); // 2
		Assert.assertEquals(2,two);

		String testString = parser.parseExpression("'test' + ' ' + 'string'").getValue(String.class); // 'test string'
		Assert.assertEquals("test string",testString);

		// Subtraction
		int four =  parser.parseExpression("1 - -3").getValue(Integer.class); // 4
		Assert.assertEquals(4,four);

		double d = parser.parseExpression("1000.00 - 1e4").getValue(Double.class); // -9000
		Assert.assertEquals(-9000.0d,d);

		// Multiplication
		int six =  parser.parseExpression("-2 * -3").getValue(Integer.class); // 6
		Assert.assertEquals(6,six);

		double twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Double.class); // 24.0
		Assert.assertEquals(24.0d,twentyFour);

		// Division
		int minusTwo =  parser.parseExpression("6 / -3").getValue(Integer.class); // -2
		Assert.assertEquals(-2,minusTwo);

		double one = parser.parseExpression("8.0 / 4e0 / 2").getValue(Double.class); // 1.0
		Assert.assertEquals(1.0d,one);

		// Modulus
		int three =  parser.parseExpression("7 % 4").getValue(Integer.class); // 3
		Assert.assertEquals(3,three);

		int oneInt = parser.parseExpression("8 / 5 % 2").getValue(Integer.class); // 1
		Assert.assertEquals(1,oneInt);

		// Operator precedence
		int minusTwentyOne = parser.parseExpression("1+2-3*8").getValue(Integer.class); // -21
		Assert.assertEquals(-21,minusTwentyOne);
	}

	// 7.5.5

	@Test
	public void testAssignment() throws Exception {
		Inventor inventor = new Inventor();
		StandardEvaluationContext inventorContext = new StandardEvaluationContext();
		inventorContext.setRootObject(inventor);

		parser.parseExpression("foo").setValue(inventorContext, "Alexander Seovic2");

		Assert.assertEquals("Alexander Seovic2",parser.parseExpression("foo").getValue(inventorContext,String.class));
		// alternatively

		String aleks = parser.parseExpression("foo = 'Alexandar Seovic'").getValue(inventorContext, String.class);
		Assert.assertEquals("Alexandar Seovic",parser.parseExpression("foo").getValue(inventorContext,String.class));
		Assert.assertEquals("Alexandar Seovic",aleks);
	}

	// 7.5.6

	@Test
	public void testTypes() throws Exception {
		Class<?> dateClass = parser.parseExpression("T(java.util.Date)").getValue(Class.class);
		Assert.assertEquals(Date.class,dateClass);
		boolean trueValue = parser.parseExpression("T(java.math.RoundingMode).CEILING < T(java.math.RoundingMode).FLOOR").getValue(Boolean.class);
		Assert.assertTrue(trueValue);
	}

	// 7.5.7

	@Test
	public void testConstructors() throws Exception {
		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());
		Inventor einstein =
			   parser.parseExpression("new org.springframework.expression.spel.testresources.Inventor('Albert Einstein',new java.util.Date(), 'German')").getValue(Inventor.class);
		Assert.assertEquals("Albert Einstein", einstein.getName());
		//create new inventor instance within add method of List
		parser.parseExpression("Members2.add(new org.springframework.expression.spel.testresources.Inventor('Albert Einstein', 'German'))").getValue(societyContext);
	}

	// 7.5.8

	@Test
	public void testVariables() throws Exception {
		Inventor tesla = new Inventor("Nikola Tesla", "Serbian");
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("newName", "Mike Tesla");

		context.setRootObject(tesla);

		parser.parseExpression("foo = #newName").getValue(context);

		Assert.assertEquals("Mike Tesla",tesla.getFoo());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSpecialVariables() throws Exception {
		// create an array of integers
		List<Integer> primes = new ArrayList<Integer>();
		primes.addAll(Arrays.asList(2,3,5,7,11,13,17));

		// create parser and set variable 'primes' as the array of integers
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("primes",primes);

		// all prime numbers > 10 from the list (using selection ?{...})
		List<Integer> primesGreaterThanTen = (List<Integer>) parser.parseExpression("#primes.?[#this>10]").getValue(context);
		Assert.assertEquals("[11, 13, 17]",primesGreaterThanTen.toString());
	}

	// 7.5.9

	@Test
	public void testFunctions() throws Exception {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();

		context.registerFunction("reverseString",
		                         StringUtils.class.getDeclaredMethod("reverseString", new Class[] { String.class }));

		String helloWorldReversed = parser.parseExpression("#reverseString('hello world')").getValue(context, String.class);
		Assert.assertEquals("dlrow olleh",helloWorldReversed);
	}

	// 7.5.10

	@Test
	public void testTernary() throws Exception {
		String falseString = parser.parseExpression("false ? 'trueExp' : 'falseExp'").getValue(String.class);
		Assert.assertEquals("falseExp",falseString);

		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());


		parser.parseExpression("Name").setValue(societyContext, "IEEE");
		societyContext.setVariable("queryName", "Nikola Tesla");

		String expression = "isMember(#queryName)? #queryName + ' is a member of the ' " +
		             "+ Name + ' Society' : #queryName + ' is not a member of the ' + Name + ' Society'";

		String queryResultString = parser.parseExpression(expression).getValue(societyContext, String.class);
		Assert.assertEquals("Nikola Tesla is a member of the IEEE Society",queryResultString);
		// queryResultString = "Nikola Tesla is a member of the IEEE Society"
	}

	// 7.5.11

	@SuppressWarnings("unchecked")
	@Test
	public void testSelection() throws Exception {
		StandardEvaluationContext societyContext = new StandardEvaluationContext();
		societyContext.setRootObject(new IEEE());
		List<Inventor> list = (List<Inventor>) parser.parseExpression("Members2.?[nationality == 'Serbian']").getValue(societyContext);
		Assert.assertEquals(1,list.size());
		Assert.assertEquals("Nikola Tesla",list.get(0).getName());
	}

	// 7.5.12

	@Test
	public void testTemplating() throws Exception {
		String randomPhrase =
			   parser.parseExpression("random number is ${T(java.lang.Math).random()}", new TemplatedParserContext()).getValue(String.class);
		Assert.assertTrue(randomPhrase.startsWith("random number"));
	}

	static class TemplatedParserContext implements ParserContext {

		  public String getExpressionPrefix() {
		    return "${";
		  }

		  public String getExpressionSuffix() {
		    return "}";
		  }

		  public boolean isTemplate() {
		    return true;
		  }
	}

	static class StringUtils {

		  public static String reverseString(String input) {
		    StringBuilder backwards = new StringBuilder();
		    for (int i = 0; i < input.length(); i++) {
		      backwards.append(input.charAt(input.length() - 1 - i));
		    }
		    return backwards.toString();
		  }
	}

}

