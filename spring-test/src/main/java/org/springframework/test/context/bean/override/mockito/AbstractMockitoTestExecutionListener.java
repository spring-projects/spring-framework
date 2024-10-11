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

import java.lang.reflect.Field;
import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

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

	private static final Predicate<MergedAnnotation<?>> isMockitoAnnotation = mergedAnnotation -> {
			String packageName = mergedAnnotation.getType().getPackageName();
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

	/**
	 * Determine if Mockito annotations are declared on the supplied class, on an
	 * interface it implements, on a superclass, or on an enclosing class or
	 * whether a field in any such class is annotated with a Mockito annotation.
	 */
	private static boolean hasMockitoAnnotations(Class<?> clazz) {
		// Declared on the class?
		if (MergedAnnotations.from(clazz, SearchStrategy.DIRECT).stream().anyMatch(isMockitoAnnotation)) {
			return true;
		}

		// Declared on a field?
		for (Field field : clazz.getDeclaredFields()) {
			if (MergedAnnotations.from(field, SearchStrategy.DIRECT).stream().anyMatch(isMockitoAnnotation)) {
				return true;
			}
		}

		// Declared on an interface?
		for (Class<?> ifc : clazz.getInterfaces()) {
			if (hasMockitoAnnotations(ifc)) {
				return true;
			}
		}

		// Declared on a superclass?
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null & superclass != Object.class) {
			if (hasMockitoAnnotations(superclass)) {
				return true;
			}
		}

		// Declared on an enclosing class of an inner class?
		if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
			if (hasMockitoAnnotations(clazz.getEnclosingClass())) {
				return true;
			}
		}

		return false;
	}

}
