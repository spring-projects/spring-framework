/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.MethodMetadata;

/**
 * Extended variant of {@link BeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only covering
 * bean name generation for component and configuration classes themselves
 * but also for {@link Bean @Bean} methods.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 7.0
 * @see AnnotationConfigApplicationContext#setBeanNameGenerator
 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
 */
public interface ConfigurationBeanNameGenerator extends BeanNameGenerator {

	/**
	 * Derive a default bean name for the given {@link Bean @Bean} method,
	 * taking into account the specified {@link Bean#name() name} attribute.
	 * @param beanMethod the method metadata for the {@link Bean @Bean} method
	 * @param beanName the {@link Bean#name() name} attribute or {@code null} if
	 * none is specified
	 * @return the default bean name to use
	 */
	String deriveBeanName(MethodMetadata beanMethod, @Nullable String beanName);

}
