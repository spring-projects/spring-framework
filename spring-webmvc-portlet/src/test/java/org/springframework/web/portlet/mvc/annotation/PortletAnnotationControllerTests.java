/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet.mvc.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.mock.web.portlet.MockActionResponse;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletContext;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.stereotype.Controller;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.portlet.DispatcherPortlet;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.NoHandlerFoundException;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;
import org.springframework.web.portlet.mvc.AbstractController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 2.5
 */
public class PortletAnnotationControllerTests {

	@Test
	public void standardHandleMethod() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			@Override
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

	public void doTestAdaptedHandleMethods(final Class<?> controllerClass) throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			@Override
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

		MockRenderRequest request = new MockRenderRequest(PortletMode.EDIT);
		request.addParameter("param1", "value1");
		request.addParameter("param2", "2");
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-value1-2", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.HELP);
		request.addParameter("name", "name1");
		request.addParameter("age", "2");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-name1-2", response.getContentAsString());

		request = new MockRenderRequest(PortletMode.VIEW);
		request.addParameter("name", "name1");
		request.addParameter("age", "value2");
		response = new MockRenderResponse();
		portlet.render(request, response);
		assertEquals("test-name1-typeMismatch", response.getContentAsString());
	}

	@Test
	public void formController() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyFormController.class));
				wac.refresh();
				return wac;
			}
			@Override
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
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyModelFormController.class));
				wac.refresh();
				return wac;
			}
			@Override
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
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyCommandProvidingFormController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues().add("webBindingInitializer", new MyWebBindingInitializer());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
			@Override
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
			@Override
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
			@Override
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
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MyBinderInitializingCommandProvidingFormController.class));
				wac.refresh();
				return wac;
			}
			@Override
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
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				StaticPortletApplicationContext wac = new StaticPortletApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(MySpecificBinderInitializingCommandProvidingFormController.class));
				wac.refresh();
				return wac;
			}
			@Override
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
			@Override
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
			@Override
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
	public void mavResolver() throws Exception {
		DispatcherPortlet portlet = new DispatcherPortlet() {
			@Override
			protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller",
						new RootBeanDefinition(ModelAndViewResolverController.class));
				RootBeanDefinition adapterDef = new RootBeanDefinition(AnnotationMethodHandlerAdapter.class);
				adapterDef.getPropertyValues()
						.add("customModelAndViewResolver", new MyModelAndViewResolver());
				wac.registerBeanDefinition("handlerAdapter", adapterDef);
				wac.refresh();
				return wac;
			}
		};
		portlet.init(new MockPortletConfig());

		MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
		MockRenderResponse response = new MockRenderResponse();
		portlet.render(request, response);
	}


	@RequestMapping("VIEW")
	private static class MyController extends AbstractController {

		@Override
		protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response) throws Exception {
			response.getWriter().write("test");
			return null;
		}
	}


	@Controller
	private static class MyAdaptedController {

		@RequestMapping("VIEW")
		public void myHandle(ActionRequest request, ActionResponse response) throws IOException {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		public void myHandle(@RequestParam("param1")String p1, @RequestParam("param2")int p2, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + p2);
		}

		@RequestMapping("HELP")
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("VIEW")
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	private static class MyAdaptedController2 {

		@RequestMapping("VIEW")
		public void myHandle(ActionRequest request, ActionResponse response) throws IOException {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		public void myHandle(@RequestParam("param1")String p1, int param2, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + param2);
		}

		@RequestMapping("HELP")
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping("VIEW")
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + errors.getFieldError("age").getCode());
		}
	}


	@Controller
	@RequestMapping({"VIEW", "EDIT", "HELP"})
	private static class MyAdaptedController3 {

		@RequestMapping
		public void myHandle(ActionRequest request, ActionResponse response) {
			response.setRenderParameter("test", "value");
		}

		@RequestMapping("EDIT")
		public void myHandle(@RequestParam("param1")String p1, @RequestParam("param2")int p2, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + p1 + "-" + p2);
		}

		@RequestMapping("HELP")
		public void myHandle(TestBean tb, RenderResponse response) throws IOException {
			response.getWriter().write("test-" + tb.getName() + "-" + tb.getAge());
		}

		@RequestMapping
		public void myHandle(TestBean tb, Errors errors, RenderResponse response) throws IOException {
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

		@Override
		@RequestMapping("VIEW")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myView";
		}

		@RequestMapping("EDIT")
		public String myOtherHandle(TB tb, BindingResult errors, ExtendedModelMap model, MySpecialArg arg) {
			TestBean tbReal = (TestBean) tb;
			tbReal.setName("myName");
			assertTrue(model.get("ITestBean") instanceof DerivedTestBean);
			assertNotNull(arg);
			return super.myHandle(tbReal, errors, model);
		}

		@ModelAttribute
		@SuppressWarnings("unchecked")
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

		@Override
		@RequestMapping("VIEW")
		public String myHandle(@ModelAttribute("myCommand") TestBean tb, BindingResult errors, ModelMap model) {
			if (!model.containsKey("myKey")) {
				model.addAttribute("myKey", "myValue");
			}
			return "myOtherView";
		}
	}


	@Controller
	@SuppressWarnings("rawtypes")
	private static class MyBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@InitBinder
		private void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	@Controller
	@SuppressWarnings("rawtypes")
	private static class MySpecificBinderInitializingCommandProvidingFormController extends MyCommandProvidingFormController {

		@InitBinder({"myCommand", "date"})
		private void initBinder(WebDataBinder binder) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}
	}


	private static class MyWebBindingInitializer implements WebBindingInitializer {

		@Override
		public void initBinder(WebDataBinder binder, WebRequest request) {
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


	@Controller
	@RequestMapping("VIEW")
	private static class MyParameterDispatchingController {

		@Autowired
		private PortletContext portletContext;

		@Autowired
		private PortletSession session;

		@Autowired
		private PortletRequest request;

		@RequestMapping
		public void myHandle(RenderResponse response) throws IOException {
			if (this.portletContext == null || this.session == null || this.request == null) {
				throw new IllegalStateException();
			}
			response.getWriter().write("myView");
		}

		@RequestMapping(params = {"view", "!lang"})
		public void myOtherHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myOtherView");
		}

		@RequestMapping(params = {"view=my", "lang=de"})
		public void myLangHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myLangView");
		}

		@RequestMapping(params = "surprise")
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

		@RequestMapping(params = "myParam=mySpecialValue")
		public void myHandle(RenderResponse response) throws IOException {
			response.getWriter().write("mySpecialView");
		}

		@RequestMapping
		public void myDefaultHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myDefaultView");
		}
	}


	@Controller
	@RequestMapping("EDIT")
	private static class MyOtherSpecialParameterDispatchingController {

		@RequestMapping(params = "myParam=myOtherSpecialValue")
		public void myHandle(RenderResponse response) throws IOException {
			response.getWriter().write("myOtherSpecialView");
		}
	}


	private static class TestView {

		@SuppressWarnings("deprecation")
		public void render(String viewName, Map<String, Object> model, PortletRequest request, MimeResponse response) throws Exception {
			TestBean tb = (TestBean) model.get("testBean");
			if (tb == null) {
				tb = (TestBean) model.get("myCommand");
			}
			if (tb.getName().endsWith("myDefaultName")) {
				assertEquals(107, tb.getDate().getYear());
			}
			Errors errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "testBean");
			if (errors == null) {
				errors = (Errors) model.get(BindingResult.MODEL_KEY_PREFIX + "myCommand");
			}
			if (errors.hasFieldErrors("date")) {
				throw new IllegalStateException();
			}
			List<?> testBeans = (List<?>) model.get("testBeanList");
			response.getWriter().write(viewName + "-" + tb.getName() + "-" + errors.getFieldError("age").getCode() +
					"-" + ((TestBean) testBeans.get(0)).getName() + "-" + model.get("myKey"));
		}
	}

	@Controller
	public static class ModelAndViewResolverController {

		@RequestMapping("VIEW")
		public MySpecialArg handle() {
			return new MySpecialArg("foo");
		}
	}

	public static class MyModelAndViewResolver implements ModelAndViewResolver {

		@Override
		public org.springframework.web.servlet.ModelAndView resolveModelAndView(Method handlerMethod,
				Class<?> handlerType,
				Object returnValue,
				ExtendedModelMap implicitModel,
				NativeWebRequest webRequest) {
			if (returnValue instanceof MySpecialArg) {
				return new org.springframework.web.servlet.ModelAndView(new View() {
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

}
