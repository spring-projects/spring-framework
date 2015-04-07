/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.test.util.AssertionErrors.*;

/**
 * A collection of assertions intended to simplify testing scenarios dealing
 * with Spring Web MVC {@link org.springframework.web.servlet.ModelAndView
 * ModelAndView} objects.
 * <p>
 * Intended for use with JUnit 4 and TestNG. All {@code assert*()} methods
 * throw {@link AssertionError}s.
 *
 * @author Sam Brannen
 * @author Alef Arendsen
 * @author Bram Smeets
 * @since 2.5
 * @see org.springframework.web.servlet.ModelAndView
 */
public abstract class ModelAndViewAssert {

	/**
	 * Checks whether the model value under the given {@code modelName}
	 * exists and checks it type, based on the {@code expectedType}. If the
	 * model entry exists and the type matches, the model value is returned.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param modelName name of the object to add to the model (never
	 * {@code null})
	 * @param expectedType expected type of the model value
	 * @return the model value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T assertAndReturnModelAttributeOfType(ModelAndView mav, String modelName, Class<T> expectedType) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("Model is null", mav.getModel() != null);
		Object obj = mav.getModel().get(modelName);
		assertTrue("Model attribute with name '" + modelName + "' is null", obj != null);
		assertTrue("Model attribute is not of expected type '" + expectedType.getName() + "' but rather of type '"
				+ obj.getClass().getName() + "'", expectedType.isAssignableFrom(obj.getClass()));
		return (T) obj;
	}

	/**
	 * Compare each individual entry in a list, without first sorting the lists.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param modelName name of the object to add to the model (never
	 * {@code null})
	 * @param expectedList the expected list
	 */
	@SuppressWarnings("rawtypes")
	public static void assertCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList) {
		assertTrue("ModelAndView is null", mav != null);
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertTrue(
			"Size of model list is '" + modelList.size() + "' while size of expected list is '" + expectedList.size()
					+ "'", expectedList.size() == modelList.size());
		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
			expectedList.equals(modelList));
	}

	/**
	 * Assert whether or not a model attribute is available.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param modelName name of the object to add to the model (never
	 * {@code null})
	 */
	public static void assertModelAttributeAvailable(ModelAndView mav, String modelName) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("Model is null", mav.getModel() != null);
		assertTrue("Model attribute with name '" + modelName + "' is not available",
			mav.getModel().containsKey(modelName));
	}

	/**
	 * Compare a given {@code expectedValue} to the value from the model
	 * bound under the given {@code modelName}.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param modelName name of the object to add to the model (never
	 * {@code null})
	 * @param expectedValue the model value
	 */
	public static void assertModelAttributeValue(ModelAndView mav, String modelName, Object expectedValue) {
		assertTrue("ModelAndView is null", mav != null);
		Object modelValue = assertAndReturnModelAttributeOfType(mav, modelName, Object.class);
		assertTrue("Model value with name '" + modelName + "' is not the same as the expected value which was '"
				+ expectedValue + "'", modelValue.equals(expectedValue));
	}

	/**
	 * Inspect the {@code expectedModel} to see if all elements in the
	 * model appear and are equal.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param expectedModel the expected model
	 */
	public static void assertModelAttributeValues(ModelAndView mav, Map<String, Object> expectedModel) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("Model is null", mav.getModel() != null);

		if (!mav.getModel().keySet().equals(expectedModel.keySet())) {
			StringBuilder sb = new StringBuilder("Keyset of expected model does not match.\n");
			appendNonMatchingSetsErrorMessage(expectedModel.keySet(), mav.getModel().keySet(), sb);
			fail(sb.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String modelName : mav.getModel().keySet()) {
			Object assertionValue = expectedModel.get(modelName);
			Object mavValue = mav.getModel().get(modelName);
			if (!assertionValue.equals(mavValue)) {
				sb.append("Value under name '").append(modelName).append("' differs, should have been '").append(
					assertionValue).append("' but was '").append(mavValue).append("'\n");
			}
		}

		if (sb.length() != 0) {
			sb.insert(0, "Values of expected model do not match.\n");
			fail(sb.toString());
		}
	}

	/**
	 * Compare each individual entry in a list after having sorted both lists
	 * (optionally using a comparator).
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param modelName name of the object to add to the model (never
	 * {@code null})
	 * @param expectedList the expected list
	 * @param comparator the comparator to use (may be {@code null}). If
	 * not specifying the comparator, both lists will be sorted not using any
	 * comparator.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void assertSortAndCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList,
			Comparator comparator) {

		assertTrue("ModelAndView is null", mav != null);
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);

		assertTrue(
			"Size of model list is '" + modelList.size() + "' while size of expected list is '" + expectedList.size()
					+ "'", expectedList.size() == modelList.size());

		if (comparator != null) {
			Collections.sort(modelList, comparator);
			Collections.sort(expectedList, comparator);
		}
		else {
			Collections.sort(modelList);
			Collections.sort(expectedList);
		}

		assertTrue("List in model under name '" + modelName + "' is not equal to the expected list.",
			expectedList.equals(modelList));
	}

	/**
	 * Check to see if the view name in the ModelAndView matches the given
	 * {@code expectedName}.
	 *
	 * @param mav ModelAndView to test against (never {@code null})
	 * @param expectedName the name of the model value
	 */
	public static void assertViewName(ModelAndView mav, String expectedName) {
		assertTrue("ModelAndView is null", mav != null);
		assertTrue("View name is not equal to '" + expectedName + "' but was '" + mav.getViewName() + "'",
			ObjectUtils.nullSafeEquals(expectedName, mav.getViewName()));
	}

	private static void appendNonMatchingSetsErrorMessage(Set<String> assertionSet, Set<String> incorrectSet,
			StringBuilder sb) {

		Set<String> tempSet = new HashSet<String>();
		tempSet.addAll(incorrectSet);
		tempSet.removeAll(assertionSet);

		if (tempSet.size() > 0) {
			sb.append("Set has too many elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}

		tempSet = new HashSet<String>();
		tempSet.addAll(assertionSet);
		tempSet.removeAll(incorrectSet);

		if (tempSet.size() > 0) {
			sb.append("Set is missing elements:\n");
			for (Object element : tempSet) {
				sb.append('-');
				sb.append(element);
				sb.append('\n');
			}
		}
	}

}
