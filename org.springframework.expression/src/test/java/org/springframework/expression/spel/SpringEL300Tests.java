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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
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

	@Test
	@Ignore
	public void testSWF1086() {
		evaluate("printDouble(T(java.math.BigDecimal).valueOf(14.35))", "anullc", String.class);
	}
	
	@Test
	public void testDoubleCoercion() {
		evaluate("printDouble(14.35)", "14.35", String.class);
	}

	@Test
	public void testDoubleArrayCoercion() {
		evaluate("printDoubles(getDoublesAsStringList())", "{14.35, 15.45}", String.class);
	}

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
	

	@Test
	public void testNestedProperties_SPR6923() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Foo());
		String name = null;
		Expression expr = null;
		
		expr = new SpelExpressionParser().parseRaw("resource.resource.server");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("abc",name);
	}
	
	static class Foo {
		public ResourceSummary resource = new ResourceSummary();
	}
	
	static class ResourceSummary {
		ResourceSummary() {
			this.resource = new Resource();
		}
		private final Resource resource;
		public Resource getResource() {
	        return resource;
	    }
	}
	
	static class Resource {
		public String getServer() {
			return "abc";
		}
	}

	/** Should be accessing Goo.getKey because 'bar' field evaluates to "key" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_1() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[bar]");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("hello",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		Assert.assertEquals("hello",name);
	}

	/** Should be accessing Goo.getKey because 'bar' variable evaluates to "key" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_2() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		eContext.setVariable("bar","key");
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("hello",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		Assert.assertEquals("hello",name);
	}
	
	/** $ related identifiers */
	@SuppressWarnings("unchecked")
	@Test
	public void testDollarPrefixedIdentifier_SPR7100() {
		Holder h = new Holder();
		StandardEvaluationContext eContext = new StandardEvaluationContext(h);
		eContext.addPropertyAccessor(new MapAccessor());
		h.map.put("$foo","wibble");
		h.map.put("foo$bar","wobble");
		h.map.put("foobar$$","wabble");
		h.map.put("$","wubble");
		h.map.put("$$","webble");
		h.map.put("$_$","tribble");
		String name = null;
		Expression expr = null;
		
		expr = new SpelExpressionParser().parseRaw("map.$foo");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("wibble",name);

		expr = new SpelExpressionParser().parseRaw("map.foo$bar");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("wobble",name);

		expr = new SpelExpressionParser().parseRaw("map.foobar$$");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("wabble",name);

		expr = new SpelExpressionParser().parseRaw("map.$");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("wubble",name);

		expr = new SpelExpressionParser().parseRaw("map.$$");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("webble",name);

		expr = new SpelExpressionParser().parseRaw("map.$_$");
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("tribble",name);
	}

	/** Should be accessing Goo.wibble field because 'bar' variable evaluates to "wibble" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_3() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		eContext.setVariable("bar","wibble");
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]");
		// will access the field 'wibble' and not use a getter
		name = expr.getValue(eContext,String.class);
		Assert.assertEquals("wobble",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		Assert.assertEquals("wobble",name);
	}
	
	/** Should be accessing (setting) Goo.wibble field because 'bar' variable evaluates to "wibble" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_4() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		eContext.setVariable("bar","wibble");
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]='world'");
		// will access the field 'wibble' and not use a getter
		expr.getValue(eContext,String.class);
		Assert.assertEquals("world",g.wibble);
		expr.getValue(eContext,String.class); // will be using the cached accessor this time
		Assert.assertEquals("world",g.wibble);
	}

	/** Should be accessing Goo.setKey field because 'bar' variable evaluates to "key" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_5() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[bar]='world'");
		expr.getValue(eContext,String.class);
		Assert.assertEquals("world",g.value);
		expr.getValue(eContext,String.class); // will be using the cached accessor this time
		Assert.assertEquals("world",g.value);
	}
	
	@Test
	public void testDollars() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m['$foo']");
		eContext.setVariable("file_name","$foo");
		Assert.assertEquals("wibble",expr.getValue(eContext,String.class));
	}
	
	@Test
	public void testDollars2() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m[$foo]");
		eContext.setVariable("file_name","$foo");
		Assert.assertEquals("wibble",expr.getValue(eContext,String.class));
	}
	
	static class XX {
		public Map<String,String> m;
		
		public String floo ="bar";
		
		public XX() {
			 m = new HashMap<String,String>();
			m.put("$foo","wibble");
			m.put("bar","siddle");
		}
	}

	static class Goo {
		
		public static Goo instance = new Goo();
		public String bar = "key";
		public String value = null;

		public String wibble = "wobble";
		
		public String getKey() {
			return "hello";
		}
		
		public void setKey(String s) {
			value = s;
		}
		
	}
	
	static class Holder {
		
		public Map map = new HashMap();
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
	
//	@Test
//	public void testFails() {
//		
//		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
//		evaluationContext.setVariable("target", new Foo2());
//		for (int i = 0; i < 300000; i++) {
//			evaluationContext.addPropertyAccessor(new MapAccessor());
//			ExpressionParser parser = new SpelExpressionParser();
//			Expression expression = parser.parseExpression("#target.execute(payload)");
//			Message message = new Message();
//			message.setPayload(i+"");
//			expression.getValue(evaluationContext, message);
//		}	
//	}
	
	static class Foo2 {
		public void execute(String str){
			System.out.println("Value: " + str);
		}
	}
	
	static class Message{
		private String payload;

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}
	}

	// bean resolver tests
	
	@Test
	public void beanResolution() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;

		// no resolver registered == exception
		try {
			expr = new SpelExpressionParser().parseRaw("@foo");
			Assert.assertEquals("custard",expr.getValue(eContext,String.class));
		} catch (SpelEvaluationException see) {
			Assert.assertEquals(SpelMessage.NO_BEAN_RESOLVER_REGISTERED,see.getMessageCode());
			Assert.assertEquals("foo",see.getInserts()[0]);
		}
		
		eContext.setBeanResolver(new MyBeanResolver());

		// bean exists
		expr = new SpelExpressionParser().parseRaw("@foo");
		Assert.assertEquals("custard",expr.getValue(eContext,String.class));

		// bean does not exist
		expr = new SpelExpressionParser().parseRaw("@bar");
		Assert.assertEquals(null,expr.getValue(eContext,String.class));

		// bean name will cause AccessException
		expr = new SpelExpressionParser().parseRaw("@goo");
		try {
			Assert.assertEquals(null,expr.getValue(eContext,String.class));
		} catch (SpelEvaluationException see) {
			Assert.assertEquals(SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,see.getMessageCode());
			Assert.assertEquals("goo",see.getInserts()[0]);
			Assert.assertTrue(see.getCause() instanceof AccessException);
			Assert.assertTrue(((AccessException)see.getCause()).getMessage().startsWith("DONT"));
		}
		
		// bean exists
		expr = new SpelExpressionParser().parseRaw("@'foo.bar'");
		Assert.assertEquals("trouble",expr.getValue(eContext,String.class));
		
		// bean exists
		try {
			expr = new SpelExpressionParser().parseRaw("@378");
			Assert.assertEquals("trouble",expr.getValue(eContext,String.class));
		} catch (SpelParseException spe) {
			Assert.assertEquals(SpelMessage.INVALID_BEAN_REFERENCE,spe.getMessageCode());
		}
	}
	
	static class MyBeanResolver implements BeanResolver {
		public Object resolve(EvaluationContext context, String beanname) throws AccessException {
			if (beanname.equals("foo")) {
				return "custard";
			} else if (beanname.equals("foo.bar")) {
				return "trouble";
			} else if (beanname.equals("goo")) {
				throw new AccessException("DONT ASK ME ABOUT GOO");
			}
			return null;
		}
	}
	
	// end bean resolver tests

	@Test
	public void elvis_SPR7209_1() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		
		// Different parts of elvis expression are null
		expr = new SpelExpressionParser().parseRaw("(?:'default')");
		Assert.assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("?:'default'");
		Assert.assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("?:");
		Assert.assertEquals(null, expr.getValue());

		// Different parts of ternary expression are null
		try {
			expr = new SpelExpressionParser().parseRaw("(?'abc':'default')");
			expr.getValue(eContext);
			Assert.fail();
		} catch (SpelEvaluationException see ) {
			Assert.assertEquals(SpelMessage.TYPE_CONVERSION_ERROR,see.getMessageCode());
		}
		expr = new SpelExpressionParser().parseRaw("(false?'abc':null)");
		Assert.assertEquals(null, expr.getValue());

		// Assignment
		try {
			expr = new SpelExpressionParser().parseRaw("(='default')");
			expr.getValue(eContext);
			Assert.fail();
		} catch (SpelEvaluationException see ) {
			Assert.assertEquals(SpelMessage.SETVALUE_NOT_SUPPORTED,see.getMessageCode());
		}
	}

	@Test
	public void elvis_SPR7209_2() {
		Expression expr = null;
		// Have empty string treated as null for elvis
		expr = new SpelExpressionParser().parseRaw("?:'default'");
		Assert.assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("\"\"?:'default'");
		Assert.assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("''?:'default'");
		Assert.assertEquals("default", expr.getValue());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testMapOfMap_SPR7244() throws Exception {
        Map<String,Object> map = new LinkedHashMap();
        map.put("uri", "http:");
        Map nameMap = new LinkedHashMap();
        nameMap.put("givenName", "Arthur");
        map.put("value", nameMap);
        
        StandardEvaluationContext ctx = new StandardEvaluationContext(map);
        ExpressionParser parser = new SpelExpressionParser();
        String el1 = "#root['value'].get('givenName')";
        Expression exp = parser.parseExpression(el1);
        Object evaluated = exp.getValue(ctx);
        Assert.assertEquals("Arthur", evaluated);

        String el2 = "#root['value']['givenName']";
        exp = parser.parseExpression(el2);
        evaluated = exp.getValue(ctx);
        Assert.assertEquals("Arthur",evaluated);
    }

	
}
