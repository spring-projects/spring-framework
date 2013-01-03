/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static test.util.TestResourceUtils.qualifiedResource;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class FactoryBeanTests {

	private static final Class<?> CLASS = FactoryBeanTests.class;
	private static final Resource RETURNS_NULL_CONTEXT = qualifiedResource(CLASS, "returnsNull.xml");
	private static final Resource WITH_AUTOWIRING_CONTEXT = qualifiedResource(CLASS, "withAutowiring.xml");

	@Test
	public void testFactoryBeanReturnsNull() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(RETURNS_NULL_CONTEXT);
		Object result = factory.getBean("factoryBean");
		assertNull(result);
	}

	@Test
	public void testFactoryBeansWithAutowiring() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(WITH_AUTOWIRING_CONTEXT);

		BeanFactoryPostProcessor ppc = (BeanFactoryPostProcessor) factory.getBean("propertyPlaceholderConfigurer");
		ppc.postProcessBeanFactory(factory);

		Alpha alpha = (Alpha) factory.getBean("alpha");
		Beta beta = (Beta) factory.getBean("beta");
		Gamma gamma = (Gamma) factory.getBean("gamma");
		Gamma gamma2 = (Gamma) factory.getBean("gammaFactory");
		assertSame(beta, alpha.getBeta());
		assertSame(gamma, beta.getGamma());
		assertSame(gamma2, beta.getGamma());
		assertEquals("yourName", beta.getName());
	}

	@Test
	public void testFactoryBeansWithIntermediateFactoryBeanAutowiringFailure() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(WITH_AUTOWIRING_CONTEXT);

		BeanFactoryPostProcessor ppc = (BeanFactoryPostProcessor) factory.getBean("propertyPlaceholderConfigurer");
		ppc.postProcessBeanFactory(factory);

		Beta beta = (Beta) factory.getBean("beta");
		Alpha alpha = (Alpha) factory.getBean("alpha");
		Gamma gamma = (Gamma) factory.getBean("gamma");
		assertSame(beta, alpha.getBeta());
		assertSame(gamma, beta.getGamma());
	}


	public static class NullReturningFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	public static class Alpha implements InitializingBean {

		private Beta beta;

		public void setBeta(Beta beta) {
			this.beta = beta;
		}

		public Beta getBeta() {
			return beta;
		}

		@Override
		public void afterPropertiesSet() {
			Assert.notNull(beta, "'beta' property is required");
		}
	}


	public static class Beta implements InitializingBean {

		private Gamma gamma;

		private String name;

		public void setGamma(Gamma gamma) {
			this.gamma = gamma;
		}

		public Gamma getGamma() {
			return gamma;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public void afterPropertiesSet() {
			Assert.notNull(gamma, "'gamma' property is required");
		}
	}


	public static class Gamma {
	}


	public static class BetaFactoryBean implements FactoryBean<Object> {

		private Beta beta;

		public void setBeta(Beta beta) {
			this.beta = beta;
		}

		@Override
		public Object getObject() {
			return this.beta;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
