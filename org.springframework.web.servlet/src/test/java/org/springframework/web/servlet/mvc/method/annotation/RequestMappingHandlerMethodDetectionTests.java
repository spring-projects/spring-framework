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

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMethodMapping;

/**
 * Test various scenarios for detecting handler methods depending on where @RequestMapping annotations 
 * are located -- super types, parameterized methods, or in combination with proxies.
 *
 * Note the following:
 * <ul>
 * 	<li>Parameterized methods cannot be used in combination with JDK dynamic proxies since the 
 * 		proxy interface does not contain the bridged methods that need to be invoked.
 * 	<li>When using JDK dynamic proxies, the proxied interface must contain all required annotations.
 *	<li>Method-level annotations can be placed on parent classes or interfaces.
 * </ul>
 * 
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class RequestMappingHandlerMethodDetectionTests {

	@Parameters
	public static Collection<Object[]> handlerTypes() {
		return Arrays.asList(new Object[][] { 
				{ new MappingInterfaceController(), false}, 
				{ new MappingAbstractClassController(), false}, 
				{ new ParameterizedInterfaceController(), false }, 
				{ new MappingParameterizedInterfaceController(), false },
				{ new MappingClassController(), false }, 
				{ new MappingAbstractClassController(), true}, 
				{ new PlainController(), true}
		});
	}

	private Object handler;
	
	private boolean useAutoProxy;
	
	public RequestMappingHandlerMethodDetectionTests(Object handler, boolean useAutoProxy) {
		this.handler = handler;
		this.useAutoProxy = useAutoProxy;
	}

	@Test
	public void detectAndMapHandlerMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/type/handle");

		TestRequestMappingHandlerMethodMapping mapping = createHandlerMapping(handler.getClass(), useAutoProxy);
		HandlerMethod handlerMethod = mapping.getHandlerInternal(request);

		assertNotNull("Failed to detect and map @RequestMapping handler method", handlerMethod);
	}

	private TestRequestMappingHandlerMethodMapping createHandlerMapping(Class<?> controllerType, boolean useAutoProxy) {
		GenericWebApplicationContext wac = new GenericWebApplicationContext();
		wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerType));
		if (useAutoProxy) {
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(wac.getBeanFactory());
			wac.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			wac.getBeanFactory().registerSingleton("advsr", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		}

		TestRequestMappingHandlerMethodMapping mapping = new TestRequestMappingHandlerMethodMapping();
		mapping.setApplicationContext(wac);
		
		return mapping;
	}

	public static class TestRequestMappingHandlerMethodMapping extends RequestMappingHandlerMethodMapping {
		public HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
			return super.getHandlerInternal(request);
		}
	}

	/* Annotation on interface method */
	
	@Controller
	public interface MappingInterface {
		@RequestMapping(value="/handle", method = RequestMethod.GET)
		void handle();
	}
	@RequestMapping(value="/type")
	public static class MappingInterfaceController implements MappingInterface {
		public void handle() {
		}
	}
	
	/* Annotation on abstract class method */
	
	@Controller
	public static abstract class MappingAbstractClass {
		@RequestMapping(value = "/handle", method = RequestMethod.GET)
		public abstract void handle();
	}
	@RequestMapping(value="/type")
	public static class MappingAbstractClassController extends MappingAbstractClass {
		public void handle() {
		}
	}

	/* Annotation on parameterized controller method */

	@Controller
	public interface ParameterizedInterface<T> {
		void handle(T object);
	}
	@RequestMapping(value="/type")
	public static class ParameterizedInterfaceController implements ParameterizedInterface<TestBean> {
		@RequestMapping(value = "/handle", method = RequestMethod.GET)
		public void handle(TestBean object) {
		}
	}

	/* Annotation on parameterized interface method */

	@Controller
	public interface MappingParameterizedInterface<T> {
		@RequestMapping(value = "/handle", method = RequestMethod.GET)
		void handle(T object);
	}
	@RequestMapping(value="/type")
	public static class MappingParameterizedInterfaceController implements MappingParameterizedInterface<TestBean> {
		public void handle(TestBean object) {
		}
	}

	/* Type + method annotations, method in parent class only (SPR-8248) */

	@Controller
	public static class MappingClass {
		@RequestMapping(value = "/handle", method = RequestMethod.GET)
		public void handle(TestBean object) {
		}
	}
	@RequestMapping(value="/type")
	public static class MappingClassController extends MappingClass {
		// Method in parent class only
	}

	/* Annotations on controller class */
	
	@Controller
	@RequestMapping(value="/type")
	public static class PlainController {
		@RequestMapping(value = "/handle", method = RequestMethod.GET)
		public void handle() {
		}
	}

}
