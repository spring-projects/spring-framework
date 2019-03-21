/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.junit.Test;

import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
public class ServletContextAwareProcessorTests {

	@Test
	public void servletContextAwareWithServletContext() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	@Test
	public void servletContextAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	@Test
	public void servletContextAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	@Test
	public void servletContextAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	@Test
	public void servletContextAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletContext should have been set", bean.getServletContext());
		assertEquals(servletContext, bean.getServletContext());
	}

	@Test
	public void servletContextAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertNull(bean.getServletContext());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletContext());
	}

	@Test
	public void servletConfigAwareWithServletContextOnly() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

	@Test
	public void servletConfigAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	@Test
	public void servletConfigAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	@Test
	public void servletConfigAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNotNull("ServletConfig should have been set", bean.getServletConfig());
		assertEquals(servletConfig, bean.getServletConfig());
	}

	@Test
	public void servletConfigAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

	@Test
	public void servletConfigAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertNull(bean.getServletConfig());
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertNull(bean.getServletConfig());
	}

}
