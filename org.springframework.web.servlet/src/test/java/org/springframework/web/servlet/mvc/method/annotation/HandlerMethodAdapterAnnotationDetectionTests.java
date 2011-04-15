/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;

/**
 * Test various scenarios for detecting method-level and method parameter annotations depending 
 * on where they are located -- on interfaces, parent classes, in parameterized methods, or in 
 * combination with proxies.
 *
 * Note the following:
 * <ul>
 * 	<li>Parameterized methods cannot be used in combination with JDK dynamic proxies since the 
 * 		proxy interface does not contain the bridged methods that need to be invoked.
 * 	<li>When using JDK dynamic proxies, the proxied interface must contain all required method 
 * 		and method parameter annotations.
 *	<li>Method-level annotations can be placed on super types (interface or parent class) while 
 *		method parameter annotations must be present on the method being invoked.    
 * </ul>
 * 
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class HandlerMethodAdapterAnnotationDetectionTests {

	@Parameters
	public static Collection<Object[]> handlerTypes() {
		return Arrays.asList(new Object[][] { 
				{ new MappingIfcController(), false }, 
				{ new MappingAbstractClassController(), false },
				{ new ParameterizedIfcController(), false },
				{ new MappingParameterizedIfcController(), false },
				{ new MappingIfcProxyController(), true },
				{ new PlainController(), true },
				{ new MappingAbstractClassController(), true }
		});
	}

	private Object handler;
	
	private boolean useAutoProxy;

	public HandlerMethodAdapterAnnotationDetectionTests(Object handler, boolean useAutoProxy) {
		this.handler = handler;
		this.useAutoProxy = useAutoProxy;
	}
	
	@Test
	public void invokeModelAttributeMethod() throws Exception {
		ServletInvocableHandlerMethod requestMappingMethod = createRequestMappingMethod(handler, useAutoProxy);

		ModelAttribute annot = requestMappingMethod.getMethodAnnotation(ModelAttribute.class);
		assertEquals("Failed to detect method annotation", "attrName", annot.value());

		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		NativeWebRequest webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());
		servletRequest.setParameter("name", "Chad");
		
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		requestMappingMethod.invokeAndHandle(webRequest, mavContainer);

		Object modelAttr = mavContainer.getAttribute("attrName");

		assertEquals(TestBean.class, modelAttr.getClass());
		assertEquals("Chad", ((TestBean) modelAttr).getName());
	}

	private ServletInvocableHandlerMethod createRequestMappingMethod(Object handler, boolean useAutoProxy) {
		if (useAutoProxy) {
			handler = getProxyBean(handler);
		}
		HandlerMethodArgumentResolverComposite argResolvers = new HandlerMethodArgumentResolverComposite();
		argResolvers.addResolver(new ModelAttributeMethodProcessor(false));
		
		HandlerMethodReturnValueHandlerComposite handlers = new HandlerMethodReturnValueHandlerComposite();
		handlers.addHandler(new ModelAttributeMethodProcessor(false));
		handlers.addHandler(new DefaultMethodReturnValueHandler(null));

		Class<?> handlerType = ClassUtils.getUserClass(handler.getClass());
		Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, REQUEST_MAPPING_METHODS);
		Method method = methods.iterator().next();

		ServletInvocableHandlerMethod attrMethod = new ServletInvocableHandlerMethod(handler, method);
		attrMethod.setHandlerMethodArgumentResolvers(argResolvers);
		attrMethod.setHandlerMethodReturnValueHandlers(handlers);
		attrMethod.setDataBinderFactory(new DefaultDataBinderFactory(null));
		
		return attrMethod;
	}

	private Object getProxyBean(Object handler) {
		GenericWebApplicationContext wac = new GenericWebApplicationContext();
		wac.registerBeanDefinition("controller", new RootBeanDefinition(handler.getClass()));
		
		DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		autoProxyCreator.setBeanFactory(wac.getBeanFactory());
		wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
		wac.getBeanFactory().registerSingleton("advsr", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		
		wac.refresh();
		
		return wac.getBean("controller");
	}

	public static MethodFilter REQUEST_MAPPING_METHODS = new MethodFilter() {

		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, RequestMapping.class) != null;
		}
	};
	
	private interface MappingIfc {
		@RequestMapping
		@ModelAttribute("attrName")
		TestBean model(TestBean input);
	}

	private static class MappingIfcController implements MappingIfc {
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}
	
	private interface MappingProxyIfc {
		@RequestMapping
		@ModelAttribute("attrName")
		TestBean model(@ModelAttribute TestBean input);
	}
	
	private static class MappingIfcProxyController implements MappingProxyIfc {
		@ModelAttribute("attrName")
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}	

	public static abstract class MappingAbstractClass {
		@RequestMapping
		@ModelAttribute("attrName")
		TestBean model(TestBean input) {
			return new TestBean(input.getName());
		}
	}

	public static class MappingAbstractClassController extends MappingAbstractClass {
		public TestBean model(@ModelAttribute TestBean input) {
			TestBean testBean = super.model(input);
			testBean.setAge(14);
			return testBean;
		}
	}

	public interface ParameterizedIfc<TB, S> {
		TB model(S input);
	}

	public static class ParameterizedIfcController implements ParameterizedIfc<TestBean, TestBean> {
		@RequestMapping
		@ModelAttribute("attrName")
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}

	public interface MappingParameterizedIfc<TB, S> {
		@RequestMapping
		@ModelAttribute("attrName")
		TB model(S input);
	}

	public static class MappingParameterizedIfcController implements MappingParameterizedIfc<TestBean, TestBean> {
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}

	public interface MappingParameterizedProxyIfc<TB, S> {
		@RequestMapping
		@ModelAttribute("attrName")
		TB model(@ModelAttribute("inputName") S input);
	}

	public static class MappingParameterizedProxyIfcController implements MappingParameterizedProxyIfc<TestBean, TestBean> {
		@RequestMapping
		@ModelAttribute("attrName")
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}

	public static class PlainController {
		public PlainController() {
		}

		@RequestMapping
		@ModelAttribute("attrName")
		public TestBean model(@ModelAttribute TestBean input) {
			return new TestBean(input.getName());
		}
	}
}
