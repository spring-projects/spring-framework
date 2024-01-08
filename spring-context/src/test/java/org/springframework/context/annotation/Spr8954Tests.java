/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, RootBeanDefinition)} now ensures
 * that not only the predicted bean type is considered, but also the original bean
 * definition's beanClass.
 *
 * @author Chris Beams
 * @author Oliver Gierke
 */
@SuppressWarnings("resource")
public class Spr8954Tests {

	@Test
	void repro() {
		AnnotationConfigApplicationContext bf = new AnnotationConfigApplicationContext();
		bf.registerBeanDefinition("fooConfig", new RootBeanDefinition(FooConfig.class));
		bf.getBeanFactory().addBeanPostProcessor(new PredictingBPP());
		bf.refresh();

		assertThat(bf.getBean("foo")).isInstanceOf(Foo.class);
		assertThat(bf.getBean("&foo")).isInstanceOf(FooFactoryBean.class);

		assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(fbBeans).containsOnlyKeys("&foo");

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(aiBeans).containsOnlyKeys("&foo");
	}

	@Test
	void findsBeansByTypeIfNotInstantiated() {
		AnnotationConfigApplicationContext bf = new AnnotationConfigApplicationContext();
		bf.registerBeanDefinition("fooConfig", new RootBeanDefinition(FooConfig.class));
		bf.getBeanFactory().addBeanPostProcessor(new PredictingBPP());
		bf.refresh();

		assertThat(bf.isTypeMatch("&foo", FactoryBean.class)).isTrue();

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(fbBeans).containsOnlyKeys("&foo");

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(aiBeans).containsOnlyKeys("&foo");
	}


	static class FooConfig {

		@Bean FooFactoryBean foo() {
			return new FooFactoryBean();
		}
	}


	static class FooFactoryBean implements FactoryBean<Foo>, AnInterface {

		@Override
		public Foo getObject() {
			return new Foo();
		}

		@Override
		public Class<?> getObjectType() {
			return Foo.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	interface AnInterface {
	}


	static class Foo {
	}


	interface PredictedType {
	}


	static class PredictingBPP implements SmartInstantiationAwareBeanPostProcessor {

		@Override
		public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ? PredictedType.class : null);
		}

		@Override
		public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
			return pvs;
		}
	}

}
