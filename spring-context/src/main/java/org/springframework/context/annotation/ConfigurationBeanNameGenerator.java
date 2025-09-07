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

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.MethodMetadata;

/**
 * Extended variant of {@link BeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only covering
 * bean name generation for component and configuration classes themselves
 * but also for {@link Bean @Bean} methods without a {@link Bean#name() name}
 * attribute specified on the annotation itself.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see AnnotationConfigApplicationContext#setBeanNameGenerator
 * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
 */
public interface ConfigurationBeanNameGenerator extends BeanNameGenerator {

	/**
	 * Derive a default bean name for the given {@link Bean @Bean} method,
	 * in the absence of a {@link Bean#name() name} attribute specified.
	 * @param beanMethod the method metadata for the {@link Bean @Bean} method
	 * @return the default bean name to use
	 */
	String deriveBeanName(MethodMetadata beanMethod);

}
