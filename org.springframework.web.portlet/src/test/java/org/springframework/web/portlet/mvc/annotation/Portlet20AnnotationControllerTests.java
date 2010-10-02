/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceResponse;
import javax.portlet.StateAwareResponse;
import javax.portlet.WindowState;
import javax.servlet.http.Cookie;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.DerivedTestBean;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockEvent;
import org.springframework.mock.web.portlet.MockEventRequest;
import org.springframework.mock.web.portlet.MockEventResponse;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.mock.web.portlet.MockResourceRequest;
import org.springframework.mock.web.portlet.MockResourceResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.portlet.DispatcherPortlet;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.NoHandlerFoundException;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;
import org.springframework.web.portlet.mvc.AbstractController;

/**
 * @author Juergen Hoeller
 * @since 3.0
 */
public class Portlet20AnnotationControllerTests {

	@Test
	public void standardHandleMethod() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyController.class));
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test", response.getContentAsString());
	}

	@Test
	public void standardHandleMethodWithResources() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.setPortletConfig(getPortletConfig());
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyController.class));
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockResourceRequest resourceRequest = new MockResourceRequest("/resource1");
		MockResourceResponse resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertEquals("/resource1", resourceResponse.getForwardedUrl());
		assertNull(resourceResponse.getProperty(ResourceResponse.HTTP_STATUS_CODE));

		resourceRequest = new MockResourceRequest("/WEB-INF/resource2");
		resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertNull(resourceResponse.getForwardedUrl());
		assertEquals("404", resourceResponse.getProperty(ResourceResponse.HTTP_STATUS_CODE));

		resourceRequest = new MockResourceRequest("/META-INF/resource3");
		resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertNull(resourceResponse.getForwardedUrl());
		assertEquals("404", resourceResponse.getProperty(ResourceResponse.HTTP_STATUS_CODE));
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

	@Test
	public void adaptedHandleMethods4() throws Exception {
		doTestAdaptedHandleMethods(MyAdaptedController4.class);
	}

	private void doTestAdaptedHandleMethods(final Class controllerClass) throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerClass));
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockActionRequest actionRequest = new MockActionRequest(PortletMode.VIEW);
		MockActionResponse actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);
		assertEquals("value", actionResponse.getRenderParameter("test"));

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.setSession(actionRequest.getPortletSession());
		request.setParameters(actionResponse.getRenderParameterMap());
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-name1-typeMismatch", response.getContentAsString());
		assertNull(request.getPortletSession().getAttribute("testBean"));

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		request.addProperty("header1", "10");
		request.setCookies(new Cookie("cookie1", "3"));
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-value1-2-10-3", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.HELP);
		request.addParameter("name", "name1");
		request.addParameter("age", "2");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-name1-2", response.getContentAsString());
	}

	@Test
	public void formController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyFormController.class));
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-name1-typeMismatch-tb1-myValue", response.getContentAsString());
	}

	@Test
	public void modelFormController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyModelFormController.class));
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-name1-typeMismatch-tb1-myValue", response.getContentAsString());
	}

	@Test
	public void commandProvidingFormController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyCommandProvidingFormController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	@Test
	public void typedCommandProvidingFormController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyTypedCommandProvidingFormController.class));
				wac.registerBeanDefinition("controller2", new RootBeanDefinition(MyOtherTypedCommandProvidingFormController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
				adapterDef.getPropertyValues().add("customArgumentResolver", new MySpecialArgumentResolver());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("myParam", "myValue");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-Integer:10-typeMismatch-tb1-myOriginalValue", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("myParam", "myOtherValue");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myOtherView-Integer:10-typeMismatch-tb1-myOriginalValue", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myValue");
		request.addParameter("defaultName", "10");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-myName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	@Test
	public void binderInitializingCommandProvidingFormController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyBinderInitializingCommandProvidingFormController.class));
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	@Test
	public void specificBinderInitializingCommandProvidingFormController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MySpecificBinderInitializingCommandProvidingFormController.class));
				wac.refresh();
				return wac;
			}
			protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
				new TestView().render(mv.getViewName(), mv.getModel(), request, response);
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("defaultName", "myDefaultName");
		request.addParameter("age", "value2");
		request.addParameter("date", "2007-10-02");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView-String:myDefaultName-typeMismatch-tb1-myOriginalValue", response.getContentAsString());
	}

	@Test
	public void parameterDispatchingController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.setPortletContext(new MockPortletContext());
				RootBeanDefinition bd = new RootBeanDefinition(MyParameterDispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("view", "other");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("surprise", "!");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	@Test
	public void typeLevelParameterDispatchingController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.setPortletContext(new MockPortletContext());
				RootBeanDefinition bd = new RootBeanDefinition(MyTypeLevelParameterDispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				RootBeanDefinition bd2 = new RootBeanDefinition(MySpecialParameterDispatchingController.class);
				bd2.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller2", bd2);
				RootBeanDefinition bd3 = new RootBeanDefinition(MyOtherSpecialParameterDispatchingController.class);
				bd3.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller3", bd3);
				RootBeanDefinition bd4 = new RootBeanDefinition(MyParameterDispatchingController.class);
				bd4.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller4", bd4);
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.HELP);
		MockRenderResponse response = new MockRenderResponse();
		try {
			portlet.render(request, response);
			fail("Should have thrown NoHandlerFoundException");
		}
		catch (NoHandlerFoundException ex) {
			// expected
		}

		request = new MockRenderRequest(PortletMode.EDIT);
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myDefaultView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myValue");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "mySpecialValue");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("mySpecialView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myOtherSpecialValue");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myOtherSpecialView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myValue");
		request.addParameter("view", "other");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myOtherView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myValue");
		request.addParameter("view", "my");
		request.addParameter("lang", "de");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLangView", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("myParam", "myValue");
		request.addParameter("surprise", "!");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("mySurpriseView", response.getContentAsString());
	}

	@Test
	public void portlet20DispatchingController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.setPortletContext(new MockPortletContext());
				RootBeanDefinition bd = new RootBeanDefinition(MyPortlet20DispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				wac.registerBeanDefinition("controller2", new RootBeanDefinition(DetailsController.class));
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		MockActionRequest actionRequest = new MockActionRequest("this");
		MockActionResponse actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(actionResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value", response.getContentAsString());

		actionRequest = new MockActionRequest("that");
		actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(actionResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value2", response.getContentAsString());

		actionRequest = new MockActionRequest("error");
		actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(actionResponse.getRenderParameterMap());
		request.setSession(actionRequest.getPortletSession());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("XXX", response.getContentAsString());

		MockEventRequest eventRequest = new MockEventRequest(new MockEvent("event1"));
		MockEventResponse eventResponse = new MockEventResponse();
		portlet.processEvent(eventRequest, eventResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(eventResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value3", response.getContentAsString());

		eventRequest = new MockEventRequest(new MockEvent("event2"));
		eventResponse = new MockEventResponse();
		portlet.processEvent(eventRequest, eventResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(eventResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value4", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.NORMAL);
		request.setParameters(actionResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		MockResourceRequest resourceRequest = new MockResourceRequest("resource1");
		MockResourceResponse resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertEquals("myResource", resourceResponse.getContentAsString());

		resourceRequest = new MockResourceRequest("resource2");
		resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertEquals("myDefaultResource", resourceResponse.getContentAsString());
	}

	@Test
	public void eventDispatchingController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.setPortletContext(new MockPortletContext());
				RootBeanDefinition bd = new RootBeanDefinition(MyPortlet20DispatchingController.class);
				bd.setScope(WebApplicationContext.SCOPE_REQUEST);
				wac.registerBeanDefinition("controller", bd);
				wac.registerBeanDefinition("controller2", new RootBeanDefinition(DetailsController.class));
				AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest();
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		MockActionRequest actionRequest = new MockActionRequest("this");
		MockActionResponse actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(actionResponse.getRenderParameterMap());
		request.setSession(actionRequest.getPortletSession());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value", response.getContentAsString());

		actionRequest = new MockActionRequest();
		actionRequest.addParameter("action", "details");
		actionResponse = new MockActionResponse();
		portlet.processAction(actionRequest, actionResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(actionResponse.getRenderParameterMap());
		request.setSession(actionRequest.getPortletSession());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-details", response.getContentAsString());

		MockEventRequest eventRequest = new MockEventRequest(new MockEvent("event1"));
		eventRequest.setParameters(actionRequest.getParameterMap());
		MockEventResponse eventResponse = new MockEventResponse();
		portlet.processEvent(eventRequest, eventResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(eventResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value3", response.getContentAsString());

		eventRequest = new MockEventRequest(new MockEvent("event3"));
		eventRequest.setParameters(actionRequest.getParameterMap());
		eventResponse = new MockEventResponse();
		portlet.processEvent(eventRequest, eventResponse);

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.MAXIMIZED);
		request.setParameters(eventResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myLargeView-value4", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW, WindowState.NORMAL);
		request.setParameters(actionResponse.getRenderParameterMap());
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("myView", response.getContentAsString());

		MockResourceRequest resourceRequest = new MockResourceRequest("resource1");
		MockResourceResponse resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertEquals("myResource", resourceResponse.getContentAsString());

		resourceRequest = new MockResourceRequest("resource2");
		resourceResponse = new MockResourceResponse();
		portlet.serveResource(resourceRequest, resourceResponse);
		assertEquals("myDefaultResource", resourceResponse.getContentAsString());
	}


	/*
	 * Controllers
	 */

	@RequestMapping("VIEW")
	private static class MyController extends AbstractController {

		protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response) throws Exception {
			response.getWriter().write("test");
			return null;
		}
	}


	@Controller
	private static class MyAdaptedController {

		@RequestMapping("VIEW")
		@ActionMapping
		public void myHandle(ActionRequest request, ActionResponse response) throws IOException {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		@RenderMapping
		public void myHandle(@RequestParam("param1") String p1, @RequestParam("param2") int p2,
				@RequestHeader("header1") long h1, @CookieValue("cookie1") Cookie c1,
				RenderResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + p2 + "-" + h1 + "-" + c1.getValue());
		}

		@RequestMapping("HELP")
		@RenderMapping
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	private static class MyAdaptedController2 {

		@RequestMapping("VIEW")
		@ActionMapping
		public void myHandle(ActionRequest request, ActionResponse response) throws IOException {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		@RenderMapping
		public void myHandle(@RequestParam("param1") String p1, int param2, RenderResponse response,
				@RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
		}

		@RequestMapping("HELP")
		@RenderMapping
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	@RequestMapping({"VIEW", "EDIT", "HELP"})
	private static class MyAdaptedController3 {

		@ActionMapping
		public void myHandle(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		@RenderMapping
		public void myHandle(@RequestParam("param1") String p1, int param2, @RequestHeader Integer header1,
				@CookieValue int cookie1, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + header1 + "-" + cookie1);
		}

		@RequestMapping("HELP")
		@RenderMapping
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RenderMapping
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	@SessionAttributes("testBean")
	private static class MyAdaptedController4 {

		@RequestMapping("VIEW")
		@ActionMapping
		public void myHandle(Model model, ActionResponse response, SessionStatus status) {
			TestBean tb = new TestBean();
			tb.setJedi(true);
			model.addAttribute("testBean", tb);
			status.setComplete();
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		@RenderMapping
		public void myHandle(@RequestParam("param1") String p1, int param2, RenderResponse response,
				@RequestHeader("header1") String h1, @CookieValue("cookie1") String c1) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2 + "-" + h1 + "-" + c1);
		}

		@RequestMapping("HELP")
		@RenderMapping
		public void myHandle(@ModelAttribute("tb") TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public void myHandle(@ModelAttribute("testBean") TestBean tb, Errors errors, RenderResponse response, PortletSession session) throws IOException {
			assertTrue(tb.isJedi());
			assertNull(session.getAttribute("testBean"));
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	private static class MyFormController {

		@ModelAttribute("testBeanList")
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public String myHandle(@ModelAttribute("myCommand")TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}


	@Controller
	private static class MyModelFormController {

		@ModelAttribute
		public List<TestBean> getTestBeans() {
			List<TestBean> list = new LinkedList<TestBean>();
			list.add(new TestBean("tb1"));
			list.add(new TestBean("tb2"));
			return list;
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public String myHandle(@ModelAttribute("myCommand")TestBean tb, BindingResult errors, Model model) {
			if (!model.containsAttribute("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}
	}


	@Controller
	private static class MyCommandProvidingFormController<T, TB, TB2> extends MyFormController {

		@ModelAttribute("myCommand")
		private TestBean createTestBean(
				@RequestParam T defaultName, Map<String, Object> model, @RequestParam Date date) {
			model.put("myKey", "myOriginalValue");
			return new TestBean(defaultName.getClass().getSimpleName() + ":" + defaultName.toString());
		}

		@RequestMapping("VIEW")
		@RenderMapping
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}

		@RequestMapping("EDIT")
		@RenderMapping
		public String myOtherHandle(TB tb, BindingResult errors, ExtendedModelMap model, MySpecialArg arg) {
			TestBean tbReal = (TestBean) tb;
			tbReal.setName("myName");
			assertTrue(model.get("ITestBean") instanceof DerivedTestBean);
			assertNotNull(arg);
			return super.myHandle(tbReal, errors, model);
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
	@RequestMapping(params = "myParam=myValue")
	private static class MyTypedCommandProvidingFormController
			extends MyCommandProvidingFormController<Integer, TestBean, ITestBean> {

	}


	@Controller
	@RequestMapping(params = "myParam=myOtherValue")
	private static class MyOtherTypedCommandProvidingFormController
			extends MyCommandProvidingFormController<Integer, TestBean, ITestBean> {

		@RequestMapping("VIEW")
		@RenderMapping
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myOtherView";
		}
	}


	@Controller
	private static class MyBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@InitBinder
		private void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	@Controller
	private static class MySpecificBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@SuppressWarnings("unused")
		@InitBinder({"myCommand", "date"})
		private void initBinder(WebDataBinder binder) {
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
	@RequestMapping("VIEW")
	private static class MyParameterDispatchingController {

		@Autowired
		private PortletContext portletContext;

		@Autowired
		private PortletSession session;

		@Autowired
		private PortletRequest request;

		@Autowired
		private WebRequest webRequest;

		@RenderMapping
		public void myHandle(RenderResponse response) throws IOException {
			if (this.portletContext == null || this.session == null || this.request == null || this.webRequest == null) {
				throw new IllegalStateException();
			}
			response.getWriter().write("myView");
		}

		@RenderMapping(params = {"view", "!lang"})
		public void myOtherHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RenderMapping(params = {"view=my", "lang=de"})
		public void myLangHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RenderMapping(params = "surprise")
		public void mySurpriseHandle(RenderResponse response) throws IOException {
			response.getWriter().write("mySurpriseView");
		}
	}


	@Controller
	@RequestMapping(value = "EDIT", params = "myParam=myValue")
	private static class MyTypeLevelParameterDispatchingController extends MyParameterDispatchingController {

	}


	@Controller
	@RequestMapping("EDIT")
	private static class MySpecialParameterDispatchingController {

		@RenderMapping(params = "myParam=mySpecialValue")
		public void myHandle(RenderResponse response) throws IOException {
			response.getWriter().write("mySpecialView");
		}

		@RenderMapping
		public void myDefaultHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myDefaultView");
		}
	}


	@Controller
	@RequestMapping("EDIT")
	private static class MyOtherSpecialParameterDispatchingController {

		@RenderMapping(params = "myParam=myOtherSpecialValue")
		public void myHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myOtherSpecialView");
		}
	}


	@Controller
	@RequestMapping("VIEW")
	private static class MyPortlet20DispatchingController {

		@ActionMapping("this")
		public void myHandle(StateAwareResponse response) {
			response.setRenderParameter("test", "value");
		}

		@ActionMapping("that")
		public void myHandle2(StateAwareResponse response) {
			response.setRenderParameter("test", "value2");
		}

		@ActionMapping("error")
		public void myError(StateAwareResponse response) {
			throw new IllegalStateException("XXX");
		}

		@EventMapping("event1")
		public void myHandle(EventResponse response) throws IOException {
			response.setRenderParameter("test", "value3");
		}

		@EventMapping("event2")
		public void myHandle2(EventResponse response) throws IOException {
			response.setRenderParameter("test", "value4");
		}

		@RenderMapping("MAXIMIZED")
		public void myHandle(Writer writer, @RequestParam("test") String renderParam) throws IOException {
			writer.write("myLargeView-" + renderParam);
		}

		@RenderMapping
		public void myDefaultHandle(Writer writer) throws IOException {
			writer.write("myView");
		}

		@ExceptionHandler
		public void handleException(Exception ex, Writer writer) throws IOException {
			writer.write(ex.getMessage());
		}

		@ResourceMapping("resource1")
		public void myResource(Writer writer) throws IOException {
			writer.write("myResource");
		}
	}


	@Controller
	@RequestMapping("VIEW")
	private static class DetailsController {

		@EventMapping("event3")
		public void myHandle2(EventResponse response) throws IOException {
			response.setRenderParameter("test", "value4");
		}

		@ActionMapping(params = "action=details")
		public void renderDetails(ActionRequest request, ActionResponse response, Model model) {
			response.setRenderParameter("test", "details");
	    }

		@ResourceMapping
		public void myDefaultResource(Writer writer) throws IOException {
			writer.write("myDefaultResource");
		}
	}


	private static class TestView {

		public void render(String viewName, Map model, PortletRequest request, MimeResponse response) throws Exception {
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
			List<TestBean> testBeans = (List<TestBean>) model.get("testBeanList");
			response.getWriter().write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
					"-" + testBeans.get(0).getName() + "-" + model.get("myKey"));
		}
	}

}
