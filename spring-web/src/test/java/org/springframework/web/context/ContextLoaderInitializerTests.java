/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context;

import java.util.EventListener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test case for {@link AbstractContextLoaderInitializer}.
 *
 * @author Arjen Poutsma
 */
class ContextLoaderInitializerTests {

	private static final String BEAN_NAME = "myBean";

	private AbstractContextLoaderInitializer initializer;

	private MockServletContext servletContext;

	private EventListener eventListener;

	@BeforeEach
	void setUp() {
		servletContext = new MyMockServletContext();
		initializer = new MyContextLoaderInitializer();
		eventListener = null;
	}

	@Test
	void register() throws ServletException {
		initializer.onStartup(servletContext);

		assertThat(eventListener).isInstanceOf(ContextLoaderListener.class);
		ContextLoaderListener cll = (ContextLoaderListener) eventListener;
		cll.contextInitialized(new ServletContextEvent(servletContext));

		WebApplicationContext applicationContext = WebApplicationContextUtils
				.getRequiredWebApplicationContext(servletContext);

		assertThat(applicationContext.containsBean(BEAN_NAME)).isTrue();
		assertThat(applicationContext.getBean(BEAN_NAME)).isInstanceOf(MyBean.class);
	}

	private class MyMockServletContext extends MockServletContext {

		@Override
		public <T extends EventListener> void addListener(T listener) {
			eventListener = listener;
		}

	}

	private static class MyContextLoaderInitializer
			extends AbstractContextLoaderInitializer {

		@Override
		protected WebApplicationContext createRootApplicationContext() {
			StaticWebApplicationContext rootContext = new StaticWebApplicationContext();
			rootContext.registerSingleton(BEAN_NAME, MyBean.class);
			return rootContext;
		}
	}

	private static class MyBean {

	}
}
