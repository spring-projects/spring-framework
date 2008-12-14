/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.context.access;

import junit.framework.TestCase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * @author Colin Sampaleanu
 */
public class ContextJndiBeanFactoryLocatorTests extends TestCase {

	public static final String BEAN_FACTORY_PATH_ENVIRONMENT_KEY = "java:comp/env/ejb/BeanFactoryPath";

	public void testBeanFactoryPathRequiredFromJndiEnvironment() throws Exception {
		// Set up initial context but don't bind anything
		SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		try {
			jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY);
			fail();
		}
		catch (BootstrapException ex) {
			// Check for helpful JNDI message
			assertTrue(ex.getMessage().indexOf(BEAN_FACTORY_PATH_ENVIRONMENT_KEY) != -1);
		}
	}

	public void testBeanFactoryPathFromJndiEnvironmentNotFound() throws Exception  {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		String bogusPath = "RUBBISH/com/xxxx/framework/server/test1.xml";

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, bogusPath);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		try {
			jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY);
			fail();
		}
		catch (BeansException ex) {
			// Check for helpful JNDI message
			assertTrue(ex.getMessage().indexOf(bogusPath) != -1);
		}
	}

	public void testBeanFactoryPathFromJndiEnvironmentNotValidXml() throws Exception {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		String nonXmlPath = "com/xxxx/framework/server/SlsbEndpointBean.class";

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, nonXmlPath);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		try {
			jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY);
			fail();
		}
		catch (BeansException ex) {
			// Check for helpful JNDI message
			assertTrue(ex.getMessage().indexOf(nonXmlPath) != -1);
		}
	}

	public void testBeanFactoryPathFromJndiEnvironmentWithSingleFile() throws Exception {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		String path = "org/springframework/beans/factory/xml/collections.xml";

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, path);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		BeanFactory bf = jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY).getFactory();
		assertTrue(bf.containsBean("rod"));
		assertTrue(bf instanceof ApplicationContext);
	}

	public void testBeanFactoryPathFromJndiEnvironmentWithMultipleFiles() throws Exception {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		String path = "/org/springframework/beans/factory/xml/collections.xml /org/springframework/beans/factory/xml/parent.xml";

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, path);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		BeanFactory bf = jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY).getFactory();
		assertTrue(bf.containsBean("rod"));
		assertTrue(bf.containsBean("inheritedTestBean"));
	}

}
