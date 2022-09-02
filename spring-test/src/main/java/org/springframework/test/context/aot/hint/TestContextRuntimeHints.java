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
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
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
		boolean servletPresent = ClassUtils.isPresent("jakarta.servlet.Servlet", classLoader);
		boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure", classLoader);

		ReflectionHints reflectionHints = runtimeHints.reflection();

		registerPublicConstructors(reflectionHints,
			// Loaded reflectively in BootstrapUtils
			org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.class,
			// Loaded reflectively in BootstrapUtils
			org.springframework.test.context.support.DefaultBootstrapContext.class,
			// Loaded reflectively in AbstractTestContextBootstrapper#resolveContextLoader()
			org.springframework.test.context.support.DelegatingSmartContextLoader.class
		);

		registerDeclaredConstructors(reflectionHints,
			// Loaded reflectively in BootstrapUtils
			org.springframework.test.context.support.DefaultTestContextBootstrapper.class
		);

		if (servletPresent) {
			registerPublicConstructors(reflectionHints,
				// Loaded reflectively in AbstractTestContextBootstrapper#resolveContextLoader()
				"org.springframework.test.context.web.WebDelegatingSmartContextLoader"
			);
			registerDeclaredConstructors(reflectionHints,
				// Loaded reflectively in BootstrapUtils
				"org.springframework.test.context.web.WebTestContextBootstrapper"
			);
		}

		if (groovyPresent) {
			registerDeclaredConstructors(reflectionHints,
				// Loaded reflectively in DelegatingSmartContextLoader
				"org.springframework.test.context.support.GenericGroovyXmlContextLoader"
			);
			if (servletPresent) {
				registerDeclaredConstructors(reflectionHints,
					// Loaded reflectively in WebDelegatingSmartContextLoader
					"org.springframework.test.context.web.GenericGroovyXmlWebContextLoader"
				);
			}
		}

		registerAnnotation(runtimeHints.reflection(),
			// Loaded reflectively in BootstrapUtils
			org.springframework.test.context.web.WebAppConfiguration.class
		);
	}

	private static void registerPublicConstructors(ReflectionHints reflectionHints, Class<?>... types) {
		registerPublicConstructors(reflectionHints, TypeReference.listOf(types));
	}

	private static void registerPublicConstructors(ReflectionHints reflectionHints, String... classNames) {
		registerPublicConstructors(reflectionHints, listOf(classNames));
	}

	private static void registerPublicConstructors(ReflectionHints reflectionHints, Iterable<TypeReference> types) {
		reflectionHints.registerTypes(types, TypeHint.builtWith(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	private static void registerDeclaredConstructors(ReflectionHints reflectionHints, Class<?>... types) {
		registerDeclaredConstructors(reflectionHints, TypeReference.listOf(types));
	}

	private static void registerDeclaredConstructors(ReflectionHints reflectionHints, String... classNames) {
		registerDeclaredConstructors(reflectionHints, listOf(classNames));
	}

	private static void registerDeclaredConstructors(ReflectionHints reflectionHints, Iterable<TypeReference> types) {
		reflectionHints.registerTypes(types, TypeHint.builtWith(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
	}

	private static List<TypeReference> listOf(String... classNames) {
		return Arrays.stream(classNames).map(TypeReference::of).toList();
	}

	private static void registerAnnotation(ReflectionHints reflectionHints, Class<? extends Annotation> annotationType) {
		reflectionHints.registerType(annotationType, MemberCategory.INVOKE_DECLARED_METHODS);
	}

}
