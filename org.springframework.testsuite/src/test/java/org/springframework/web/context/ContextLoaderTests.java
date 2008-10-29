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

package org.springframework.web.context;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

import org.springframework.beans.BeansException;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.LifecycleBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.SimpleWebApplicationContext;

/**
 * JUnit 3.8 based tests for {@link ContextLoader},
 * {@link ContextLoaderListener}, {@link ContextLoaderServlet}, and related
 * classes.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 12.08.2003
 */
public class ContextLoaderTests extends TestCase {

	public void testContextLoaderListenerWithDefaultContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml "
						+ "/org/springframework/web/context/WEB-INF/context-addition.xml");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
		WebApplicationContext context = (WebApplicationContext) sc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertTrue("Correct WebApplicationContext exposed in ServletContext",
				context instanceof XmlWebApplicationContext);
		LifecycleBean lb = (LifecycleBean) context.getBean("lifecycle");
		assertTrue("Has father", context.containsBean("father"));
		assertTrue("Has rod", context.containsBean("rod"));
		assertTrue("Has kerry", context.containsBean("kerry"));
		assertTrue("Not destroyed", !lb.isDestroyed());
		assertFalse(context.containsBean("beans1.bean1"));
		assertFalse(context.containsBean("beans1.bean2"));
		listener.contextDestroyed(event);
		assertTrue("Destroyed", lb.isDestroyed());
	}

	/**
	 * Addresses the issues raised in <a
	 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-4008"
	 * target="_blank">SPR-4008</a>: <em>Supply an opportunity to customize
	 * context before calling refresh in ContextLoaders</em>.
	 */
	public void testContextLoaderListenerWithCustomizedContextLoader() throws Exception {
		final StringBuffer buffer = new StringBuffer();
		final String expectedContents = "customizeContext() was called";
		final MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml");
		final ServletContextListener listener = new ContextLoaderListener() {
			protected ContextLoader createContextLoader() {
				return new ContextLoader() {
					protected void customizeContext(ServletContext servletContext, ConfigurableWebApplicationContext applicationContext) {
						assertNotNull("The ServletContext should not be null.", servletContext);
						assertEquals("Verifying that we received the expected ServletContext.", sc, servletContext);
						assertFalse("The ApplicationContext should not yet have been refreshed.", applicationContext.isActive());
						buffer.append(expectedContents);
					}
				};
			}
		};
		listener.contextInitialized(new ServletContextEvent(sc));
		assertEquals("customizeContext() should have been called.", expectedContents, buffer.toString());
	}

	public void testContextLoaderServletWithDefaultContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml");
		HttpServlet servlet = new ContextLoaderServlet();
		ServletConfig config = new MockServletConfig(sc, "test");
		servlet.init(config);
		WebApplicationContext context = (WebApplicationContext) sc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertTrue("Correct WebApplicationContext exposed in ServletContext",
				context instanceof XmlWebApplicationContext);
		LifecycleBean lb = (LifecycleBean) context.getBean("lifecycle");
		assertTrue("Not destroyed", !lb.isDestroyed());
		assertFalse(context.containsBean("beans1.bean1"));
		assertFalse(context.containsBean("beans1.bean2"));
		servlet.destroy();
		assertTrue("Destroyed", lb.isDestroyed());
	}

	public void testContextLoaderWithDefaultContextAndParent() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM,
				"/org/springframework/web/context/WEB-INF/applicationContext.xml "
						+ "/org/springframework/web/context/WEB-INF/context-addition.xml");
		sc.addInitParameter(ContextLoader.LOCATOR_FACTORY_SELECTOR_PARAM,
				"classpath:org/springframework/beans/factory/access/ref1.xml");
		sc.addInitParameter(ContextLoader.LOCATOR_FACTORY_KEY_PARAM, "a.qualified.name.of.some.sort");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
		WebApplicationContext context = (WebApplicationContext) sc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertTrue("Correct WebApplicationContext exposed in ServletContext",
				context instanceof XmlWebApplicationContext);
		LifecycleBean lb = (LifecycleBean) context.getBean("lifecycle");
		assertTrue("Has father", context.containsBean("father"));
		assertTrue("Has rod", context.containsBean("rod"));
		assertTrue("Has kerry", context.containsBean("kerry"));
		assertTrue("Not destroyed", !lb.isDestroyed());
		assertTrue(context.containsBean("beans1.bean1"));
		assertTrue(context.isTypeMatch("beans1.bean1", org.springframework.beans.factory.access.TestBean.class));
		assertTrue(context.containsBean("beans1.bean2"));
		assertTrue(context.isTypeMatch("beans1.bean2", org.springframework.beans.factory.access.TestBean.class));
		listener.contextDestroyed(event);
		assertTrue("Destroyed", lb.isDestroyed());
	}

	public void testContextLoaderWithCustomContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				"org.springframework.web.servlet.SimpleWebApplicationContext");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		listener.contextInitialized(event);
		WebApplicationContext wc = (WebApplicationContext) sc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertTrue("Correct WebApplicationContext exposed in ServletContext", wc instanceof SimpleWebApplicationContext);
	}

	public void testContextLoaderWithInvalidLocation() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, "/WEB-INF/myContext.xml");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		try {
			listener.contextInitialized(event);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
		}
	}

	public void testContextLoaderWithInvalidContext() throws Exception {
		MockServletContext sc = new MockServletContext("");
		sc.addInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				"org.springframework.web.context.support.InvalidWebApplicationContext");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		try {
			listener.contextInitialized(event);
			fail("Should have thrown ApplicationContextException");
		}
		catch (ApplicationContextException ex) {
			// expected
			assertTrue(ex.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testContextLoaderWithDefaultLocation() throws Exception {
		MockServletContext sc = new MockServletContext("");
		ServletContextListener listener = new ContextLoaderListener();
		ServletContextEvent event = new ServletContextEvent(sc);
		try {
			listener.contextInitialized(event);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getCause() instanceof IOException);
			assertTrue(ex.getCause().getMessage().indexOf("/WEB-INF/applicationContext.xml") != -1);
		}
	}

	public void testFrameworkServletWithDefaultLocation() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextClass(XmlWebApplicationContext.class);
		try {
			servlet.init(new MockServletConfig(new MockServletContext(""), "test"));
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getCause() instanceof IOException);
			assertTrue(ex.getCause().getMessage().indexOf("/WEB-INF/test-servlet.xml") != -1);
		}
	}

	public void testFrameworkServletWithCustomLocation() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setContextConfigLocation("/org/springframework/web/context/WEB-INF/testNamespace.xml "
				+ "/org/springframework/web/context/WEB-INF/context-addition.xml");
		servlet.init(new MockServletConfig(new MockServletContext(""), "test"));
		assertTrue(servlet.getWebApplicationContext().containsBean("kerry"));
		assertTrue(servlet.getWebApplicationContext().containsBean("kerryX"));
	}

	public void testClassPathXmlApplicationContext() throws IOException {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"/org/springframework/web/context/WEB-INF/applicationContext.xml");
		assertTrue("Has father", context.containsBean("father"));
		assertTrue("Has rod", context.containsBean("rod"));
		assertFalse("Hasn't kerry", context.containsBean("kerry"));
		assertTrue("Doesn't have spouse", ((TestBean) context.getBean("rod")).getSpouse() == null);
		assertTrue("myinit not evaluated", "Roderick".equals(((TestBean) context.getBean("rod")).getName()));

		context = new ClassPathXmlApplicationContext(new String[] {
			"/org/springframework/web/context/WEB-INF/applicationContext.xml",
			"/org/springframework/web/context/WEB-INF/context-addition.xml" });
		assertTrue("Has father", context.containsBean("father"));
		assertTrue("Has rod", context.containsBean("rod"));
		assertTrue("Has kerry", context.containsBean("kerry"));
	}

	public void testSingletonDestructionOnStartupFailure() throws IOException {
		try {
			new ClassPathXmlApplicationContext(new String[] {
				"/org/springframework/web/context/WEB-INF/applicationContext.xml",
				"/org/springframework/web/context/WEB-INF/fail.xml" }) {

				public void refresh() throws BeansException {
					try {
						super.refresh();
					}
					catch (BeanCreationException ex) {
						DefaultListableBeanFactory factory = (DefaultListableBeanFactory) getBeanFactory();
						assertEquals(0, factory.getSingletonCount());
						throw ex;
					}
				}
			};
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
		}
	}

}
