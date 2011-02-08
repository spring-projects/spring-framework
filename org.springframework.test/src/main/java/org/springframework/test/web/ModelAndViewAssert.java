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

package org.springframework.test.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 * A collection of assertions intended to simplify testing scenarios dealing
 * with Spring Web MVC {@link org.springframework.web.servlet.ModelAndView
 * ModelAndView} objects.
 * <p>
 * Intended for use with JUnit 4 and TestNG. All <code>assert*()</code> methods
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
	 * Checks whether the model value under the given <code>modelName</code>
	 * exists and checks it type, based on the <code>expectedType</code>. If the
	 * model entry exists and the type matches, the model value is returned.
	 * 
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedType expected type of the model value
	 * @return the model value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T assertAndReturnModelAttributeOfType(ModelAndView mav, String modelName, Class<T> expectedType) {
		assertCondition(mav != null, "ModelAndView is null");
		assertCondition(mav.getModel() != null, "Model is null");
		Object obj = mav.getModel().get(modelName);
		assertCondition(obj != null, "Model attribute with name '" + modelName + "' is null");
		assertCondition(expectedType.isAssignableFrom(obj.getClass()), "Model attribute is not of expected type '"
				+ expectedType.getName() + "' but rather of type '" + obj.getClass().getName() + "'");
		return (T) obj;
	}

	/**
	 * Compare each individual entry in a list, without first sorting the lists.
	 * 
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedList the expected list
	 */
	@SuppressWarnings("rawtypes")
	public static void assertCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList) {
		assertCondition(mav != null, "ModelAndView is null");
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);
		assertCondition(expectedList.size() == modelList.size(), "Size of model list is '" + modelList.size()
				+ "' while size of expected list is '" + expectedList.size() + "'");
		assertCondition(expectedList.equals(modelList), "List in model under name '" + modelName
				+ "' is not equal to the expected list.");
	}

	/**
	 * Assert whether or not a model attribute is available.
	 * 
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 */
	public static void assertModelAttributeAvailable(ModelAndView mav, String modelName) {
		assertCondition(mav != null, "ModelAndView is null");
		assertCondition(mav.getModel() != null, "Model is null");
		assertCondition(mav.getModel().containsKey(modelName), "Model attribute with name '" + modelName
				+ "' is not available");
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
	public static void assertModelAttributeValue(ModelAndView mav, String modelName, Object expectedValue) {
		assertCondition(mav != null, "ModelAndView is null");
		Object modelValue = assertAndReturnModelAttributeOfType(mav, modelName, Object.class);
		assertCondition(modelValue.equals(expectedValue), "Model value with name '" + modelName
				+ "' is not the same as the expected value which was '" + expectedValue + "'");
	}

	/**
	 * Inspect the <code>expectedModel</code> to see if all elements in the
	 * model appear and are equal.
	 * 
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param expectedModel the expected model
	 */
	public static void assertModelAttributeValues(ModelAndView mav, Map<String, Object> expectedModel) {
		assertCondition(mav != null, "ModelAndView is null");
		assertCondition(mav.getModel() != null, "Model is null");

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
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param modelName name of the object to add to the model (never
	 * <code>null</code>)
	 * @param expectedList the expected list
	 * @param comparator the comparator to use (may be <code>null</code>). If
	 * not specifying the comparator, both lists will be sorted not using any
	 * comparator.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void assertSortAndCompareListModelAttribute(ModelAndView mav, String modelName, List expectedList,
			Comparator comparator) {

		assertCondition(mav != null, "ModelAndView is null");
		List modelList = assertAndReturnModelAttributeOfType(mav, modelName, List.class);

		assertCondition(expectedList.size() == modelList.size(), "Size of model list is '" + modelList.size()
				+ "' while size of expected list is '" + expectedList.size() + "'");

		if (comparator != null) {
			Collections.sort(modelList, comparator);
			Collections.sort(expectedList, comparator);
		}
		else {
			Collections.sort(modelList);
			Collections.sort(expectedList);
		}

		assertCondition(expectedList.equals(modelList), "List in model under name '" + modelName
				+ "' is not equal to the expected list.");
	}

	/**
	 * Check to see if the view name in the ModelAndView matches the given
	 * <code>expectedName</code>.
	 * 
	 * @param mav ModelAndView to test against (never <code>null</code>)
	 * @param expectedName the name of the model value
	 */
	public static void assertViewName(ModelAndView mav, String expectedName) {
		assertCondition(mav != null, "ModelAndView is null");
		assertCondition(ObjectUtils.nullSafeEquals(expectedName, mav.getViewName()), "View name is not equal to '"
				+ expectedName + "' but was '" + mav.getViewName() + "'");
	}

	/**
	 * Fails by throwing an <code>AssertionError</code> with the supplied
	 * <code>message</code>.
	 * 
	 * @param message the exception message to use
	 * @see #assertCondition(boolean,String)
	 */
	private static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * Assert the provided boolean <code>condition</code>, throwing
	 * <code>AssertionError</code> with the supplied <code>message</code> if the
	 * test result is <code>false</code>.
	 * 
	 * @param condition a boolean expression
	 * @param message the exception message to use if the assertion fails
	 * @see #fail(String)
	 */
	private static void assertCondition(boolean condition, String message) {
		if (!condition) {
			fail(message);
		}
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
