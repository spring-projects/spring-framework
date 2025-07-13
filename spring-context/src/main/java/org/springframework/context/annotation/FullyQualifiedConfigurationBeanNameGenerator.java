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

import org.springframework.core.type.MethodMetadata;

/**
 * Extended variant of {@link FullyQualifiedAnnotationBeanNameGenerator} for
 * {@link Configuration @Configuration} class purposes, not only enforcing
 * fully-qualified names for component and configuration classes themselves
 * but also fully-qualified default bean names ("className.methodName") for
 * {@link Bean @Bean} methods. This only affects methods without an explicit
 * {@link Bean#name() name} attribute specified.
 *
 * <p>This provides an alternative to the default bean name generation for
 * {@code @Bean} methods (which uses the plain method name), primarily for use
 * in large applications with potential bean name overlaps. Favor this bean
 * naming strategy over {@code FullyQualifiedAnnotationBeanNameGenerator} if
 * you expect such naming conflicts for {@code @Bean} methods, as long as the
 * application does not depend on {@code @Bean} method names as bean names.
 * Where the name does matter, make sure to declare {@code @Bean("myBeanName")}
 * in such a scenario, even if it repeats the method name as the bean name.
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
	public String deriveBeanName(MethodMetadata beanMethod) {
		return beanMethod.getDeclaringClassName() + "." + beanMethod.getMethodName();
	}

}
