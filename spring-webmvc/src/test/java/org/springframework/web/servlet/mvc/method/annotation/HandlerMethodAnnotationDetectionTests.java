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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test various scenarios for detecting method-level and method parameter annotations depending
 * on where they are located -- on interfaces, parent classes, in parameterized methods, or in
 * combination with proxies.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandlerMethodAnnotationDetectionTests {

	static Object[][] handlerTypes() {
		return new Object[][] {
				{ SimpleController.class, true }, // CGLIB proxy
				{ SimpleController.class, false },

				{ AbstractClassController.class, true }, // CGLIB proxy
				{ AbstractClassController.class, false },

				{ ParameterizedAbstractClassController.class, true }, // CGLIB proxy
				{ ParameterizedAbstractClassController.class, false },

				{ ParameterizedSubclassOverridesDefaultMappings.class, true }, // CGLIB proxy
				{ ParameterizedSubclassOverridesDefaultMappings.class, false },

				// TODO [SPR-9517] Enable ParameterizedSubclassDoesNotOverrideConcreteImplementationsFromGenericAbstractSuperclass test cases
				// { ParameterizedSubclassDoesNotOverrideConcreteImplementationsFromGenericAbstractSuperclass.class, true }, // CGLIB proxy
				// { ParameterizedSubclassDoesNotOverrideConcreteImplementationsFromGenericAbstractSuperclass.class, false },

				{ InterfaceController.class, true }, // JDK dynamic proxy
				{ InterfaceController.class, false },

				{ ParameterizedInterfaceController.class, false }, // no AOP

				{ SupportClassController.class, true }, // CGLIB proxy
				{ SupportClassController.class, false }
		};
	}

	private RequestMappingHandlerMapping handlerMapping;

	private RequestMappingHandlerAdapter handlerAdapter;

	private ExceptionHandlerExceptionResolver exceptionResolver;


	private void setUp(Class<?> controllerType, boolean useAutoProxy) {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBeanDefinition("controller", new RootBeanDefinition(controllerType));
		context.registerBeanDefinition("handlerMapping", new RootBeanDefinition(RequestMappingHandlerMapping.class));
		context.registerBeanDefinition("handlerAdapter", new RootBeanDefinition(RequestMappingHandlerAdapter.class));
		context.registerBeanDefinition("exceptionResolver", new RootBeanDefinition(ExceptionHandlerExceptionResolver.class));
		if (useAutoProxy) {
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(context.getBeanFactory());
			context.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			context.registerBeanDefinition("controllerAdvice", new RootBeanDefinition(ControllerAdvisor.class));
		}
		context.refresh();

		this.handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
		this.handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
		this.exceptionResolver = context.getBean(ExceptionHandlerExceptionResolver.class);
		context.close();
	}


	@ParameterizedTest(name = "[{index}] controller [{0}], auto-proxy [{1}]")
	@MethodSource("handlerTypes")
	void testRequestMappingMethod(Class<?> controllerType, boolean useAutoProxy) throws Exception {
		setUp(controllerType, useAutoProxy);

		String datePattern = "MM:dd:yyyy";
		SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
		String dateA = "11:01:2011";
		String dateB = "11:02:2011";

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/path1/path2");
		request.setParameter("datePattern", datePattern);
		request.addHeader("header1", dateA);
		request.addHeader("header2", dateB);

		HandlerExecutionChain chain = handlerMapping.getHandler(request);
		assertThat(chain).isNotNull();

		ModelAndView mav = handlerAdapter.handle(request, new MockHttpServletResponse(), chain.getHandler());

		assertThat(mav.getModel().get("attr1")).as("model attr1:").isEqualTo(dateFormat.parse(dateA));
		assertThat(mav.getModel().get("attr2")).as("model attr2:").isEqualTo(dateFormat.parse(dateB));

		MockHttpServletResponse response = new MockHttpServletResponse();
		exceptionResolver.resolveException(request, response, chain.getHandler(), new Exception("failure"));
		assertThat(response.getHeader("Content-Type")).isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(response.getContentAsString()).isEqualTo("failure");
	}


	/**
	 * SIMPLE CASE
	 */
	@Controller
	static class SimpleController {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String pattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(pattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@ModelAttribute
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		public Date handle(@RequestHeader("header2") Date date) throws Exception {
			return date;
		}

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@Controller
	static abstract class MappingAbstractClass {

		@InitBinder
		public abstract void initBinder(WebDataBinder dataBinder, String pattern);

		@ModelAttribute
		public abstract void initModel(Date date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		public abstract Date handle(Date date, Model model) throws Exception;

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}


	/**
	 * CONTROLLER WITH ABSTRACT CLASS
	 * <p>All annotations can be on methods in the abstract class except parameter annotations.
	 */
	static class AbstractClassController extends MappingAbstractClass {

		@Override
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String pattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(pattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@Override
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@Override
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	// SPR-9374
	@RequestMapping
	interface MappingInterface {

		@InitBinder
		void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern);

		@ModelAttribute
		void initModel(@RequestHeader("header1") Date date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		Date handle(@RequestHeader("header2") Date date, Model model) throws Exception;

		@ExceptionHandler(Exception.class)
		@ResponseBody
		String handleException(Exception exception);
	}


	/**
	 * CONTROLLER WITH INTERFACE
	 * <p>JDK Dynamic proxy: All annotations must be on the interface.
	 * <p>Without AOP: Annotations can be on interface methods except parameter annotations.
	 */
	static class InterfaceController implements MappingInterface {

		@Override
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@Override
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@Override
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@Controller
	static abstract class MappingGenericAbstractClass<A, B, C> {

		@InitBinder
		public abstract void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		public abstract void initModel(B date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		public abstract Date handle(C date, Model model) throws Exception;

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}


	/**
	 * CONTROLLER WITH PARAMETERIZED BASE CLASS
	 * <p>All annotations can be on methods in the abstract class except parameter annotations.
	 */
	static class ParameterizedAbstractClassController extends MappingGenericAbstractClass<String, Date, Date> {

		@Override
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@Override
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@Override
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@Controller
	static abstract class MappedGenericAbstractClassWithConcreteImplementations<A, B, C> {

		@InitBinder
		public abstract void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		public abstract void initModel(B date, Model model);

		@RequestMapping(value = "/path1/path2", method = RequestMethod.POST)
		@ModelAttribute("attr2")
		public Date handle(C date, Model model) throws Exception {
			return (Date) date;
		}

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}


	static class ParameterizedSubclassDoesNotOverrideConcreteImplementationsFromGenericAbstractSuperclass extends
			MappedGenericAbstractClassWithConcreteImplementations<String, Date, Date> {

		@Override
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		// does not override handle()

		@Override
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@Controller
	static abstract class GenericAbstractClassDeclaresDefaultMappings<A, B, C> {

		@InitBinder
		public abstract void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		public abstract void initModel(B date, Model model);

		// /foo/bar should be overridden in concrete subclass
		@RequestMapping(value = "/foo/bar", method = RequestMethod.POST)
		// attrFoo should be overridden in concrete subclass
		@ModelAttribute("attrFoo")
		public abstract Date handle(C date, Model model) throws Exception;

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}


	static class ParameterizedSubclassOverridesDefaultMappings
			extends GenericAbstractClassDeclaresDefaultMappings<String, Date, Date> {

		@Override
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@Override
		@RequestMapping(value = "/path1/path2", method = RequestMethod.POST)
		// NOTE: @ModelAttribute will NOT be found on the abstract superclass if
		// @RequestMapping is declared locally. Thus, we have to redeclare
		// @ModelAttribute locally as well.
		@ModelAttribute("attr2")
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@Override
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@RequestMapping
	interface MappingGenericInterface<A, B, C> {

		@InitBinder
		void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		void initModel(B date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		Date handle(C date, Model model) throws Exception;

		@ExceptionHandler(Exception.class)
		@ResponseBody
		String handleException(Exception exception);
	}


	/**
	 * CONTROLLER WITH PARAMETERIZED INTERFACE
	 * <p>All annotations can be on interface except parameter annotations.
	 * <p>Cannot be used as JDK dynamic proxy since parameterized interface does not contain type information.
	 */
	static class ParameterizedInterfaceController implements MappingGenericInterface<String, Date, Date> {

		@Override
		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@Override
		@ModelAttribute
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@Override
		@RequestMapping(value="/path1/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@Override
		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	/**
	 * SPR-8248
	 * <p>Support class contains all annotations. Subclass has type-level @{@link RequestMapping}.
	 */
	@Controller
	static class MappingSupportClass {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@ModelAttribute
		public void initModel(@RequestHeader("header1") Date date, Model model) {
			model.addAttribute("attr1", date);
		}

		@RequestMapping(value="/path2", method=RequestMethod.POST)
		@ModelAttribute("attr2")
		public Date handle(@RequestHeader("header2") Date date, Model model) throws Exception {
			return date;
		}

		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}

	@Controller
	@RequestMapping("/path1")
	static class SupportClassController extends MappingSupportClass {
	}


	@SuppressWarnings("serial")
	static class ControllerAdvisor extends DefaultPointcutAdvisor {

		public ControllerAdvisor() {
			super(getControllerPointcut(), new SimpleTraceInterceptor());
		}

		private static StaticMethodMatcherPointcut getControllerPointcut() {
			return new StaticMethodMatcherPointcut() {
				@Override
				public boolean matches(Method method, @Nullable Class<?> targetClass) {
					return ((AnnotationUtils.findAnnotation(targetClass, Controller.class) != null) ||
							(AnnotationUtils.findAnnotation(targetClass, RequestMapping.class) != null));
				}
			};
		}
	}

}
