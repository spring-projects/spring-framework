/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;

/**
 * Collection of utilities related to autowiring of individual method parameters.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see #isAutowirable(Parameter)
 * @see #resolveDependency(Parameter, Class, ApplicationContext)
 */
abstract class ParameterAutowireUtils {

	private ParameterAutowireUtils() {
		/* no-op */
	}

	/**
	 * Determine if the supplied {@link Parameter} can potentially be
	 * autowired from an {@link ApplicationContext}.
	 * <p>Returns {@code true} if the supplied parameter is of type
	 * {@link ApplicationContext} (or a sub-type thereof) or is annotated or
	 * meta-annotated with {@link Autowired @Autowired},
	 * {@link Qualifier @Qualifier}, or {@link Value @Value}.
	 * @see #resolveDependency(Parameter, Class, ApplicationContext)
	 */
	static boolean isAutowirable(Parameter parameter) {
		return ApplicationContext.class.isAssignableFrom(parameter.getType())
				|| AnnotatedElementUtils.hasAnnotation(parameter, Autowired.class)
				|| AnnotatedElementUtils.hasAnnotation(parameter, Qualifier.class)
				|| AnnotatedElementUtils.hasAnnotation(parameter, Value.class);
	}

	/**
	 * Resolve the dependency for the supplied {@link Parameter} from the
	 * supplied {@link ApplicationContext}.
	 * <p>Provides comprehensive autowiring support for individual method parameters
	 * on par with Spring's dependency injection facilities for autowired fields and
	 * methods, including support for {@link Autowired @Autowired},
	 * {@link Qualifier @Qualifier}, and {@link Value @Value} with support for property
	 * placeholders and SpEL expressions in {@code @Value} declarations.
	 * <p>The dependency is required unless the parameter is annotated with
	 * {@link Autowired @Autowired} with the {@link Autowired#required required}
	 * flag set to {@code false}.
	 * <p>If an explicit <em>qualifier</em> is not declared, the name of the parameter
	 * will be used as the qualifier for resolving ambiguities.
	 * @param parameter the parameter whose dependency should be resolved
	 * @param containingClass the concrete class that contains the parameter; this may
	 * differ from the class that declares the parameter in that it may be a subclass
	 * thereof, potentially substituting type variables
	 * @param applicationContext the application context from which to resolve the
	 * dependency
	 * @return the resolved object, or {@code null} if none found
	 * @throws BeansException if dependency resolution failed
	 * @see #isAutowirable(Parameter)
	 * @see Autowired#required
	 * @see SynthesizingMethodParameter#forParameter(Parameter)
	 * @see AutowireCapableBeanFactory#resolveDependency(DependencyDescriptor, String)
	 */
	@Nullable
	static Object resolveDependency(Parameter parameter, Class<?> containingClass, ApplicationContext applicationContext) {
		boolean required = findMergedAnnotation(parameter, Autowired.class).map(Autowired::required).orElse(true);
		MethodParameter methodParameter = SynthesizingMethodParameter.forParameter(parameter);
		DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
		descriptor.setContainingClass(containingClass);
		return applicationContext.getAutowireCapableBeanFactory().resolveDependency(descriptor, null);
	}

	private static <A extends Annotation> Optional<A> findMergedAnnotation(AnnotatedElement element, Class<A> annotationType) {
		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
	}

}
