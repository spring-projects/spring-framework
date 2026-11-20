/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.type.MethodMetadata;

/**
 * Extended variant of {@link FullyQualifiedAnnotationBeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only enforcing
 * fully-qualified names for component and configuration classes themselves
 * but also fully-qualified default bean names ("className.methodName") for
 * {@link Bean @Bean} methods. By default, this only affects methods without
 * an explicit {@link Bean#name() name} attribute specified.
 *
 * <p>This provides an alternative to the default bean name generation for
 * {@code @Bean} methods (which uses the plain method name), primarily for use
 * in large applications with potential bean name overlaps. Favor this bean
 * naming strategy over {@code FullyQualifiedAnnotationBeanNameGenerator} if
 * you expect such naming conflicts for {@code @Bean} methods.
 *
 * <p>As of 7.1, the original {@code @Bean} method name will be registered
 * as an alias if that name has not been taken already: effectively on first
 * occurrence, whereas any later {@code @Bean} methods of the same name will
 * not have aliases applied. This preserves the availability of common beans
 * under the original bean names for retrieval and injection purposes.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see AnnotationBeanNameGenerator
 * @see FullyQualifiedAnnotationBeanNameGenerator
 * @see AnnotationConfigApplicationContext#setBeanNameGenerator
 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
 */
public class FullyQualifiedConfigurationBeanNameGenerator extends FullyQualifiedAnnotationBeanNameGenerator
		implements ConfigurationBeanNameGenerator {

	/**
	 * A convenient constant for a default {@code FullyQualifiedConfigurationBeanNameGenerator}
	 * instance, as used for configuration-level import purposes.
	 */
	public static final FullyQualifiedConfigurationBeanNameGenerator INSTANCE =
			new FullyQualifiedConfigurationBeanNameGenerator();


	@Override
	public String deriveBeanName(MethodMetadata beanMethod, @Nullable String beanName) {
		return (beanName != null ? beanName : beanMethod.getDeclaringClassName() + "." + beanMethod.getMethodName());
	}

}
