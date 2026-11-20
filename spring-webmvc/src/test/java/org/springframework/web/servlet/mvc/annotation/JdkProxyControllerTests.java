/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;

import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @since 3.0
 */
class JdkProxyControllerTests {

	private DispatcherServlet servlet;


	@Test
	void typeLevel() throws Exception {
		initServlet(TypeLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("doIt");
	}

	@Test
	void methodLevel() throws Exception {
		initServlet(MethodLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("doIt");
	}

	@Test
	void typeAndMethodLevel() throws Exception {
		initServlet(TypeAndMethodLevelImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/bookings");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("doIt");
	}


	@SuppressWarnings("serial")
	private void initServlet(final Class<?> controllerclass) throws ServletException {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerclass));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor(true)));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());
	}


	@Controller
	@RequestMapping("/test")
	public interface TypeLevel {

		@RequestMapping
		void doIt(Writer writer) throws IOException;
	}


	public static class TypeLevelImpl implements TypeLevel {

		@Override
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}


	@Controller
	public interface MethodLevel {

		@RequestMapping("/test")
		void doIt(Writer writer) throws IOException;
	}


	public static class MethodLevelImpl implements MethodLevel {

		@Override
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}


	@Controller
	@RequestMapping("/hotels")
	public interface TypeAndMethodLevel {

		@RequestMapping("/bookings")
		void doIt(Writer writer) throws IOException;
	}


	public static class TypeAndMethodLevelImpl implements TypeAndMethodLevel {

		@Override
		public void doIt(Writer writer) throws IOException {
			writer.write("doIt");
		}
	}

}
