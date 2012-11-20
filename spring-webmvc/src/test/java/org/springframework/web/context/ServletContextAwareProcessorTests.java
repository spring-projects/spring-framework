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

package org.springframework.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
public class ServletContextAwareProcessorTests extends TestCase {

	public void testServletContextAwareWithServletContext() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	public void testServletContextAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	public void testServletContextAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	public void testServletContextAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	public void testServletContextAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	public void testServletContextAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletContext());
	}

	public void testServletConfigAwareWithServletContextOnly() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

	public void testServletConfigAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	public void testServletConfigAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	public void testServletConfigAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	public void testServletConfigAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

	public void testServletConfigAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

}
