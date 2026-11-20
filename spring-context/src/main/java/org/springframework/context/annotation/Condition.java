/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A single condition that must be {@linkplain #matches matched} in order for a
 * component to be registered.
 *
 * <p>Conditions are checked immediately before the bean definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 *
 * <p>Conditions must follow the same restrictions as a {@link BeanFactoryPostProcessor}
 * and take care to never interact with bean instances. For more fine-grained control
 * over conditions that interact with {@code @Configuration} beans, consider implementing
 * the {@link ConfigurationCondition} interface.
 *
 * <p>Multiple conditions on a given class or on a given method will be ordered
 * according to the semantics of Spring's {@link org.springframework.core.Ordered}
 * interface and {@link org.springframework.core.annotation.Order @Order} annotation.
 * See {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
 * for details.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see ConfigurationCondition
 * @see Conditional
 * @see ConditionContext
 */
@FunctionalInterface
public interface Condition {

	/**
	 * Determine if the condition matches.
	 * @param context the condition context
	 * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
	 * @return {@code true} if the condition matches and the component can be registered,
	 * or {@code false} to veto the annotated component's registration
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

}
