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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;

/**
 * Tests based on Jiras up to the release of Spring 3.0.0
 * 
 * @author Andy Clement
 */
public class SpringEL300Tests extends ExpressionTestCase {

	@Test
	public void testNPE_SPR5661() {
		evaluate("joinThreeStrings('a',null,'c')", "anullc", String.class);
	}

//	@Test
//	public void testSWF1086() {
//		evaluate("printDouble(T(java.math.BigDecimal).valueOf(14.35))", "anullc", String.class);
//	}
	
	@Test
	public void testSPR5899() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Spr5899Class());
		Expression expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull(12)");
		Assert.assertEquals(12,expr.getValue(eContext));
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull(null)");
		Assert.assertEquals(null,expr.getValue(eContext));
		try {
			expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull2(null)");
			expr.getValue();
			Assert.fail("Should have failed to find a method to which it could pass null");
		} catch (EvaluationException see) {
			// success
		}
		eContext.setTypeLocator(new MyTypeLocator());
		
		// varargs
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull3(null,'a','b')");
		Assert.assertEquals("ab",expr.getValue(eContext));
		
		// varargs 2 - null is packed into the varargs
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull3(12,'a',null,'c')");
		Assert.assertEquals("anullc",expr.getValue(eContext));
		
		// check we can find the ctor ok
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class().toString()");
		Assert.assertEquals("instance",expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null).toString()");
		Assert.assertEquals("instance",expr.getValue(eContext));

		// ctor varargs
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a','b').toString()");
		Assert.assertEquals("instance",expr.getValue(eContext));

		// ctor varargs 2
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a', null, 'b').toString()");
		Assert.assertEquals("instance",expr.getValue(eContext));
	}
	
	static class MyTypeLocator extends StandardTypeLocator {

		public Class<?> findType(String typename) throws EvaluationException {
			if (typename.equals("Spr5899Class")) {
				return Spr5899Class.class;
			}
			if (typename.equals("Outer")) {
				return Outer.class;
			}
			return super.findType(typename);
		}
	}

	static class Spr5899Class {
		 public Spr5899Class() {}
		 public Spr5899Class(Integer i) {  }
		 public Spr5899Class(Integer i, String... s) {  }
		 
		 public Integer tryToInvokeWithNull(Integer value) { return value; }
		 public Integer tryToInvokeWithNull2(int i) { return new Integer(i); }
		 public String tryToInvokeWithNull3(Integer value,String... strings) { 
			 StringBuilder sb = new StringBuilder();
			 for (int i=0;i<strings.length;i++) {
				 if (strings[i]==null) {
					 sb.append("null");
				 } else {
					 sb.append(strings[i]);
				 }
			 }
			 return sb.toString();
		 }
		 
		 public String toString() {
			 return "instance";
		 }
	}
	
	@Test
	public void testSPR5905_InnerTypeReferences() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Spr5899Class());
		Expression expr = new SpelExpressionParser().parseRaw("T(java.util.Map$Entry)");
		Assert.assertEquals(Map.Entry.class,expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("T(org.springframework.expression.spel.SpringEL300Tests$Outer$Inner).run()");
		Assert.assertEquals(12,expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new org.springframework.expression.spel.SpringEL300Tests$Outer$Inner().run2()");
		Assert.assertEquals(13,expr.getValue(eContext));
}
	
	static class Outer {
		static class Inner {
			public Inner() {}
			public static int run() {
				return 12;
			}
			public int run2() {
				return 13;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSPR5804() throws Exception {
		Map m = new HashMap();
		m.put("foo", "bar");
		StandardEvaluationContext eContext = new StandardEvaluationContext(m); // root is a map instance
		eContext.addPropertyAccessor(new MapAccessor());
		Expression expr = new SpelExpressionParser().parseRaw("['foo']");
		Assert.assertEquals("bar", expr.getValue(eContext));
	}
	
	@Test
	public void testSPR5847() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new TestProperties());
		String name = null;
		Expression expr = null;
		
		expr = new SpelExpressionParser().parseRaw("jdbcProperties['username']");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("Dave",name);
		
		expr = new SpelExpressionParser().parseRaw("jdbcProperties[username]");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("Dave",name);

		// MapAccessor required for this to work
		expr = new SpelExpressionParser().parseRaw("jdbcProperties.username");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("Dave",name);
		
		// --- dotted property names

		// lookup foo on the root, then bar on that, then use that as the key into jdbcProperties
		expr = new SpelExpressionParser().parseRaw("jdbcProperties[foo.bar]");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("Dave2",name);

		// key is foo.bar
		expr = new SpelExpressionParser().parseRaw("jdbcProperties['foo.bar']");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("Elephant",name);	
	}
	
	static class TestProperties {
		public Properties jdbcProperties = new Properties();
		public Properties foo = new Properties();
		TestProperties() {
			jdbcProperties.put("username","Dave");
			jdbcProperties.put("alias","Dave2");
			jdbcProperties.put("foo.bar","Elephant");
			foo.put("bar","alias");
		}
	}

	static class MapAccessor implements PropertyAccessor {

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map) target).containsKey(name));
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map) target).get(name));
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			((Map) target).put(name, newValue);
		}

		public Class[] getSpecificTargetClasses() {
			return new Class[] {Map.class};
		}
		
	}
	
	@Test
	public void testNPE_SPR5673() throws Exception {
		ParserContext hashes = TemplateExpressionParsingTests.HASH_DELIMITED_PARSER_CONTEXT;
		ParserContext dollars = TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT;
		
		checkTemplateParsing("abc${'def'} ghi","abcdef ghi");
		
		checkTemplateParsingError("abc${ {}( 'abc'","Missing closing ')' for '(' at position 8");
		checkTemplateParsingError("abc${ {}[ 'abc'","Missing closing ']' for '[' at position 8");
		checkTemplateParsingError("abc${ {}{ 'abc'","Missing closing '}' for '{' at position 8");
		checkTemplateParsingError("abc${ ( 'abc' }","Found closing '}' at position 14 but most recent opening is '(' at position 6");
		checkTemplateParsingError("abc${ '... }","Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ \"... }","Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ ) }","Found closing ')' at position 6 without an opening '('");
		checkTemplateParsingError("abc${ ] }","Found closing ']' at position 6 without an opening '['");
		checkTemplateParsingError("abc${ } }","No expression defined within delimiter '${}' at character 3");
		checkTemplateParsingError("abc$[ } ]",DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT,"Found closing '}' at position 6 without an opening '{'");
		
		checkTemplateParsing("abc ${\"def''g}hi\"} jkl","abc def'g}hi jkl");
		checkTemplateParsing("abc ${'def''g}hi'} jkl","abc def'g}hi jkl");
		checkTemplateParsing("}","}");
		checkTemplateParsing("${'hello'} world","hello world");
		checkTemplateParsing("Hello ${'}'}]","Hello }]");
		checkTemplateParsing("Hello ${'}'}","Hello }");
		checkTemplateParsingError("Hello ${ ( ","No ending suffix '}' for expression starting at character 6: ${ ( ");
		checkTemplateParsingError("Hello ${ ( }","Found closing '}' at position 11 but most recent opening is '(' at position 9");
		checkTemplateParsing("#{'Unable to render embedded object: File ({#this == 2}'}", hashes,"Unable to render embedded object: File ({#this == 2}");
		checkTemplateParsing("This is the last odd number in the list: ${listOfNumbersUpToTen.$[#this%2==1]}",dollars,"This is the last odd number in the list: 9");
		checkTemplateParsing("Hello ${'here is a curly bracket }'}",dollars,"Hello here is a curly bracket }");
		checkTemplateParsing("He${'${'}llo ${'here is a curly bracket }'}}",dollars,"He${llo here is a curly bracket }}");
		checkTemplateParsing("Hello ${'()()()}{}{}{][]{}{][}[][][}{()()'} World",dollars,"Hello ()()()}{}{}{][]{}{][}[][][}{()() World");
		checkTemplateParsing("Hello ${'inner literal that''s got {[(])]}an escaped quote in it'} World","Hello inner literal that's got {[(])]}an escaped quote in it World");
		checkTemplateParsingError("Hello ${","No ending suffix '}' for expression starting at character 6: ${");
	}
	
	@Test
	public void testAccessingNullPropertyViaReflection_SPR5663() throws AccessException {
		PropertyAccessor propertyAccessor = new ReflectivePropertyAccessor();
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		Assert.assertFalse(propertyAccessor.canRead(context, null, "abc"));
		Assert.assertFalse(propertyAccessor.canWrite(context, null, "abc"));
		try {
			propertyAccessor.read(context, null, "abc");
			Assert.fail("Should have failed with an AccessException");
		} catch (AccessException ae) {
			// success
		}
		try {
			propertyAccessor.write(context, null, "abc","foo");
			Assert.fail("Should have failed with an AccessException");
		} catch (AccessException ae) {
			// success
		}
	}
	
	
	// ---

	private void checkTemplateParsing(String expression, String expectedValue) throws Exception {
		checkTemplateParsing(expression,TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT, expectedValue);
	}
	
	private void checkTemplateParsing(String expression, ParserContext context, String expectedValue) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression(expression,context);
		Assert.assertEquals(expectedValue,expr.getValue(TestScenarioCreator.getTestEvaluationContext()));
	}

	private void checkTemplateParsingError(String expression,String expectedMessage) throws Exception {
		checkTemplateParsingError(expression, TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT,expectedMessage);
	}
	
	private void checkTemplateParsingError(String expression,ParserContext context, String expectedMessage) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		try {
			parser.parseExpression(expression,context);
			Assert.fail("Should have failed");
		} catch (Exception e) {
			if (!e.getMessage().equals(expectedMessage)) {
				e.printStackTrace();
			}
			Assert.assertEquals(expectedMessage,e.getMessage());
		}
	}
	
	private static final ParserContext DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT = new ParserContext() {
		public String getExpressionPrefix() {
			return "$[";
		}
		public String getExpressionSuffix() {
			return "]";
		}
		public boolean isTemplate() {
			return true;
		}
	};
	

}
