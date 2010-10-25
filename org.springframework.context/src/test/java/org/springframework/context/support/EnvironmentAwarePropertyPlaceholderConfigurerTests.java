/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.support;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import test.beans.TestBean;

/**
 * Unit tests for {@link EnvironmentAwarePropertyPlaceholderConfigurer}.
 * 
 * @author Chris Beams
 * @since 3.1
 * @see EnvironmentAwarePropertyPlaceholderConfigurerTests
 */
public class EnvironmentAwarePropertyPlaceholderConfigurerTests {

	@Test(expected=IllegalArgumentException.class)
	public void environmentNotNull() {
		new EnvironmentAwarePropertyPlaceholderConfigurer().postProcessBeanFactory(new DefaultListableBeanFactory());
	}

	@Test
	public void localPropertiesOverrideFalse() {
		localPropertiesOverride(false);
	}

	@Test
	public void localPropertiesOverrideTrue() {
		localPropertiesOverride(true);
	}

	private void localPropertiesOverride(boolean override) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo}")
					.getBeanDefinition());

		EnvironmentAwarePropertyPlaceholderConfigurer ppc = new EnvironmentAwarePropertyPlaceholderConfigurer();

		ppc.setLocalOverride(override);
		ppc.setProperties(MockEnvironment.withProperty("foo", "local").asProperties());
		ppc.setEnvironment(MockEnvironment.withProperty("foo", "enclosing"));
		ppc.postProcessBeanFactory(bf);
		if (override) {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("local"));
		} else {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("enclosing"));
		}
	}

	@Test
	public void simpleReplacement() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MockEnvironment env = new MockEnvironment();
		env.setProperty("my.name", "myValue");

		EnvironmentAwarePropertyPlaceholderConfigurer ppc =
			new EnvironmentAwarePropertyPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("myValue"));
	}

}
