/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.env;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

class PropertyPlaceholderConfigurerEnvironmentIntegrationTests {

	@Test
	@SuppressWarnings({"deprecation", "removal"})
	void test() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("ppc",
				rootBeanDefinition(org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class)
				.addPropertyValue("searchSystemEnvironment", false)
				.getBeanDefinition());
		ctx.refresh();
		ctx.getBean("ppc");
		ctx.close();
	}

}
