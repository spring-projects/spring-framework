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

import org.springframework.util.ReflectionUtils;

/**
 * Utility class that detects {@code org.mockito} annotations as well as the
 * annotations in this package (like {@link MockitoBeanSettings @MockitoBeanSettings}).
 *
 * @author Simon Baslé
 * @author Sam Brannen
 */
abstract class MockitoAnnotationDetector {

	private static final String MOCKITO_BEAN_PACKAGE = MockitoBeanSettings.class.getPackageName();

	private static final String ORG_MOCKITO_PACKAGE = "org.mockito";

	private static final Predicate<Annotation> isMockitoAnnotation = annotation -> {
			String packageName = annotation.annotationType().getPackageName();
			return (packageName.startsWith(MOCKITO_BEAN_PACKAGE) ||
					packageName.startsWith(ORG_MOCKITO_PACKAGE));
		};

	static boolean hasMockitoAnnotations(Class<?> testClass) {
		if (isAnnotated(testClass)) {
			return true;
		}
		// TODO Ideally we should short-circuit the search once we've found a Mockito annotation,
		// since there's no need to continue searching additional fields or further up the class
		// hierarchy; however, that is not possible with ReflectionUtils#doWithFields. Plus, the
		// previous invocation of isAnnotated(testClass) only finds annotations declared directly
		// on the test class. So, we'll likely need a completely different approach that combines
		// the "test class/interface is annotated?" and "field is annotated?" checks in a single
		// search algorithm.
		AtomicBoolean found = new AtomicBoolean();
		ReflectionUtils.doWithFields(testClass, field -> found.set(true), MockitoAnnotationDetector::isAnnotated);
		return found.get();
	}

	private static boolean isAnnotated(AnnotatedElement annotatedElement) {
		return Arrays.stream(annotatedElement.getAnnotations()).anyMatch(isMockitoAnnotation);
	}

}
