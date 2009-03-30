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

import java.util.ArrayList;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.PlaceOfBirth;

/**
 * Tests set value expressions.
 * 
 * @author Keith Donald
 * @author Andy Clement
 */
public class SetValueTests extends ExpressionTestCase {

	private final static boolean DEBUG = false;

	public void testSetProperty() {
		setValue("wonNobelPrize", true);
	}
	
	public void testSetNestedProperty() {
		setValue("placeOfBirth.city", "Wien");
	}

	public void testSetArrayElementValue() {
		setValue("inventions[0]", "Just the telephone");
	}
	
	public void testSetArrayElementValueAllPrimitiveTypes() {
		setValue("arrayContainer.ints[1]", 3);
		setValue("arrayContainer.floats[1]", 3.0f);
		setValue("arrayContainer.booleans[1]", false);
		setValue("arrayContainer.doubles[1]", 3.4d);
		setValue("arrayContainer.shorts[1]", (short)3);
		setValue("arrayContainer.longs[1]", 3L);
		setValue("arrayContainer.bytes[1]", (byte) 3);
		setValue("arrayContainer.chars[1]", (char) 3);
	}

	public void testSetArrayElementValueAllPrimitiveTypesErrors() {
		// none of these sets are possible due to (expected) conversion problems
		setValueExpectError("arrayContainer.ints[1]", "wibble");
		setValueExpectError("arrayContainer.floats[1]", "dribble");
		setValueExpectError("arrayContainer.booleans[1]", "nein");
		setValueExpectError("arrayContainer.doubles[1]", new ArrayList<String>());
		setValueExpectError("arrayContainer.shorts[1]", new ArrayList<String>());
		setValueExpectError("arrayContainer.longs[1]", new ArrayList<String>());
		setValueExpectError("arrayContainer.bytes[1]", "NaB");
		setValueExpectError("arrayContainer.chars[1]", "NaC");
	}
	
	public void testSetArrayElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}
	
	public void testSetListElementValue() {
		setValue("placesLivedList[0]", new PlaceOfBirth("Wien"));
	}
	
	public void testSetGenericListElementValueTypeCoersion() {
		setValue("placesLivedList[0]", "Wien");
	}

	public void testSetListElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}

	public void testSetArrayElementInvalidIndex() {
		setValueExpectError("placesLived[23]", "Wien");
		setValueExpectError("placesLivedList[23]", "Wien");
	}
	
	public void testSetMapElements() {
		setValue("testMap['montag']","lundi");
	}
	
	public void testIndexingIntoUnsupportedType() {
		setValueExpectError("'hello'[3]", 'p');
	}
	
//	public void testSetPropertyTypeCoersion() {
//		setValue("publicBoolean", "true");
//	}


	/**
	 * Call setValue() but expect it to fail.
	 */
	protected void setValueExpectError(String expression, Object value) {
		try {
			Expression e = parser.parseExpression(expression);
			if (e == null) {
				fail("Parser returned null for expression");
			}
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, e);
			}
			StandardEvaluationContext lContext = TestScenarioCreator.getTestEvaluationContext();
//			assertTrue("Expression is not writeable but should be", e.isWritable(lContext));
			e.setValue(lContext, value);
			fail("expected an error");
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		} catch (EvaluationException ee) {
			// success!
		}
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
			StandardEvaluationContext lContext = TestScenarioCreator.getTestEvaluationContext();
			assertTrue("Expression is not writeable but should be", e.isWritable(lContext));
			e.setValue(lContext, value);
			assertEquals("Retrieved value was not equal to set value", value, e.getValue(lContext));
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			fail("Unexpected Exception: " + ee.getMessage());
		} catch (ParseException pe) {
			pe.printStackTrace();
			fail("Unexpected Exception: " + pe.getMessage());
		}
	}
}
