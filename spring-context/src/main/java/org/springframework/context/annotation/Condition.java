/*
 * Copyright 2002-present the original author or authors.
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
