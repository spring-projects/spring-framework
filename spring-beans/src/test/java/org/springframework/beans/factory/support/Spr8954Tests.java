/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Unit tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, RootBeanDefinition)} now ensures
 * that not only the predicted bean type is considered, but also the original bean
 * definition's beanClass.
 *
 * @author Chris Beams
 * @author Oliver Gierke
 */
public class Spr8954Tests {

	@Test
	public void repro() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("foo", new RootBeanDefinition(FooFactoryBean.class));
		bf.addBeanPostProcessor(new PredictingBPP());

		assertThat(bf.getBean("foo"), instanceOf(Foo.class));
		assertThat(bf.getBean("&foo"), instanceOf(FooFactoryBean.class));

		assertThat(bf.isTypeMatch("&foo", FactoryBean.class), is(true));

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(1, equalTo(fbBeans.size()));
		assertThat("&foo", equalTo(fbBeans.keySet().iterator().next()));

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(1, equalTo(aiBeans.size()));
		assertThat("&foo", equalTo(aiBeans.keySet().iterator().next()));
	}

	@Test
	public void findsBeansByTypeIfNotInstantiated() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("foo", new RootBeanDefinition(FooFactoryBean.class));
		bf.addBeanPostProcessor(new PredictingBPP());

		assertThat(bf.isTypeMatch("&foo", FactoryBean.class), is(true));

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(1, equalTo(fbBeans.size()));
		assertThat("&foo", equalTo(fbBeans.keySet().iterator().next()));

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(1, equalTo(aiBeans.size()));
		assertThat("&foo", equalTo(aiBeans.keySet().iterator().next()));
	}


	static class FooFactoryBean implements FactoryBean<Foo>, AnInterface {

		@Override
		public Foo getObject() throws Exception {
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

	static class PredictedTypeImpl implements PredictedType {
	}

	static class PredictingBPP extends InstantiationAwareBeanPostProcessorAdapter {

		@Override
		public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
			return FactoryBean.class.isAssignableFrom(beanClass) ?
					PredictedType.class : null;
		}
	}

}
