/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract base class for {@code TestExecutionListener} implementations involving
 * Mockito.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 6.2
 */
abstract class AbstractMockitoTestExecutionListener extends AbstractTestExecutionListener {

	static final boolean mockitoPresent = ClassUtils.isPresent("org.mockito.Mockito",
			AbstractMockitoTestExecutionListener.class.getClassLoader());

	private static final String SPRING_MOCKITO_PACKAGE = "org.springframework.test.context.bean.override.mockito";

	private static final String ORG_MOCKITO_PACKAGE = "org.mockito";

	private static final Predicate<Annotation> isMockitoAnnotation = annotation -> {
			String packageName = annotation.annotationType().getPackageName();
			return (packageName.startsWith(SPRING_MOCKITO_PACKAGE) ||
					packageName.startsWith(ORG_MOCKITO_PACKAGE));
		};


	/**
	 * Determine if the test class for the supplied {@linkplain TestContext
	 * test context} uses {@code org.mockito} annotations or any of the annotations
	 * in this package (such as {@link MockitoBeanSettings @MockitoBeanSettings}).
	 */
	static boolean hasMockitoAnnotations(TestContext testContext) {
		return hasMockitoAnnotations(testContext.getTestClass());
	}

	private static boolean hasMockitoAnnotations(Class<?> testClass) {
		if (isAnnotated(testClass)) {
			return true;
		}
		// TODO Ideally we should short-circuit the search once we've found a Mockito annotation,
		// since there's no need to continue searching additional fields or further up the class
		// hierarchy; however, that is not possible with ReflectionUtils#doWithFields. Plus, the
		// previous invocation of isAnnotated(testClass) only finds annotations declared directly
		// on the test class. So, we'll likely need a completely different approach that combines
		// the "test class/interface is annotated?" and "field is annotated?" checks in a single
		// search algorithm, and we'll also need to support @Nested class hierarchies.
		AtomicBoolean found = new AtomicBoolean();
		ReflectionUtils.doWithFields(testClass,
				field -> found.set(true), AbstractMockitoTestExecutionListener::isAnnotated);
		return found.get();
	}

	private static boolean isAnnotated(AnnotatedElement annotatedElement) {
		return Arrays.stream(annotatedElement.getAnnotations()).anyMatch(isMockitoAnnotation);
	}

}
