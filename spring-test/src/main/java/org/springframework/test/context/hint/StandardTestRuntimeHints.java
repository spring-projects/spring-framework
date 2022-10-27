/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.hint;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.aot.TestRuntimeHintsRegistrar;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * {@link TestRuntimeHintsRegistrar} implementation that registers run-time hints
 * for standard functionality in the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see TestContextRuntimeHints
 */
class StandardTestRuntimeHints implements TestRuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader) {
		// @ActiveProfiles(resolver = ...)
		MergedAnnotations.search(TYPE_HIERARCHY)
				.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
				.from(testClass)
				.stream(ActiveProfiles.class)
				.map(mergedAnnotation -> mergedAnnotation.getClass("resolver"))
				.filter(type -> type != ActiveProfilesResolver.class)
				.forEach(resolverClass -> registerDeclaredConstructors(resolverClass, runtimeHints));
	}

	private void registerDeclaredConstructors(Class<?> type, RuntimeHints runtimeHints) {
		runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
	}

}
