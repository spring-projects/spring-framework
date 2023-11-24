/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.context.support;

import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the interaction between a WebApplicationContext and ContextLoaderListener with
 * regard to config location precedence, overriding and defaulting in programmatic
 * configuration use cases, e.g. with WebApplicationInitializer.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.web.context.ContextLoaderTests
 */
public class Spr8510Tests {

	@Test
	public void abstractRefreshableWAC_respectsProgrammaticConfigLocations() {
		XmlWebApplicationContext ctx = new XmlWebApplicationContext();
		ctx.setConfigLocation("programmatic.xml");
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		MockServletContext sc = new MockServletContext();

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				cll.contextInitialized(new ServletContextEvent(sc)))
			.withMessageEndingWith("ServletContext resource [/programmatic.xml]");
	}

	/**
	 * If a contextConfigLocation init-param has been specified for the ContextLoaderListener,
	 * then it should take precedence. This is generally not a recommended practice, but
	 * when it does happen, the init-param should be considered more specific than the
	 * programmatic configuration, given that it still quite possibly externalized in
	 * hybrid web.xml + WebApplicationInitializer cases.
	 */
	@Test
	public void abstractRefreshableWAC_respectsInitParam_overProgrammaticConfigLocations() {
		XmlWebApplicationContext ctx = new XmlWebApplicationContext();
		ctx.setConfigLocation("programmatic.xml");
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		MockServletContext sc = new MockServletContext();
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "from-init-param.xml");

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				cll.contextInitialized(new ServletContextEvent(sc)))
			.withMessageEndingWith("ServletContext resource [/from-init-param.xml]");
	}

	/**
	 * If setConfigLocation has not been called explicitly against the application context,
	 * then fall back to the ContextLoaderListener init-param if present.
	 */
	@Test
	public void abstractRefreshableWAC_fallsBackToInitParam() {
		XmlWebApplicationContext ctx = new XmlWebApplicationContext();
		//ctx.setConfigLocation("programmatic.xml"); // nothing set programmatically
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		MockServletContext sc = new MockServletContext();
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "from-init-param.xml");

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				cll.contextInitialized(new ServletContextEvent(sc)))
			.withMessageEndingWith("ServletContext resource [/from-init-param.xml]");
	}

	/**
	 * Ensure that any custom default locations are still respected.
	 */
	@Test
	public void customAbstractRefreshableWAC_fallsBackToInitParam() {
		XmlWebApplicationContext ctx = new XmlWebApplicationContext() {
			@Override
			protected String[] getDefaultConfigLocations() {
				return new String[] { "/WEB-INF/custom.xml" };
			}
		};
		//ctx.setConfigLocation("programmatic.xml"); // nothing set programmatically
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		MockServletContext sc = new MockServletContext();
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "from-init-param.xml");

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				cll.contextInitialized(new ServletContextEvent(sc)))
			.withMessageEndingWith("ServletContext resource [/from-init-param.xml]");
	}

	/**
	 * If context config locations have been specified neither against the application
	 * context nor the context loader listener, then fall back to default values.
	 */
	@Test
	public void abstractRefreshableWAC_fallsBackToConventionBasedNaming() {
		XmlWebApplicationContext ctx = new XmlWebApplicationContext();
		//ctx.setConfigLocation("programmatic.xml"); // nothing set programmatically
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		MockServletContext sc = new MockServletContext();
		// no init-param set
		//sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "from-init-param.xml");

		assertThatExceptionOfType(Throwable.class).isThrownBy(() ->
				cll.contextInitialized(new ServletContextEvent(sc)))
			.withMessageEndingWith("ServletContext resource [/WEB-INF/applicationContext.xml]");
	}

	/**
	 * Ensure that ContextLoaderListener and GenericWebApplicationContext interact nicely.
	 */
	@Test
	public void genericWAC() {
		GenericWebApplicationContext ctx = new GenericWebApplicationContext();
		ContextLoaderListener cll = new ContextLoaderListener(ctx);

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("bogus.pkg");

		cll.contextInitialized(new ServletContextEvent(new MockServletContext()));
	}

	/**
	 * Ensure that ContextLoaderListener and AnnotationConfigApplicationContext interact nicely.
	 */
	@Test
	public void annotationConfigWAC() {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();

		ctx.scan("does.not.matter");

		ContextLoaderListener cll = new ContextLoaderListener(ctx);
		cll.contextInitialized(new ServletContextEvent(new MockServletContext()));
	}
}
