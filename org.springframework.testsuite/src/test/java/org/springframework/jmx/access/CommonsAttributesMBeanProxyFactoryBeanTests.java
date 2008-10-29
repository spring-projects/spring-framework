/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.access;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jmx.AbstractJmxTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * Tests creation of JMX MBean proxies.
 *
 * @author Rob Harrop
 */
public class CommonsAttributesMBeanProxyFactoryBeanTests extends AbstractJmxTests {
	
	private static final String OBJECT_NAME = "bean:name=testBean1";

	protected ObjectName getObjectName() throws Exception {
		return ObjectNameManager.getInstance(OBJECT_NAME);
	}

	public void testProxyFactory() throws Exception {
		MBeanProxyFactoryBean fb = getProxyFactory();
		fb.setProxyInterface(IJmxTestBean.class);
		fb.afterPropertiesSet();

		IJmxTestBean bean = (IJmxTestBean) fb.getObject();
		assertNotNull("Proxy should not be null", bean);
	}

	public void testInvalidJdkProxy() throws Exception {
		MBeanProxyFactoryBean fb = getProxyFactory();
		try {
			fb.afterPropertiesSet();
			fail("Should not be able to create JDK proxy with no proxy interfaces");
		}
		catch (Exception ex) {
			// expected
		}
	}

	public void testWithLocatedMBeanServer() throws Exception {
		MBeanProxyFactoryBean fb = new MBeanProxyFactoryBean();
		fb.setProxyInterface(IJmxTestBean.class);
		fb.setObjectName(OBJECT_NAME);
		fb.afterPropertiesSet();
		IJmxTestBean proxy = (IJmxTestBean)fb.getObject();
		assertNotNull("Proxy should not be null", proxy);
		assertEquals("Incorrect name value", "TEST", proxy.getName());
	}

	public void testProxyFactoryBeanWithAutodetect() throws Exception {
		try {
			XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("proxyFactoryBean.xml", getClass()));
			bf.preInstantiateSingletons();
		}
		catch (BeanCreationException ex) {
			if (ex.getCause().getClass() == MBeanInfoRetrievalException.class) {
				fail("MBeanProxyFactoryBean should be ignored by MBeanExporter when running autodetect process");
			}
			else {
				throw ex;
			}
		}
	}

	private MBeanProxyFactoryBean getProxyFactory() throws MalformedObjectNameException {
		MBeanProxyFactoryBean fb = new MBeanProxyFactoryBean();
		fb.setServer(getServer());
		fb.setObjectName(OBJECT_NAME);
		return fb;
	}

}
