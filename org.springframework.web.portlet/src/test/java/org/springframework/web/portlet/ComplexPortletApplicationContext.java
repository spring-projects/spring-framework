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

package org.springframework.web.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.portlet.bind.PortletRequestBindingException;
import org.springframework.web.portlet.context.PortletRequestHandledEvent;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;
import org.springframework.web.portlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.portlet.handler.ParameterHandlerMapping;
import org.springframework.web.portlet.handler.ParameterMappingInterceptor;
import org.springframework.web.portlet.handler.PortletModeHandlerMapping;
import org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping;
import org.springframework.web.portlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.portlet.handler.SimplePortletHandlerAdapter;
import org.springframework.web.portlet.handler.SimplePortletPostProcessor;
import org.springframework.web.portlet.handler.UserRoleAuthorizationInterceptor;
import org.springframework.web.portlet.multipart.DefaultMultipartActionRequest;
import org.springframework.web.portlet.multipart.MultipartActionRequest;
import org.springframework.web.portlet.multipart.PortletMultipartResolver;
import org.springframework.web.portlet.mvc.Controller;
import org.springframework.web.portlet.mvc.SimpleControllerHandlerAdapter;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Arjen Poutsma
 */
public class ComplexPortletApplicationContext extends StaticPortletApplicationContext {

	public void refresh() throws BeansException {
		registerSingleton("standardHandlerAdapter", SimpleControllerHandlerAdapter.class);
		registerSingleton("portletHandlerAdapter", SimplePortletHandlerAdapter.class);
		registerSingleton("myHandlerAdapter", MyHandlerAdapter.class);
		
		registerSingleton("viewController", ViewController.class);
		registerSingleton("editController", EditController.class);
		registerSingleton("helpController1", HelpController1.class);
		registerSingleton("helpController2", HelpController2.class);
		registerSingleton("testController1", TestController1.class);
		registerSingleton("testController2", TestController2.class);
		registerSingleton("requestLocaleCheckingController", RequestLocaleCheckingController.class);
		registerSingleton("localeContextCheckingController", LocaleContextCheckingController.class);
		
		registerSingleton("exceptionThrowingHandler1", ExceptionThrowingHandler.class);
		registerSingleton("exceptionThrowingHandler2", ExceptionThrowingHandler.class);
		registerSingleton("unknownHandler", Object.class);
		
		registerSingleton("myPortlet", MyPortlet.class);
		registerSingleton("portletMultipartResolver", MockMultipartResolver.class);
		registerSingleton("portletPostProcessor", SimplePortletPostProcessor.class);
		registerSingleton("testListener", TestApplicationListener.class);
		
		ConstructorArgumentValues cvs = new ConstructorArgumentValues();
		cvs.addIndexedArgumentValue(0, new MockPortletContext());
		cvs.addIndexedArgumentValue(1, "complex");
		registerBeanDefinition("portletConfig", new RootBeanDefinition(MockPortletConfig.class, cvs, null));
		
		UserRoleAuthorizationInterceptor userRoleInterceptor = new UserRoleAuthorizationInterceptor();
		userRoleInterceptor.setAuthorizedRoles(new String[] {"role1", "role2"});

		ParameterHandlerMapping interceptingHandlerMapping = new ParameterHandlerMapping();
		interceptingHandlerMapping.setParameterName("interceptingParam");
		ParameterMappingInterceptor parameterMappingInterceptor = new ParameterMappingInterceptor();
		parameterMappingInterceptor.setParameterName("interceptingParam");

		List interceptors = new ArrayList();
		interceptors.add(parameterMappingInterceptor);
		interceptors.add(userRoleInterceptor);
		interceptors.add(new MyHandlerInterceptor1());
		interceptors.add(new MyHandlerInterceptor2());

		MutablePropertyValues pvs = new MutablePropertyValues();
		Map portletModeMap = new ManagedMap();
		portletModeMap.put("view", new RuntimeBeanReference("viewController"));
		portletModeMap.put("edit", new RuntimeBeanReference("editController"));
		pvs.add("portletModeMap", portletModeMap);
		pvs.add("interceptors", interceptors);
		registerSingleton("handlerMapping3", PortletModeHandlerMapping.class, pvs);
		
		pvs = new MutablePropertyValues();
		Map parameterMap = new ManagedMap();
		parameterMap.put("test1", new RuntimeBeanReference("testController1"));
		parameterMap.put("test2", new RuntimeBeanReference("testController2"));
		parameterMap.put("requestLocaleChecker", new RuntimeBeanReference("requestLocaleCheckingController"));
		parameterMap.put("contextLocaleChecker", new RuntimeBeanReference("localeContextCheckingController"));
		parameterMap.put("exception1", new RuntimeBeanReference("exceptionThrowingHandler1"));
		parameterMap.put("exception2", new RuntimeBeanReference("exceptionThrowingHandler2"));
		parameterMap.put("myPortlet", new RuntimeBeanReference("myPortlet"));
		parameterMap.put("unknown", new RuntimeBeanReference("unknownHandler"));
		pvs.add("parameterMap", parameterMap);
		pvs.add("parameterName", "myParam");
		pvs.add("order", "2");
		registerSingleton("handlerMapping2", ParameterHandlerMapping.class, pvs);
		
		pvs = new MutablePropertyValues();
		Map innerMap = new ManagedMap();
		innerMap.put("help1", new RuntimeBeanReference("helpController1"));
		innerMap.put("help2", new RuntimeBeanReference("helpController2"));
		Map outerMap = new ManagedMap();
		outerMap.put("help", innerMap);
		pvs.add("portletModeParameterMap", outerMap);
		pvs.add("order", "1");
		registerSingleton("handlerMapping1", PortletModeParameterHandlerMapping.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("order", "1");
		pvs.add("exceptionMappings",
		    "java.lang.IllegalAccessException=failed-illegalaccess\n" +
		    "PortletRequestBindingException=failed-binding\n" +
		    "NoHandlerFoundException=failed-unavailable");
		pvs.add("defaultErrorView", "failed-default-1");
		registerSingleton("exceptionResolver", SimpleMappingExceptionResolver.class, pvs);

		pvs = new MutablePropertyValues();
		pvs.add("order", "0");
		pvs.add("exceptionMappings",
				"java.lang.Exception=failed-exception\n" +
				"java.lang.RuntimeException=failed-runtime");
		List mappedHandlers = new ManagedList();
		mappedHandlers.add(new RuntimeBeanReference("exceptionThrowingHandler1"));
		pvs.add("mappedHandlers", mappedHandlers);
		pvs.add("defaultErrorView", "failed-default-0");
		registerSingleton("handlerExceptionResolver", SimpleMappingExceptionResolver.class, pvs);

		addMessage("test", Locale.ENGLISH, "test message");
		addMessage("test", Locale.CANADA, "Canadian & test message");
		addMessage("test.args", Locale.ENGLISH, "test {0} and {1}");
		
		super.refresh();
	}


	public static class TestController1 implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("result", "test1-action");
		}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			return null;
		}
	}


	public static class TestController2 implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			response.setProperty("result", "test2-view");
			return null;
		}
	}


	public static class ViewController implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			return new ModelAndView("someViewName", "result", "view was here");
		}
	}
	

	public static class EditController implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("param", "edit was here");
		}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			return new ModelAndView(request.getParameter("param"));
		}
	}
	

	public static class HelpController1 implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("param", "help1 was here");
		}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			return new ModelAndView("help1-view");
		}
	}


	public static class HelpController2 implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("param", "help2 was here");
		}

		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
			return new ModelAndView("help2-view");
		}
	}
	
	public static class RequestLocaleCheckingController implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) throws PortletException {
			if (!Locale.CANADA.equals(request.getLocale())) {
				throw new PortletException("Incorrect Locale in ActionRequest");
			}
		}
		
		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) 
			throws PortletException, IOException {
			if (!Locale.CANADA.equals(request.getLocale())) {
				throw new PortletException("Incorrect Locale in RenderRequest");
			}
			response.getWriter().write("locale-ok");
			return null;
		}
	}


	public static class LocaleContextCheckingController implements Controller {

		public void handleActionRequest(ActionRequest request, ActionResponse response) throws PortletException {
			if (!Locale.CANADA.equals(LocaleContextHolder.getLocale())) {
				throw new PortletException("Incorrect Locale in LocaleContextHolder");
			}
		}
		
		public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) 
			throws PortletException, IOException {
			if (!Locale.CANADA.equals(LocaleContextHolder.getLocale())) {
				throw new PortletException("Incorrect Locale in LocaleContextHolder");
			}
			response.getWriter().write("locale-ok");
			return null;
		}
	}


	public static class MyPortlet implements Portlet {

		private PortletConfig portletConfig;

		public void init(PortletConfig portletConfig) throws PortletException {
			this.portletConfig = portletConfig;
		}

		public void processAction(ActionRequest request, ActionResponse response) throws PortletException {
			response.setRenderParameter("result", "myPortlet action called");
		}

		public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
			response.getWriter().write("myPortlet was here");
		}
		
		public PortletConfig getPortletConfig() {
			return this.portletConfig;
		}

		public void destroy() {
			this.portletConfig = null;
		}
	}


	public static interface MyHandler {

		public void doSomething(PortletRequest request) throws Exception;
	}
	

	public static class ExceptionThrowingHandler implements MyHandler {

		public void doSomething(PortletRequest request) throws Exception {
			if (request.getParameter("fail") != null) {
				throw new ModelAndViewDefiningException(new ModelAndView("failed-modelandview"));
			}
			if (request.getParameter("access") != null) {
				throw new IllegalAccessException("portlet-illegalaccess");
			}
			if (request.getParameter("binding") != null) {
				throw new PortletRequestBindingException("portlet-binding");
			}
			if (request.getParameter("generic") != null) {
				throw new Exception("portlet-generic");
			}
			if (request.getParameter("runtime") != null) {
				throw new RuntimeException("portlet-runtime");
			}
			throw new IllegalArgumentException("illegal argument");
		}
	}


	public static class MyHandlerAdapter implements HandlerAdapter, Ordered {

		public int getOrder() {
			return 99;
		}

		public boolean supports(Object handler) {
			return handler != null && MyHandler.class.isAssignableFrom(handler.getClass());
		}

		public void handleAction(ActionRequest request, ActionResponse response, Object delegate) throws Exception {
			((MyHandler) delegate).doSomething(request);
		}

		public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object delegate) throws Exception {
			((MyHandler) delegate).doSomething(request);
			return null;
		}

		public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler)
				throws Exception {
			return null;
		}

		public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		}
	}


	public static class MyHandlerInterceptor1 extends HandlerInterceptorAdapter {

		public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
		    throws PortletException {
			if (request.getAttribute("test2-remove-never") != null) {
				throw new PortletException("Wrong interceptor order");
			}
			request.setAttribute("test1-remove-never", "test1-remove-never");
			request.setAttribute("test1-remove-post", "test1-remove-post");
			request.setAttribute("test1-remove-after", "test1-remove-after");
			return true;
		}

		public void postHandleRender(
				RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
				throws PortletException {
			if (request.getAttribute("test2-remove-post") != null) {
				throw new PortletException("Wrong interceptor order");
			}
			if (!"test1-remove-post".equals(request.getAttribute("test1-remove-post"))) {
				throw new PortletException("Incorrect request attribute");
			}
			request.removeAttribute("test1-remove-post");
		}

		public void afterRenderCompletion(
				RenderRequest request, RenderResponse response, Object handler, Exception ex)
				throws PortletException {
			if (request.getAttribute("test2-remove-after") != null) {
				throw new PortletException("Wrong interceptor order");
			}
			request.removeAttribute("test1-remove-after");
		}
	}


	public static class MyHandlerInterceptor2 extends HandlerInterceptorAdapter {

		public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
		    throws PortletException {
			if (request.getAttribute("test1-remove-post") == null) {
				throw new PortletException("Wrong interceptor order");
			}
			if ("true".equals(request.getParameter("abort"))) {
				return false;
			}
			request.setAttribute("test2-remove-never", "test2-remove-never");
			request.setAttribute("test2-remove-post", "test2-remove-post");
			request.setAttribute("test2-remove-after", "test2-remove-after");
			return true;
		}

		public void postHandleRender(
				RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
				throws PortletException {
			if ("true".equals(request.getParameter("noView"))) {
				modelAndView.clear();
			}
			if (request.getAttribute("test1-remove-post") == null) {
				throw new PortletException("Wrong interceptor order");
			}
			if (!"test2-remove-post".equals(request.getAttribute("test2-remove-post"))) {
				throw new PortletException("Incorrect request attribute");
			}
			request.removeAttribute("test2-remove-post");
		}

		public void afterRenderCompletion(
				RenderRequest request, RenderResponse response, Object handler, Exception ex)
				throws Exception {
			if (request.getAttribute("test1-remove-after") == null) {
				throw new PortletException("Wrong interceptor order");
			}
			request.removeAttribute("test2-remove-after");
		}
	}


	public static class MultipartCheckingHandler implements MyHandler {

		public void doSomething(PortletRequest request) throws PortletException, IllegalAccessException {
			if (!(request instanceof MultipartActionRequest)) {
				throw new PortletException("Not in a MultipartActionRequest");
			}
		}
	}


	public static class MockMultipartResolver implements PortletMultipartResolver {

		public boolean isMultipart(ActionRequest request) {
			return true;
		}

		public MultipartActionRequest resolveMultipart(ActionRequest request) throws MultipartException {
			if (request.getAttribute("fail") != null) {
				throw new MaxUploadSizeExceededException(1000);
			}
			if (request instanceof MultipartActionRequest) {
				throw new IllegalStateException("Already a multipart request");
			}
			if (request.getAttribute("resolved") != null) {
				throw new IllegalStateException("Already resolved");
			}
			request.setAttribute("resolved", Boolean.TRUE);
			MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<String, MultipartFile>();
			files.set("someFile", new MockMultipartFile("someFile", "someContent".getBytes()));
			Map<String, String[]> params = new HashMap<String, String[]>();
			params.put("someParam", new String[] {"someParam"});
			return new DefaultMultipartActionRequest(request, files, params, Collections.<String, String>emptyMap());
		}

		public void cleanupMultipart(MultipartActionRequest request) {
			if (request.getAttribute("cleanedUp") != null) {
				throw new IllegalStateException("Already cleaned up");
			}
			request.setAttribute("cleanedUp", Boolean.TRUE);
		}
	}


	public static class TestApplicationListener implements ApplicationListener {

		public int counter = 0;

		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof PortletRequestHandledEvent) {
				this.counter++;
			}
		}
	}

}
