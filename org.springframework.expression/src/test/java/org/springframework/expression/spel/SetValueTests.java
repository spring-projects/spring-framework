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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;

/**
 * Tests set value expressions.
 * 
 * @author Keith Donald
 */
public class SetValueTests extends ExpressionTestCase {

	private final static boolean DEBUG = false;

	public void testSetProperty() {
		setValue("wonNobelPrize", true);
	}

	public void testSetNestedProperty() {
		setValue("placeOfBirth.city", "Wien");
	}

	//public void testSetPropertyTypeCoersion() {
	//	setValue("wonNobelPrize", "true");
	//}
	
	//public void testSetArrayElementValue() {
	//	setValue("inventions[0]", "Just the telephone");
	//}
	
	public void testSetArrayElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}
	
	//public void testSetListElementValue() {
	//	setValue("placesLivedList[0]", new PlaceOfBirth("Wien"));
	//}
	
	//public void testSetGenericListElementValueTypeCoersion() {
	//	setValue("placesLivedList[0]", "Wien");
	//}

	public void testSetListElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}

	protected void setValue(String expression, Object value) {
		try {
			Expression e = parser.parseExpression(expression);
			if (e == null) {
				fail("Parser returned null for expression");
			}
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, e);
			}
			assertTrue("Expression is not writeable but should be", e.isWritable(eContext));
			e.setValue(eContext, value);
			assertEquals("Retrieved value was not equal to set value", value, e.getValue(eContext));
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}
}
