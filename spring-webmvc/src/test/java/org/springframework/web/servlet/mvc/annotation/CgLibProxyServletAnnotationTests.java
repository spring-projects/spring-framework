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

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletException;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Arjen Poutsma
 * @since 3.0
 */
public class CgLibProxyServletAnnotationTests {

	private DispatcherServlet servlet;

	@Test
	public void typeLevel() throws Exception {
		initServlet(TypeLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("doIt", response.getContentAsString());
	}

	@Test
	public void methodLevel() throws Exception {
		initServlet(MethodLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("doIt", response.getContentAsString());
	}

	@Test
	public void typeAndMethodLevel() throws Exception {
		initServlet(TypeAndMethodLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/bookings");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("doIt", response.getContentAsString());
	}


	@SuppressWarnings("serial")
	private void initServlet(final Class<?> controllerclass) throws ServletException {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent)
					throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerclass));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setProxyTargetClass(true);
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor(true)));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());
	}

	/*
	 * Controllers
	 */

	@Controller
	@RequestMapping("/test")
	public static class TypeLevelImpl {

		@RequestMapping
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}

	@Controller
	public static class MethodLevelImpl {

		@RequestMapping("/test")
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}

	@Controller
	@RequestMapping("/hotels")
	public static class TypeAndMethodLevelImpl {

		@RequestMapping("/bookings")
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}

}
