/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.core.type.MethodMetadata;

/**
 * Extended {@link org.springframework.beans.factory.config.BeanDefinition} for beans
 * defined using {@code @Bean} methods. Exposes
 * {@link org.springframework.core.type.AnnotationMetadata} about its bean class and
 * {@link org.springframework.core.type.MethodMetadata} about its {@code @Bean} method -
 * without requiring the class to be loaded yet.
 *
 * @author Phillip Webb
 * @since 4.1
 * @see AnnotatedBeanDefinition
 * @see org.springframework.core.type.AnnotationMetadata
 * @see org.springframework.core.type.MethodMetadata
 */
public interface BeanMethodBeanDefinition extends AnnotatedBeanDefinition {

	/**
	 * Obtain the method metadata for this bean definition's {@code @Bean} method.
	 * @return the {@code @Bean} method metadata object (never {@code null})
	 */
	MethodMetadata getBeanMethodMetadata();

}
