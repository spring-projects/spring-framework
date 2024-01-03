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

package org.springframework.context.support;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.testfixture.AbstractApplicationContextTests;
import org.springframework.context.testfixture.beans.ACATester;
import org.springframework.context.testfixture.beans.BeanThatListens;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests for static application context.
 *
 * @author Rod Johnson
 */
class StaticApplicationContextTests extends AbstractApplicationContextTests {

	protected StaticApplicationContext sac;

	@SuppressWarnings("deprecation")
	@Override
	protected ConfigurableApplicationContext createContext() {
		StaticApplicationContext parent = new StaticApplicationContext();
		Map<String, String> m = new HashMap<>();
		m.put("name", "Roderick");
		parent.registerPrototype("rod", TestBean.class, new MutablePropertyValues(m));
		m.put("name", "Albert");
		parent.registerPrototype("father", TestBean.class, new MutablePropertyValues(m));
		parent.refresh();
		parent.addApplicationListener(parentListener) ;

		parent.getStaticMessageSource().addMessage("code1", Locale.getDefault(), "message1");

		this.sac = new StaticApplicationContext(parent);
		sac.registerSingleton("beanThatListens", BeanThatListens.class, new MutablePropertyValues());
		sac.registerSingleton("aca", ACATester.class, new MutablePropertyValues());
		sac.registerPrototype("aca-prototype", ACATester.class, new MutablePropertyValues());
		org.springframework.beans.factory.support.PropertiesBeanDefinitionReader reader =
				new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(sac.getDefaultListableBeanFactory());
		reader.loadBeanDefinitions(new ClassPathResource("testBeans.properties", getClass()));
		sac.refresh();
		sac.addApplicationListener(listener);

		sac.getStaticMessageSource().addMessage("code2", Locale.getDefault(), "message2");

		return sac;
	}

	@Test
	@Override
	public void count() {
		assertCount(15);
	}

}
