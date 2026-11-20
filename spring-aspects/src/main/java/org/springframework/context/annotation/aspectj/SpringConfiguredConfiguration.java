/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.aspectj;

import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration} class that registers an {@code AnnotationBeanConfigurerAspect}
 * capable of performing dependency injection services for non-Spring managed objects
 * annotated with @{@link org.springframework.beans.factory.annotation.Configurable
 * Configurable}.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableSpringConfigured @EnableSpringConfigured} annotation. See
 * {@code @EnableSpringConfigured}'s javadoc for complete usage details.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableSpringConfigured
 */
@Configuration
public class SpringConfiguredConfiguration {

	/**
	 * The bean name used for the configurer aspect.
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	@Bean(name = BEAN_CONFIGURER_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationBeanConfigurerAspect beanConfigurerAspect() {
		return AnnotationBeanConfigurerAspect.aspectOf();
	}

}
