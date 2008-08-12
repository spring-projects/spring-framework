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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.CacheablePropertyAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.PropertyReaderExecutor;
import org.springframework.expression.PropertyWriterExecutor;
import org.springframework.expression.spel.standard.StandardEvaluationContext;
import org.springframework.expression.spel.standard.StandardIndividualTypeConverter;

/**
 * Testcases showing the common scenarios/use-cases for picking up the expression language support.
 * The first test shows very basic usage, just drop it in and go.  By 'standard infrastructure', it means:<br>
 * <ul>
 * <li>The context classloader is used (so, the default classpath)
 * <li>Some basic type converters are included
 * <li>properties/methods/constructors are discovered and invoked using reflection
 * </ul>
 * The scenarios after that then how how to plug in extensions:<br>
 * <ul>
 * <li>Adding entries to the classpath that will be used to load types and define well known 'imports'
 * <li>Defining variables that are then accessible in the expression
 * <li>Changing the root context object against which non-qualified references are resolved
 * <li>Registering java methods as functions callable from the expression
 * <li>Adding a basic property resolver
 * <li>Adding an advanced (better performing) property resolver
 * <li>Adding your own type converter to support conversion between any types you like
 * </ul>
 * 
 * @author Andy Clement
 */
public class ExpressionLanguageScenarioTests extends ExpressionTestCase {

	/**
	 * Scenario: using the standard infrastructure and running simple expression evaluation.
	 */
	public void testScenario_UsingStandardInfrastructure() {
		try {
			// Create a parser
			SpelExpressionParser parser = new SpelExpressionParser();
			// Parse an expression
			Expression expr = parser.parseExpression("new String('hello world')");
			// Evaluate it using a 'standard' context
			Object value = expr.getValue();
			// They are reusable
			value = expr.getValue();
				
			assertEquals("hello world", value);
			assertEquals(String.class, value.getClass());
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}

	/**
	 * Scenario: using the standard context but adding a jar to the classpath and registering an import.
	 */
	public void testScenario_LoadingDifferentClassesAndUsingImports() {
		try {
			// Create a parser
			SpelExpressionParser parser = new SpelExpressionParser();
			// Use the standard evaluation context
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			// Set the classpath (creates a new classloader with this classpath and uses it)
			ctx.setClasspath("target/test-classes/testcode.jar");
			// Register an import (so types in a.b.c can be referred to by their short name)
			ctx.registerImport("a.b.c");

			// Parse an expression (here, PackagedType is in package a.b.c)
			Expression expr = parser.parseExpression("new PackagedType().sayHi('Andy')");

			// Evaluate the expression in our context
			Object value = expr.getValue(ctx);

			assertEquals("Hi! Andy", value);
			assertEquals(String.class, value.getClass());
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}

	/**
	 * Scenario: using the standard context but adding your own variables
	 */
	public void testScenario_DefiningVariablesThatWillBeAccessibleInExpressions() throws Exception {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("favouriteColour","blue");
		List<Integer> primes = new ArrayList<Integer>();
		primes.addAll(Arrays.asList(2,3,5,7,11,13,17));
		ctx.setVariable("primes",primes);
		
		Expression expr = parser.parseExpression("#favouriteColour");
		Object value = expr.getValue(ctx);
		assertEquals("blue", value);

		expr = parser.parseExpression("#primes.get(1)");
		value = expr.getValue(ctx);
		assertEquals(3, value);

		// all prime numbers > 10 from the list (using selection ?{...})
		expr = parser.parseExpression("#primes.?{#this>10}");
		value = expr.getValue(ctx);
		assertEquals("[11, 13, 17]", value.toString());			
	}

	
	static class TestClass {
		public String str;
		private int property;
		public int getProperty() { return property; }
		public void setProperty(int i) { property = i; }
	}
	
	/**
	 * Scenario: using your own root context object
	 */
	public void testScenario_UsingADifferentRootContextObject() throws Exception {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		TestClass tc = new TestClass();
		tc.setProperty(42);
		tc.str = "wibble";
		
		ctx.setRootObject(tc);
		
		// read it, set it, read it again
		Expression expr = parser.parseExpression("str");
		Object value = expr.getValue(ctx);
		assertEquals("wibble", value);			
		expr =  parser.parseExpression("str");
		expr.setValue(ctx,"wobble");
		expr = parser.parseExpression("str");
		value = expr.getValue(ctx);
		assertEquals("wobble", value);
		// or using assignment within the expression
		expr = parser.parseExpression("str='wabble'");
		value = expr.getValue(ctx);
		expr = parser.parseExpression("str");
		value = expr.getValue(ctx);
		assertEquals("wabble", value);
		
		// private property will be accessed through getter()
		expr = parser.parseExpression("property");
		value = expr.getValue(ctx);
		assertEquals(42, value);

		// ... and set through setter
		expr = parser.parseExpression("property=4");
		value = expr.getValue(ctx);
		expr = parser.parseExpression("property");
		value = expr.getValue(ctx);
		assertEquals(4,value);
	}
	
	public static String repeat(String s) { return s+s; }

	/**
	 * Scenario: using your own java methods and calling them from the expression
	 */
	public void testScenario_RegisteringJavaMethodsAsFunctionsAndCallingThem() throws SecurityException, NoSuchMethodException {
		try {
			// Create a parser
			SpelExpressionParser parser = new SpelExpressionParser();
			// Use the standard evaluation context
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			ctx.registerFunction("repeat",ExpressionLanguageScenarioTests.class.getDeclaredMethod("repeat",String.class));
			
			Expression expr = parser.parseExpression("#repeat('hello')");
			Object value = expr.getValue(ctx);
			assertEquals("hellohello", value);

		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}
	
	/**
	 * Scenario: add a property resolver that will get called in the resolver chain, this one only supports reading.
	 */
	public void testScenario_AddingYourOwnPropertyResolvers_1() throws SecurityException, NoSuchMethodException {
		try {
			// Create a parser
			SpelExpressionParser parser = new SpelExpressionParser();
			// Use the standard evaluation context
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			
			ctx.addPropertyAccessor(new FruitColourAccessor());
			Expression expr = parser.parseExpression("orange");
			Object value = expr.getValue(ctx);
			assertEquals(Color.orange,value);
			
			try {
				expr.setValue(ctx,Color.blue);
				fail("Should not be allowed to set oranges to be blue !");
			} catch (EvaluationException ee) {
				SpelException ele = (SpelException)ee;
				assertEquals(ele.getMessageUnformatted(),SpelMessages.PROPERTY_OR_FIELD_SETTER_NOT_FOUND);
			}

		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}

	/**
	 * Regardless of the current context object, or root context object, this resolver can tell you what colour a fruit is !
	 * It only supports property reading, not writing.  To support writing it would need to override canWrite() and write()
	 */
	static class FruitColourAccessor implements PropertyAccessor {

		private static Map<String,Color> propertyMap = new HashMap<String,Color>();

		static {
			propertyMap.put("banana",Color.yellow);
			propertyMap.put("apple",Color.red);
			propertyMap.put("orange",Color.orange);
		}

		/**
		 * Null means you might be able to read any property, if an earlier property resolver hasn't beaten you to it
		 */
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}
		
		public boolean canRead(EvaluationContext context, Object target, Object name) throws AccessException {
			return propertyMap.containsKey(name);
		}


		public Object read(EvaluationContext context, Object target, Object name) throws AccessException {
			return propertyMap.get(name);
		}

		public boolean canWrite(EvaluationContext context, Object target, Object name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, Object name, Object newValue)
				throws AccessException {
		}

	}


	/**
	 * Scenario: add an optimized property resolver.  Property resolution can be thought of it two parts: resolving (finding the property you mean) and accessing (reading or writing that property).
	 * In some cases the act of discovering which property is meant is expensive - and there is no benefit to rediscovering it every time the expression is evaluated as it will
	 * always be the same property.  For example, with reflection it can be expensive to find out which field on an object maps to the property, but it will always be the same field
	 * for each evaluation.  In these cases we use a Resolver/Executor based property accessor.  In this setup the property resolver does not immediately return the value of the property,
	 * instead it returns an executor object that can be used to read the property.  The executor can be cached and reused by SPEL so it does not go back to the resolver every time the
	 * expression is evaluated.  In this testcase we use this different accessor mechanism to return the colours of vegetables.
	 */
	public void testScenario_AddingYourOwnPropertyResolvers_2() throws SecurityException, NoSuchMethodException {
		try {
			// Create a parser
			SpelExpressionParser parser = new SpelExpressionParser();
			// Use the standard evaluation context
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			
			ctx.addPropertyAccessor(new VegetableColourAccessor());
			Expression expr = parser.parseExpression("pea");
			Object value = expr.getValue(ctx);
			assertEquals(Color.green,value);

			try {
				expr.setValue(ctx,Color.blue);
				fail("Should not be allowed to set peas to be blue !");
			} catch (EvaluationException ee) {
				SpelException ele = (SpelException)ee;
				assertEquals(ele.getMessageUnformatted(),SpelMessages.PROPERTY_OR_FIELD_SETTER_NOT_FOUND);
			}

		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}
	
	/**
	 * Regardless of the current context object, or root context object, this resolver can tell you what colour a vegetable is !
	 * It only supports property reading, not writing.
	 */
	static class VegetableColourAccessor extends CacheablePropertyAccessor {

		private static Map<String,Color> propertyMap = new HashMap<String,Color>();

		static {
			propertyMap.put("carrot",Color.orange);
			propertyMap.put("pea",Color.green);
		}

		/**
		 * Null means you might be able to read any property, if an earlier property resolver hasn't beaten you to it
		 */
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		/**
		 * Work out if we can resolve the named property and if so return an executor that can be cached and reused to
		 * discover the value.
		 */
		public PropertyReaderExecutor getReaderAccessor(EvaluationContext relatedContext, Object target, Object name) {
			if (propertyMap.containsKey(name)) {
				return new VegetableColourExecutor(propertyMap.get(name));
			}
			return null;
		}

		public PropertyWriterExecutor getWriterAccessor(EvaluationContext context, Object target, Object name) {
			return null;
		}
		
	}
	
	static class VegetableColourExecutor implements PropertyReaderExecutor {
		private Color colour;
		
		public VegetableColourExecutor(Color colour) {
			this.colour = colour;
		}

		public Object execute(EvaluationContext context, Object target) throws AccessException {
			return colour;
		}
		
	}
	
	/**
	 * Scenario: adding your own type converter
	 */
	public void testScenario_AddingYourOwnTypeConverter() throws SecurityException, NoSuchMethodException {
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			ctx.registerFunction("functionTakesColour",ExpressionLanguageScenarioTests.class.getDeclaredMethod("functionTakesColour",Color.class));

			Expression expr = parser.parseExpression("#functionTakesColour('orange')");
			try {
				@SuppressWarnings("unused")
				Object value = expr.getValue(ctx);
				fail("Should have failed, no type converter registered");
			} catch (EvaluationException ee) {}
			
			ctx.addTypeConverter(new StringToColorConverter());	
			Object value = expr.getValue(ctx);
			
			assertEquals(Color.orange,value);
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}
	
	public static Color functionTakesColour(Color c) {return c;}
	
	static class StringToColorConverter implements StandardIndividualTypeConverter {

		public Object convert(Object value) throws EvaluationException {
			String colourName = (String)value;
			if (colourName.equals("orange")) return Color.orange;
			else if (colourName.equals("red")) return Color.red;
			else return Color.blue; // hmm, quite a simplification here
		}

		public Class<?>[] getFrom() {
			return new Class[]{String.class};
		}

		public Class<?> getTo() {
			return Color.class;
		}
		
	}
	
}
