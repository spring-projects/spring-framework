/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * AOT processor that makes bean factory initialization contributions by
 * processing {@link ConfigurableListableBeanFactory} instances.
 *
 * <p>{@code BeanFactoryInitializationAotProcessor} implementations may be
 * registered in a {@value AotServices#FACTORIES_RESOURCE_LOCATION} resource or
 * as a bean.
 *
 * <p>Using this interface on a registered bean will cause the bean <em>and</em>
 * all of its dependencies to be initialized during AOT processing. We generally
 * recommend that this interface is only used with infrastructure beans such as
 * {@link BeanFactoryPostProcessor} which have limited dependencies and are
 * already initialized early in the bean factory lifecycle. If such a bean is
 * registered using a factory method, make sure to make it {@code static} so
 * that its enclosing class does not have to be initialized.
 *
 * <p>A component that implements this interface is not contributed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see BeanFactoryInitializationAotContribution
 */
@FunctionalInterface
public interface BeanFactoryInitializationAotProcessor {

	/**
	 * Process the given {@link ConfigurableListableBeanFactory} instance
	 * ahead-of-time and return a contribution or {@code null}.
	 * <p>Processors are free to use any techniques they like to analyze the given
	 * bean factory. Most typically use reflection to find fields or methods to
	 * use in the contribution. Contributions typically generate source code or
	 * resource files that can be used when the AOT optimized application runs.
	 * <p>If the given bean factory does not contain anything that is relevant to
	 * the processor, this method should return a {@code null} contribution.
	 * @param beanFactory the bean factory to process
	 * @return a {@link BeanFactoryInitializationAotContribution} or {@code null}
	 */
	@Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory);

}
