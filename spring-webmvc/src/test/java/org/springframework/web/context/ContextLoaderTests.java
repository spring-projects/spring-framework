/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.LifecycleBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.SimpleWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ContextLoader} and {@link ContextLoaderListener}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @since 12.08.2003
 * @see org.springframework.web.context.support.Spr8510Tests
 */
public class ContextLoaderTests {

	@Test
	public void testContextLoaderListenerWithDefaultContext() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml " +
				"/org/springframework/web/context/WEB-INF/context-addition.xml");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
		String contextAttr = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		WebApplicationContext context = (WebApplicationContext) sc.getAttribute(contextAttr);
		boolean condition1 = context instanceof XmlWebApplicationContext;
		assertThat(condition1).as("Correct WebApplicationContext exposed in ServletContext").isTrue();
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(sc) instanceof XmlWebApplicationContext).isTrue();
		LifecycleBean lb = (LifecycleBean) context.getBean("lifecycle");
		assertThat(context.containsBean("father")).as("Has father").isTrue();
		assertThat(context.containsBean("rod")).as("Has rod").isTrue();
		assertThat(context.containsBean("kerry")).as("Has kerry").isTrue();
		boolean condition = !lb.isDestroyed();
		assertThat(condition).as("Not destroyed").isTrue();
		assertThat(context.containsBean("beans1.bean1")).isFalse();
		assertThat(context.containsBean("beans1.bean2")).isFalse();
		listener.contextDestroyed(event);
		assertThat(lb.isDestroyed()).as("Destroyed").isTrue();
		assertThat(sc.getAttribute(contextAttr)).isNull();
		assertThat(WebApplicationContextUtils.getWebApplicationContext(sc)).isNull();
	}

	/**
	 * Addresses the issues raised in <a
	 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4008"
	 * target="_blank">SPR-4008</a>: <em>Supply an opportunity to customize
	 * context before calling refresh in ContextLoaders</em>.
	 */
	@Test
	public void testContextLoaderListenerWithCustomizedContextLoader() {
		final StringBuffer buffer = new StringBuffer();
		final String expectedContents = "customizeContext() was called";
		final MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml");
		ServletContextListener listener = new ContextLoaderListener() {
			@Override
			protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
				assertThat(sc).as("The ServletContext should not be null.").isNotNull();
				assertThat(sc).as("Verifying that we received the expected ServletContext.").isEqualTo(sc);
				assertThat(wac.isActive()).as("The ApplicationContext should not yet have been refreshed.").isFalse();
				buffer.append(expectedContents);
			}
		};
		listener.contextInitialized(new ServletContextEvent(sc));
		assertThat(buffer.toString()).as("customizeContext() should have been called.").isEqualTo(expectedContents);
	}

	@Test
	public void testContextLoaderListenerWithLocalContextInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM, StringUtils.arrayToCommaDelimitedString(
				new Object[] {TestContextInitializer.class.getName(), TestWebContextInitializer.class.getName()}));
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testContextLoaderListenerWithGlobalContextInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		sc.addInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, StringUtils.arrayToCommaDelimitedString(
				new Object[] {TestContextInitializer.class.getName(), TestWebContextInitializer.class.getName()}));
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testContextLoaderListenerWithMixedContextInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM, TestContextInitializer.class.getName());
		sc.addInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, TestWebContextInitializer.class.getName());
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testContextLoaderListenerWithProgrammaticInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.setContextInitializers(new TestContextInitializer(), new TestWebContextInitializer());
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testContextLoaderListenerWithProgrammaticAndLocalInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM, TestContextInitializer.class.getName());
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.setContextInitializers(new TestWebContextInitializer());
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testContextLoaderListenerWithProgrammaticAndGlobalInitializers() {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"org/springframework/web/context/WEB-INF/ContextLoaderTests-acc-context.xml");
		sc.addInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, TestWebContextInitializer.class.getName());
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.setContextInitializers(new TestContextInitializer());
		listener.contextInitialized(new ServletContextEvent(sc));
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
		TestBean testBean = wac.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("testName");
		assertThat(wac.getServletContext().getAttribute("initialized")).isNotNull();
	}

	@Test
	public void testRegisteredContextInitializerCanAccessServletContextParamsViaEnvironment() {
		MockServletContext sc = new MockServletContext("");
		// config file doesn't matter - just a placeholder
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/empty-context.xml");

		sc.addInitParameter("someProperty", "someValue");
		sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
				EnvApplicationContextInitializer.class.getName());
		ContextLoaderListener listener = new ContextLoaderListener();
		listener.contextInitialized(new ServletContextEvent(sc));
	}

	@Test
	public void testContextLoaderListenerWithUnknownContextInitializer() {
		MockServletContext sc = new MockServletContext("");
		// config file doesn't matter.  just a placeholder
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/empty-context.xml");
		sc.addInitParameter(ContextLoader.CONTEXT_INITIALIZER_CLASSES_PARAM,
				StringUtils.arrayToCommaDelimitedString(new Object[] {UnknownContextInitializer.class.getName()}));
		ContextLoaderListener listener = new ContextLoaderListener();
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
				listener.contextInitialized(new ServletContextEvent(sc)))
			.withMessageContaining("not assignable");
	}

	@Test
	public void testContextLoaderWithCustomContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				"org.springframework.web.servlet.SimpleWebApplicationContext");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
		String contextAttr = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		WebApplicationContext wc = (WebApplicationContext) sc.getAttribute(contextAttr);
		boolean condition = wc instanceof SimpleWebApplicationContext;
		assertThat(condition).as("Correct WebApplicationContext exposed in ServletContext").isTrue();
	}

	@Test
	public void testContextLoaderWithInvalidLocation() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "/WEB-INF/myContext.xml");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				listener.contextInitialized(event))
			.withCauseInstanceOf(FileNotFoundException.class);
	}

	@Test
	public void testContextLoaderWithInvalidContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				"org.springframework.web.context.support.InvalidWebApplicationContext");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
				listener.contextInitialized(event))
			.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	public void testContextLoaderWithDefaultLocation() throws Exception {
		MockServletContext sc = new MockServletContext("");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				listener.contextInitialized(event))
			.withCauseInstanceOf(IOException.class)
			.satisfies(ex -> assertThat(ex.getCause()).hasMessageContaining("/WEB-INF/applicationContext.xml"));
	}

	@Test
	public void testFrameworkServletWithDefaultLocation() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(XmlWebApplicationContext.class);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				servlet.init(new MockServletConfig(new MockServletContext(""), "test")))
			.withCauseInstanceOf(IOException.class)
			.satisfies(ex -> assertThat(ex.getCause()).hasMessageContaining("/WEB-INF/test-servlet.xml"));
	}

	@Test
	public void testFrameworkServletWithCustomLocation() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextConfigLocation("/org/springframework/web/context/WEB-INF/testNamespace.xml "
				+ "/org/springframework/web/context/WEB-INF/context-addition.xml");
		servlet.init(new MockServletConfig(new MockServletContext(""), "test"));
		assertThat(servlet.getWebApplicationContext().containsBean("kerry")).isTrue();
		assertThat(servlet.getWebApplicationContext().containsBean("kerryX")).isTrue();
	}

	@Test
	@SuppressWarnings("resource")
	public void testClassPathXmlApplicationContext() throws IOException {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"/org/springframework/web/context/WEB-INF/applicationContext.xml");
		assertThat(context.containsBean("father")).as("Has father").isTrue();
		assertThat(context.containsBean("rod")).as("Has rod").isTrue();
		assertThat(context.containsBean("kerry")).as("Hasn't kerry").isFalse();
		assertThat(((TestBean) context.getBean("rod")).getSpouse() == null).as("Doesn't have spouse").isTrue();
		assertThat("Roderick".equals(((TestBean) context.getBean("rod")).getName())).as("myinit not evaluated").isTrue();

		context = new ClassPathXmlApplicationContext(new String[] {
			"/org/springframework/web/context/WEB-INF/applicationContext.xml",
			"/org/springframework/web/context/WEB-INF/context-addition.xml" });
		assertThat(context.containsBean("father")).as("Has father").isTrue();
		assertThat(context.containsBean("rod")).as("Has rod").isTrue();
		assertThat(context.containsBean("kerry")).as("Has kerry").isTrue();
	}

	@Test
	@SuppressWarnings("resource")
	public void testSingletonDestructionOnStartupFailure() throws IOException {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(new String[] {
					"/org/springframework/web/context/WEB-INF/applicationContext.xml",
					"/org/springframework/web/context/WEB-INF/fail.xml" }) {

					@Override
					public void refresh() throws BeansException {
						try {
							super.refresh();
						}
						catch (BeanCreationException ex) {
							DefaultListableBeanFactory factory = (DefaultListableBeanFactory) getBeanFactory();
							assertThat(factory.getSingletonCount()).isEqualTo(0);
							throw ex;
						}
					}
				});
	}


	private static class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();
			environment.getPropertySources().addFirst(new PropertySource<Object>("testPropertySource") {
				@Override
				public Object getProperty(String key) {
					return "name".equals(key) ? "testName" : null;
				}
			});
		}
	}


	private static class TestWebContextInitializer implements
			ApplicationContextInitializer<ConfigurableWebApplicationContext> {

		@Override
		public void initialize(ConfigurableWebApplicationContext applicationContext) {
			ServletContext ctx = applicationContext.getServletContext(); // type-safe access to servlet-specific methods
			ctx.setAttribute("initialized", true);
		}
	}


	private static class EnvApplicationContextInitializer
			implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

		@Override
		public void initialize(ConfigurableWebApplicationContext applicationContext) {
			// test that ApplicationContextInitializers can access ServletContext properties
			// via the environment (SPR-8991)
			String value = applicationContext.getEnvironment().getRequiredProperty("someProperty");
			assertThat(value).isEqualTo("someValue");
		}
	}


	private static interface UnknownApplicationContext extends ConfigurableApplicationContext {

		void unheardOf();
	}


	private static class UnknownContextInitializer implements ApplicationContextInitializer<UnknownApplicationContext> {

		@Override
		public void initialize(UnknownApplicationContext applicationContext) {
			applicationContext.unheardOf();
		}
	}

}
