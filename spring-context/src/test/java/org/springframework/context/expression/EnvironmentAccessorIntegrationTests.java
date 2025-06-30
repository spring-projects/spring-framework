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

package org.springframework.context.expression;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * Integration tests for {@link EnvironmentAccessor}, which is registered with
 * SpEL for all {@link AbstractApplicationContext} implementations via
 * {@link StandardBeanExpressionResolver}.
 *
 * @author Chris Beams
 */
class EnvironmentAccessorIntegrationTests {

	@Test
	void braceAccess() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "#{environment['my.name']}")
					.getBeanDefinition());

		GenericApplicationContext ctx = new GenericApplicationContext(bf);
		ctx.getEnvironment().getPropertySources().addFirst(new MockPropertySource().withProperty("my.name", "myBean"));
		ctx.refresh();

		assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("myBean");
		ctx.close();
	}

}
