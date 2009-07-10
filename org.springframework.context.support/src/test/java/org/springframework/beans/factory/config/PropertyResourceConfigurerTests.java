/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.factory.config;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Unit tests for various {@link PropertyResourceConfigurer} implementations including:
 * {@link PropertyPlaceholderConfigurer}, {@link PropertyOverrideConfigurer} and
 * {@link PreferencesPlaceholderConfigurer}.
 * 
 * @since 02.10.2003
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class PropertyResourceConfigurerTests {

	@Ignore
	@Test
	public void testPropertyPlaceholderConfigurerWithAutowireByType() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${test}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("target", new RuntimeBeanReference("tb"));
		ac.registerSingleton("tbProxy", org.springframework.aop.framework.ProxyFactoryBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("test", "mytest");
		pvs.addPropertyValue("properties", new Properties(props));
		RootBeanDefinition ppcDef = new RootBeanDefinition(PropertyPlaceholderConfigurer.class, pvs);
		// fails when set to AUTOWIRE_BY_TYPE
		ppcDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		ac.registerBeanDefinition("configurer", ppcDef);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
	}

}
