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
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class BeanDefinitionBuilderTests {

	@Test
	void builderWithBeanClassWithSimpleProperty() {
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
	void builderWithBeanClassAndFactoryMethod() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class, "create");
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isTrue();
		assertThat(rbd.getBeanClass()).isEqualTo(TestBean.class);
		assertThat(rbd.getFactoryMethodName()).isEqualTo("create");
	}

	@Test
	void builderWithBeanClassName() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class.getName());
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isFalse();
		assertThat(rbd.getBeanClassName()).isEqualTo(TestBean.class.getName());
	}

	@Test
	void builderWithBeanClassNameAndFactoryMethod() {
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class.getName(), "create");
		RootBeanDefinition rbd = (RootBeanDefinition) bdb.getBeanDefinition();
		assertThat(rbd.hasBeanClass()).isFalse();
		assertThat(rbd.getBeanClassName()).isEqualTo(TestBean.class.getName());
		assertThat(rbd.getFactoryMethodName()).isEqualTo("create");
	}

	@Test
	void builderWithResolvableTypeAndInstanceSupplier() {
		ResolvableType type = ResolvableType.forClassWithGenerics(Function.class, Integer.class, String.class);
		Function<Integer, String> function = i -> "value " + i;
		RootBeanDefinition rbd = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(type, () -> function).getBeanDefinition();
		assertThat(rbd.getResolvableType()).isEqualTo(type);
		assertThat(rbd.getInstanceSupplier()).isNotNull();
		assertThat(rbd.getInstanceSupplier().get()).isInstanceOf(Function.class);
	}

	@Test
	void builderWithBeanClassAndInstanceSupplier() {
		RootBeanDefinition rbd = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(String.class, () -> "test").getBeanDefinition();
		assertThat(rbd.getResolvableType().resolve()).isEqualTo(String.class);
		assertThat(rbd.getInstanceSupplier()).isNotNull();
		assertThat(rbd.getInstanceSupplier().get()).isEqualTo("test");
	}

	@Test
	void builderWithAutowireMode() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE).getBeanDefinition().getAutowireMode())
				.isEqualTo(RootBeanDefinition.AUTOWIRE_BY_TYPE);
	}

	@Test
	void builderWithDependencyCheck() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_ALL)
				.getBeanDefinition().getDependencyCheck())
				.isEqualTo(RootBeanDefinition.DEPENDENCY_CHECK_ALL);
	}

	@Test
	void builderWithDependsOn() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).addDependsOn("test")
				.addDependsOn("test2").getBeanDefinition().getDependsOn())
				.containsExactly("test", "test2");
	}

	@Test
	void builderWithPrimary() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.setPrimary(true).getBeanDefinition().isPrimary()).isTrue();
	}

	@Test
	void builderWithRole() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition().getRole())
				.isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

	@Test
	void builderWithSynthetic() {
		assertThat(BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.setSynthetic(true).getBeanDefinition().isSynthetic()).isTrue();
	}

	@Test
	void builderWithCustomizers() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class)
				.applyCustomizers(builder -> {
					builder.setFactoryMethodName("create");
					builder.setRole(BeanDefinition.ROLE_SUPPORT);
				})
				.applyCustomizers(builder -> builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE))
				.getBeanDefinition();
		assertThat(beanDefinition.getFactoryMethodName()).isEqualTo("create");
		assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

}
