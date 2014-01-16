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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.ParserContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.expression.spel.testresources.le.div.mod.reserved.Reserver;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Reproduction tests cornering various SpEL JIRA issues.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Clark Duplichien
 * @author Phillip Webb
 */
public class SpelReproTests extends ExpressionTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNPE_SPR5661() {
		evaluate("joinThreeStrings('a',null,'c')", "anullc", String.class);
	}

	@Test
	public void testSWF1086() {
		evaluate("printDouble(T(java.math.BigDecimal).valueOf(14.35))", "14.35", String.class);
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
		assertEquals(12,expr.getValue(eContext));
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull(null)");
		assertEquals(null,expr.getValue(eContext));
		try {
			expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull2(null)");
			expr.getValue();
			fail("Should have failed to find a method to which it could pass null");
		}
		catch (EvaluationException see) {
			// success
		}
		eContext.setTypeLocator(new MyTypeLocator());

		// varargs
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull3(null,'a','b')");
		assertEquals("ab",expr.getValue(eContext));

		// varargs 2 - null is packed into the varargs
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull3(12,'a',null,'c')");
		assertEquals("anullc",expr.getValue(eContext));

		// check we can find the ctor ok
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class().toString()");
		assertEquals("instance",expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null).toString()");
		assertEquals("instance",expr.getValue(eContext));

		// ctor varargs
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a','b').toString()");
		assertEquals("instance",expr.getValue(eContext));

		// ctor varargs 2
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a', null, 'b').toString()");
		assertEquals("instance",expr.getValue(eContext));
	}

	static class MyTypeLocator extends StandardTypeLocator {

		@Override
		public Class<?> findType(String typeName) throws EvaluationException {
			if (typeName.equals("Spr5899Class")) {
				return Spr5899Class.class;
			}
			if (typeName.equals("Outer")) {
				return Outer.class;
			}
			return super.findType(typeName);
		}
	}

	static class Spr5899Class {

		 public Spr5899Class() {
		 }

		 public Spr5899Class(Integer i) {
		 }

		 public Spr5899Class(Integer i, String... s) {
		 }

		 public Integer tryToInvokeWithNull(Integer value) {
			 return value;
		 }

		 public Integer tryToInvokeWithNull2(int i) {
			 return new Integer(i);
		 }

		 public String tryToInvokeWithNull3(Integer value, String... strings) {
			 StringBuilder sb = new StringBuilder();
			 for (String string : strings) {
				 if (string == null) {
					 sb.append("null");
				 }
				 else {
					 sb.append(string);
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
		assertEquals(Map.Entry.class,expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("T(org.springframework.expression.spel.SpelReproTests$Outer$Inner).run()");
		assertEquals(12,expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new org.springframework.expression.spel.SpelReproTests$Outer$Inner().run2()");
		assertEquals(13,expr.getValue(eContext));
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

	@Test
	public void testSPR5804() throws Exception {
		Map<String,String> m = new HashMap<String,String>();
		m.put("foo", "bar");
		StandardEvaluationContext eContext = new StandardEvaluationContext(m); // root is a map instance
		eContext.addPropertyAccessor(new MapAccessor());
		Expression expr = new SpelExpressionParser().parseRaw("['foo']");
		assertEquals("bar", expr.getValue(eContext));
	}

	@Test
	public void testSPR5847() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new TestProperties());
		String name = null;
		Expression expr = null;

		expr = new SpelExpressionParser().parseRaw("jdbcProperties['username']");
		name = expr.getValue(eContext,String.class);
		assertEquals("Dave",name);

		expr = new SpelExpressionParser().parseRaw("jdbcProperties[username]");
		name = expr.getValue(eContext,String.class);
		assertEquals("Dave",name);

		// MapAccessor required for this to work
		expr = new SpelExpressionParser().parseRaw("jdbcProperties.username");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		assertEquals("Dave",name);

		// --- dotted property names

		// lookup foo on the root, then bar on that, then use that as the key into jdbcProperties
		expr = new SpelExpressionParser().parseRaw("jdbcProperties[foo.bar]");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		assertEquals("Dave2",name);

		// key is foo.bar
		expr = new SpelExpressionParser().parseRaw("jdbcProperties['foo.bar']");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext,String.class);
		assertEquals("Elephant",name);
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

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map<?,?>) target).containsKey(name));
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map<?,?>) target).get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			((Map<String, Object>) target).put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
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
		assertFalse(propertyAccessor.canRead(context, null, "abc"));
		assertFalse(propertyAccessor.canWrite(context, null, "abc"));
		try {
			propertyAccessor.read(context, null, "abc");
			fail("Should have failed with an AccessException");
		}
		catch (AccessException ae) {
			// success
		}
		try {
			propertyAccessor.write(context, null, "abc","foo");
			fail("Should have failed with an AccessException");
		}
		catch (AccessException ae) {
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
		assertEquals("abc",name);
	}

	static class Foo {

		public ResourceSummary resource = new ResourceSummary();
	}

	static class ResourceSummary {

		private final Resource resource;

		ResourceSummary() {
			this.resource = new Resource();
		}

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
		assertEquals("hello",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		assertEquals("hello",name);
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
		assertEquals("hello",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		assertEquals("hello",name);
	}

	/** $ related identifiers */
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
		assertEquals("wibble",name);

		expr = new SpelExpressionParser().parseRaw("map.foo$bar");
		name = expr.getValue(eContext,String.class);
		assertEquals("wobble",name);

		expr = new SpelExpressionParser().parseRaw("map.foobar$$");
		name = expr.getValue(eContext,String.class);
		assertEquals("wabble",name);

		expr = new SpelExpressionParser().parseRaw("map.$");
		name = expr.getValue(eContext,String.class);
		assertEquals("wubble",name);

		expr = new SpelExpressionParser().parseRaw("map.$$");
		name = expr.getValue(eContext,String.class);
		assertEquals("webble",name);

		expr = new SpelExpressionParser().parseRaw("map.$_$");
		name = expr.getValue(eContext,String.class);
		assertEquals("tribble",name);
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
		assertEquals("wobble",name);
		name = expr.getValue(eContext,String.class); // will be using the cached accessor this time
		assertEquals("wobble",name);
	}

	/**
	 * Should be accessing (setting) Goo.wibble field because 'bar' variable evaluates to
	 * "wibble"
	 */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_4() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		eContext.setVariable("bar","wibble");
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]='world'");
		// will access the field 'wibble' and not use a getter
		expr.getValue(eContext,String.class);
		assertEquals("world",g.wibble);
		expr.getValue(eContext,String.class); // will be using the cached accessor this time
		assertEquals("world",g.wibble);
	}

	/** Should be accessing Goo.setKey field because 'bar' variable evaluates to "key" */
	@Test
	public void testIndexingAsAPropertyAccess_SPR6968_5() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[bar]='world'");
		expr.getValue(eContext,String.class);
		assertEquals("world",g.value);
		expr.getValue(eContext,String.class); // will be using the cached accessor this time
		assertEquals("world",g.value);
	}

	@Test
	public void testDollars() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m['$foo']");
		eContext.setVariable("file_name","$foo");
		assertEquals("wibble",expr.getValue(eContext,String.class));
	}

	@Test
	public void testDollars2() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m[$foo]");
		eContext.setVariable("file_name","$foo");
		assertEquals("wibble",expr.getValue(eContext,String.class));
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

		public Map<String,String> map = new HashMap<String,String>();
	}

	// ---

	private void checkTemplateParsing(String expression, String expectedValue) throws Exception {
		checkTemplateParsing(expression,TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT, expectedValue);
	}

	private void checkTemplateParsing(String expression, ParserContext context, String expectedValue) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression(expression,context);
		assertEquals(expectedValue,expr.getValue(TestScenarioCreator.getTestEvaluationContext()));
	}

	private void checkTemplateParsingError(String expression,String expectedMessage) throws Exception {
		checkTemplateParsingError(expression, TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT,expectedMessage);
	}

	private void checkTemplateParsingError(String expression,ParserContext context, String expectedMessage) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		try {
			parser.parseExpression(expression,context);
			fail("Should have failed");
		}
		catch (Exception ex) {
			if (!ex.getMessage().equals(expectedMessage)) {
				ex.printStackTrace();
			}
			assertEquals(expectedMessage, ex.getMessage());
		}
	}

	private static final ParserContext DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT = new ParserContext() {

		@Override
		public String getExpressionPrefix() {
			return "$[";
		}

		@Override
		public String getExpressionSuffix() {
			return "]";
		}

		@Override
		public boolean isTemplate() {
			return true;
		}
	};

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
			assertEquals("custard",expr.getValue(eContext,String.class));
		}
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NO_BEAN_RESOLVER_REGISTERED,see.getMessageCode());
			assertEquals("foo",see.getInserts()[0]);
		}

		eContext.setBeanResolver(new MyBeanResolver());

		// bean exists
		expr = new SpelExpressionParser().parseRaw("@foo");
		assertEquals("custard",expr.getValue(eContext,String.class));

		// bean does not exist
		expr = new SpelExpressionParser().parseRaw("@bar");
		assertEquals(null,expr.getValue(eContext,String.class));

		// bean name will cause AccessException
		expr = new SpelExpressionParser().parseRaw("@goo");
		try {
			assertEquals(null,expr.getValue(eContext,String.class));
		}
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,see.getMessageCode());
			assertEquals("goo",see.getInserts()[0]);
			assertTrue(see.getCause() instanceof AccessException);
			assertTrue(((AccessException)see.getCause()).getMessage().startsWith("DONT"));
		}

		// bean exists
		expr = new SpelExpressionParser().parseRaw("@'foo.bar'");
		assertEquals("trouble",expr.getValue(eContext,String.class));

		// bean exists
		try {
			expr = new SpelExpressionParser().parseRaw("@378");
			assertEquals("trouble",expr.getValue(eContext,String.class));
		}
		catch (SpelParseException spe) {
			assertEquals(SpelMessage.INVALID_BEAN_REFERENCE,spe.getMessageCode());
		}
	}

	static class MyBeanResolver implements BeanResolver {

		@Override
		public Object resolve(EvaluationContext context, String beanName) throws AccessException {
			if (beanName.equals("foo")) {
				return "custard";
			}
			else if (beanName.equals("foo.bar")) {
				return "trouble";
			}
			else if (beanName.equals("goo")) {
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
		assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("?:'default'");
		assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("?:");
		assertEquals(null, expr.getValue());

		// Different parts of ternary expression are null
		try {
			expr = new SpelExpressionParser().parseRaw("(?'abc':'default')");
			expr.getValue(eContext);
			fail();
		}
		catch (SpelEvaluationException see ) {
			assertEquals(SpelMessage.TYPE_CONVERSION_ERROR,see.getMessageCode());
		}
		expr = new SpelExpressionParser().parseRaw("(false?'abc':null)");
		assertEquals(null, expr.getValue());

		// Assignment
		try {
			expr = new SpelExpressionParser().parseRaw("(='default')");
			expr.getValue(eContext);
			fail();
		}
		catch (SpelEvaluationException see ) {
			assertEquals(SpelMessage.SETVALUE_NOT_SUPPORTED,see.getMessageCode());
		}
	}

	@Test
	public void elvis_SPR7209_2() {
		Expression expr = null;
		// Have empty string treated as null for elvis
		expr = new SpelExpressionParser().parseRaw("?:'default'");
		assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("\"\"?:'default'");
		assertEquals("default", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("''?:'default'");
		assertEquals("default", expr.getValue());
	}

	@Test
	public void testMapOfMap_SPR7244() throws Exception {
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		map.put("uri", "http:");
		Map<String,String> nameMap = new LinkedHashMap<String,String>();
		nameMap.put("givenName", "Arthur");
		map.put("value", nameMap);

		StandardEvaluationContext ctx = new StandardEvaluationContext(map);
		ExpressionParser parser = new SpelExpressionParser();
		String el1 = "#root['value'].get('givenName')";
		Expression exp = parser.parseExpression(el1);
		Object evaluated = exp.getValue(ctx);
		assertEquals("Arthur", evaluated);

		String el2 = "#root['value']['givenName']";
		exp = parser.parseExpression(el2);
		evaluated = exp.getValue(ctx);
		assertEquals("Arthur",evaluated);
	}

	@Test
	public void testProjectionTypeDescriptors_1() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "ls.![#this.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		List<?> value = (List<?>)exp.getValue(ctx);
		// value is list containing [true,false]
		assertEquals(Boolean.class,value.get(0).getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(null, evaluated.getElementTypeDescriptor());
	}

	@Test
	public void testProjectionTypeDescriptors_2() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "as.![#this.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		Object[] value = (Object[])exp.getValue(ctx);
		// value is array containing [true,false]
		assertEquals(Boolean.class,value[0].getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(Boolean.class, evaluated.getElementTypeDescriptor().getType());
	}

	@Test
	public void testProjectionTypeDescriptors_3() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "ms.![key.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		List<?> value = (List<?>)exp.getValue(ctx);
		// value is list containing [true,false]
		assertEquals(Boolean.class,value.get(0).getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(null, evaluated.getElementTypeDescriptor());
	}

	static class C {
		public List<String> ls;
		public String[] as;
		public Map<String,String> ms;
		C() {
			ls = new ArrayList<String>();
			ls.add("abc");
			ls.add("def");
			as = new String[]{"abc","def"};
			ms = new HashMap<String,String>();
			ms.put("abc","xyz");
			ms.put("def","pqr");
		}
	}

	static class D {
		public String a;

		private D(String s) {
			a=s;
		}

		public String toString() {
			return "D("+a+")";
		}
	}

	@Test
	public void testGreaterThanWithNulls_SPR7840() throws Exception {
		List<D> list = new ArrayList<D>();
		list.add(new D("aaa"));
		list.add(new D("bbb"));
		list.add(new D(null));
		list.add(new D("ccc"));
		list.add(new D(null));
		list.add(new D("zzz"));

		StandardEvaluationContext ctx = new StandardEvaluationContext(list);
		SpelExpressionParser parser = new SpelExpressionParser();

		String el1 = "#root.?[a < 'hhh']";
		SpelExpression exp = parser.parseRaw(el1);
		Object value = exp.getValue(ctx);
		assertEquals("[D(aaa), D(bbb), D(null), D(ccc), D(null)]",value.toString());

		String el2 = "#root.?[a > 'hhh']";
		SpelExpression exp2 = parser.parseRaw(el2);
		Object value2 = exp2.getValue(ctx);
		assertEquals("[D(zzz)]",value2.toString());

		// trim out the nulls first
		String el3 = "#root.?[a!=null].?[a < 'hhh']";
		SpelExpression exp3 = parser.parseRaw(el3);
		Object value3 = exp3.getValue(ctx);
		assertEquals("[D(aaa), D(bbb), D(ccc)]",value3.toString());
	}

	/**
	 * Test whether {@link ReflectiveMethodResolver} follows Java Method Invocation
	 * Conversion order. And more precisely that widening reference conversion is 'higher'
	 * than a unboxing conversion.
	 */
	@Test
	public void testConversionPriority_8224() throws Exception {

		@SuppressWarnings("unused")
		class ConversionPriority1 {
			public int getX(Number i) {
				return 20;
			}

			public int getX(int i) {
				return 10;
			}
		}

		@SuppressWarnings("unused")
		class ConversionPriority2 {
			public int getX(int i) {
				return 10;
			}

			public int getX(Number i) {
				return 20;
			}
		}

		final Integer INTEGER = Integer.valueOf(7);

		EvaluationContext emptyEvalContext = new StandardEvaluationContext();

		List<TypeDescriptor> args = new ArrayList<TypeDescriptor>();
		args.add(TypeDescriptor.forObject(new Integer(42)));

		ConversionPriority1 target = new ConversionPriority1();
		MethodExecutor me = new ReflectiveMethodResolver(true).resolve(emptyEvalContext, target, "getX", args);
		// MethodInvoker chooses getX(int i) when passing Integer
		final int actual = (Integer) me.execute(emptyEvalContext, target, new Integer(42)).getValue();
		// Compiler chooses getX(Number i) when passing Integer
		final int compiler = target.getX(INTEGER);
		// Fails!
		assertEquals(compiler, actual);

		ConversionPriority2 target2 = new ConversionPriority2();
		MethodExecutor me2 = new ReflectiveMethodResolver(true).resolve(emptyEvalContext, target2, "getX", args);
		// MethodInvoker chooses getX(int i) when passing Integer
		int actual2 = (Integer) me2.execute(emptyEvalContext, target2, new Integer(42)).getValue();
		// Compiler chooses getX(Number i) when passing Integer
		int compiler2 = target2.getX(INTEGER);
		// Fails!
		assertEquals(compiler2, actual2);

	}

	/**
	 * Test whether {@link ReflectiveMethodResolver} handles Widening Primitive Conversion. That's passing an 'int' to a
	 * method accepting 'long' is ok.
	 */
	@Test
	public void testWideningPrimitiveConversion_8224() throws Exception {

		class WideningPrimitiveConversion {

			public int getX(long i) {
				return 10;
			}
		}

		final Integer INTEGER_VALUE = Integer.valueOf(7);

		WideningPrimitiveConversion target = new WideningPrimitiveConversion();

		EvaluationContext emptyEvalContext = new StandardEvaluationContext();

		List<TypeDescriptor> args = new ArrayList<TypeDescriptor>();
		args.add(TypeDescriptor.forObject(INTEGER_VALUE));

		MethodExecutor me = new ReflectiveMethodResolver(true).resolve(emptyEvalContext, target, "getX", args);

		final int actual = (Integer) me.execute(emptyEvalContext, target, INTEGER_VALUE).getValue();

		final int compiler = target.getX(INTEGER_VALUE);

		assertEquals(compiler, actual);
	}


	@Test
	public void varargsAndPrimitives_SPR8174() throws Exception  {
		EvaluationContext emptyEvalContext = new StandardEvaluationContext();
		List<TypeDescriptor> args = new ArrayList<TypeDescriptor>();

		args.add(TypeDescriptor.forObject(34L));
		ReflectionUtil<Integer> ru = new ReflectionUtil<Integer>();
		MethodExecutor me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"methodToCall",args);

		args.set(0,TypeDescriptor.forObject(23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, 45);

		args.set(0,TypeDescriptor.forObject(23f));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, 45f);

		args.set(0,TypeDescriptor.forObject(23d));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, 23d);

		args.set(0,TypeDescriptor.forObject((short)23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, (short)23);

		args.set(0,TypeDescriptor.forObject(23L));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, 23L);

		args.set(0,TypeDescriptor.forObject((char)65));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, (char)65);

		args.set(0,TypeDescriptor.forObject((byte)23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, (byte)23);

		args.set(0,TypeDescriptor.forObject(true));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"foo",args);
		me.execute(emptyEvalContext, ru, true);

		// trickier:
		args.set(0,TypeDescriptor.forObject(12));
		args.add(TypeDescriptor.forObject(23f));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext,ru,"bar",args);
		me.execute(emptyEvalContext, ru, 12,23f);
	}


	public class ReflectionUtil<T extends Number> {

		public Object methodToCall(T param) {
			System.out.println(param+" "+param.getClass());
			return "Object methodToCall(T param)";
		}

		public void foo(int... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(float...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(double...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(short...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(long...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(boolean...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(char...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
		public void foo(byte...array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void bar(int... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}
	}


	@Test
	public void testReservedWords_8228() throws Exception {
		// "DIV","EQ","GE","GT","LE","LT","MOD","NE","NOT"
		@SuppressWarnings("unused")
		class Reserver {
			public Reserver getReserver() {
				return this;
			}
			public String NE = "abc";
			public String ne = "def";

			public int DIV = 1;
			public int div = 3;

			public Map<String,String> m = new HashMap<String,String>();

			Reserver() {
				m.put("NE","xyz");
			}
		}

		StandardEvaluationContext ctx = new StandardEvaluationContext(new Reserver());
		SpelExpressionParser parser = new SpelExpressionParser();
		String ex = "getReserver().NE";
		SpelExpression exp = parser.parseRaw(ex);
		String value = (String) exp.getValue(ctx);
		assertEquals("abc", value);

		ex = "getReserver().ne";
		exp = parser.parseRaw(ex);
		value = (String) exp.getValue(ctx);
		assertEquals("def", value);

		ex = "getReserver().m[NE]";
		exp = parser.parseRaw(ex);
		value = (String) exp.getValue(ctx);
		assertEquals("xyz", value);

		ex = "getReserver().DIV";
		exp = parser.parseRaw(ex);
		assertEquals(1, exp.getValue(ctx));

		ex = "getReserver().div";
		exp = parser.parseRaw(ex);
		assertEquals(3, exp.getValue(ctx));

		exp = parser.parseRaw("NE");
		assertEquals("abc", exp.getValue(ctx));
	}

	@Test
	public void testReservedWordProperties_9862() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expression = parser.parseRaw(
				"T(org.springframework.expression.spel.testresources.le.div.mod.reserved.Reserver).CONST");
		Object value = expression.getValue(ctx);
		assertEquals(value, Reserver.CONST);
	}

	/**
	 * We add property accessors in the order:
	 * First, Second, Third, Fourth.
	 * They are not utilized in this order; preventing a priority or order of operations
	 * in evaluation of SPEL expressions for a given context.
	 */
	@Test
	public void testPropertyAccessorOrder_8211() {
		ExpressionParser expressionParser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext =
			new StandardEvaluationContext(new ContextObject());

		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("firstContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("secondContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("thirdContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("fourthContext"));

		assertEquals("first",
			expressionParser.parseExpression("shouldBeFirst").getValue(evaluationContext));
		assertEquals("second",
				expressionParser.parseExpression("shouldBeSecond").getValue(evaluationContext));
		assertEquals("third",
				expressionParser.parseExpression("shouldBeThird").getValue(evaluationContext));
		assertEquals("fourth",
				expressionParser.parseExpression("shouldBeFourth").getValue(evaluationContext));
	}

	class TestPropertyAccessor implements PropertyAccessor {

		private String mapName;

		public TestPropertyAccessor(String mapName) {
			this.mapName = mapName;
		}

		@SuppressWarnings("unchecked")
		public Map<String, String> getMap(Object target) {
			try {
				Field f = target.getClass().getDeclaredField(mapName);
				return (Map<String,String>) f.get(target);
			}
			catch (Exception ex) {
			}
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return getMap(target).containsKey(name);
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return getMap(target).containsKey(name);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] {ContextObject.class};
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(getMap(target).get(name));
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue)
			throws AccessException {
			getMap(target).put(name, (String) newValue);
		}
	}

	class ContextObject {

		public Map<String, String> firstContext = new HashMap<String, String>();
		public Map<String, String> secondContext = new HashMap<String, String>();
		public Map<String, String> thirdContext = new HashMap<String, String>();
		public Map<String, String> fourthContext = new HashMap<String, String>();

		public ContextObject() {
			firstContext.put("shouldBeFirst", "first");
			secondContext.put("shouldBeFirst", "second");
			thirdContext.put("shouldBeFirst", "third");
			fourthContext.put("shouldBeFirst", "fourth");

			secondContext.put("shouldBeSecond", "second");
			thirdContext.put("shouldBeSecond", "third");
			fourthContext.put("shouldBeSecond", "fourth");

			thirdContext.put("shouldBeThird", "third");
			fourthContext.put("shouldBeThird", "fourth");

			fourthContext.put("shouldBeFourth", "fourth");
		}

		public Map<String, String> getFirstContext() {return firstContext;}
		public Map<String, String> getSecondContext() {return secondContext;}
		public Map<String, String> getThirdContext() {return thirdContext;}
		public Map<String, String> getFourthContext() {return fourthContext;}
	}

	/**
	 * Test the ability to subclass the ReflectiveMethodResolver and change how it
	 * determines the set of methods for a type.
	 */
	@Test
	public void testCustomStaticFunctions_SPR9038() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		List<MethodResolver> methodResolvers = new ArrayList<MethodResolver>();
		methodResolvers.add(new ReflectiveMethodResolver() {
			@Override
			protected Method[] getMethods(Class<?> type) {
				try {
					return new Method[] {Integer.class.getDeclaredMethod("parseInt",
							new Class[] {String.class, Integer.TYPE })};
				}
				catch (NoSuchMethodException ex) {
					return new Method[0];
				}
			}
		});

		context.setMethodResolvers(methodResolvers);
		org.springframework.expression.Expression expression =
				parser.parseExpression("parseInt('-FF', 16)");

		Integer result = expression.getValue(context, "", Integer.class);
		assertEquals("Equal assertion failed: ", -255, result.intValue());
	}

	@Test
	public void testArray() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = null;
		Object result = null;

		expression = parser.parseExpression("new java.lang.Long[0].class");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [Ljava.lang.Long;", result.toString());

		expression = parser.parseExpression("T(java.lang.Long[])");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [Ljava.lang.Long;", result.toString());

		expression = parser.parseExpression("T(java.lang.String[][][])");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [[[Ljava.lang.String;", result.toString());
		assertEquals("T(java.lang.String[][][])",((SpelExpression)expression).toStringAST());

		expression = parser.parseExpression("new int[0].class");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [I", result.toString());

		expression = parser.parseExpression("T(int[][])");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [[I", result.toString());
	}

	@Test
	public void SPR_9486_floatFunctionResolverTest() throws Exception {
		Number expectedResult = Math.abs(-10.2f);
		ExpressionParser parser = new SpelExpressionParser();
		SPR_9486_FunctionsClass testObject = new SPR_9486_FunctionsClass();

		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("abs(-10.2f)");
		Number result = expression.getValue(context, testObject, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_floatFunctionResolverTest Test: ", expectedResult, result);
	}

	class SPR_9486_FunctionsClass {

		public int abs(int value) {
			return Math.abs(value);
		}

		public float abs(float value) {
			return Math.abs(value);
		}
	}

	@Test
	public void SPR_9486_addFloatWithDoubleTest() {
		Number expectedNumber = 10.21f + 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f + 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_addFloatWithDoubleTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_addFloatWithFloatTest() {
		Number expectedNumber = 10.21f + 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f + 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_addFloatWithFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_subtractFloatWithDoubleTest() {
		Number expectedNumber = 10.21f - 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f - 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_subtractFloatWithDoubleTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_subtractFloatWithFloatTest() {
		Number expectedNumber = 10.21f - 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f - 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_subtractFloatWithFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_multiplyFloatWithDoubleTest() {
		Number expectedNumber = 10.21f * 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f * 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for float multiplied by double Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_multiplyFloatWithFloatTest() {
		Number expectedNumber = 10.21f * 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f * 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for float multiply by another float Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatDivideByFloatTest() {
		Number expectedNumber = -10.21f/-10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f / -10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for float divide Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatDivideByDoubleTest() {
		Number expectedNumber = -10.21f/-10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f / -10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for float divide Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatEqFloatUnaryMinusTest() {
		Boolean expectedResult =  -10.21f == -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f == -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatEqFloatUnaryMinusTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatEqDoubleUnaryMinusTest() {
		Boolean expectedResult =  -10.21f == -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f == -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatEqDoubleUnaryMinusTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatEqFloatTest() {
		Boolean expectedResult =  10.215f == 10.2109f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.215f == 10.2109f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatEqFloatTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatEqDoubleTest() {
		Boolean expectedResult =  10.215f == 10.2109;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.215f == 10.2109");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatEqDoubleTest() Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatNotEqFloatTest() {
		Boolean expectedResult =  10.215f != 10.2109f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.215f != 10.2109f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatEqFloatTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatNotEqDoubleTest() {
		Boolean expectedResult =  10.215f != 10.2109;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.215f != 10.2109");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatNotEqDoubleTest Test: ", expectedResult, result);
	}



	@Test
	public void SPR_9486_floatLessThanFloatTest() {
		Boolean expectedNumber = -10.21f < -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f < -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatLessThanFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatLessThanDoubleTest() {
		Boolean expectedNumber = -10.21f < -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f < -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatLessThanDoubleTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatLessThanOrEqualFloatTest() {
		Boolean expectedNumber = -10.21f <= -10.22f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f <= -10.22f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatLessThanOrEqualFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatLessThanOrEqualDoubleTest() {
		Boolean expectedNumber = -10.21f <= -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f <= -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatLessThanOrEqualDoubleTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatGreaterThanFloatTest() {
		Boolean expectedNumber = -10.21f > -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f > -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatGreaterThanFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatGreaterThanDoubleTest() {
		Boolean expectedResult = -10.21f > -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f > -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatGreaterThanDoubleTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatGreaterThanOrEqualFloatTest() {
		Boolean expectedNumber = -10.21f >= -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f >= -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatGreaterThanFloatTest Test: ", expectedNumber, result);
	}

	@Test
	public void SPR_9486_floatGreaterThanEqualDoubleTest() {
		Boolean expectedResult = -10.21f >= -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("-10.21f >= -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals("Equal assertion failed for SPR_9486_floatGreaterThanDoubleTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatModulusFloatTest() {
		Number expectedResult = 10.21f % 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f % 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_floatModulusFloatTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatModulusDoubleTest() {
		Number expectedResult = 10.21f % 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f % 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_floatModulusDoubleTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatPowerFloatTest() {
		Number expectedResult = Math.pow(10.21f, -10.2f);
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f ^ -10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_floatPowerFloatTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9486_floatPowerDoubleTest() {
		Number expectedResult = Math.pow(10.21f, 10.2);
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		org.springframework.expression.Expression expression = parser.parseExpression("10.21f ^ 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals("Equal assertion failed for SPR_9486_floatPowerDoubleTest Test: ", expectedResult, result);
	}

	@Test
	public void SPR_9994_bridgeMethodsTest() throws Exception {
		ReflectivePropertyAccessor accessor = new ReflectivePropertyAccessor();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Object target = new GenericImplementation();
		TypedValue value = accessor.read(context, target , "property");
		assertEquals(Integer.class, value.getTypeDescriptor().getType());
	}

	@Test
	public void SPR_10162_onlyBridgeMethodTest() throws Exception {
		ReflectivePropertyAccessor accessor = new ReflectivePropertyAccessor();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Object target = new OnlyBridgeMethod();
		TypedValue value = accessor.read(context, target , "property");
		assertEquals(Integer.class, value.getTypeDescriptor().getType());
	}

	@Test
	public void SPR_10091_simpleTestValueType() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Class<?> valueType = parser.parseExpression("simpleProperty").getValueType(evaluationContext);
		assertNotNull(valueType);
	}

	@Test
	public void SPR_10091_simpleTestValue() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Object value = parser.parseExpression("simpleProperty").getValue(evaluationContext);
		assertNotNull(value);
	}

	@Test
	public void SPR_10091_primitiveTestValueType() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Class<?> valueType = parser.parseExpression("primitiveProperty").getValueType(evaluationContext);
		assertNotNull(valueType);
	}

	@Test
	public void SPR_10091_primitiveTestValue() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Object value = parser.parseExpression("primitiveProperty").getValue(evaluationContext);
		assertNotNull(value);
	}

	@Test
	public void SPR_10146_malformedExpressions() throws Exception {
		doTestSpr10146("/foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("*foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("%foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("<foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146(">foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("&&foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("||foo", "EL1070E:(pos 0): Problem parsing left operand");
		doTestSpr10146("&foo", "EL1069E:(pos 0): missing expected character '&'");
		doTestSpr10146("|foo", "EL1069E:(pos 0): missing expected character '|'");
	}

	private void doTestSpr10146(String expression, String expectedMessage) {
		thrown.expect(SpelParseException.class);
		thrown.expectMessage(expectedMessage);
		new SpelExpressionParser().parseExpression(expression);
	}

	@Test
	public void SPR_10125() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		String fromInterface = parser.parseExpression("T("+StaticFinalImpl1.class.getName()+").VALUE").getValue(context, String.class);
		assertThat(fromInterface, is("interfaceValue"));
		String fromClass = parser.parseExpression("T("+StaticFinalImpl2.class.getName()+").VALUE").getValue(context, String.class);
		assertThat(fromClass, is("interfaceValue"));
	}

	@Test
	public void SPR_10210() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("bridgeExample", new org.springframework.expression.spel.spr10210.D());
		Expression parseExpression = parser.parseExpression("#bridgeExample.bridgeMethod()");
		parseExpression.getValue(context);
	}

	@Test
	public void SPR_10328() throws Exception {
		thrown.expect(SpelParseException.class);
		thrown.expectMessage("EL1071E:(pos 2): A required selection expression has not been specified");
		Expression exp = parser.parseExpression("$[]");
		exp.getValue(Arrays.asList("foo", "bar", "baz"));
	}

	@Test
	public void SPR_10452() throws Exception {
		SpelParserConfiguration configuration = new SpelParserConfiguration(false, false);
		ExpressionParser parser = new SpelExpressionParser(configuration);

		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression spel = parser.parseExpression("#enumType.values()");

		context.setVariable("enumType", ABC.class);
		Object result = spel.getValue(context);
		assertNotNull(result);
		assertTrue(result.getClass().isArray());
		assertEquals(ABC.A, Array.get(result, 0));
		assertEquals(ABC.B, Array.get(result, 1));
		assertEquals(ABC.C, Array.get(result, 2));

		context.setVariable("enumType", XYZ.class);
		result = spel.getValue(context);
		assertNotNull(result);
		assertTrue(result.getClass().isArray());
		assertEquals(XYZ.X, Array.get(result, 0));
		assertEquals(XYZ.Y, Array.get(result, 1));
		assertEquals(XYZ.Z, Array.get(result, 2));
	}

	@Test
	public void SPR_9495() throws Exception {
		SpelParserConfiguration configuration = new SpelParserConfiguration(false, false);
		ExpressionParser parser = new SpelExpressionParser(configuration);

		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression spel = parser.parseExpression("#enumType.values()");

		context.setVariable("enumType", ABC.class);
		Object result = spel.getValue(context);
		assertNotNull(result);
		assertTrue(result.getClass().isArray());
		assertEquals(ABC.A, Array.get(result, 0));
		assertEquals(ABC.B, Array.get(result, 1));
		assertEquals(ABC.C, Array.get(result, 2));

		context.addMethodResolver(new MethodResolver() {
			@Override
			public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
				return new MethodExecutor() {
					@Override
					public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
						try {
							Method method = XYZ.class.getMethod("values");
							Object value = method.invoke(target, arguments);
							return new TypedValue(value, new TypeDescriptor(new MethodParameter(method, -1)).narrow(value));
						}
						catch (Exception ex) {
							throw new AccessException(ex.getMessage(), ex);
						}
					}
				};
			}
		});
		result = spel.getValue(context);
		assertNotNull(result);
		assertTrue(result.getClass().isArray());
		assertEquals(XYZ.X, Array.get(result, 0));
		assertEquals(XYZ.Y, Array.get(result, 1));
		assertEquals(XYZ.Z, Array.get(result, 2));
	}

	@Test
	public void SPR_10486() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		SPR10486 rootObject = new SPR10486();
		Expression classNameExpression = parser.parseExpression("class.name");
		Expression nameExpression = parser.parseExpression("name");
		assertThat(classNameExpression.getValue(context, rootObject),
				equalTo((Object) SPR10486.class.getName()));
		assertThat(nameExpression.getValue(context, rootObject),
				equalTo((Object) "name"));
	}

	@Test
	public void SPR_11142() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		SPR11142 rootObject = new SPR11142();
		Expression expression = parser.parseExpression("something");
		thrown.expect(SpelEvaluationException.class);
		thrown.expectMessage("property 'something' cannot be found");
		expression.getValue(context, rootObject);
	}


	private static enum ABC {A, B, C}

	private static enum XYZ {X, Y, Z}


	public static class BooleanHolder {

		private Boolean simpleProperty = true;

		private boolean primitiveProperty = true;

		public Boolean isSimpleProperty() {
			return simpleProperty;
		}

		public void setSimpleProperty(Boolean simpleProperty) {
			this.simpleProperty = simpleProperty;
		}

		public boolean isPrimitiveProperty() {
			return primitiveProperty;
		}

		public void setPrimitiveProperty(boolean primitiveProperty) {
			this.primitiveProperty = primitiveProperty;
		}
	}


	private static interface GenericInterface<T extends Number> {

		public T getProperty();
	}


	private static class GenericImplementation implements GenericInterface<Integer> {

		@Override
		public Integer getProperty() {
			return null;
		}
	}


	static class PackagePrivateClassWithGetter {

		public Integer getProperty() {
			return null;
		}
	}


	public static class OnlyBridgeMethod extends PackagePrivateClassWithGetter {
	}


	public static interface StaticFinal {

		public static final String VALUE = "interfaceValue";
	}


	public abstract static class AbstractStaticFinal implements StaticFinal {
	}


	public static class StaticFinalImpl1 extends AbstractStaticFinal implements StaticFinal {
	}


	public static class StaticFinalImpl2 extends AbstractStaticFinal {
	}


	public static class SPR10486 {

		private String name = "name";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	static class SPR11142 {

		public String isSomething() {
			return "";
		}
	}

}
