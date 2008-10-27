/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.web;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.springframework.web.servlet.ModelAndView;

/**
 * <p>
 * Convenient JUnit 3.8 base class for tests dealing with Spring Web MVC
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView} objects.
 * </p>
 * <p>
 * All <code>assert*()</code> methods throw {@link AssertionFailedError}s.
 * </p>
 * <p>
 * Consider the use of {@link ModelAndViewAssert} with JUnit 4 and TestNG.
 * </p>
 *
 * @author Alef Arendsen
 * @author Bram Smeets
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.web.servlet.ModelAndView
 * @see ModelAndViewAssert
 */
public abstract class AbstractModelAndViewTests extends TestCase {

	/**
	 * Checks whether the model value under the given <code>modelName</code>
	 * exists and checks it type, based on the <code>expectedType</code>. If
	 * the model entry exists and the type matches, the model value is returned.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedType expected type of the model value
	 * @return the model value
	 */
	protected Object assertAndReturnModelAttributeOfType(ModelAndView mav, Object modelName, Class expectedType) {

		try {
			return ModelAndViewAssert.assertAndReturnModelAttributeOfType(mav, modelName, expectedType);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Compare each individual entry in a list, without first sorting the lists.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedList the expected list
	 */
	protected void assertCompareListModelAttribute(ModelAndView mav, Object modelName, List expectedList) {

		try {
			ModelAndViewAssert.assertCompareListModelAttribute(mav, modelName, expectedList);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Assert whether or not a model attribute is available.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 */
	protected void assertModelAttributeAvailable(ModelAndView mav, Object modelName) {

		try {
			ModelAndViewAssert.assertModelAttributeAvailable(mav, modelName);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Compare a given <code>expectedValue</code> to the value from the model
	 * bound under the given <code>modelName</code>.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedValue the model value
	 */
	protected void assertModelAttributeValue(ModelAndView mav, Object modelName, Object expectedValue) {

		try {
			ModelAndViewAssert.assertModelAttributeValue(mav, modelName, expectedValue);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Inspect the <code>expectedModel</code> to see if all elements in the
	 * model appear and are equal.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param expectedModel the expected model
	 */
	protected void assertModelAttributeValues(ModelAndView mav, Map expectedModel) {

		try {
			ModelAndViewAssert.assertModelAttributeValues(mav, expectedModel);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Compare each individual entry in a list after having sorted both lists
	 * (optionally using a comparator).
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedList the expected list
	 * @param comparator the comparator to use (may be <code>null</code>). If
	 * not specifying the comparator, both lists will be sorted not using
	 * any comparator.
	 */
	protected void assertSortAndCompareListModelAttribute(
			ModelAndView mav, Object modelName, List expectedList, Comparator comparator) {

		try {
			ModelAndViewAssert.assertSortAndCompareListModelAttribute(mav, modelName, expectedList, comparator);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

	/**
	 * Check to see if the view name in the ModelAndView matches the given
	 * <code>expectedName</code>.
	 *
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param expectedName the name of the model value
	 */
	protected void assertViewName(ModelAndView mav, String expectedName) {

		try {
			ModelAndViewAssert.assertViewName(mav, expectedName);
		}
		catch (AssertionError e) {
			throw new AssertionFailedError(e.getMessage());
		}
	}

}
