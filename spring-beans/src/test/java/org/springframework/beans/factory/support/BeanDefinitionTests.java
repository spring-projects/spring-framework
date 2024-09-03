/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class BeanDefinitionTests {

	@Test
	public void beanDefinitionEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithPropertyValues() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getPropertyValues().add("name", "myName");
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getPropertyValues().add("age", "11");
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getPropertyValues().add("age", "99");
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test");
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 9);
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	public void beanDefinitionEqualityWithTypedConstructorArguments() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		otherBd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "int");
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	public void genericBeanDefinitionEquality() {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName("parent");
		bd.setScope("request");
		bd.setAbstract(true);
		bd.setLazyInit(true);
		GenericBeanDefinition otherBd = new GenericBeanDefinition();
		otherBd.setScope("request");
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.setParentName("parent");
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());

		bd.getPropertyValues();
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());

		bd.getConstructorArgumentValues();
		assertThat(bd.equals(otherBd)).isTrue();
		assertThat(otherBd.equals(bd)).isTrue();
		assertThat(bd.hashCode()).isEqualTo(otherBd.hashCode());
	}

	@Test
	public void beanDefinitionHolderEquality() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setAbstract(true);
		bd.setLazyInit(true);
		bd.setScope("request");
		BeanDefinitionHolder holder = new BeanDefinitionHolder(bd, "bd");
		RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
		assertThat(!bd.equals(otherBd)).isTrue();
		assertThat(!otherBd.equals(bd)).isTrue();
		otherBd.setAbstract(true);
		otherBd.setLazyInit(true);
		otherBd.setScope("request");
		BeanDefinitionHolder otherHolder = new BeanDefinitionHolder(bd, "bd");
		assertThat(holder.equals(otherHolder)).isTrue();
		assertThat(otherHolder.equals(holder)).isTrue();
		assertThat(holder.hashCode()).isEqualTo(otherHolder.hashCode());
	}

	@Test
	public void beanDefinitionMerging() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test");
		bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
		bd.getPropertyValues().add("name", "myName");
		bd.getPropertyValues().add("age", "99");
		bd.setQualifiedElement(getClass());

		GenericBeanDefinition childBd = new GenericBeanDefinition();
		childBd.setParentName("bd");

		RootBeanDefinition mergedBd = new RootBeanDefinition(bd);
		mergedBd.overrideFrom(childBd);
		assertThat(mergedBd.getConstructorArgumentValues().getArgumentCount()).isEqualTo(2);
		assertThat(mergedBd.getPropertyValues()).hasSize(2);
		assertThat(mergedBd).isEqualTo(bd);

		mergedBd.getConstructorArgumentValues().getArgumentValue(1, null).setValue(9);
		assertThat(bd.getConstructorArgumentValues().getArgumentValue(1, null).getValue()).isEqualTo(5);
		assertThat(bd.getQualifiedElement()).isEqualTo(getClass());
	}

}
