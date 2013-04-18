/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.InitializationError;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;

/**
 * <p>
 * {@code SpringJUnit4ClassRunner} is a custom extension of {@link BlockJUnit4ClassRunner}
 * which provides functionality of the <em>Spring TestContext Framework</em> to standard
 * JUnit 4.5+ tests by means of the {@link TestContextManager} and associated support
 * classes and annotations.
 * </p>
 * <p>
 * The following list constitutes all annotations currently supported directly by
 * {@code SpringJUnit4ClassRunner}.
 * <em>(Note that additional annotations may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners})</em>
 * </p>
 * <ul>
 * <li>{@link Test#expected() &#064;Test(expected=...)}</li>
 * <li>{@link ExpectedException &#064;ExpectedException}</li>
 * <li>{@link Test#timeout() &#064;Test(timeout=...)}</li>
 * <li>{@link Timed &#064;Timed}</li>
 * <li>{@link Repeat &#064;Repeat}</li>
 * <li>{@link Ignore &#064;Ignore}</li>
 * <li>{@link Parameters &#064;Parameters}</li>
 * <li>
 * {@link org.springframework.test.annotation.ProfileValueSourceConfiguration
 * &#064;ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue &#064;IfProfileValue}</li>
 * </ul>
 * <p>
 * <b>NOTE:</b> As of Spring 3.0, {@code SpringJUnit4ClassRunner} requires JUnit 4.5+.
 * </p>
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Gaetan Pitteloud
 * @since 2.5
 * @see TestContextManager
 */
public class SpringJUnit4ClassRunner extends Runner implements Filterable, Sortable {

	private final Runner internalRunner;

	public SpringJUnit4ClassRunner(Class<?> testClass) throws InitializationError {
		List<Method> parametersMethods = findParametersMethods(testClass);
		int nbMethods = parametersMethods.size();
		switch (nbMethods) {
			case 0:
				internalRunner = new InternalSpringJUnit4ClassRunner(testClass);
				break;
			case 1:
				internalRunner = new SpringJUnit4ParameterizedClassRunner(testClass,
						parametersMethods.get(0));
				break;
			default:
				throw new InitializationError(String.format(
						"More than one @Parameters method found on %s", testClass));
		}
	}

	@Override
	public Description getDescription() {
		return internalRunner.getDescription();
	}

	@Override
	public void run(RunNotifier notifier) {
		internalRunner.run(notifier);
	}

	public void filter(Filter filter) throws NoTestsRemainException {
		((Filterable) internalRunner).filter(filter);
	}

	public void sort(Sorter sorter) {
		((Sortable) internalRunner).sort(sorter);
	}

	/**
	 * Gets all methods in the supplied {@link Class class} and its superclasses which are
	 * annotated with {@link Parameters}.
	 * 
	 * @param clazz the class for which to retrieve the annotated methods
	 * @return all annotated methods in the supplied class and its superclasses
	 */
	private List<Method> findParametersMethods(Class<?> clazz) {
		Class<? extends Annotation> annotationType = Parameters.class;
		List<Method> results = new ArrayList<Method>();
		for (Class<?> eachClass : getSuperClasses(clazz)) {
			Method[] methods = eachClass.getDeclaredMethods();
			for (Method eachMethod : methods) {
				Annotation annotation = eachMethod.getAnnotation(annotationType);
				if (annotation != null) {
					// no need to check for overridden/shadowed methods:
					// the method must be static (checked later)
					results.add(eachMethod);
				}
			}
		}
		return results;
	}

	/**
	 * Gets all superclasses of the supplied {@link Class class}, including the class
	 * itself. The ordering of the returned list will begin with the supplied class and
	 * continue up the class hierarchy.
	 * <p>
	 * Note: This code has been borrowed from
	 * {@link org.junit.internal.runners.TestClass#getSuperClasses(Class)} and adapted.
	 * 
	 * @param clazz the class for which to retrieve the superclasses.
	 * @return all superclasses of the supplied class.
	 */
	private List<Class<?>> getSuperClasses(Class<?> clazz) {
		ArrayList<Class<?>> results = new ArrayList<Class<?>>();
		Class<?> current = clazz;
		while (current != null) {
			results.add(current);
			current = current.getSuperclass();
		}
		return results;
	}
}
