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

package org.springframework.test.context.aot.hint;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.support.RuntimeHintsUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes types and annotations
 * from the <em>Spring TestContext Framework</em> available at runtime.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestContextRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints runtimeHints, ClassLoader classLoader) {
		ReflectionHints reflectionHints = runtimeHints.reflection();
		boolean txPresent = ClassUtils.isPresent("org.springframework.transaction.annotation.Transactional", classLoader);
		boolean servletPresent = ClassUtils.isPresent("jakarta.servlet.Servlet", classLoader);
		boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure", classLoader);

		registerPublicConstructors(reflectionHints,
			org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.class,
			org.springframework.test.context.support.DefaultBootstrapContext.class,
			org.springframework.test.context.support.DelegatingSmartContextLoader.class
		);

		registerDeclaredConstructors(reflectionHints,
			org.springframework.test.context.support.DefaultTestContextBootstrapper.class
		);

		if (servletPresent) {
			registerPublicConstructors(reflectionHints,
				"org.springframework.test.context.web.WebDelegatingSmartContextLoader"
			);
			registerDeclaredConstructors(reflectionHints,
				"org.springframework.test.context.web.WebTestContextBootstrapper"
			);
		}

		if (groovyPresent) {
			registerDeclaredConstructors(reflectionHints,
				"org.springframework.test.context.support.GenericGroovyXmlContextLoader"
			);
			if (servletPresent) {
				registerDeclaredConstructors(reflectionHints,
					"org.springframework.test.context.web.GenericGroovyXmlWebContextLoader"
				);
			}
		}

		registerSynthesizedAnnotation(runtimeHints,
			// Legacy and JUnit 4
			org.springframework.test.annotation.Commit.class,
			org.springframework.test.annotation.DirtiesContext.class,
			org.springframework.test.annotation.IfProfileValue.class,
			org.springframework.test.annotation.ProfileValueSourceConfiguration.class,
			org.springframework.test.annotation.Repeat.class,
			org.springframework.test.annotation.Rollback.class,
			org.springframework.test.annotation.Timed.class,

			// Core TestContext framework
			org.springframework.test.context.ActiveProfiles.class,
			org.springframework.test.context.BootstrapWith.class,
			org.springframework.test.context.ContextConfiguration.class,
			org.springframework.test.context.ContextHierarchy.class,
			org.springframework.test.context.DynamicPropertySource.class,
			org.springframework.test.context.NestedTestConfiguration.class,
			org.springframework.test.context.TestConstructor.class,
			org.springframework.test.context.TestExecutionListeners.class,
			org.springframework.test.context.TestPropertySource.class,
			org.springframework.test.context.TestPropertySources.class,

			// Application Events
			org.springframework.test.context.event.RecordApplicationEvents.class,

			// JUnit Jupiter
			org.springframework.test.context.junit.jupiter.EnabledIf.class,
			org.springframework.test.context.junit.jupiter.DisabledIf.class,
			org.springframework.test.context.junit.jupiter.SpringJUnitConfig.class,
			org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig.class,

			// Web
			org.springframework.test.context.web.WebAppConfiguration.class
		);

		if (txPresent) {
			registerSynthesizedAnnotation(runtimeHints,
				org.springframework.test.context.jdbc.Sql.class,
				org.springframework.test.context.jdbc.SqlConfig.class,
				org.springframework.test.context.jdbc.SqlGroup.class,
				org.springframework.test.context.jdbc.SqlMergeMode.class,
				org.springframework.test.context.transaction.AfterTransaction.class,
				org.springframework.test.context.transaction.BeforeTransaction.class
			);
		}
	}

	private static void registerPublicConstructors(ReflectionHints reflectionHints, Class<?>... types) {
		reflectionHints.registerTypes(TypeReference.listOf(types),
				builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	private static void registerPublicConstructors(ReflectionHints reflectionHints, String... classNames) {
		reflectionHints.registerTypes(listOf(classNames),
				builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	private static void registerDeclaredConstructors(ReflectionHints reflectionHints, Class<?>... types) {
		reflectionHints.registerTypes(TypeReference.listOf(types),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
	}

	private static void registerDeclaredConstructors(ReflectionHints reflectionHints, String... classNames) {
		reflectionHints.registerTypes(listOf(classNames),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
	}

	private static List<TypeReference> listOf(String... classNames) {
		return Arrays.stream(classNames).map(TypeReference::of).toList();
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	private static void registerSynthesizedAnnotation(RuntimeHints runtimeHints, Class<? extends Annotation>... annotationTypes) {
		for (Class<? extends Annotation> annotationType : annotationTypes) {
			registerAnnotation(runtimeHints.reflection(), annotationType);
			RuntimeHintsUtils.registerSynthesizedAnnotation(runtimeHints, annotationType);
		}
	}

	private static void registerAnnotation(ReflectionHints reflectionHints, Class<? extends Annotation> annotationType) {
		reflectionHints.registerType(annotationType,
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));
	}

}
