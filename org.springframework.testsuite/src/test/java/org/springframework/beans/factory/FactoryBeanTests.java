/*
 * Copyright 2002-2007 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class FactoryBeanTests extends TestCase {

	public void testFactoryBeanReturnsNull() throws Exception {
		XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("factoryBeanReturnsNull.xml", getClass()));
		Object result = factory.getBean("factoryBean");
		assertNull(result);
	}

	public void testFactoryBeansWithAutowiring() throws Exception {
		ClassPathXmlApplicationContext factory =
				new ClassPathXmlApplicationContext("factoryBeansWithAutowiring.xml", getClass());
		Alpha alpha = (Alpha) factory.getBean("alpha");
		Beta beta = (Beta) factory.getBean("beta");
		Gamma gamma = (Gamma) factory.getBean("gamma");
		Gamma gamma2 = (Gamma) factory.getBean("gammaFactory");
		assertSame(beta, alpha.getBeta());
		assertSame(gamma, beta.getGamma());
		assertSame(gamma2, beta.getGamma());
		assertEquals("yourName", beta.getName());
	}

	public void testFactoryBeansWithIntermediateFactoryBeanAutowiringFailure() throws Exception {
		ClassPathXmlApplicationContext factory =
				new ClassPathXmlApplicationContext("factoryBeansWithAutowiring.xml", getClass());
		Beta beta = (Beta) factory.getBean("beta");
		Alpha alpha = (Alpha) factory.getBean("alpha");
		Gamma gamma = (Gamma) factory.getBean("gamma");
		assertSame(beta, alpha.getBeta());
		assertSame(gamma, beta.getGamma());
	}


	public static class NullReturningFactoryBean implements FactoryBean {

		public Object getObject() {
			return null;
		}

		public Class getObjectType() {
			return null;
		}

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

		public void afterPropertiesSet() {
			Assert.notNull(gamma, "'gamma' property is required");
		}
	}


	public static class Gamma {
	}


	public static class BetaFactoryBean implements FactoryBean {

		private Beta beta;

		public void setBeta(Beta beta) {
			this.beta = beta;
		}

		public Object getObject() {
			return this.beta;
		}

		public Class getObjectType() {
			return null;
		}

		public boolean isSingleton() {
			return true;
		}
	}

}
