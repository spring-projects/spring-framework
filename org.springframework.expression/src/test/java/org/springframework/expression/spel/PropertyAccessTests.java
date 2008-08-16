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

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * Tests accessing of properties.
 * 
 * @author Andy Clement
 */
public class PropertyAccessTests extends ExpressionTestCase {

	public void testSimpleAccess01() {
		evaluate("name", "Nikola Tesla", String.class);
	}

	public void testSimpleAccess02() {
		evaluate("placeOfBirth.city", "SmilJan", String.class);
	}

	public void testNonExistentPropertiesAndMethods() {
		// madeup does not exist as a property
		evaluateAndCheckError("madeup", SpelMessages.PROPERTY_OR_FIELD_NOT_FOUND, 0);

		// name is ok but foobar does not exist:
		evaluateAndCheckError("name.foobar", SpelMessages.PROPERTY_OR_FIELD_NOT_FOUND, 5);
	}

	// This can resolve the property 'flibbles' on any String (very useful...)
	static class StringyPropertyAccessor implements PropertyAccessor {

		int flibbles = 7;

		public Class<?>[] getSpecificTargetClasses() {
			return new Class[] { String.class };
		}

		public boolean canRead(EvaluationContext context, Object target, Object name) throws AccessException {
			if (!(target instanceof String))
				throw new RuntimeException("Assertion Failed! target should be String");
			return (name.equals("flibbles"));
		}

		public boolean canWrite(EvaluationContext context, Object target, Object name) throws AccessException {
			if (!(target instanceof String))
				throw new RuntimeException("Assertion Failed! target should be String");
			return (name.equals("flibbles"));
		}

		public Object read(EvaluationContext context, Object target, Object name) throws AccessException {
			if (!name.equals("flibbles"))
				throw new RuntimeException("Assertion Failed! name should be flibbles");
			return flibbles;
		}

		public void write(EvaluationContext context, Object target, Object name, Object newValue)
				throws AccessException {
			if (!name.equals("flibbles"))
				throw new RuntimeException("Assertion Failed! name should be flibbles");
			try {
				flibbles = (Integer) context.getTypeUtils().getTypeConverter().convertValue(newValue, Integer.class);
			} catch (EvaluationException e) {
				throw new AccessException("Cannot set flibbles to an object of type '" + newValue.getClass() + "'");
			}
		}

	}

	// Adding a new property accessor just for a particular type
	public void testAddingSpecificPropertyAccessor() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// Even though this property accessor is added after the reflection one, it specifically
		// names the String class as the type it is interested in so is chosen in preference to
		// any 'default' ones
		ctx.addPropertyAccessor(new StringyPropertyAccessor());
		Expression expr = parser.parseExpression("new String('hello').flibbles");
		Integer i = (Integer) expr.getValue(ctx, Integer.class);
		assertEquals((int) i, 7);

		// The reflection one will be used for other properties...
		expr = parser.parseExpression("new String('hello').CASE_INSENSITIVE_ORDER");
		Object o = expr.getValue(ctx);
		assertNotNull(o);

		expr = parser.parseExpression("new String('hello').flibbles");
		expr.setValue(ctx, 99);
		i = (Integer) expr.getValue(ctx, Integer.class);
		assertEquals((int) i, 99);

		// Cannot set it to a string value
		try {
			expr.setValue(ctx, "not allowed");
			fail("Should not have been allowed");
		} catch (EvaluationException e) {
			// success - message will be: EL1063E:(pos 20): A problem occurred whilst attempting to set the property
			// 'flibbles': 'Cannot set flibbles to an object of type 'class java.lang.String''
			System.out.println(e.getMessage());
		}
	}

}
