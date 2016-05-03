/*
 * Copyright 2002-2016 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.expression.ExpressionException;
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
 * @author Sam Brannen
 */
public class SpelReproTests extends AbstractExpressionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void NPE_SPR5661() {
		evaluate("joinThreeStrings('a',null,'c')", "anullc", String.class);
	}

	@Test
	public void SWF1086() {
		evaluate("printDouble(T(java.math.BigDecimal).valueOf(14.35))", "14.35", String.class);
	}

	@Test
	public void doubleCoercion() {
		evaluate("printDouble(14.35)", "14.35", String.class);
	}

	@Test
	public void doubleArrayCoercion() {
		evaluate("printDoubles(getDoublesAsStringList())", "{14.35, 15.45}", String.class);
	}

	@Test
	public void SPR5899() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Spr5899Class());
		Expression expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull(12)");
		assertEquals(12, expr.getValue(eContext));
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull(null)");
		assertEquals(null, expr.getValue(eContext));
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
		assertEquals("ab", expr.getValue(eContext));

		// varargs 2 - null is packed into the varargs
		expr = new SpelExpressionParser().parseRaw("tryToInvokeWithNull3(12,'a',null,'c')");
		assertEquals("anullc", expr.getValue(eContext));

		// check we can find the ctor ok
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class().toString()");
		assertEquals("instance", expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null).toString()");
		assertEquals("instance", expr.getValue(eContext));

		// ctor varargs
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a','b').toString()");
		assertEquals("instance", expr.getValue(eContext));

		// ctor varargs 2
		expr = new SpelExpressionParser().parseRaw("new Spr5899Class(null,'a', null, 'b').toString()");
		assertEquals("instance", expr.getValue(eContext));
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

		@Override
		public String toString() {
			return "instance";
		}
	}


	@Test
	public void SPR5905_InnerTypeReferences() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Spr5899Class());
		Expression expr = new SpelExpressionParser().parseRaw("T(java.util.Map$Entry)");
		assertEquals(Map.Entry.class, expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("T(org.springframework.expression.spel.SpelReproTests$Outer$Inner).run()");
		assertEquals(12, expr.getValue(eContext));

		expr = new SpelExpressionParser().parseRaw("new org.springframework.expression.spel.SpelReproTests$Outer$Inner().run2()");
		assertEquals(13, expr.getValue(eContext));
	}


	static class Outer {

		static class Inner {

			public Inner() {
			}

			public static int run() {
				return 12;
			}

			public int run2() {
				return 13;
			}
		}
	}


	@Test
	public void SPR5804() throws Exception {
		Map<String, String> m = new HashMap<String, String>();
		m.put("foo", "bar");
		StandardEvaluationContext eContext = new StandardEvaluationContext(m); // root is a map instance
		eContext.addPropertyAccessor(new MapAccessor());
		Expression expr = new SpelExpressionParser().parseRaw("['foo']");
		assertEquals("bar", expr.getValue(eContext));
	}

	@Test
	public void SPR5847() throws Exception {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new TestProperties());
		String name = null;
		Expression expr = null;

		expr = new SpelExpressionParser().parseRaw("jdbcProperties['username']");
		name = expr.getValue(eContext, String.class);
		assertEquals("Dave", name);

		expr = new SpelExpressionParser().parseRaw("jdbcProperties[username]");
		name = expr.getValue(eContext, String.class);
		assertEquals("Dave", name);

		// MapAccessor required for this to work
		expr = new SpelExpressionParser().parseRaw("jdbcProperties.username");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext, String.class);
		assertEquals("Dave", name);

		// --- dotted property names

		// lookup foo on the root, then bar on that, then use that as the key into
		// jdbcProperties
		expr = new SpelExpressionParser().parseRaw("jdbcProperties[foo.bar]");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext, String.class);
		assertEquals("Dave2", name);

		// key is foo.bar
		expr = new SpelExpressionParser().parseRaw("jdbcProperties['foo.bar']");
		eContext.addPropertyAccessor(new MapAccessor());
		name = expr.getValue(eContext, String.class);
		assertEquals("Elephant", name);
	}


	static class TestProperties {

		public Properties jdbcProperties = new Properties();
		public Properties foo = new Properties();

		TestProperties() {
			jdbcProperties.put("username", "Dave");
			jdbcProperties.put("alias", "Dave2");
			jdbcProperties.put("foo.bar", "Elephant");
			foo.put("bar", "alias");
		}
	}


	static class MapAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Map.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map<?, ?>) target).containsKey(name));
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map<?, ?>) target).get(name));
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
	}


	@Test
	public void NPE_SPR5673() throws Exception {
		ParserContext hashes = TemplateExpressionParsingTests.HASH_DELIMITED_PARSER_CONTEXT;
		ParserContext dollars = TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT;

		checkTemplateParsing("abc${'def'} ghi", "abcdef ghi");

		checkTemplateParsingError("abc${ {}( 'abc'", "Missing closing ')' for '(' at position 8");
		checkTemplateParsingError("abc${ {}[ 'abc'", "Missing closing ']' for '[' at position 8");
		checkTemplateParsingError("abc${ {}{ 'abc'", "Missing closing '}' for '{' at position 8");
		checkTemplateParsingError("abc${ ( 'abc' }", "Found closing '}' at position 14 but most recent opening is '(' at position 6");
		checkTemplateParsingError("abc${ '... }", "Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ \"... }", "Found non terminating string literal starting at position 6");
		checkTemplateParsingError("abc${ ) }", "Found closing ')' at position 6 without an opening '('");
		checkTemplateParsingError("abc${ ] }", "Found closing ']' at position 6 without an opening '['");
		checkTemplateParsingError("abc${ } }", "No expression defined within delimiter '${}' at character 3");
		checkTemplateParsingError("abc$[ } ]", DOLLARSQUARE_TEMPLATE_PARSER_CONTEXT, "Found closing '}' at position 6 without an opening '{'");

		checkTemplateParsing("abc ${\"def''g}hi\"} jkl", "abc def'g}hi jkl");
		checkTemplateParsing("abc ${'def''g}hi'} jkl", "abc def'g}hi jkl");
		checkTemplateParsing("}", "}");
		checkTemplateParsing("${'hello'} world", "hello world");
		checkTemplateParsing("Hello ${'}'}]", "Hello }]");
		checkTemplateParsing("Hello ${'}'}", "Hello }");
		checkTemplateParsingError("Hello ${ ( ", "No ending suffix '}' for expression starting at character 6: ${ ( ");
		checkTemplateParsingError("Hello ${ ( }", "Found closing '}' at position 11 but most recent opening is '(' at position 9");
		checkTemplateParsing("#{'Unable to render embedded object: File ({#this == 2}'}", hashes, "Unable to render embedded object: File ({#this == 2}");
		checkTemplateParsing("This is the last odd number in the list: ${listOfNumbersUpToTen.$[#this%2==1]}", dollars, "This is the last odd number in the list: 9");
		checkTemplateParsing("Hello ${'here is a curly bracket }'}", dollars, "Hello here is a curly bracket }");
		checkTemplateParsing("He${'${'}llo ${'here is a curly bracket }'}}", dollars, "He${llo here is a curly bracket }}");
		checkTemplateParsing("Hello ${'()()()}{}{}{][]{}{][}[][][}{()()'} World", dollars, "Hello ()()()}{}{}{][]{}{][}[][][}{()() World");
		checkTemplateParsing("Hello ${'inner literal that''s got {[(])]}an escaped quote in it'} World", "Hello inner literal that's got {[(])]}an escaped quote in it World");
		checkTemplateParsingError("Hello ${", "No ending suffix '}' for expression starting at character 6: ${");
	}

	@Test
	public void accessingNullPropertyViaReflection_SPR5663() throws AccessException {
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
			propertyAccessor.write(context, null, "abc", "foo");
			fail("Should have failed with an AccessException");
		}
		catch (AccessException ae) {
			// success
		}
	}

	@Test
	public void nestedProperties_SPR6923() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Foo());
		Expression expr = new SpelExpressionParser().parseRaw("resource.resource.server");
		String name = expr.getValue(eContext, String.class);
		assertEquals("abc", name);
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
	public void indexingAsAPropertyAccess_SPR6968_1() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[bar]");
		name = expr.getValue(eContext, String.class);
		assertEquals("hello", name);
		name = expr.getValue(eContext, String.class); // will be using the cached accessor this time
		assertEquals("hello", name);
	}

	/** Should be accessing Goo.getKey because 'bar' variable evaluates to "key" */
	@Test
	public void indexingAsAPropertyAccess_SPR6968_2() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		eContext.setVariable("bar", "key");
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]");
		name = expr.getValue(eContext, String.class);
		assertEquals("hello", name);
		name = expr.getValue(eContext, String.class); // will be using the cached accessor this time
		assertEquals("hello", name);
	}

	/** $ related identifiers */
	@Test
	public void dollarPrefixedIdentifier_SPR7100() {
		Holder h = new Holder();
		StandardEvaluationContext eContext = new StandardEvaluationContext(h);
		eContext.addPropertyAccessor(new MapAccessor());
		h.map.put("$foo", "wibble");
		h.map.put("foo$bar", "wobble");
		h.map.put("foobar$$", "wabble");
		h.map.put("$", "wubble");
		h.map.put("$$", "webble");
		h.map.put("$_$", "tribble");
		String name = null;
		Expression expr = null;

		expr = new SpelExpressionParser().parseRaw("map.$foo");
		name = expr.getValue(eContext, String.class);
		assertEquals("wibble", name);

		expr = new SpelExpressionParser().parseRaw("map.foo$bar");
		name = expr.getValue(eContext, String.class);
		assertEquals("wobble", name);

		expr = new SpelExpressionParser().parseRaw("map.foobar$$");
		name = expr.getValue(eContext, String.class);
		assertEquals("wabble", name);

		expr = new SpelExpressionParser().parseRaw("map.$");
		name = expr.getValue(eContext, String.class);
		assertEquals("wubble", name);

		expr = new SpelExpressionParser().parseRaw("map.$$");
		name = expr.getValue(eContext, String.class);
		assertEquals("webble", name);

		expr = new SpelExpressionParser().parseRaw("map.$_$");
		name = expr.getValue(eContext, String.class);
		assertEquals("tribble", name);
	}

	/** Should be accessing Goo.wibble field because 'bar' variable evaluates to "wibble" */
	@Test
	public void indexingAsAPropertyAccess_SPR6968_3() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new Goo());
		eContext.setVariable("bar", "wibble");
		String name = null;
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]");
		// will access the field 'wibble' and not use a getter
		name = expr.getValue(eContext, String.class);
		assertEquals("wobble", name);
		name = expr.getValue(eContext, String.class); // will be using the cached accessor this time
		assertEquals("wobble", name);
	}

	/**
	 * Should be accessing (setting) Goo.wibble field because 'bar' variable evaluates to
	 * "wibble"
	 */
	@Test
	public void indexingAsAPropertyAccess_SPR6968_4() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		eContext.setVariable("bar", "wibble");
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[#bar]='world'");
		// will access the field 'wibble' and not use a getter
		expr.getValue(eContext, String.class);
		assertEquals("world", g.wibble);
		expr.getValue(eContext, String.class); // will be using the cached accessor this time
		assertEquals("world", g.wibble);
	}

	/** Should be accessing Goo.setKey field because 'bar' variable evaluates to "key" */
	@Test
	public void indexingAsAPropertyAccess_SPR6968_5() {
		Goo g = Goo.instance;
		StandardEvaluationContext eContext = new StandardEvaluationContext(g);
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("instance[bar]='world'");
		expr.getValue(eContext, String.class);
		assertEquals("world", g.value);
		expr.getValue(eContext, String.class); // will be using the cached accessor this time
		assertEquals("world", g.value);
	}

	@Test
	public void dollars() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m['$foo']");
		eContext.setVariable("file_name", "$foo");
		assertEquals("wibble", expr.getValue(eContext, String.class));
	}

	@Test
	public void dollars2() {
		StandardEvaluationContext eContext = new StandardEvaluationContext(new XX());
		Expression expr = null;
		expr = new SpelExpressionParser().parseRaw("m[$foo]");
		eContext.setVariable("file_name", "$foo");
		assertEquals("wibble", expr.getValue(eContext, String.class));
	}


	static class XX {

		public Map<String, String> m;

		public String floo = "bar";

		public XX() {
			m = new HashMap<String, String>();
			m.put("$foo", "wibble");
			m.put("bar", "siddle");
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

		public Map<String, String> map = new HashMap<String, String>();
	}


	// ---

	private void checkTemplateParsing(String expression, String expectedValue) throws Exception {
		checkTemplateParsing(expression, TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT, expectedValue);
	}

	private void checkTemplateParsing(String expression, ParserContext context, String expectedValue) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression(expression, context);
		assertEquals(expectedValue, expr.getValue(TestScenarioCreator.getTestEvaluationContext()));
	}

	private void checkTemplateParsingError(String expression, String expectedMessage) throws Exception {
		checkTemplateParsingError(expression, TemplateExpressionParsingTests.DEFAULT_TEMPLATE_PARSER_CONTEXT, expectedMessage);
	}

	private void checkTemplateParsingError(String expression, ParserContext context, String expectedMessage) throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		try {
			parser.parseExpression(expression, context);
			fail("Should have failed with message: " + expectedMessage);
		}
		catch (Exception ex) {
			String message = ex.getMessage();
			if (ex instanceof ExpressionException) {
				message = ((ExpressionException) ex).getSimpleMessage();
			}
			if (!message.equals(expectedMessage)) {
				ex.printStackTrace();
			}
			assertThat(expectedMessage, equalTo(message));
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

		public void execute(String str) {
			System.out.println("Value: " + str);
		}
	}


	static class Message {

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
			assertEquals("custard", expr.getValue(eContext, String.class));
		}
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.NO_BEAN_RESOLVER_REGISTERED, see.getMessageCode());
			assertEquals("foo", see.getInserts()[0]);
		}

		eContext.setBeanResolver(new MyBeanResolver());

		// bean exists
		expr = new SpelExpressionParser().parseRaw("@foo");
		assertEquals("custard", expr.getValue(eContext, String.class));

		// bean does not exist
		expr = new SpelExpressionParser().parseRaw("@bar");
		assertEquals(null, expr.getValue(eContext, String.class));

		// bean name will cause AccessException
		expr = new SpelExpressionParser().parseRaw("@goo");
		try {
			assertEquals(null, expr.getValue(eContext, String.class));
		}
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION, see.getMessageCode());
			assertEquals("goo", see.getInserts()[0]);
			assertTrue(see.getCause() instanceof AccessException);
			assertTrue(see.getCause().getMessage().startsWith("DONT"));
		}

		// bean exists
		expr = new SpelExpressionParser().parseRaw("@'foo.bar'");
		assertEquals("trouble", expr.getValue(eContext, String.class));

		// bean exists
		try {
			expr = new SpelExpressionParser().parseRaw("@378");
			assertEquals("trouble", expr.getValue(eContext, String.class));
		}
		catch (SpelParseException spe) {
			assertEquals(SpelMessage.INVALID_BEAN_REFERENCE, spe.getMessageCode());
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
			else if (beanName.equals("&foo")) {
				return "foo factory";
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
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.TYPE_CONVERSION_ERROR, see.getMessageCode());
		}
		expr = new SpelExpressionParser().parseRaw("(false?'abc':null)");
		assertEquals(null, expr.getValue());

		// Assignment
		try {
			expr = new SpelExpressionParser().parseRaw("(='default')");
			expr.getValue(eContext);
			fail();
		}
		catch (SpelEvaluationException see) {
			assertEquals(SpelMessage.SETVALUE_NOT_SUPPORTED, see.getMessageCode());
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
	public void mapOfMap_SPR7244() throws Exception {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("uri", "http:");
		Map<String, String> nameMap = new LinkedHashMap<String, String>();
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
		assertEquals("Arthur", evaluated);
	}

	@Test
	public void projectionTypeDescriptors_1() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "ls.![#this.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		List<?> value = (List<?>) exp.getValue(ctx);
		// value is list containing [true,false]
		assertEquals(Boolean.class, value.get(0).getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(null, evaluated.getElementTypeDescriptor());
	}

	@Test
	public void projectionTypeDescriptors_2() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "as.![#this.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		Object[] value = (Object[]) exp.getValue(ctx);
		// value is array containing [true,false]
		assertEquals(Boolean.class, value[0].getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(Boolean.class, evaluated.getElementTypeDescriptor().getType());
	}

	@Test
	public void projectionTypeDescriptors_3() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext(new C());
		SpelExpressionParser parser = new SpelExpressionParser();
		String el1 = "ms.![key.equals('abc')]";
		SpelExpression exp = parser.parseRaw(el1);
		List<?> value = (List<?>) exp.getValue(ctx);
		// value is list containing [true,false]
		assertEquals(Boolean.class, value.get(0).getClass());
		TypeDescriptor evaluated = exp.getValueTypeDescriptor(ctx);
		assertEquals(null, evaluated.getElementTypeDescriptor());
	}


	static class C {

		public List<String> ls;
		public String[] as;
		public Map<String, String> ms;

		C() {
			ls = new ArrayList<String>();
			ls.add("abc");
			ls.add("def");
			as = new String[] { "abc", "def" };
			ms = new HashMap<String, String>();
			ms.put("abc", "xyz");
			ms.put("def", "pqr");
		}
	}


	static class D {

		public String a;

		private D(String s) {
			a = s;
		}

		@Override
		public String toString() {
			return "D(" + a + ")";
		}
	}


	@Test
	public void greaterThanWithNulls_SPR7840() throws Exception {
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
		assertEquals("[D(aaa), D(bbb), D(null), D(ccc), D(null)]", value.toString());

		String el2 = "#root.?[a > 'hhh']";
		SpelExpression exp2 = parser.parseRaw(el2);
		Object value2 = exp2.getValue(ctx);
		assertEquals("[D(zzz)]", value2.toString());

		// trim out the nulls first
		String el3 = "#root.?[a!=null].?[a < 'hhh']";
		SpelExpression exp3 = parser.parseRaw(el3);
		Object value3 = exp3.getValue(ctx);
		assertEquals("[D(aaa), D(bbb), D(ccc)]", value3.toString());
	}

	/**
	 * Test whether {@link ReflectiveMethodResolver} follows Java Method Invocation
	 * Conversion order. And more precisely that widening reference conversion is 'higher'
	 * than a unboxing conversion.
	 */
	@Test
	public void conversionPriority_8224() throws Exception {

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
	public void wideningPrimitiveConversion_8224() throws Exception {

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
	public void varargsAndPrimitives_SPR8174() throws Exception {
		EvaluationContext emptyEvalContext = new StandardEvaluationContext();
		List<TypeDescriptor> args = new ArrayList<TypeDescriptor>();

		args.add(TypeDescriptor.forObject(34L));
		ReflectionUtil<Integer> ru = new ReflectionUtil<Integer>();
		MethodExecutor me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "methodToCall", args);

		args.set(0, TypeDescriptor.forObject(23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, 45);

		args.set(0, TypeDescriptor.forObject(23f));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, 45f);

		args.set(0, TypeDescriptor.forObject(23d));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, 23d);

		args.set(0, TypeDescriptor.forObject((short) 23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, (short) 23);

		args.set(0, TypeDescriptor.forObject(23L));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, 23L);

		args.set(0, TypeDescriptor.forObject((char) 65));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, (char) 65);

		args.set(0, TypeDescriptor.forObject((byte) 23));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, (byte) 23);

		args.set(0, TypeDescriptor.forObject(true));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "foo", args);
		me.execute(emptyEvalContext, ru, true);

		// trickier:
		args.set(0, TypeDescriptor.forObject(12));
		args.add(TypeDescriptor.forObject(23f));
		me = new ReflectiveMethodResolver().resolve(emptyEvalContext, ru, "bar", args);
		me.execute(emptyEvalContext, ru, 12, 23f);
	}


	public class ReflectionUtil<T extends Number> {

		public Object methodToCall(T param) {
			System.out.println(param + " " + param.getClass());
			return "Object methodToCall(T param)";
		}

		public void foo(int... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(float... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(double... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(short... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(long... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(boolean... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(char... array) {
			if (array.length == 0) {
				throw new RuntimeException();
			}
		}

		public void foo(byte... array) {
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
	public void reservedWords_8228() throws Exception {
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

			public Map<String, String> m = new HashMap<String, String>();

			Reserver() {
				m.put("NE", "xyz");
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
	public void reservedWordProperties_9862() throws Exception {
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expression = parser.parseRaw("T(org.springframework.expression.spel.testresources.le.div.mod.reserved.Reserver).CONST");
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
	public void propertyAccessorOrder_8211() {
		ExpressionParser expressionParser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new ContextObject());

		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("firstContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("secondContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("thirdContext"));
		evaluationContext.addPropertyAccessor(new TestPropertyAccessor("fourthContext"));

		assertEquals("first", expressionParser.parseExpression("shouldBeFirst").getValue(evaluationContext));
		assertEquals("second", expressionParser.parseExpression("shouldBeSecond").getValue(evaluationContext));
		assertEquals("third", expressionParser.parseExpression("shouldBeThird").getValue(evaluationContext));
		assertEquals("fourth", expressionParser.parseExpression("shouldBeFourth").getValue(evaluationContext));
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
				return (Map<String, String>) f.get(target);
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
			return new Class<?>[] {ContextObject.class};
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(getMap(target).get(name));
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
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

		public Map<String, String> getFirstContext() {
			return firstContext;
		}

		public Map<String, String> getSecondContext() {
			return secondContext;
		}

		public Map<String, String> getThirdContext() {
			return thirdContext;
		}

		public Map<String, String> getFourthContext() {
			return fourthContext;
		}
	}


	/**
	 * Test the ability to subclass the ReflectiveMethodResolver and change how it
	 * determines the set of methods for a type.
	 */
	@Test
	public void customStaticFunctions_SPR9038() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		List<MethodResolver> methodResolvers = new ArrayList<MethodResolver>();
		methodResolvers.add(new ReflectiveMethodResolver() {
			@Override
			protected Method[] getMethods(Class<?> type) {
				try {
					return new Method[] {
							Integer.class.getDeclaredMethod("parseInt", new Class<?>[] {String.class, Integer.TYPE})};
				}
				catch (NoSuchMethodException ex) {
					return new Method[0];
				}
			}
		});

		context.setMethodResolvers(methodResolvers);
		Expression expression = parser.parseExpression("parseInt('-FF', 16)");

		Integer result = expression.getValue(context, "", Integer.class);
		assertEquals(-255, result.intValue());
	}

	@Test
	public void array() {
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
		assertEquals("T(java.lang.String[][][])", ((SpelExpression) expression).toStringAST());

		expression = parser.parseExpression("new int[0].class");
		result = expression.getValue(context, "");
		assertEquals("Equal assertion failed: ", "class [I", result.toString());

		expression = parser.parseExpression("T(int[][])");
		result = expression.getValue(context, "");
		assertEquals("class [[I", result.toString());
	}

	@Test
	public void SPR9486_floatFunctionResolver() throws Exception {
		Number expectedResult = Math.abs(-10.2f);
		ExpressionParser parser = new SpelExpressionParser();
		SPR9486_FunctionsClass testObject = new SPR9486_FunctionsClass();

		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("abs(-10.2f)");
		Number result = expression.getValue(context, testObject, Number.class);
		assertEquals(expectedResult, result);
	}


	class SPR9486_FunctionsClass {

		public int abs(int value) {
			return Math.abs(value);
		}

		public float abs(float value) {
			return Math.abs(value);
		}
	}


	@Test
	public void SPR9486_addFloatWithDouble() {
		Number expectedNumber = 10.21f + 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f + 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_addFloatWithFloat() {
		Number expectedNumber = 10.21f + 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f + 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_subtractFloatWithDouble() {
		Number expectedNumber = 10.21f - 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f - 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_subtractFloatWithFloat() {
		Number expectedNumber = 10.21f - 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f - 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_multiplyFloatWithDouble() {
		Number expectedNumber = 10.21f * 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f * 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_multiplyFloatWithFloat() {
		Number expectedNumber = 10.21f * 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f * 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatDivideByFloat() {
		Number expectedNumber = -10.21f / -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f / -10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatDivideByDouble() {
		Number expectedNumber = -10.21f / -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f / -10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatEqFloatUnaryMinus() {
		Boolean expectedResult = -10.21f == -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f == -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatEqDoubleUnaryMinus() {
		Boolean expectedResult = -10.21f == -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f == -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatEqFloat() {
		Boolean expectedResult = 10.215f == 10.2109f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.215f == 10.2109f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatEqDouble() {
		Boolean expectedResult = 10.215f == 10.2109;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.215f == 10.2109");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatNotEqFloat() {
		Boolean expectedResult = 10.215f != 10.2109f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.215f != 10.2109f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatNotEqDouble() {
		Boolean expectedResult = 10.215f != 10.2109;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.215f != 10.2109");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatLessThanFloat() {
		Boolean expectedNumber = -10.21f < -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f < -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatLessThanDouble() {
		Boolean expectedNumber = -10.21f < -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f < -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatLessThanOrEqualFloat() {
		Boolean expectedNumber = -10.21f <= -10.22f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f <= -10.22f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatLessThanOrEqualDouble() {
		Boolean expectedNumber = -10.21f <= -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f <= -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatGreaterThanFloat() {
		Boolean expectedNumber = -10.21f > -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f > -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatGreaterThanDouble() {
		Boolean expectedResult = -10.21f > -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f > -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatGreaterThanOrEqualFloat() {
		Boolean expectedNumber = -10.21f >= -10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f >= -10.2f");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedNumber, result);
	}

	@Test
	public void SPR9486_floatGreaterThanEqualDouble() {
		Boolean expectedResult = -10.21f >= -10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("-10.21f >= -10.2");
		Boolean result = expression.getValue(context, null, Boolean.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatModulusFloat() {
		Number expectedResult = 10.21f % 10.2f;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f % 10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatModulusDouble() {
		Number expectedResult = 10.21f % 10.2;
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f % 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatPowerFloat() {
		Number expectedResult = Math.pow(10.21f, -10.2f);
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f ^ -10.2f");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9486_floatPowerDouble() {
		Number expectedResult = Math.pow(10.21f, 10.2);
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Expression expression = parser.parseExpression("10.21f ^ 10.2");
		Number result = expression.getValue(context, null, Number.class);
		assertEquals(expectedResult, result);
	}

	@Test
	public void SPR9994_bridgeMethods() throws Exception {
		ReflectivePropertyAccessor accessor = new ReflectivePropertyAccessor();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Object target = new GenericImplementation();
		TypedValue value = accessor.read(context, target, "property");
		assertEquals(Integer.class, value.getTypeDescriptor().getType());
	}

	@Test
	public void SPR10162_onlyBridgeMethod() throws Exception {
		ReflectivePropertyAccessor accessor = new ReflectivePropertyAccessor();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Object target = new OnlyBridgeMethod();
		TypedValue value = accessor.read(context, target, "property");
		assertEquals(Integer.class, value.getTypeDescriptor().getType());
	}

	@Test
	public void SPR10091_simpleTestValueType() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Class<?> valueType = parser.parseExpression("simpleProperty").getValueType(evaluationContext);
		assertNotNull(valueType);
	}

	@Test
	public void SPR10091_simpleTestValue() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Object value = parser.parseExpression("simpleProperty").getValue(evaluationContext);
		assertNotNull(value);
	}

	@Test
	public void SPR10091_primitiveTestValueType() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Class<?> valueType = parser.parseExpression("primitiveProperty").getValueType(evaluationContext);
		assertNotNull(valueType);
	}

	@Test
	public void SPR10091_primitiveTestValue() {
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext(new BooleanHolder());
		Object value = parser.parseExpression("primitiveProperty").getValue(evaluationContext);
		assertNotNull(value);
	}

	@Test
	public void SPR10146_malformedExpressions() throws Exception {
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
	public void SPR10125() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		String fromInterface = parser.parseExpression("T(" + StaticFinalImpl1.class.getName() + ").VALUE").getValue(
				context, String.class);
		assertThat(fromInterface, is("interfaceValue"));
		String fromClass = parser.parseExpression("T(" + StaticFinalImpl2.class.getName() + ").VALUE").getValue(
				context, String.class);
		assertThat(fromClass, is("interfaceValue"));
	}

	@Test
	public void SPR10210() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("bridgeExample", new org.springframework.expression.spel.spr10210.D());
		Expression parseExpression = parser.parseExpression("#bridgeExample.bridgeMethod()");
		parseExpression.getValue(context);
	}

	@Test
	public void SPR10328() throws Exception {
		thrown.expect(SpelParseException.class);
		thrown.expectMessage("EL1071E:(pos 2): A required selection expression has not been specified");
		Expression exp = parser.parseExpression("$[]");
		exp.getValue(Arrays.asList("foo", "bar", "baz"));
	}

	@Test
	public void SPR10452() throws Exception {
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
	public void SPR9495() throws Exception {
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
			public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
					List<TypeDescriptor> argumentTypes) throws AccessException {
				return new MethodExecutor() {
					@Override
					public TypedValue execute(EvaluationContext context, Object target, Object... arguments)
							throws AccessException {
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
	public void SPR10486() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Spr10486 rootObject = new Spr10486();
		Expression classNameExpression = parser.parseExpression("class.name");
		Expression nameExpression = parser.parseExpression("name");
		assertThat(classNameExpression.getValue(context, rootObject), equalTo((Object) Spr10486.class.getName()));
		assertThat(nameExpression.getValue(context, rootObject), equalTo((Object) "name"));
	}

	@Test
	public void SPR11142() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		Spr11142 rootObject = new Spr11142();
		Expression expression = parser.parseExpression("something");
		thrown.expect(SpelEvaluationException.class);
		thrown.expectMessage("'something' cannot be found");
		expression.getValue(context, rootObject);
	}

	@Test
	public void SPR9194() {
		TestClass2 one = new TestClass2("abc");
		TestClass2 two = new TestClass2("abc");
		Map<String, TestClass2> map = new HashMap<String, TestClass2>();
		map.put("one", one);
		map.put("two", two);

		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("['one'] == ['two']");
		assertTrue(expr.getValue(map, Boolean.class));
	}

	@Test
	public void SPR11348() {
		Collection<String> coll = new LinkedHashSet<String>();
		coll.add("one");
		coll.add("two");
		coll = Collections.unmodifiableCollection(coll);

		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("new java.util.ArrayList(#root)");
		Object value = expr.getValue(coll);
		assertTrue(value instanceof ArrayList);
		@SuppressWarnings("rawtypes")
		ArrayList list = (ArrayList) value;
		assertEquals("one", list.get(0));
		assertEquals("two", list.get(1));
	}

	@Test
	public void SPR11445_simple() {
		StandardEvaluationContext context = new StandardEvaluationContext(new Spr11445Class());
		Expression expr = new SpelExpressionParser().parseRaw("echo(parameter())");
		assertEquals(1, expr.getValue(context));
	}

	@Test
	public void SPR11445_beanReference() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(new Spr11445Class());
		Expression expr = new SpelExpressionParser().parseRaw("@bean.echo(@bean.parameter())");
		assertEquals(1, expr.getValue(context));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void SPR11494() {
		Expression exp = new SpelExpressionParser().parseExpression("T(java.util.Arrays).asList('a','b')");
		List<String> list = (List<String>) exp.getValue();
		assertThat(list.size(), is(2));
	}

	@Test
	public void SPR11609() {
		StandardEvaluationContext sec = new StandardEvaluationContext();
		sec.addPropertyAccessor(new MapAccessor());
		Expression exp = new SpelExpressionParser().parseExpression(
				"T(org.springframework.expression.spel.SpelReproTests$MapWithConstant).X");
		assertEquals(1, exp.getValue(sec));
	}

	@Test
	public void SPR9735() {
		Item item = new Item();
		item.setName("parent");

		Item item1 = new Item();
		item1.setName("child1");

		Item item2 = new Item();
		item2.setName("child2");

		item.add(item1);
		item.add(item2);

		ExpressionParser parser = new SpelExpressionParser();
		EvaluationContext context = new StandardEvaluationContext();
		Expression exp = parser.parseExpression("#item[0].name");
		context.setVariable("item", item);

		assertEquals("child1", exp.getValue(context));
	}

	@Test
	public void SPR12502() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("#root.getClass().getName()");
		assertEquals(UnnamedUser.class.getName(), expression.getValue(new UnnamedUser()));
		assertEquals(NamedUser.class.getName(), expression.getValue(new NamedUser()));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void SPR12522() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("T(java.util.Arrays).asList('')");
		Object value = expression.getValue();
		assertTrue(value instanceof List);
		assertTrue(((List) value).isEmpty());
	}

	@Test
	public void SPR12803() {
		StandardEvaluationContext sec = new StandardEvaluationContext();
		sec.setVariable("iterable", Collections.emptyList());
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("T(org.springframework.expression.spel.SpelReproTests.GuavaLists).newArrayList(#iterable)");
		assertTrue(expression.getValue(sec) instanceof ArrayList);
	}

	@Test
	public void SPR12808() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("T(org.springframework.expression.spel.SpelReproTests.DistanceEnforcer).from(#no)");
		StandardEvaluationContext sec = new StandardEvaluationContext();
		sec.setVariable("no", new Integer(1));
		assertTrue(expression.getValue(sec).toString().startsWith("Integer"));
		sec = new StandardEvaluationContext();
		sec.setVariable("no", new Float(1.0));
		assertTrue(expression.getValue(sec).toString().startsWith("Number"));
		sec = new StandardEvaluationContext();
		sec.setVariable("no", "1.0");
		assertTrue(expression.getValue(sec).toString().startsWith("Object"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void SPR13055() throws Exception {
		List<Map<String, Object>> myPayload = new ArrayList<Map<String, Object>>();

		Map<String, Object> v1 = new HashMap<String, Object>();
		Map<String, Object> v2 = new HashMap<String, Object>();

		v1.put("test11", "test11");
		v1.put("test12", "test12");
		v2.put("test21", "test21");
		v2.put("test22", "test22");

		myPayload.add(v1);
		myPayload.add(v2);

		EvaluationContext context = new StandardEvaluationContext(myPayload);

		ExpressionParser parser = new SpelExpressionParser();

		String ex = "#root.![T(org.springframework.util.StringUtils).collectionToCommaDelimitedString(#this.values())]";
		List res = parser.parseExpression(ex).getValue(context, List.class);
		assertEquals("[test12,test11, test22,test21]", res.toString());

		res = parser.parseExpression("#root.![#this.values()]").getValue(context,
				List.class);
		assertEquals("[[test12, test11], [test22, test21]]", res.toString());

		res = parser.parseExpression("#root.![values()]").getValue(context, List.class);
		assertEquals("[[test12, test11], [test22, test21]]", res.toString());
	}

	@Test
	public void AccessingFactoryBean_spr9511() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(new MyBeanResolver());
		Expression expr = new SpelExpressionParser().parseRaw("@foo");
		assertEquals("custard", expr.getValue(context));
		expr = new SpelExpressionParser().parseRaw("&foo");
		assertEquals("foo factory",expr.getValue(context));
	
		try {
			expr = new SpelExpressionParser().parseRaw("&@foo");
			fail("Illegal syntax, error expected");
		}
		catch (SpelParseException spe) {
			assertEquals(SpelMessage.INVALID_BEAN_REFERENCE,spe.getMessageCode());
			assertEquals(0,spe.getPosition());
		}
	
		try {
			expr = new SpelExpressionParser().parseRaw("@&foo");
			fail("Illegal syntax, error expected");
		}
		catch (SpelParseException spe) {
			assertEquals(SpelMessage.INVALID_BEAN_REFERENCE,spe.getMessageCode());
			assertEquals(0,spe.getPosition());
		}	
	}

	@Test
	public void SPR12035() {
		ExpressionParser parser = new SpelExpressionParser();

		Expression expression1 = parser.parseExpression("list.?[ value>2 ].size()!=0");
		assertTrue(expression1.getValue(new BeanClass(new ListOf(1.1), new ListOf(2.2)),
				Boolean.class));

		Expression expression2 = parser.parseExpression("list.?[ T(java.lang.Math).abs(value) > 2 ].size()!=0");
		assertTrue(expression2.getValue(new BeanClass(new ListOf(1.1), new ListOf(-2.2)),
				Boolean.class));
	}

	static class CCC {
		public boolean method(Object o) {
			System.out.println(o);
			return false;
		}
	}

	@Test
	public void SPR13055_maps() {
		EvaluationContext context = new StandardEvaluationContext();
		ExpressionParser parser = new SpelExpressionParser();

		Expression ex = parser.parseExpression("{'a':'y','b':'n'}.![value=='y'?key:null]");
		assertEquals("[a, null]", ex.getValue(context).toString());

		ex = parser.parseExpression("{2:4,3:6}.![T(java.lang.Math).abs(#this.key) + 5]");
		assertEquals("[7, 8]", ex.getValue(context).toString());

		ex = parser.parseExpression("{2:4,3:6}.![T(java.lang.Math).abs(#this.value) + 5]");
		assertEquals("[9, 11]", ex.getValue(context).toString());
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void SPR10417() {
		List list1 = new ArrayList();
		list1.add("a");
		list1.add("b");
		list1.add("x");
		List list2 = new ArrayList();
		list2.add("c");
		list2.add("x");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("list1", list1);
		context.setVariable("list2", list2);

		// #this should be the element from list1
		Expression ex = parser.parseExpression("#list1.?[#list2.contains(#this)]");
		Object result = ex.getValue(context);
		assertEquals("[x]", result.toString());

		// toString() should be called on the element from list1
		ex = parser.parseExpression("#list1.?[#list2.contains(toString())]");
		result = ex.getValue(context);
		assertEquals("[x]", result.toString());

		List list3 = new ArrayList();
		list3.add(1);
		list3.add(2);
		list3.add(3);
		list3.add(4);

		context = new StandardEvaluationContext();
		context.setVariable("list3", list3);
		ex = parser.parseExpression("#list3.?[#this > 2]");
		result = ex.getValue(context);
		assertEquals("[3, 4]", result.toString());

		ex = parser.parseExpression("#list3.?[#this >= T(java.lang.Math).abs(T(java.lang.Math).abs(#this))]");
		result = ex.getValue(context);
		assertEquals("[1, 2, 3, 4]", result.toString());
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void SPR10417_maps() {
		Map map1 = new HashMap();
		map1.put("A", 65);
		map1.put("B", 66);
		map1.put("X", 66);
		Map map2 = new HashMap();
		map2.put("X", 66);

		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("map1", map1);
		context.setVariable("map2", map2);

		// #this should be the element from list1
		Expression ex = parser.parseExpression("#map1.?[#map2.containsKey(#this.getKey())]");
		Object result = ex.getValue(context);
		assertEquals("{X=66}", result.toString());

		ex = parser.parseExpression("#map1.?[#map2.containsKey(key)]");
		result = ex.getValue(context);
		assertEquals("{X=66}", result.toString());
	}

	@Test
	public void SPR13918() {
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("encoding", "UTF-8");

		Expression ex = parser.parseExpression("T(java.nio.charset.Charset).forName(#encoding)");
		Object result = ex.getValue(context);
		assertEquals(Charset.forName("UTF-8"), result);
	}


	public static class ListOf {

		private final double value;

		public ListOf(double v) {
			this.value = v;
		}

		public double getValue() {
			return value;
		}
	}


	public static class BeanClass {

		private final List<ListOf> list;

		public BeanClass(ListOf... list) {
			this.list = Arrays.asList(list);
		}

		public List<ListOf> getList() {
			return list;
		}
	}


	private enum ABC { A, B, C }

	private enum XYZ { X, Y, Z }


	public static class BooleanHolder {

		private Boolean simpleProperty = true;

		private boolean primitiveProperty = true;

		public void setSimpleProperty(Boolean simpleProperty) {
			this.simpleProperty = simpleProperty;
		}

		public Boolean isSimpleProperty() {
			return this.simpleProperty;
		}

		public void setPrimitiveProperty(boolean primitiveProperty) {
			this.primitiveProperty = primitiveProperty;
		}

		public boolean isPrimitiveProperty() {
			return this.primitiveProperty;
		}
	}


	private interface GenericInterface<T extends Number> {

		T getProperty();
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


	public interface StaticFinal {

		String VALUE = "interfaceValue";
	}


	public abstract static class AbstractStaticFinal implements StaticFinal {
	}


	public static class StaticFinalImpl1 extends AbstractStaticFinal implements StaticFinal {
	}


	public static class StaticFinalImpl2 extends AbstractStaticFinal {
	}


	public static class Spr10486 {

		private String name = "name";

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	static class Spr11142 {

		public String isSomething() {
			return "";
		}
	}


	static class TestClass2 {  // SPR-9194

		String string;

		public TestClass2(String string) {
			this.string = string;
		}

		public boolean equals(Object other) {
			return (this == other || (other instanceof TestClass2 &&
					this.string.equals(((TestClass2) other).string)));
		}

		@Override
		public int hashCode() {
			return this.string.hashCode();
		}
	}


	static class Spr11445Class implements BeanResolver {

		private final AtomicInteger counter = new AtomicInteger();

		public int echo(int invocation) {
			return invocation;
		}

		public int parameter() {
			return this.counter.incrementAndGet();
		}

		@Override
		public Object resolve(EvaluationContext context, String beanName) throws AccessException {
			return (beanName.equals("bean") ? this : null);
		}
	}


	@SuppressWarnings({"rawtypes", "serial"})
	public static class MapWithConstant extends HashMap {

		public static final int X = 1;
	}


	public class Item implements List<Item> {

		private String name;

		private List<Item> children = new ArrayList<Item>();

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public int size() {
			return this.children.size();
		}

		@Override
		public boolean isEmpty() {
			return this.children.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return this.children.contains(o);
		}

		@Override
		public Iterator<Item> iterator() {
			return this.children.iterator();
		}

		@Override
		public Object[] toArray() {
			return this.children.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return this.children.toArray(a);
		}

		@Override
		public boolean add(Item e) {
			return this.children.add(e);
		}

		@Override
		public boolean remove(Object o) {
			return this.children.remove(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return this.children.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends Item> c) {
			return this.children.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends Item> c) {
			return this.children.addAll(index, c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return this.children.removeAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return this.children.retainAll(c);
		}

		@Override
		public void clear() {
			this.children.clear();
		}

		@Override
		public Item get(int index) {
			return this.children.get(index);
		}

		@Override
		public Item set(int index, Item element) {
			return this.children.set(index, element);
		}

		@Override
		public void add(int index, Item element) {
			this.children.add(index, element);
		}

		@Override
		public Item remove(int index) {
			return this.children.remove(index);
		}

		@Override
		public int indexOf(Object o) {
			return this.children.indexOf(o);
		}

		@Override
		public int lastIndexOf(Object o) {
			return this.children.lastIndexOf(o);
		}

		@Override
		public ListIterator<Item> listIterator() {
			return this.children.listIterator();
		}

		@Override
		public ListIterator<Item> listIterator(int index) {
			return this.children.listIterator(index);
		}

		@Override
		public List<Item> subList(int fromIndex, int toIndex) {
			return this.children.subList(fromIndex, toIndex);
		}
	}


	public static class UnnamedUser {
	}


	public static class NamedUser {

		public String getName() {
			return "foo";
		}
	}


	public static class GuavaLists {

		public static <T> List<T> newArrayList(Iterable<T> iterable) {
			return new ArrayList<T>();
		}

		public static <T> List<T> newArrayList(Object... elements) {
			throw new UnsupportedOperationException();
		}
	}


	public static class DistanceEnforcer {

		public static String from(Number no) {
			return "Number:" + no.toString();
		}

		public static String from(Integer no) {
			return "Integer:" + no.toString();
		}

		public static String from(Object no) {
			return "Object:" + no.toString();
		}
	}

}
