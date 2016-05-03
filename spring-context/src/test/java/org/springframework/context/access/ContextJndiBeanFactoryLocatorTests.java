/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BootstrapException;
import org.springframework.context.ApplicationContext;
import org.springframework.tests.mock.jndi.SimpleNamingContextBuilder;

import static org.junit.Assert.*;

/**
 * @author Colin Sampaleanu
 * @author Chris Beams
 */
public final class ContextJndiBeanFactoryLocatorTests {

	private static final String BEAN_FACTORY_PATH_ENVIRONMENT_KEY = "java:comp/env/ejb/BeanFactoryPath";

	private static final Class<?> CLASS = ContextJndiBeanFactoryLocatorTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String FQ_PATH = "/org/springframework/context/access/";

	private static final String COLLECTIONS_CONTEXT = FQ_PATH + CLASSNAME + "-collections.xml";
	private static final String PARENT_CONTEXT = FQ_PATH + CLASSNAME + "-parent.xml";


	@Test
	public void beanFactoryPathRequiredFromJndiEnvironment() throws Exception {
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

	@Test
	public void beanFactoryPathFromJndiEnvironmentNotFound() throws Exception  {
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

	@Test
	public void beanFactoryPathFromJndiEnvironmentNotValidXml() throws Exception {
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

	@Test
	public void beanFactoryPathFromJndiEnvironmentWithSingleFile() throws Exception {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, COLLECTIONS_CONTEXT);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		BeanFactory bf = jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY).getFactory();
		assertTrue(bf.containsBean("rod"));
		assertTrue(bf instanceof ApplicationContext);
	}

	@Test
	public void beanFactoryPathFromJndiEnvironmentWithMultipleFiles() throws Exception {
		SimpleNamingContextBuilder sncb = SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		String path = String.format("%s %s", COLLECTIONS_CONTEXT, PARENT_CONTEXT);

		// Set up initial context
		sncb.bind(BEAN_FACTORY_PATH_ENVIRONMENT_KEY, path);

		ContextJndiBeanFactoryLocator jbfl = new ContextJndiBeanFactoryLocator();
		BeanFactory bf = jbfl.useBeanFactory(BEAN_FACTORY_PATH_ENVIRONMENT_KEY).getFactory();
		assertTrue(bf.containsBean("rod"));
		assertTrue(bf.containsBean("inheritedTestBean"));
	}

}


class MapAndSet {

	private Object obj;

	public MapAndSet(Map<?, ?> map) {
		this.obj = map;
	}

	public MapAndSet(Set<?> set) {
		this.obj = set;
	}

	public Object getObject() {
		return obj;
	}
}



/**
 * Bean that exposes a simple property that can be set
 * to a mix of references and individual values.
 */
class MixedCollectionBean {

	private Collection<?> jumble;


	public void setJumble(Collection<?> jumble) {
		this.jumble = jumble;
	}

	public Collection<?> getJumble() {
		return jumble;
	}

}