/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for SPR-8954, in which a custom {@link InstantiationAwareBeanPostProcessor}
 * forces the predicted type of a FactoryBean, effectively preventing retrieval of the
 * bean from calls to #getBeansOfType(FactoryBean.class). The implementation of
 * {@link AbstractBeanFactory#isFactoryBean(String, RootBeanDefinition)} now ensures that
 * not only the predicted bean type is considered, but also the original bean definition's
 * beanClass.
 *
 * @author Chris Beams
 * @author Oliver Gierke
 */
public class Spr8954Tests {

	private DefaultListableBeanFactory bf;

	@Before
	public void setUp() {
		bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("foo", new RootBeanDefinition(FooFactoryBean.class));
		bf.addBeanPostProcessor(new PredictingBPP());
	}

	@Test
	public void repro() {
		assertThat(bf.getBean("foo"), instanceOf(Foo.class));
		assertThat(bf.getBean("&foo"), instanceOf(FooFactoryBean.class));
		assertThat(bf.isTypeMatch("&foo", FactoryBean.class), is(true));

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(fbBeans.size(), is(1));
		assertThat(fbBeans.keySet(), hasItem("&foo"));

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(aiBeans.size(), is(1));
		assertThat(aiBeans.keySet(), hasItem("&foo"));
	}

	@Test
	public void findsBeansByTypeIfNotInstantiated() {
		assertThat(bf.isTypeMatch("&foo", FactoryBean.class), is(true));

		@SuppressWarnings("rawtypes")
		Map<String, FactoryBean> fbBeans = bf.getBeansOfType(FactoryBean.class);
		assertThat(1, equalTo(fbBeans.size()));
		assertThat("&foo", equalTo(fbBeans.keySet().iterator().next()));

		Map<String, AnInterface> aiBeans = bf.getBeansOfType(AnInterface.class);
		assertThat(aiBeans.size(), is(1));
		assertThat(aiBeans.keySet(), hasItem("&foo"));
	}

	/**
	 * SPR-10517
	 */
	@Test
	public void findsFactoryBeanNameByTypeWithoutInstantiation() {
		String[] names = bf.getBeanNamesForType(AnInterface.class, false, false);
		assertThat(Arrays.asList(names), hasItem("&foo"));

		Map<String, AnInterface> beans = bf.getBeansOfType(AnInterface.class, false, false);
		assertThat(beans.size(), is(1));
		assertThat(beans.keySet(), hasItem("&foo"));
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
			return FactoryBean.class.isAssignableFrom(beanClass) ? PredictedType.class : null;
		}
	}

}
