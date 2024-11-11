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

package org.springframework.web.context;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
class ServletContextAwareProcessorTests {

	@Test
	void servletContextAwareWithServletContext() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
		assertThat(bean.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	void servletContextAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
		assertThat(bean.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	void servletContextAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
		assertThat(bean.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	void servletContextAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
		assertThat(bean.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	void servletContextAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
		assertThat(bean.getServletContext()).isEqualTo(servletContext);
	}

	@Test
	void servletContextAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletContextAwareBean bean = new ServletContextAwareBean();
		assertThat(bean.getServletContext()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletContext()).isNull();
	}

	@Test
	void servletConfigAwareWithServletContextOnly() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).isNull();
	}

	@Test
	void servletConfigAwareWithServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
		assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
	}

	@Test
	void servletConfigAwareWithServletContextAndServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
		assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
	}

	@Test
	void servletConfigAwareWithNullServletContextAndNonNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletConfig servletConfig = new MockServletConfig(servletContext);
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
		assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
	}

	@Test
	void servletConfigAwareWithNonNullServletContextAndNullServletConfig() {
		ServletContext servletContext = new MockServletContext();
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).isNull();
	}

	@Test
	void servletConfigAwareWithNullServletContext() {
		ServletContext servletContext = null;
		ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
		ServletConfigAwareBean bean = new ServletConfigAwareBean();
		assertThat(bean.getServletConfig()).isNull();
		processor.postProcessBeforeInitialization(bean, "testBean");
		assertThat(bean.getServletConfig()).isNull();
	}

}
