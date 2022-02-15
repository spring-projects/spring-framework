/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Strategy interface to be implemented by a {@link BeanPostProcessor} that
 * participates in a bean instance setup and can provide an equivalent setup
 * using generated code.
 *
 * <p>Contrary to other bean post processors, implementations of this interface
 * are instantiated at build-time and should not rely on other beans in the
 * context.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface BeanInstanceAotPostProcessor extends BeanPostProcessor {

	/**
	 * Build a {@link BeanInstanceContributor} for the given bean definition.
	 * @param beanDefinition the merged bean definition for the bean
	 * @param beanType the actual type of the managed bean instance
	 * @param beanName the name of the bean
	 * @return the contributor to use
	 */
	BeanInstanceContributor buildAotContributor(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

}
