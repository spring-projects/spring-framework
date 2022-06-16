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
public class ContextLoaderInitializerTests {

	private static final String BEAN_NAME = "myBean";

	private AbstractContextLoaderInitializer initializer;

	private MockServletContext servletContext;

	private EventListener eventListener;

	@BeforeEach
	public void setUp() throws Exception {
		servletContext = new MyMockServletContext();
		initializer = new MyContextLoaderInitializer();
		eventListener = null;
	}

	@Test
	public void register() throws ServletException {
		initializer.onStartup(servletContext);

		boolean condition1 = eventListener instanceof ContextLoaderListener;
		assertThat(condition1).isTrue();
		ContextLoaderListener cll = (ContextLoaderListener) eventListener;
		cll.contextInitialized(new ServletContextEvent(servletContext));

		WebApplicationContext applicationContext = WebApplicationContextUtils
				.getRequiredWebApplicationContext(servletContext);

		assertThat(applicationContext.containsBean(BEAN_NAME)).isTrue();
		boolean condition = applicationContext.getBean(BEAN_NAME) instanceof MyBean;
		assertThat(condition).isTrue();
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
