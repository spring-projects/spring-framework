/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class SimpleComponentRegistrar implements ComponentRegistrar {

	private final BeanDefinitionRegistry registry;

	public SimpleComponentRegistrar(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	public String registerWithGeneratedName(BeanDefinition beanDefinition) {
		return BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, this.registry);
	}

	public void registerBeanComponent(BeanComponentDefinition component) {
		BeanDefinitionReaderUtils.registerBeanDefinition(component, this.registry);
		registerComponent(component);
	}

	public void registerComponent(ComponentDefinition component) {
		// no-op
	}
}
