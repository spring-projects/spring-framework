/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
