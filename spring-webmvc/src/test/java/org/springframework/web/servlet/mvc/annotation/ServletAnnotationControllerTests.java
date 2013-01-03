/*
 * Copyright 2002-2013 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.GenericBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockServletConfig;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver;
import org.springframework.web.servlet.mvc.support.ControllerClassNameHandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.NestedServletException;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @since 2.5
 */
public class ServletAnnotationControllerTests {

	private DispatcherServlet servlet;

	@Test
	public void standardHandleMethod() throws Exception {
		initServlet(MyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void emptyRequestMapping() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(ControllerWithEmptyMapping.class));
				RootBeanDefinition mbd = new RootBeanDefinition(ControllerClassNameHandlerMapping.class);
				mbd.getPropertyValues().add("excludedPackages", null);
				mbd.getPropertyValues().add("order", 0);
				wac.registerBeanDefinition("mapping", mbd);
				wac.registerBeanDefinition("mapping2", new RootBeanDefinition(DefaultAnnotationHandlerMapping.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servletannotationcontrollertests.controllerwithemptymapping");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void emptyValueMapping() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(ControllerWithEmptyValueMapping.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContextPath("/foo");
		request.setServletPath("");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());
	}

	@Test
	public void customAnnotationController() throws Exception {
		initServlet(CustomAnnotationController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void requiredParamMissing() throws Exception {
		initServlet(RequiredParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
		assertTrue(servlet.getWebApplicationContext().isSingleton("controller"));
	}

	@Test
	public void typeConversionError() throws Exception {
		initServlet(RequiredParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("id", "foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void optionalParamPresent() throws Exception {
		initServlet(OptionalParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("id", "val");
		request.addParameter("flag", "true");
		request.addHeader("header", "otherVal");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("val-true-otherVal", response.getContentAsString());
	}

	@Test
	public void optionalParamMissing() throws Exception {
		initServlet(OptionalParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("null-false-null", response.getContentAsString());
	}

	@Test
	public void defaultParameters() throws Exception {
		initServlet(DefaultValueParamController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("foo--bar", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void defaultExpressionParameters() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(DefaultExpressionValueParamController.class));
				RootBeanDefinition ppc = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
				ppc.getPropertyValues().add("properties", "myKey=foo");
				wac.registerBeanDefinition("ppc", ppc);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myPath.do");
		request.setContextPath("/myApp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		System.setProperty("myHeader", "bar");
		try {
			servlet.service(request, response);
		}
		finally {
			System.clearProperty("myHeader");
		}
		assertEquals("foo-bar-/myApp", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void typeNestedSetBinding() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(NestedSetController.class));
				RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
				csDef.getPropertyValues().add("converters", new TestBeanConverter());
				RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
				wbiDef.getPropertyValues().add("conversionService", csDef);
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		request.addParameter("testBeanSet", new String[] {"1", "2"});
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("[1, 2]-org.springframework.tests.sample.beans.TestBean", response.getContentAsString());
	}

	@Test
	public void methodNotAllowed() throws Exception {
		initServlet(MethodNotAllowedController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status", HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());
		String allowHeader = response.getHeader("Allow");
		assertNotNull("No Allow header", allowHeader);
		Set<String> allowedMethods = new HashSet<String>();
		allowedMethods.addAll(Arrays.asList(StringUtils.delimitedListToStringArray(allowHeader, ", ")));
		assertEquals("Invalid amount of supported methods", 6, allowedMethods.size());
		assertTrue("PUT not allowed", allowedMethods.contains("PUT"));
		assertTrue("DELETE not allowed", allowedMethods.contains("DELETE"));
		assertTrue("HEAD not allowed", allowedMethods.contains("HEAD"));
		assertTrue("TRACE not allowed", allowedMethods.contains("TRACE"));
		assertTrue("OPTIONS not allowed", allowedMethods.contains("OPTIONS"));
		assertTrue("POST not allowed", allowedMethods.contains("POST"));
	}

	@Test
	@SuppressWarnings("serial")
	public void proxiedStandardHandleMethod() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet() {
			@Override
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

	@Test
	public void emptyParameterListHandleMethod() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(EmptyParameterListHandlerMethodController.class));
				RootBeanDefinition vrDef = new RootBeanDefinition(InternalResourceViewResolver.class);
				vrDef.getPropertyValues().add("suffix", ".jsp");
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

	@Test
	public void sessionAttributeExposure() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MySessionAttributesController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page1", request.getAttribute("viewName"));
		HttpSession session = request.getSession();
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page2", request.getAttribute("viewName"));
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
	}

	@Test
	public void sessionAttributeExposureWithInterface() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MySessionAttributesControllerImpl.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page1", request.getAttribute("viewName"));
		HttpSession session = request.getSession();
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page2", request.getAttribute("viewName"));
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
	}

	@Test
	public void parameterizedAnnotatedInterface() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyParameterizedControllerImpl.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page1", request.getAttribute("viewName"));
		HttpSession session = request.getSession();
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("testBeanList"));

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page2", request.getAttribute("viewName"));
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("testBeanList"));
	}

	@Test
	public void parameterizedAnnotatedInterfaceWithOverriddenMappingsInImpl() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MyParameterizedControllerImplWithOverriddenMappings.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPage");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page1", request.getAttribute("viewName"));
		HttpSession session = request.getSession();
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("testBeanList"));

		request = new MockHttpServletRequest("POST", "/myPage");
		request.setSession(session);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("page2", request.getAttribute("viewName"));
		assertTrue(session.getAttribute("object1") != null);
		assertTrue(session.getAttribute("object2") != null);
		assertTrue(((Map) session.getAttribute("model")).containsKey("object1"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("object2"));
		assertTrue(((Map) session.getAttribute("model")).containsKey("testBeanList"));
	}

	@Test
	public void adaptedHandleMethods() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController.class);
	}

	@Test
	public void adaptedHandleMethods2() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController2.class);
	}

	@Test
	public void adaptedHandleMethods3() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController3.class);
	}

	@SuppressWarnings("serial")
	private void initServlet(final Class<?> controllerClass) throws ServletException {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerClass));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());
	}

	private void doTestAdaptedHandleMethods(final Class<?> controllerClass) throws Exception {
		initServlet(controllerClass);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath1.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		servlet.service(request, response);
		assertEquals("test", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/myPath2.do");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addHeader("header1", "10");
		request.setCookies(new Cookie("cookie1", "3"));
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-value1-2-10-3", response.getContentAsString());

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

	@Test
	public void formController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
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

	@Test
	public void modelFormController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
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
		assertEquals("myPath-name1-typeMismatch-tb1-myValue-yourValue", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void proxiedFormController() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory()
						.registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
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

	@Test
	public void commandProvidingFormControllerWithCustomEditor() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MyCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
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

	@Test
	public void typedCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MyTypedCommandProvidingFormController.class));
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(TestViewResolver.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
				adapterDef.getPropertyValues().add("customArgumentResolver", new MySpecialArgumentResolver());
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

	@Test
	public void binderInitializingCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MyBinderInitializingCommandProvidingFormController.class));
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

	@Test
	public void specificBinderInitializingCommandProvidingFormController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MySpecificBinderInitializingCommandProvidingFormController.class));
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

	@Test
	public void parameterDispatchingController() throws Exception {
		final MockServletContext servletContext = new MockServletContext();
		final MockServletConfig servletConfig = new MockServletConfig(servletContext);

		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.setServletContext(servletContext);
				RootBeanDefinition bd = new RootBeanDefinition(MyParameterDispatchingController.class);
				//bd.setScope(WebApplicationContext.SCOPE_REQUEST);
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
		assertSame(session.getId(), request.getAttribute("sessionId"));
		assertSame(request.getRequestURI(), request.getAttribute("requestUri"));
		assertSame(request.getLocale(), request.getAttribute("locale"));

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		response = new MockHttpServletResponse();
		session = request.getSession();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());
		assertSame(servletContext, request.getAttribute("servletContext"));
		assertSame(servletConfig, request.getAttribute("servletConfig"));
		assertSame(session.getId(), request.getAttribute("sessionId"));
		assertSame(request.getRequestURI(), request.getAttribute("requestUri"));

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

		MyParameterDispatchingController deserialized = (MyParameterDispatchingController) SerializationTestUtils
				.serializeAndDeserialize(servlet.getWebApplicationContext().getBean("controller"));
		assertNotNull(deserialized.request);
		assertNotNull(deserialized.session);
	}

	@Test
	public void constrainedParameterDispatchingController() throws Exception {
		final MockServletContext servletContext = new MockServletContext();
		final MockServletConfig servletConfig = new MockServletConfig(servletContext);

		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.setServletContext(servletContext);
				RootBeanDefinition bd = new RootBeanDefinition(MyConstrainedParameterDispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(servletConfig);

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "other");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(400, response.getStatus());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("active", "true");
		request.addParameter("view", "other");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(400, response.getStatus());

		request = new MockHttpServletRequest(servletContext, "GET", "/myPath.do");
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		request.addParameter("active", "true");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());
	}

	@Test
	public void methodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
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

	@Test
	public void methodNameDispatchingControllerWithSuffix() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MethodNameDispatchingController.class));
				InternalPathMethodNameResolver methodNameResolver = new InternalPathMethodNameResolver();
				methodNameResolver.setSuffix("Handle");
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("methodNameResolver", methodNameResolver);
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

	@Test
	public void controllerClassNamePlusMethodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				RootBeanDefinition mapping = new RootBeanDefinition(ControllerClassNameHandlerMapping.class);
				mapping.getPropertyValues().add("excludedPackages", null);
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

	@Test
	public void postMethodNameDispatchingController() throws Exception {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(MyPostMethodNameDispatchingController.class));
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

	@Test
	public void relativePathDispatchingController() throws Exception {
		initServlet(MyRelativePathDispatchingController.class);
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

	@Test
	public void relativeMethodPathDispatchingController() throws Exception {
		initServlet(MyRelativeMethodPathDispatchingController.class);
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myApp/myHandle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/yourApp/myOther");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/hisApp/myLang");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/herApp/surprise.do");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	@Test
	public void nullCommandController() throws Exception {
		initServlet(MyNullCommandController.class);
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myPath");
		request.setUserPrincipal(new OtherPrincipal());
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myView", response.getContentAsString());
	}

	@Test
	public void equivalentMappingsWithSameMethodName() throws Exception {
		initServlet(ChildController.class);
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/child/test");
		request.addParameter("childId", "100");
		MockHttpServletResponse response = new MockHttpServletResponse();
		try {
			servlet.service(request, response);
			fail("Didn't fail with due to ambiguous method mapping");
		}
		catch (NestedServletException ex) {
			assertTrue(ex.getCause() instanceof IllegalStateException);
			assertTrue(ex.getCause().getMessage().contains("doGet"));
		}
	}

	@Test
	public void pathOrdering() throws ServletException, IOException {
		initServlet(PathOrderingController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dir/myPath1.do");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("method1", response.getContentAsString());
	}

	@Test
	public void requestBodyResponseBody() throws ServletException, IOException {
		initServlet(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "text/*, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals(requestBody, response.getContentAsString());
	}

	@Test
	public void responseBodyNoAcceptableMediaType() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(RequestResponseBodyController.class));
				RootBeanDefinition converterDef = new RootBeanDefinition(StringHttpMessageConverter.class);
				converterDef.getPropertyValues().add("supportedMediaTypes", new MediaType("text", "plain"));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				StringHttpMessageConverter converter = new StringHttpMessageConverter();
				converter.setSupportedMediaTypes(Collections.singletonList(new MediaType("text", "plain")));
				adapterDef.getPropertyValues().add("messageConverters", converter);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "application/pdf, application/msword");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(406, response.getStatus());
	}

	@Test
	public void responseBodyWildCardMediaType() throws ServletException, IOException {
		initServlet(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "*/*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(requestBody, response.getContentAsString());
	}

	@Test
	public void unsupportedRequestBody() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(RequestResponseBodyController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("messageConverters", new ByteArrayHttpMessageConverter());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(415, response.getStatus());
		assertNotNull("No Accept response header set", response.getHeader("Accept"));
	}

	@Test
	public void responseBodyNoAcceptHeader() throws ServletException, IOException {
		initServlet(RequestResponseBodyController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());
		assertEquals(requestBody, response.getContentAsString());
	}

	@Test
	public void badRequestRequestBody() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(RequestResponseBodyController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("messageConverters", new NotReadableMessageConverter());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void httpEntity() throws ServletException, IOException {
		initServlet(ResponseEntityController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/foo");
		String requestBody = "Hello World";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "text/*, */*");
		request.addHeader("MyRequestHeader", "MyValue");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(201, response.getStatus());
		assertEquals(requestBody, response.getContentAsString());
		assertEquals("MyValue", response.getHeader("MyResponseHeader"));

		request = new MockHttpServletRequest("PUT", "/bar");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("MyValue", response.getHeader("MyResponseHeader"));
		assertEquals(404, response.getStatus());
	}


	/*
	 * See SPR-6877
	 */
	@Test
	public void overlappingMesssageConvertersRequestBody() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(RequestResponseBodyController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				List<HttpMessageConverter> messageConverters = new ArrayList<HttpMessageConverter>();
				messageConverters.add(new StringHttpMessageConverter());
				messageConverters
						.add(new SimpleMessageConverter(new MediaType("application","json"), MediaType.ALL));
				adapterDef.getPropertyValues().add("messageConverters", messageConverters);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		request.setContent("Hello World".getBytes("UTF-8"));
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("Accept", "application/json, text/javascript, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", "application/json", response.getHeader("Content-Type"));
	}

	@Test
	public void responseBodyVoid() throws ServletException, IOException {
		initServlet(ResponseBodyVoidController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/*, */*");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());
	}

	@Test
	public void responseBodyArgMismatch() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(RequestBodyArgMismatchController.class));

				Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
				marshaller.setClassesToBeBound(A.class, B.class);
				try {
					marshaller.afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanCreationException(ex.getMessage(), ex);
				}

				MarshallingHttpMessageConverter messageConverter = new MarshallingHttpMessageConverter(marshaller);

				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("messageConverters", messageConverter);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());


		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/something");
		String requestBody = "<b/>";
		request.setContent(requestBody.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/xml; charset=utf-8");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(400, response.getStatus());
	}


	@Test
	public void contentTypeHeaders() throws ServletException, IOException {
		initServlet(ContentTypeHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("pdf", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/something");
		request.addHeader("Content-Type", "text/html");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("text", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/something");
		request.addHeader("Content-Type", "application/xml");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(404, response.getStatus());
	}

	@Test
	public void negatedContentTypeHeaders() throws ServletException, IOException {
		initServlet(NegatedContentTypeHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/something");
		request.addHeader("Content-Type", "application/pdf");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("pdf", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/something");
		request.addHeader("Content-Type", "text/html");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("non-pdf", response.getContentAsString());
	}

	@Test
	public void acceptHeaders() throws ServletException, IOException {
		initServlet(AcceptHeadersController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("html", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("xml", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "application/xml, text/html");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("xml", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/something");
		request.addHeader("Accept", "text/html;q=0.9, application/xml");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("xml", response.getContentAsString());
	}

	@Test
	public void responseStatus() throws ServletException, IOException {
		initServlet(ResponseStatusController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/something");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("something", response.getContentAsString());
		assertEquals(201, response.getStatus());
		assertEquals("It's alive!", response.getErrorMessage());
	}

	@Test
	public void mavResolver() throws ServletException, IOException {
		@SuppressWarnings("serial") DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(ModelAndViewResolverController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("customModelAndViewResolver", new MyModelAndViewResolver());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myValue", response.getContentAsString());

	}

	@Test
	public void bindingCookieValue() throws ServletException, IOException {
		initServlet(BindingCookieValueController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.setCookies(new Cookie("date", "2008-11-18"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-2008", response.getContentAsString());
	}

	@Test
	public void ambiguousParams() throws ServletException, IOException {
		initServlet(AmbiguousParamsController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("noParams", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/test");
		request.addParameter("myParam", "42");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("myParam-42", response.getContentAsString());
	}

	@Test
	public void bridgeMethods() throws Exception {
		initServlet(TestControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/method");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
	}

	@Test
	public void requestParamMap() throws Exception {
		initServlet(RequestParamMapController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
		request.addParameter("key1", "value1");
		request.addParameter("key2", new String[]{"value21", "value22"});
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);
		assertEquals("key1=value1,key2=value21", response.getContentAsString());

		request.setRequestURI("/multiValueMap");
		response = new MockHttpServletResponse();

		servlet.service(request, response);
		assertEquals("key1=[value1],key2=[value21,value22]", response.getContentAsString());
	}

	@Test
	public void requestHeaderMap() throws Exception {
		initServlet(RequestHeaderMapController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/map");
		request.addHeader("Content-Type", "text/html");
		request.addHeader("Custom-Header", new String[]{"value21", "value22"});
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);
		assertEquals("Content-Type=text/html,Custom-Header=value21", response.getContentAsString());

		request.setRequestURI("/multiValueMap");
		response = new MockHttpServletResponse();

		servlet.service(request, response);
		assertEquals("Content-Type=[text/html],Custom-Header=[value21,value22]", response.getContentAsString());

		request.setRequestURI("/httpHeaders");
		response = new MockHttpServletResponse();

		servlet.service(request, response);
		assertEquals("Content-Type=[text/html],Custom-Header=[value21,value22]", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void controllerClassNameNoTypeLevelAnn() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent)
					throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(BookController.class));
				RootBeanDefinition mapping = new RootBeanDefinition(ControllerClassNameHandlerMapping.class);
				mapping.getPropertyValues().add("excludedPackages", null);
				wac.registerBeanDefinition("handlerMapping", mapping);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/list");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("list", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/book/show");
		request.addParameter("id", "12");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("show-id=12", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/book");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("create", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void simpleUrlHandlerMapping() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent)
					throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(BookController.class));
				RootBeanDefinition hmDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
				hmDef.getPropertyValues().add("mappings", "/book/*=controller\n/book=controller");
				wac.registerBeanDefinition("handlerMapping", hmDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/list");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("list", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/book/show");
		request.addParameter("id", "12");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("show-id=12", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/book");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("create", response.getContentAsString());
	}

	@Test
	public void requestMappingInterface() throws Exception {
		initServlet(IMyControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle null", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("p", "value");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle value", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void requestMappingInterfaceWithProxy() throws Exception {
		DispatcherServlet servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(IMyControllerImpl.class));
				DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
				autoProxyCreator.setBeanFactory(wac.getBeanFactory());
				wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
				wac.getBeanFactory().registerSingleton("advisor", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle null", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("p", "value");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle value", response.getContentAsString());
	}

	@Test
	public void requestMappingBaseClass() throws Exception {
		initServlet(MyAbstractControllerImpl.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle", response.getContentAsString());

	}

	@Test
	public void trailingSlash() throws Exception {
		initServlet(TrailingSlashController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("templatePath", response.getContentAsString());
	}

	@Test
	public void testMatchWithoutMethodLevelPath() throws Exception {
		initServlet(NoPathGetAndM2PostController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/t1/m2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(405, response.getStatus());
	}

	// SPR-8536

	@Test
	public void testHeadersCondition() throws Exception {
		initServlet(HeadersConditionController.class);

		// No "Accept" header
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("home", response.getForwardedUrl());

		// Accept "*/*"
		request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "*/*");
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("home", response.getForwardedUrl());

		// Accept "application/json"
		request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "application/json");
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("application/json", response.getHeader("Content-Type"));
		assertEquals("homeJson", response.getContentAsString());
	}

	@Test
	public void redirectAttribute() throws Exception {
		initServlet(RedirectAttributesController.class);
		try {
			servlet.service(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse());
		}
		catch (NestedServletException ex) {
			assertTrue(ex.getMessage().contains("not assignable from the actual model"));
		}
	}

	/*
	 * See SPR-6021
	 */
	@Test
	public void customMapEditor() throws Exception {
		initServlet(CustomMapEditorController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/handle");
		request.addParameter("map", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);

		assertEquals("test-{foo=bar}", response.getContentAsString());
	}

	@Test
	public void multipartFileAsSingleString() throws Exception {
		initServlet(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/singleString");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen", response.getContentAsString());
	}

	@Test
	public void regularParameterAsSingleString() throws Exception {
		initServlet(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/singleString");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen", response.getContentAsString());
	}

	@Test
	public void multipartFileAsStringArray() throws Exception {
		initServlet(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen", response.getContentAsString());
	}

	@Test
	public void regularParameterAsStringArray() throws Exception {
		initServlet(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen", response.getContentAsString());
	}

	@Test
	public void multipartFilesAsStringArray() throws Exception {
		initServlet(MultipartController.class);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.addFile(new MockMultipartFile("content", "Juergen".getBytes()));
		request.addFile(new MockMultipartFile("content", "Eva".getBytes()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen-Eva", response.getContentAsString());
	}

	@Test
	public void regularParametersAsStringArray() throws Exception {
		initServlet(MultipartController.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/stringArray");
		request.setMethod("POST");
		request.addParameter("content", "Juergen");
		request.addParameter("content", "Eva");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Juergen-Eva", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void parameterCsvAsIntegerArray() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(CsvController.class));
				RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
				RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
				wbiDef.getPropertyValues().add("conversionService", csDef);
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/integerArray");
		request.setMethod("POST");
		request.addParameter("content", "1,2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("1-2", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void parameterCsvAsIntegerSet() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(CsvController.class));
				RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
				RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
				wbiDef.getPropertyValues().add("conversionService", csDef);
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/integerSet");
		request.setMethod("POST");
		request.addParameter("content", "1,2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("1-2", response.getContentAsString());
	}

	@Test
	@SuppressWarnings("serial")
	public void parameterCsvAsIntegerSetWithCustomSeparator() throws Exception {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(CsvController.class));
				RootBeanDefinition csDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
				RootBeanDefinition wbiDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
				wbiDef.getPropertyValues().add("conversionService", csDef);
				wbiDef.getPropertyValues().add("propertyEditorRegistrars", new RootBeanDefinition(ListEditorRegistrar.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", wbiDef);
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/integerSet");
		request.setMethod("POST");
		request.addParameter("content", "1;2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("1-2", response.getContentAsString());
	}

	public static class ListEditorRegistrar implements PropertyEditorRegistrar {

		@Override
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(Set.class, new ListEditor());
		}
	}

	public static class ListEditor extends PropertyEditorSupport {

		@SuppressWarnings("unchecked")
		@Override
		public String getAsText() {
			return StringUtils.collectionToDelimitedString((Collection<String>) getValue(), ";");
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			Set<String> s = new LinkedHashSet<String>();
			for (String t : text.split(";")) {
			  s.add(t);
			}
			setValue(s);
		}
	}


	/*
	 * Controllers
	 */

	@RequestMapping("/myPath.do")
	private static class MyController extends AbstractController {

		@Override
		protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			response.getWriter().write("test");
			return null;
		}
	}

	@Controller
	private static class ControllerWithEmptyMapping {

		@RequestMapping
		public void myPath2(HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}
	}

	@Controller
	private static class ControllerWithEmptyValueMapping {

		@RequestMapping("")
		public void myPath2(HttpServletResponse response) throws IOException {
			throw new IllegalStateException("test");
		}

		@RequestMapping("/bar")
		public void myPath3(HttpServletResponse response) throws IOException {
			response.getWriter().write("testX");
		}

		@ExceptionHandler
		public void myPath2(Exception ex, HttpServletResponse response) throws IOException {
			response.getWriter().write(ex.getMessage());
		}
	}

	@Controller
	private static class MyAdaptedController {

		@RequestMapping("/myPath1.do")
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2,
				@RequestHeader("header1") long h1, @CookieValue("cookie1") Cookie c1,
				HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + p2 + "-" + h1 + "-" + c1.getValue());
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.do")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}

	@Controller
	@RequestMapping("/*.do")
	private static class MyAdaptedController2 {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") String p1, int param2, HttpServletResponse response,
				@RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}

	@Controller
	private static class MyAdaptedControllerBase<T> {

		@RequestMapping("/myPath2.do")
		public void myHandle(@RequestParam("param1") T p1, int param2, @RequestHeader Integer header1,
				@CookieValue int cookie1, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
		}

		@InitBinder
		public void initBinder(@RequestParam("param1") String p1, @RequestParam(value="paramX", required=false) String px, int param2) {
			assertNull(px);
		}

		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") String p1, @RequestParam(value="paramX", required=false) String px, int param2) {
			assertNull(px);
		}
	}

	@RequestMapping("/*.do")
	private static class MyAdaptedController3 extends MyAdaptedControllerBase<String> {

		@RequestMapping
		public void myHandle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.getWriter().write("test");
		}

		@Override
		public void myHandle(@RequestParam("param1") String p1, int param2, @RequestHeader Integer header1,
				@CookieValue int cookie1, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
		}

		@RequestMapping("/myPath3")
		public void myHandle(TestBean tb, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("/myPath4.*")
		public void myHandle(TestBean tb, Errors errors, HttpServletResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}

		@Override
		@InitBinder
		public void initBinder(@RequestParam("param1") String p1, @RequestParam(value="paramX", required=false) String px, int param2) {
			assertNull(px);
		}

		@Override
		@ModelAttribute
		public void modelAttribute(@RequestParam("param1") String p1, @RequestParam(value="paramX", required=false) String px, int param2) {
			assertNull(px);
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
	@RequestMapping("/myPage")
	@SessionAttributes({"object1", "object2"})
	public static class MySessionAttributesController {

		@RequestMapping(method = RequestMethod.GET)
		public String get(Model model) {
			model.addAttribute("object1", new Object());
			model.addAttribute("object2", new Object());
			return "page1";
		}

		@RequestMapping(method = RequestMethod.POST)
		public String post(@ModelAttribute("object1") Object object1) {
			//do something with object1
			return "page2";

		}
	}

	@RequestMapping("/myPage")
	@SessionAttributes({"object1", "object2"})
	public interface MySessionAttributesControllerIfc {

		@RequestMapping(method = RequestMethod.GET)
		String get(Model model);

		@RequestMapping(method = RequestMethod.POST)
		String post(@ModelAttribute("object1") Object object1);
	}

	@Controller
	public static class MySessionAttributesControllerImpl implements MySessionAttributesControllerIfc {

		@Override
		public String get(Model model) {
			model.addAttribute("object1", new Object());
			model.addAttribute("object2", new Object());
			return "page1";
		}

		@Override
		public String post(@ModelAttribute("object1") Object object1) {
			//do something with object1
			return "page2";
		}
	}

	@RequestMapping("/myPage")
	@SessionAttributes({"object1", "object2"})
	public interface MyParameterizedControllerIfc<T> {

		@ModelAttribute("testBeanList")
		List<TestBean> getTestBeans();

		@RequestMapping(method = RequestMethod.GET)
		String get(Model model);
	}

	public interface MyEditableParameterizedControllerIfc<T> extends MyParameterizedControllerIfc<T> {

		@RequestMapping(method = RequestMethod.POST)
		String post(@ModelAttribute("object1") T object);
	}

	@Controller
	public static class MyParameterizedControllerImpl implements MyEditableParameterizedControllerIfc<TestBean> {

		@Override
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@Override
		public String get(Model model) {
			model.addAttribute("object1", new TestBean());
			model.addAttribute("object2", new TestBean());
			return "page1";
		}

		@Override
		public String post(TestBean object) {
			//do something with object1
			return "page2";
		}
	}

	@Controller
	public static class MyParameterizedControllerImplWithOverriddenMappings implements MyEditableParameterizedControllerIfc<TestBean> {

		@Override
		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@Override
		@RequestMapping(method = RequestMethod.GET)
		public String get(Model model) {
			model.addAttribute("object1", new TestBean());
			model.addAttribute("object2", new TestBean());
			return "page1";
		}

		@Override
		@RequestMapping(method = RequestMethod.POST)
		public String post(@ModelAttribute("object1") TestBean object1) {
			//do something with object1
			return "page2";
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
			FieldError error = errors.getFieldError("age");
			assertNotNull("Must have field error for age property", error);
			assertEquals("value2", error.getRejectedValue());
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}

	public static class ValidTestBean extends TestBean {

		@NotNull(groups = MyGroup.class)
		private String validCountry;

		public void setValidCountry(String validCountry) {
			this.validCountry = validCountry;
		}

		public String getValidCountry() {
			return this.validCountry;
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
		@ModelAttribute("yourKey")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, Model model) {
			if (!model.containsAttribute("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "yourValue";
		}
	}

	@Controller
	private static class MyCommandProvidingFormController<T, TB, TB2> extends MyFormController {

		@SuppressWarnings("unused")
		@ModelAttribute("myCommand")
		private ValidTestBean createTestBean(@RequestParam T defaultName, Map<String, Object> model, @RequestParam Date date) {
			model.put("myKey", "myOriginalValue");
			ValidTestBean tb = new ValidTestBean();
			tb.setName(defaultName.getClass().getSimpleName() + ":" + defaultName.toString());
			return tb;
		}

		@Override
		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") @Validated(MyGroup.class) TestBean tb, BindingResult errors, ModelMap model) {
			if (!errors.hasFieldErrors("validCountry")) {
				throw new IllegalStateException("Declarative validation not applied");
			}
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
			binder.setRequiredFields("sex");
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@Override
		@RequestMapping("/myPath.do")
		public String myHandle(@ModelAttribute("myCommand") @Validated(MyGroup.class) TestBean tb, BindingResult errors, ModelMap model) {
			if (!errors.hasFieldErrors("sex")) {
				throw new IllegalStateException("requiredFields not applied");
			}
			return super.myHandle(tb, errors, model);
		}
	}

	@Controller
	private static class MySpecificBinderInitializingCommandProvidingFormController
			extends MyCommandProvidingFormController {

		@SuppressWarnings("unused")
		@InitBinder({"myCommand", "date"})
		private void initBinder(WebDataBinder binder, String date, @RequestParam("date") String[] date2) {
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			assertEquals("2007-10-02", date);
			assertEquals(1, date2.length);
			assertEquals("2007-10-02", date2[0]);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}

	public interface MyGroup {
	}

	private static class MyWebBindingInitializer implements WebBindingInitializer {

		@Override
		public void initBinder(WebDataBinder binder, WebRequest request) {
			LocalValidatorFactoryBean vf = new LocalValidatorFactoryBean();
			vf.afterPropertiesSet();
			binder.setValidator(vf);
			assertNotNull(request.getLocale());
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}

	private static class MySpecialArgumentResolver implements WebArgumentResolver {

		@Override
		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
			if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
				return new MySpecialArg("myValue");
			}
			return UNRESOLVED;
		}
	}

	@SuppressWarnings("serial")
	@Controller
	@RequestMapping("/myPath.do")
	private static class MyParameterDispatchingController implements Serializable {

		@Autowired
		private transient ServletContext servletContext;

		@Autowired
		private transient ServletConfig servletConfig;

		@Autowired
		private HttpSession session;

		@Autowired
		private HttpServletRequest request;

		@Autowired
		private WebRequest webRequest;

		@RequestMapping
		public void myHandle(HttpServletResponse response, HttpServletRequest request) throws IOException {
			if (this.servletContext == null || this.servletConfig == null || this.session == null ||
					this.request == null || this.webRequest == null) {
				throw new IllegalStateException();
			}
			response.getWriter().write("myView");
			request.setAttribute("servletContext", this.servletContext);
			request.setAttribute("servletConfig", this.servletConfig);
			request.setAttribute("sessionId", this.session.getId());
			request.setAttribute("requestUri", this.request.getRequestURI());
			request.setAttribute("locale", this.webRequest.getLocale());
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
	@RequestMapping(value = "/myPath.do", params = {"active"})
	private static class MyConstrainedParameterDispatchingController {

		@RequestMapping(params = {"view", "!lang"})
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping(method = RequestMethod.GET, params = {"view=my", "lang=de"})
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
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
	private static class MyRelativeMethodPathDispatchingController {

		@RequestMapping("**/myHandle")
		public void myHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myView");
		}

		@RequestMapping("/**/*Other")
		public void myOtherHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping("**/myLang")
		public void myLangHandle(HttpServletResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping("/**/surprise")
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
		public void handle(@ModelAttribute TestBean testBean,
				Errors errors,
				@ModelAttribute TestPrincipal modelPrinc,
				OtherPrincipal requestPrinc,
				Writer writer) throws IOException {
			assertNull(testBean);
			assertNotNull(modelPrinc);
			assertNotNull(requestPrinc);
			assertFalse(errors.hasErrors());
			errors.reject("myCode");
			writer.write("myView");
		}
	}

	private static class TestPrincipal implements Principal {

		@Override
		public String getName() {
			return "test";
		}
	}

	private static class OtherPrincipal implements Principal {

		@Override
		public String getName() {
			return "other";
		}
	}

	private static class TestViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				@Override
				public String getContentType() {
					return null;
				}

				@Override
				@SuppressWarnings({"unchecked", "deprecation"})
				public void render(Map model, HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					TestBean tb = (TestBean) model.get("testBean");
					if (tb == null) {
						tb = (TestBean) model.get("myCommand");
					}
					if (tb.getName() != null && tb.getName().endsWith("myDefaultName")) {
						assertEquals(107, tb.getDate().getYear());
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
						response.getWriter()
								.write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
										"-" + testBeans.get(0).getName() + "-" + model.get("myKey") +
										(model.containsKey("yourKey") ? "-" + model.get("yourKey") : ""));
					}
					else {
						response.getWriter().write(viewName + "-" + tb.getName() + "-" + tb.getAge() + "-" +
								errors.getFieldValue("name") + "-" + errors.getFieldValue("age"));
					}
				}
			};
		}
	}

	public static class ModelExposingViewResolver implements ViewResolver {

		@Override
		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				@Override
				public String getContentType() {
					return null;
				}
				@Override
				public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
					request.setAttribute("viewName", viewName);
					request.getSession().setAttribute("model", model);
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

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Controller
	public @interface MyControllerAnnotation {

	}

	@MyControllerAnnotation
	public static class CustomAnnotationController {

		@RequestMapping("/myPath.do")
		public void myHandle() {
		}
	}

	@Controller
	public static class RequiredParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", required = true) int id,
				@RequestHeader(value = "header", required = true) String header) {
		}
	}

	@Controller
	public static class OptionalParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(required = false) String id,
				@RequestParam(required = false) boolean flag,
				@RequestHeader(value = "header", required = false) String header,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + flag + "-" + String.valueOf(header));
		}
	}

	@Controller
	public static class DefaultValueParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", defaultValue = "foo") String id,
				@RequestParam(value = "otherId", defaultValue = "") String id2,
				@RequestHeader(defaultValue = "bar") String header,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(id2) + "-" + String.valueOf(header));
		}
	}

	@Controller
	public static class DefaultExpressionValueParamController {

		@RequestMapping("/myPath.do")
		public void myHandle(@RequestParam(value = "id", defaultValue = "${myKey}") String id,
				@RequestHeader(defaultValue = "#{systemProperties.myHeader}") String header,
				@Value("#{request.contextPath}") String contextPath,
				HttpServletResponse response) throws IOException {
			response.getWriter().write(String.valueOf(id) + "-" + String.valueOf(header) + "-" + contextPath);
		}
	}

	@Controller
	public static class NestedSetController {

		@RequestMapping("/myPath.do")
		public void myHandle(GenericBean gb, HttpServletResponse response) throws Exception {
			response.getWriter().write(gb.getTestBeanSet().toString() + "-" +
					gb.getTestBeanSet().iterator().next().getClass().getName());
		}
	}

	public static class TestBeanConverter implements Converter<String, ITestBean> {

		@Override
		public ITestBean convert(String source) {
			return new TestBean(source);
		}
	}

	@Controller
	public static class MethodNotAllowedController {

		@RequestMapping(value = "/myPath.do", method = RequestMethod.DELETE)
		public void delete() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.HEAD)
		public void head() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.OPTIONS)
		public void options() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.POST)
		public void post() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.PUT)
		public void put() {
		}

		@RequestMapping(value = "/myPath.do", method = RequestMethod.TRACE)
		public void trace() {
		}

		@RequestMapping(value = "/otherPath.do", method = RequestMethod.GET)
		public void get() {
		}
	}

	@Controller
	public static class PathOrderingController {

		@RequestMapping(value = {"/dir/myPath1.do", "/**/*.do"})
		public void method1(Writer writer) throws IOException {
			writer.write("method1");
		}

		@RequestMapping("/dir/*.do")
		public void method2(Writer writer) throws IOException {
			writer.write("method2");
		}
	}

	@Controller
	public static class RequestResponseBodyController {

		@RequestMapping(value = "/something", method = RequestMethod.PUT)
		@ResponseBody
		public String handle(@RequestBody String body) throws IOException {
			return body;
		}
	}

	@Controller
	public static class ResponseBodyVoidController {

		@RequestMapping("/something")
		@ResponseBody
		public void handle() throws IOException {
		}
	}

	@Controller
	public static class RequestBodyArgMismatchController {

		@RequestMapping(value = "/something", method = RequestMethod.PUT)
		public void handle(@RequestBody A a) throws IOException {
		}
	}

	@XmlRootElement
	public static class A {

	}

	@XmlRootElement
	public static class B {

	}


	public static class NotReadableMessageConverter implements HttpMessageConverter {

		@Override
		public boolean canRead(Class clazz, MediaType mediaType) {
			return true;
		}

		@Override
		public boolean canWrite(Class clazz, MediaType mediaType) {
			return true;
		}

		@Override
		public List getSupportedMediaTypes() {
			return Collections.singletonList(new MediaType("application", "pdf"));
		}

		@Override
		public Object read(Class clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			throw new HttpMessageNotReadableException("Could not read");
		}

		@Override
		public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

	public static class SimpleMessageConverter implements HttpMessageConverter {

		private final List<MediaType> supportedMediaTypes;

		public SimpleMessageConverter(MediaType... supportedMediaTypes) {
			this.supportedMediaTypes = Arrays.asList(supportedMediaTypes);
		}

		@Override
		public boolean canRead(Class clazz, MediaType mediaType) {
			return supportedMediaTypes.contains(mediaType);
		}

		@Override
		public boolean canWrite(Class clazz, MediaType mediaType) {
			return supportedMediaTypes.contains(mediaType);
		}

		@Override
		public List getSupportedMediaTypes() {
			return supportedMediaTypes;
		}

		@Override
		public Object read(Class clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			return null;
		}

		@Override
		public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			outputMessage.getHeaders().setContentType(contentType);
			outputMessage.getBody(); // force a header write
		}
	}

	@Controller
	public static class ContentTypeHeadersController {

		@RequestMapping(value = "/something", headers = "content-type=application/pdf")
		public void handlePdf(Writer writer) throws IOException {
			writer.write("pdf");
		}

		@RequestMapping(value = "/something", headers = "content-type=text/*")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("text");
		}
	}

	@Controller
	public static class NegatedContentTypeHeadersController {

		@RequestMapping(value = "/something", headers = "content-type=application/pdf")
		public void handlePdf(Writer writer) throws IOException {
			writer.write("pdf");
		}

		@RequestMapping(value = "/something", headers = "content-type!=application/pdf")
		public void handleNonPdf(Writer writer) throws IOException {
			writer.write("non-pdf");
		}

	}

	@Controller
	public static class AcceptHeadersController {

		@RequestMapping(value = "/something", headers = "accept=text/html")
		public void handleHtml(Writer writer) throws IOException {
			writer.write("html");
		}

		@RequestMapping(value = "/something", headers = "accept=application/xml")
		public void handleXml(Writer writer) throws IOException {
			writer.write("xml");
		}
	}

	@Controller
	public static class ResponseStatusController {

		@RequestMapping("/something")
		@ResponseStatus(value = HttpStatus.CREATED, reason = "It's alive!")
		public void handle(Writer writer) throws IOException {
			writer.write("something");
		}
	}

	@Controller
	public static class ModelAndViewResolverController {

		@RequestMapping("/")
		public MySpecialArg handle() {
			return new MySpecialArg("foo");
		}
	}

	public static class MyModelAndViewResolver implements ModelAndViewResolver {

		@Override
		public ModelAndView resolveModelAndView(Method handlerMethod,
				Class handlerType,
				Object returnValue,
				ExtendedModelMap implicitModel,
				NativeWebRequest webRequest) {
			if (returnValue instanceof MySpecialArg) {
				return new ModelAndView(new View() {
					@Override
					public String getContentType() {
						return "text/html";
					}

					@Override
					public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
							throws Exception {
						response.getWriter().write("myValue");
					}

				});
			}
			return UNRESOLVED;
		}
	}

	@Controller
	@RequestMapping("/test*")
	private static class AmbiguousParamsController {

		@RequestMapping(method = RequestMethod.GET)
		public void noParams(Writer writer) throws IOException {
			writer.write("noParams");
		}

		@RequestMapping(params = "myParam")
		public void param(@RequestParam("myParam") int myParam, Writer writer) throws IOException {
			writer.write("myParam-" + myParam);
		}
	}

	@Controller
	@RequestMapping("/test*")
	public static class BindingCookieValueController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@RequestMapping(method = RequestMethod.GET)
		public void handle(@CookieValue("date") Date date, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", new GregorianCalendar(2008, 10, 18).getTime(), date);
			Calendar c = new GregorianCalendar();
			c.setTime(date);
			writer.write("test-" + c.get(Calendar.YEAR));
		}
	}

	public interface TestController<T> {

		ModelAndView method(T object);
	}

	public static class MyEntity {

	}

	@Controller
	public static class TestControllerImpl implements TestController<MyEntity> {

		@Override
		@RequestMapping("/method")
		public ModelAndView method(MyEntity object) {
			return new ModelAndView("/something");
		}
	}

	@Controller
	public static class RequestParamMapController {

		@RequestMapping("/map")
		public void map(@RequestParam Map<String, String> params, Writer writer) throws IOException {
			for (Iterator<Map.Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, String> entry = it.next();
				writer.write(entry.getKey() + "=" + entry.getValue());
				if (it.hasNext()) {
					writer.write(',');
				}

			}
		}

		@RequestMapping("/multiValueMap")
		public void multiValueMap(@RequestParam MultiValueMap<String, String> params, Writer writer)
				throws IOException {
			for (Iterator<Map.Entry<String, List<String>>> it1 = params.entrySet().iterator(); it1.hasNext();) {
				Map.Entry<String, List<String>> entry = it1.next();
				writer.write(entry.getKey() + "=[");
				for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext();) {
					String value = it2.next();
					writer.write(value);
					if (it2.hasNext()) {
						writer.write(',');
					}
				}
				writer.write(']');
				if (it1.hasNext()) {
					writer.write(',');
				}
			}
		}
	}

	@Controller
	public static class RequestHeaderMapController {

		@RequestMapping("/map")
		public void map(@RequestHeader Map<String, String> headers, Writer writer) throws IOException {
			for (Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, String> entry = it.next();
				writer.write(entry.getKey() + "=" + entry.getValue());
				if (it.hasNext()) {
					writer.write(',');
				}

			}
		}

		@RequestMapping("/multiValueMap")
		public void multiValueMap(@RequestHeader MultiValueMap<String, String> headers, Writer writer)
				throws IOException {
			for (Iterator<Map.Entry<String, List<String>>> it1 = headers.entrySet().iterator(); it1.hasNext();) {
				Map.Entry<String, List<String>> entry = it1.next();
				writer.write(entry.getKey() + "=[");
				for (Iterator<String> it2 = entry.getValue().iterator(); it2.hasNext();) {
					String value = it2.next();
					writer.write(value);
					if (it2.hasNext()) {
						writer.write(',');
					}
				}
				writer.write(']');
				if (it1.hasNext()) {
					writer.write(',');
				}
			}
		}

		@RequestMapping("/httpHeaders")
		public void httpHeaders(@RequestHeader HttpHeaders headers, Writer writer) throws IOException {
			assertEquals("Invalid Content-Type", new MediaType("text", "html"), headers.getContentType());
			multiValueMap(headers, writer);
		}

	}

	@Controller
	public interface IMyController {

		@RequestMapping("/handle")
		void handle(Writer writer, @RequestParam(value="p", required=false) String param) throws IOException;
	}

	@Controller
	public static class IMyControllerImpl implements IMyController {

		@Override
		public void handle(Writer writer, @RequestParam(value="p", required=false) String param) throws IOException {
			writer.write("handle " + param);
		}
	}

	public static abstract class MyAbstractController {

		@RequestMapping("/handle")
		public abstract void handle(Writer writer) throws IOException;
	}

	@Controller
	public static class MyAbstractControllerImpl extends MyAbstractController {

		@Override
		public void handle(Writer writer) throws IOException {
			writer.write("handle");
		}
	}

	@Controller
	public static class TrailingSlashController  {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public void root(Writer writer) throws IOException {
			writer.write("root");
		}

		@RequestMapping(value = "/{templatePath}/", method = RequestMethod.GET)
		public void templatePath(Writer writer) throws IOException {
			writer.write("templatePath");
		}
	}

	@Controller
	public static class ResponseEntityController {

		@RequestMapping("/foo")
		public ResponseEntity<String> foo(HttpEntity<byte[]> requestEntity) throws UnsupportedEncodingException {
			assertNotNull(requestEntity);
			assertEquals("MyValue", requestEntity.getHeaders().getFirst("MyRequestHeader"));
			String requestBody = new String(requestEntity.getBody(), "UTF-8");
			assertEquals("Hello World", requestBody);

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("MyResponseHeader", "MyValue");
			return new ResponseEntity<String>(requestBody, responseHeaders, HttpStatus.CREATED);
		}

		@RequestMapping("/bar")
		public ResponseEntity<String> bar() {
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("MyResponseHeader", "MyValue");
			return new ResponseEntity<String>(responseHeaders, HttpStatus.NOT_FOUND);
		}

	}

	@Controller
	public static class CustomMapEditorController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.initBeanPropertyAccess();
			binder.registerCustomEditor(Map.class, new CustomMapEditor());
		}

		@RequestMapping("/handle")
		public void handle(@RequestParam("map") Map map, Writer writer) throws IOException {
			writer.write("test-" + map);
		}
	}

	public static class CustomMapEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			if (StringUtils.hasText(text)) {
				setValue(Collections.singletonMap("foo", text));
			}
			else {
				setValue(null);
			}
		}

	}

	@Controller
	public static class MultipartController {

		@InitBinder
		public void initBinder(WebDataBinder binder) {
			binder.registerCustomEditor(String.class, new StringMultipartFileEditor());
		}

		@RequestMapping("/singleString")
		public void processMultipart(@RequestParam("content") String content, HttpServletResponse response) throws IOException {
			response.getWriter().write(content);
		}

		@RequestMapping("/stringArray")
		public void processMultipart(@RequestParam("content") String[] content, HttpServletResponse response) throws IOException {
			response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
		}
	}

	@Controller
	public static class CsvController {

		@RequestMapping("/singleInteger")
		public void processCsv(@RequestParam("content") Integer content, HttpServletResponse response) throws IOException {
			response.getWriter().write(content.toString());
		}

		@RequestMapping("/integerArray")
		public void processCsv(@RequestParam("content") Integer[] content, HttpServletResponse response) throws IOException {
			response.getWriter().write(StringUtils.arrayToDelimitedString(content, "-"));
		}

		@RequestMapping("/integerSet")
		public void processCsv(@RequestParam("content") Set<Integer> content, HttpServletResponse response) throws IOException {
			assertTrue(content.iterator().next() instanceof Integer);
			response.getWriter().write(StringUtils.collectionToDelimitedString(content, "-"));
		}
	}

	@Controller
	@RequestMapping("/t1")
	protected static class NoPathGetAndM2PostController {
		@RequestMapping(method = RequestMethod.GET)
		public void handle1(Writer writer) throws IOException {
			writer.write("handle1");
		}

		@RequestMapping(value = "/m2", method = RequestMethod.POST)
		public void handle2(Writer writer) throws IOException {
			writer.write("handle2");
		}
	}

	@Controller
	static class HeadersConditionController {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		public String home() {
			return "home";
		}

		@RequestMapping(value = "/", method = RequestMethod.GET, headers="Accept=application/json")
		@ResponseBody
		public String homeJson() {
			return "homeJson";
		}
	}

	@Controller
	static class RedirectAttributesController {

		@RequestMapping(value = "/")
		public void handle(RedirectAttributes redirectAttrs) {
		}
	}

}
