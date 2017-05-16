/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Common delegate code for the handling of parser strategies, e.g.
 * {@code TypeFilter}, {@code ImportSelector}, {@code ImportBeanDefinitionRegistrar}
 *
 * @author Juergen Hoeller
 * @since 4.3.3
 */
abstract class ParserStrategyUtils {

	/**
	 * Invoke {@link BeanClassLoaderAware}, {@link BeanFactoryAware},
	 * {@link EnvironmentAware}, and {@link ResourceLoaderAware} contracts
	 * if implemented by the given object.
	 */
	public static void invokeAwareMethods(Object parserStrategyBean, Environment environment,
			ResourceLoader resourceLoader, BeanDefinitionRegistry registry) {

		if (parserStrategyBean instanceof Aware) {
			if (parserStrategyBean instanceof BeanClassLoaderAware) {
				ClassLoader classLoader = (registry instanceof ConfigurableBeanFactory ?
						((ConfigurableBeanFactory) registry).getBeanClassLoader() : resourceLoader.getClassLoader());
				((BeanClassLoaderAware) parserStrategyBean).setBeanClassLoader(classLoader);
			}
			if (parserStrategyBean instanceof BeanFactoryAware && registry instanceof BeanFactory) {
				((BeanFactoryAware) parserStrategyBean).setBeanFactory((BeanFactory) registry);
			}
			if (parserStrategyBean instanceof EnvironmentAware) {
				((EnvironmentAware) parserStrategyBean).setEnvironment(environment);
			}
			if (parserStrategyBean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) parserStrategyBean).setResourceLoader(resourceLoader);
			}
		}
	}

}
