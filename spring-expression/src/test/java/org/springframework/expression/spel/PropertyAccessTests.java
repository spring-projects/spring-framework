/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Person;

import static org.junit.Assert.*;

/**
 * Tests accessing of properties.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class PropertyAccessTests extends AbstractExpressionTests {

	@Test
	public void testSimpleAccess01() {
		evaluate("name", "Nikola Tesla", String.class);
	}

	@Test
	public void testSimpleAccess02() {
		evaluate("placeOfBirth.city", "SmilJan", String.class);
	}

	@Test
	public void testSimpleAccess03() {
		evaluate("stringArrayOfThreeItems.length", "3", Integer.class);
	}

	@Test
	public void testNonExistentPropertiesAndMethods() {
		// madeup does not exist as a property
		evaluateAndCheckError("madeup", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 0);

		// name is ok but foobar does not exist:
		evaluateAndCheckError("name.foobar", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 5);
	}

	/**
	 * The standard reflection resolver cannot find properties on null objects but some
	 * supplied resolver might be able to - so null shouldn't crash the reflection resolver.
	 */
	@Test
	public void testAccessingOnNullObject() {
		SpelExpression expr = (SpelExpression)parser.parseExpression("madeup");
		EvaluationContext context = new StandardEvaluationContext(null);
		try {
			expr.getValue(context);
			fail("Should have failed - default property resolver cannot resolve on null");
		}
		catch (Exception ex) {
			checkException(ex, SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL);
		}
		assertFalse(expr.isWritable(context));
		try {
			expr.setValue(context, "abc");
			fail("Should have failed - default property resolver cannot resolve on null");
		}
		catch (Exception ex) {
			checkException(ex, SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL);
		}
	}

	private void checkException(Exception ex, SpelMessage expectedMessage) {
		if (ex instanceof SpelEvaluationException) {
			SpelMessage sm = ((SpelEvaluationException) ex).getMessageCode();
			assertEquals("Expected exception type did not occur", expectedMessage, sm);
		}
		else {
			fail("Should be a SpelException " + ex);
		}
	}

	@Test
	// Adding a new property accessor just for a particular type
	public void testAddingSpecificPropertyAccessor() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// Even though this property accessor is added after the reflection one, it specifically
		// names the String class as the type it is interested in so is chosen in preference to
		// any 'default' ones
		ctx.addPropertyAccessor(new StringyPropertyAccessor());
		Expression expr = parser.parseRaw("new String('hello').flibbles");
		Integer i = expr.getValue(ctx, Integer.class);
		assertEquals(7, (int) i);

		// The reflection one will be used for other properties...
		expr = parser.parseRaw("new String('hello').CASE_INSENSITIVE_ORDER");
		Object o = expr.getValue(ctx);
		assertNotNull(o);

		expr = parser.parseRaw("new String('hello').flibbles");
		expr.setValue(ctx, 99);
		i = expr.getValue(ctx, Integer.class);
		assertEquals(99, (int) i);

		// Cannot set it to a string value
		try {
			expr.setValue(ctx, "not allowed");
			fail("Should not have been allowed");
		}
		catch (EvaluationException ex) {
			// success - message will be: EL1063E:(pos 20): A problem occurred whilst attempting to set the property
			// 'flibbles': 'Cannot set flibbles to an object of type 'class java.lang.String''
			// System.out.println(e.getMessage());
		}
	}

	@Test
	public void testAddingRemovingAccessors() {
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// reflective property accessor is the only one by default
		List<PropertyAccessor> propertyAccessors = ctx.getPropertyAccessors();
		assertEquals(1,propertyAccessors.size());

		StringyPropertyAccessor spa = new StringyPropertyAccessor();
		ctx.addPropertyAccessor(spa);
		assertEquals(2,ctx.getPropertyAccessors().size());

		List<PropertyAccessor> copy = new ArrayList<>();
		copy.addAll(ctx.getPropertyAccessors());
		assertTrue(ctx.removePropertyAccessor(spa));
		assertFalse(ctx.removePropertyAccessor(spa));
		assertEquals(1,ctx.getPropertyAccessors().size());

		ctx.setPropertyAccessors(copy);
		assertEquals(2,ctx.getPropertyAccessors().size());
	}

	@Test
	public void testAccessingPropertyOfClass() {
		Expression expression = parser.parseExpression("name");
		Object value = expression.getValue(new StandardEvaluationContext(String.class));
		assertEquals("java.lang.String", value);
	}

	@Test
	public void shouldAlwaysUsePropertyAccessorFromEvaluationContext() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("name");

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new ConfigurablePropertyAccessor(Collections.singletonMap("name", "Ollie")));
		assertEquals("Ollie", expression.getValue(context));

		context = new StandardEvaluationContext();
		context.addPropertyAccessor(new ConfigurablePropertyAccessor(Collections.singletonMap("name", "Jens")));
		assertEquals("Jens", expression.getValue(context));
	}

	@Test
	public void standardGetClassAccess() {
		assertEquals(String.class.getName(), parser.parseExpression("'a'.class.name").getValue());
	}

	@Test(expected = SpelEvaluationException.class)
	public void noGetClassAccess() {
		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

		parser.parseExpression("'a'.class.name").getValue(context);
	}

	@Test
	public void propertyReadOnly() {
		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

		Expression expr = parser.parseExpression("name");
		Person target = new Person("p1");
		assertEquals("p1", expr.getValue(context, target));
		target.setName("p2");
		assertEquals("p2", expr.getValue(context, target));

		try {
			parser.parseExpression("name='p3'").getValue(context, target);
			fail("Should have thrown SpelEvaluationException");
		}
		catch (SpelEvaluationException ex) {
			// expected
		}
	}

	@Test
	public void propertyReadWrite() {
		EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

		Expression expr = parser.parseExpression("name");
		Person target = new Person("p1");
		assertEquals("p1", expr.getValue(context, target));
		target.setName("p2");
		assertEquals("p2", expr.getValue(context, target));

		parser.parseExpression("name='p3'").getValue(context, target);
		assertEquals("p3", target.getName());
		assertEquals("p3", expr.getValue(context, target));

		expr.setValue(context, target, "p4");
		assertEquals("p4", target.getName());
		assertEquals("p4", expr.getValue(context, target));
	}

	@Test
	public void propertyReadWriteWithRootObject() {
		Person target = new Person("p1");
		EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().withRootObject(target).build();
		assertSame(target, context.getRootObject().getValue());

		Expression expr = parser.parseExpression("name");
		assertEquals("p1", expr.getValue(context, target));
		target.setName("p2");
		assertEquals("p2", expr.getValue(context, target));

		parser.parseExpression("name='p3'").getValue(context, target);
		assertEquals("p3", target.getName());
		assertEquals("p3", expr.getValue(context, target));

		expr.setValue(context, target, "p4");
		assertEquals("p4", target.getName());
		assertEquals("p4", expr.getValue(context, target));
	}

	@Test
	public void propertyAccessWithoutMethodResolver() {
		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		Person target = new Person("p1");
		try {
			parser.parseExpression("name.substring(1)").getValue(context, target);
			fail("Should have thrown SpelEvaluationException");
		}
		catch (SpelEvaluationException ex) {
			// expected
		}
	}

	@Test
	public void propertyAccessWithInstanceMethodResolver() {
		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
		Person target = new Person("p1");
		assertEquals("1", parser.parseExpression("name.substring(1)").getValue(context, target));
	}

	@Test
	public void propertyAccessWithInstanceMethodResolverAndTypedRootObject() {
		Person target = new Person("p1");
		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().
				withInstanceMethods().withTypedRootObject(target, TypeDescriptor.valueOf(Object.class)).build();

		assertEquals("1", parser.parseExpression("name.substring(1)").getValue(context, target));
		assertSame(target, context.getRootObject().getValue());
		assertSame(Object.class, context.getRootObject().getTypeDescriptor().getType());
	}


	// This can resolve the property 'flibbles' on any String (very useful...)
	private static class StringyPropertyAccessor implements PropertyAccessor {

		int flibbles = 7;

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {String.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			if (!(target instanceof String)) {
				throw new RuntimeException("Assertion Failed! target should be String");
			}
			return (name.equals("flibbles"));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			if (!(target instanceof String)) {
				throw new RuntimeException("Assertion Failed! target should be String");
			}
			return (name.equals("flibbles"));
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			if (!name.equals("flibbles")) {
				throw new RuntimeException("Assertion Failed! name should be flibbles");
			}
			return new TypedValue(flibbles);
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			if (!name.equals("flibbles")) {
				throw new RuntimeException("Assertion Failed! name should be flibbles");
			}
			try {
				flibbles = (Integer) context.getTypeConverter().convertValue(newValue,
						TypeDescriptor.forObject(newValue), TypeDescriptor.valueOf(Integer.class));
			}
			catch (EvaluationException ex) {
				throw new AccessException("Cannot set flibbles to an object of type '" + newValue.getClass() + "'");
			}
		}
	}


	private static class ConfigurablePropertyAccessor implements PropertyAccessor {

		private final Map<String, Object> values;

		public ConfigurablePropertyAccessor(Map<String, Object> values) {
			this.values = values;
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return true;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(this.values.get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}
	}

}
