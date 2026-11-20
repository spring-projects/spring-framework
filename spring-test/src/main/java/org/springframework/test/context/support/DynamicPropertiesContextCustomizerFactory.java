/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} which supports
 * {@link DynamicPropertySource @DynamicPropertySource} methods in test classes
 * and {@link org.springframework.test.context.DynamicPropertyRegistrar
 * DynamicPropertyRegistrar} beans in the container.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Yanming Zhou
 * @since 5.2.5
 * @see DynamicPropertiesContextCustomizer
 */
class DynamicPropertiesContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable DynamicPropertiesContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		Set<Method> methods = new LinkedHashSet<>();
		findMethods(testClass, methods);
		if (methods.isEmpty()) {
			methods = Collections.emptySet();
		}
		return new DynamicPropertiesContextCustomizer(methods);
	}

	private void findMethods(Class<?> testClass, Set<Method> methods) {
		// Beginning with Java 16, inner classes may contain static members.
		// We therefore need to search for @DynamicPropertySource methods in the
		// current class after searching enclosing classes so that a local
		// @DynamicPropertySource method can override properties registered in
		// an enclosing class.
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			findMethods(testClass.getEnclosingClass(), methods);
		}
		methods.addAll(MethodIntrospector.selectMethods(testClass, this::isAnnotated));
	}

	private boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

}
