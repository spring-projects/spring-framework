/*
 * Copyright 2002-2008 the original author or authors.
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
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.DerivedTestBean;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver;
import org.springframework.web.servlet.mvc.support.ControllerClassNameHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.NestedServletException;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
public class ServletAnnotationControllerTests extends TestCase {

	public void testStandardHandleMethod() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());
	}

	public void testProxiedStandardHandleMethod() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyController.class));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());
	}

	public void testEmptyParameterListHandleMethod() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(EmptyParameterListHandlerMethodController.class));
				RootBeanDefinition vrDef = new RootBeanDefinition(InternalResourceViewResolver.class);
				vrDef.getPropertyValues().addPropertyValue("suffix", ".jsp");
				wac.registerBeanDefinition("viewResolver", vrDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/emptyParameterListHandler");
		MockHttpServletResponse response = new MockHttpServletResponse();

		EmptyParameterListHandlerMethodController.called = false;
		servlet.service(request, response);
		assertTrue(EmptyParameterListHandlerMethodController.called);
		assertEquals("", response.getContentAsString());
	}

	public void testAdaptedHandleMethods() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController.class);
	}

	public void testAdaptedHandleMethods2() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController2.class);
	}

	public void testAdaptedHandleMethods3() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController3.class);
	}

	private void doTestAdaptedHandleMethods(final Class<?> controllerClass) throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerClass));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath1.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myPath2.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-value1-2", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myPath3.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addParameter("name", "name1");
		request.addParameter("age", "2");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-name1-2", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myPath4.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-name1-typeMismatch", response.getContentAsString());
	}

	public void testFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-name1-typeMismatch-tb1-myValue", response.getContentAsString());
	}

	public void testModelFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyModelFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-name1-typeMismatch-tb1-myValue", response.getContentAsString());
	}

	public void testProxiedFormController() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-name1-typeMismatch-tb1-myValue", response.getContentAsString());
	}

	public void testCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().addPropertyValue("webBindingInitializer", new MyWebBindingInitializer());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	public void testTypedCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyTypedCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().addPropertyValue("webBindingInitializer", new MyWebBindingInitializer());
				adapterDef.getPropertyValues().addPropertyValue("customArgumentResolver", new MySpecialArgumentResolver());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-Integer:10-typeMismatch-tb1-myOriginalValue", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myOtherPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-myName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myThirdPath.do");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "100");
		request.addParameter("date", "2007-10-02");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-special-99-special-99", response.getContentAsString());
	}

	public void testBinderInitializingCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyBinderInitializingCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	public void testSpecificBinderInitializingCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MySpecificBinderInitializingCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	public void testParameterDispatchingController() throws Exception {
		final MockServletContext servletContext = new MockServletContext();
		final MockServletConfig servletConfig = new MockServletConfig(servletContext);

		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.setServletContext(servletContext);
				RootBeanDefinition bd = new RootBeanDefinition(MyParameterDispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.getBeanFactory().registerResolvableDependency(ServletConfig.class, servletConfig);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(servletConfig);

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpSession session = request.getSession();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());
		assertSame(servletContext, request.getAttribute("servletContext"));
		assertSame(servletConfig, request.getAttribute("servletConfig"));
		assertSame(session, request.getAttribute("session"));
		assertSame(request, request.getAttribute("request"));

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		response = new MockHttpServletResponse();
		session = request.getSession();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());
		assertSame(servletContext, request.getAttribute("servletContext"));
		assertSame(servletConfig, request.getAttribute("servletConfig"));
		assertSame(session, request.getAttribute("session"));
		assertSame(request, request.getAttribute("request"));

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "other");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("surprise", "!");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testMethodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MethodNameDispatchingController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myHandle.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myOtherHandle.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/myLangHandle.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/mySurpriseHandle.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testMethodNameDispatchingControllerWithSuffix() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MethodNameDispatchingController.class));
				InternalPathMethodNameResolver methodNameResolver = new InternalPathMethodNameResolver();
				methodNameResolver.setSuffix("Handle");
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().addPropertyValue("methodNameResolver", methodNameResolver);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/my.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myOther.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/myLang.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/mySurprise.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testControllerClassNamePlusMethodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				RootBeanDefinition mapping = new RootBeanDefinition(ControllerClassNameHandlerMapping.class);
				mapping.getPropertyValues().addPropertyValue("excludedPackages", null);
				wac.registerBeanDefinition("handlerMapping", mapping);
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MethodNameDispatchingController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/methodnamedispatching/myHandle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/methodnamedispatching/myOtherHandle.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/methodnamedispatching/myLangHandle.x");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/methodnamedispatching/mySurpriseHandle.y");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testPostMethodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyPostMethodNameDispatchingController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myHandle.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(405, response.getStatus());

		request = new MockHttpServletRequest("POST", "/myUnknownHandle.do");
		request.addParameter("myParam", "myValue");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(404, response.getStatus());

		request = new MockHttpServletRequest("POST", "/myHandle.do");
		request.addParameter("myParam", "myValue");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/myOtherHandle.do");
		request.addParameter("myParam", "myValue");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/myLangHandle.do");
		request.addParameter("myParam", "myValue");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/mySurpriseHandle.do");
		request.addParameter("myParam", "myValue");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testRelativePathDispatchingController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyRelativePathDispatchingController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myApp/myOther");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myApp/myLang");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myApp/surprise.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	public void testNullCommandController() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyNullCommandController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath");
		request.setUserPrincipal(new OtherPrincipal());
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());
	}

	public void testEquivalentMappingsWithSameMethodName() throws Exception {
		@SuppressWarnings("serial")
		DispatcherServlet servlet = new DispatcherServlet() {
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(ChildController.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/child/test");
		request.addParameter("childId", "100");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			servlet.service(request, response);
		}
		catch (NestedServletException ex) {
			assertTrue(ex.getCause() instanceof IllegalStateException);
			assertTrue(ex.getCause().getMessage().contains("doGet"));
		}
	}


	@RequestMapping("/myPath.do")
	private static class MyController extends AbstractController {

		protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
			response.getWriter().write("test");
			return null;
		}
	}


	private static class BaseController {

		@RequestMapping(method = RequestMethod.GET)
		public void myPath2(HttpServletResponse response) throws IOException  {
			response.getWriter().write("test");
		}
	}


	@Controller
	private static class MyAdaptedController {

		@RequestMapping("/myPath1.do")
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + p1 + "-" + p2);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.do")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	@RequestMapping("/*.do")
	private static class MyAdaptedController2 {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, int param2, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + p1 + "-" + param2);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	private static class MyAdaptedControllerBase<T> {

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") T p1, int param2, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + p1 + "-" + param2);
		}

		@InitBinder
		public void initBinder(@RequestParam("param1") T p1, int param2) {
		}

		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") T p1, int param2) {
		}
	}


	@RequestMapping("/*.do")
	private static class MyAdaptedController3 extends MyAdaptedControllerBase<String> {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test");
		}

		@Override
		public void myHandle(@RequestParam("param1") String p1, int param2, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + p1 + "-" + param2);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException  {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}

		@InitBinder
		public void initBinder(@RequestParam("param1") String p1, int param2) {
		}

		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") String p1, int param2) {
		}
	}


	@Controller
	@RequestMapping(method = RequestMethod.GET)
	private static class EmptyParameterListHandlerMethodController {

		static boolean called;

		@RequestMapping("/emptyParameterListHandler")
		public void emptyParameterListHandler() {
			EmptyParameterListHandlerMethodController.called = true;
		}

		@RequestMapping("/nonEmptyParameterListHandler")
		public void nonEmptyParameterListHandler(HttpServletResponse response) {
		}
	}


	@Controller
	public static class MyFormController {

		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}


	@Controller
	public static class MyModelFormController {

		@ModelAttribute
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, Model model) {
			if (!model.containsAttribute("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}


	@Controller
	private static class MyCommandProvidingFormController<T, TB, TB2> extends MyFormController {

		@SuppressWarnings("unused")
		@ModelAttribute("myCommand")
		private TestBean createTestBean(
				@RequestParam T defaultName, Map<String, Object> model, @RequestParam Date date) {
			model.put("myKey", "myOriginalValue");
			return new TestBean(defaultName.getClass().getSimpleName() + ":" + defaultName.toString());
		}

		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			return super.myHandle(tb, errors, model);
		}

		@RequestMapping("/myOtherPath.do")
		public String myOtherHandle(TB tb, BindingResult errors, ExtendedModelMap model, MySpecialArg arg) {
			TestBean tbReal = (TestBean) tb;
			tbReal.setName("myName");
			assertTrue(model.get("ITestBean") instanceof DerivedTestBean);
			assertNotNull(arg);
			return super.myHandle(tbReal, errors, model);
		}

		@RequestMapping("/myThirdPath.do")
		public String myThirdHandle(TB tb, Model model) {
			model.addAttribute("testBean", new TestBean("special", 99));
			return "myView";
		}

		@ModelAttribute
		protected TB2 getModelAttr() {
			return (TB2) new DerivedTestBean();
		}
	}


	private static class MySpecialArg {

		public MySpecialArg(String value) {
		}
	}


	@Controller
	private static class MyTypedCommandProvidingFormController
			extends MyCommandProvidingFormController<Integer, TestBean, ITestBean> {
	}


	@Controller
	private static class MyBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@SuppressWarnings("unused")
		@InitBinder
		private void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	@Controller
	private static class MySpecificBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@SuppressWarnings("unused")
		@InitBinder({"myCommand", "date"})
		private void initBinder(WebDataBinder binder, String date, @RequestParam("date") String[] date2) {
			assertEquals("2007-10-02", date);
			assertEquals(1, date2.length);
			assertEquals("2007-10-02", date2[0]);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	private static class MyWebBindingInitializer implements WebBindingInitializer {

		public void initBinder(WebDataBinder binder, WebRequest request) {
			assertNotNull(request.getLocale());
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	private static class MySpecialArgumentResolver implements WebArgumentResolver {

		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
			if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
				return new MySpecialArg("myValue");
			}
			return UNRESOLVED;
		}
	}


	@Controller
	@RequestMapping("/myPath.do")
	private static class MyParameterDispatchingController {

		@Autowired
		private ServletContext servletContext;

		@Autowired
		private ServletConfig servletConfig;

		@Autowired
		private HttpSession session;

		@Autowired
		private HttpServletRequest request;

		@RequestMapping
		public void myHandle(HttpServletResponse response, HttpServletRequest request) throws IOException {
			if (this.servletContext == null || this.servletConfig == null || this.session == null || this.request == null) {
				throw new IllegalStateException();
			}
			response.getWriter().write("myView");
			request.setAttribute("servletContext", this.servletContext);
			request.setAttribute("servletConfig", this.servletConfig);
			request.setAttribute("session", this.session);
			request.setAttribute("request", this.request);
		}

		@RequestMapping(params = {"view", "!lang"})
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping(method = RequestMethod.GET, params = {"view=my", "lang=de"})
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, params = "surprise")
		public void mySurpriseHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}


	@Controller
	@RequestMapping(value = "/*.do", method = RequestMethod.POST, params = "myParam=myValue")
	private static class MyPostMethodNameDispatchingController extends MethodNameDispatchingController {

	}


	@Controller
	@RequestMapping("/myApp/*")
	private static class MyRelativePathDispatchingController {

		@RequestMapping
		public void myHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myView");
		}

		@RequestMapping("*Other")
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping("myLang")
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping("surprise")
		public void mySurpriseHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}


	@Controller
	private static class MyNullCommandController {

		@ModelAttribute
		public TestBean getTestBean() {
			return null;
		}

		@ModelAttribute
		public Principal getPrincipal() {
			return new TestPrincipal();
		}

		@RequestMapping("/myPath")
		public void handle(@ModelAttribute TestBean testBean, Errors errors,
				@ModelAttribute TestPrincipal modelPrinc, OtherPrincipal requestPrinc, Writer writer) throws IOException {
			assertNull(testBean);
			assertNotNull(modelPrinc);
			assertNotNull(requestPrinc);
			assertFalse(errors.hasErrors());
			errors.reject("myCode");
			writer.write("myView");
		}
	}


	private static class TestPrincipal implements Principal {

		public String getName() {
			return "test";
		}
	}


	private static class OtherPrincipal implements Principal {

		public String getName() {
			return "other";
		}
	}


	private static class TestViewResolver implements ViewResolver {

		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				public String getContentType() {
					return null;
				}
				@SuppressWarnings({"unchecked", "deprecation"})
				public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
					TestBean tb = (TestBean) model.get("testBean");
					if (tb == null) {
						tb = (TestBean) model.get("myCommand");
					}
					if (tb.getName().endsWith("myDefaultName")) {
						assertTrue(tb.getDate().getYear() == 107);
					}
					Errors errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "testBean");
					if (errors == null) {
						errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "myCommand");
					}
					if (errors.hasFieldErrors("date")) {
						throw new IllegalStateException();
					}
					if (model.containsKey("ITestBean")) {
						assertTrue(model.get(BindingResult.MODEL_KEY_PREFIX + "ITestBean") instanceof Errors);
					}
					List<TestBean> testBeans = (List<TestBean>) model.get("testBeanList");
					if (errors.hasFieldErrors("age")) {
						response.getWriter().write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
								"-" + testBeans.get(0).getName() + "-" + model.get("myKey"));
					}
					else {
						response.getWriter().write(viewName + "-" + tb.getName() + "-" + tb.getAge() + "-" +
								errors.getFieldValue("name") + "-" + errors.getFieldValue("age"));
					}
				}
			};
		}
	}


	public static class ParentController {

		@RequestMapping(method = RequestMethod.GET)
		public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		}
	}


	@Controller
	@RequestMapping("/child/test")
	public static class ChildController extends ParentController {

		@RequestMapping(method = RequestMethod.GET)
		public void doGet(HttpServletRequest req, HttpServletResponse resp, @RequestParam("childId") String id) {
		}
	}

}
