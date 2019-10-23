/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class BeanDefinitionBuilderTests {

	@Test
	public void beanClassWithSimpleProperty() {
		String[] dependsOn = new String[] { "A", "B", "C" };
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		bdb.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bdb.addPropertyValue("age", "15");
		for (String dependsOnEntry : dependsOn) {
			bdb.addDependsOn(dependsOnEntry);
		}

		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.isSingleton()).isFalse();
		assertThat(rbd.getBeanClass()).isEqualTo(TestBean.class);
		assertThat(Arrays.equals(dependsOn, rbd.getDependsOn())).as("Depends on was added").isTrue();
		assertThat(rbd.getPropertyValues().contains("age")).isTrue();
	}

	@Test
	public void beanClassWithFactoryMethod() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class, "create");
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isTrue();
		assertThat(rbd.getBeanClass()).isEqualTo(TestBean.class);
		assertThat(rbd.getFactoryMethodName()).isEqualTo("create");
	}

	@Test
	public void beanClassName() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class.getName());
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isFalse();
		assertThat(rbd.getBeanClassName()).isEqualTo(TestBean.class.getName());
	}

	@Test
	public void beanClassNameWithFactoryMethod() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class.getName(), "create");
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isFalse();
		assertThat(rbd.getBeanClassName()).isEqualTo(TestBean.class.getName());
		assertThat(rbd.getFactoryMethodName()).isEqualTo("create");
	}

}
