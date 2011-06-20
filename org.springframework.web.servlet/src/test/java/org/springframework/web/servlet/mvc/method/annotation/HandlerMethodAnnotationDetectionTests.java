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
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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

/**
 * Test various scenarios for detecting method-level and method parameter annotations depending 
 * on where they are located -- on interfaces, parent classes, in parameterized methods, or in 
 * combination with proxies.
 * 
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class HandlerMethodAnnotationDetectionTests {
	
	@Parameters
	public static Collection<Object[]> handlerTypes() {
		Object[][] array = new Object[12][2];

		array[0] = new Object[] { SimpleController.class, true};  // CGLib proxy
		array[1] = new Object[] { SimpleController.class, false};
		
		array[2] = new Object[] { AbstractClassController.class, true };	// CGLib proxy
		array[3] = new Object[] { AbstractClassController.class, false };
		
		array[4] = new Object[] { ParameterizedAbstractClassController.class, false}; // CGLib proxy	
		array[5] = new Object[] { ParameterizedAbstractClassController.class, false};	
		
		array[6] = new Object[] { InterfaceController.class, true };	// JDK dynamic proxy
		array[7] = new Object[] { InterfaceController.class, false };	
		
		array[8] = new Object[] { ParameterizedInterfaceController.class, false}; // no AOP	
		array[9] = new Object[] { ParameterizedInterfaceController.class, false};	
		
		array[10] = new Object[] { SupportClassController.class, true};  // CGLib proxy
		array[11] = new Object[] { SupportClassController.class, false};
		
		return Arrays.asList(array);
	}

	private RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
//	private DefaultAnnotationHandlerMapping handlerMapping = new DefaultAnnotationHandlerMapping();

	private RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
//	AnnotationMethodHandlerAdapter handlerAdapter = new AnnotationMethodHandlerAdapter();

	private ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver();
//	private AnnotationMethodHandlerExceptionResolver exceptionResolver = new AnnotationMethodHandlerExceptionResolver();

	public HandlerMethodAnnotationDetectionTests(Class<?> controllerType, boolean useAutoProxy) {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBeanDefinition("controller", new RootBeanDefinition(controllerType));
		if (useAutoProxy) {
			DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
			autoProxyCreator.setBeanFactory(context.getBeanFactory());
			context.getBeanFactory().addBeanPostProcessor(autoProxyCreator);
			context.getBeanFactory().registerSingleton("advsr", new DefaultPointcutAdvisor(new SimpleTraceInterceptor()));
		}
		context.refresh();
		
		handlerMapping.setApplicationContext(context);

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new MappingJacksonHttpMessageConverter());

		handlerAdapter.setMessageConverters(messageConverters);
		handlerAdapter.afterPropertiesSet();
//		handlerAdapter.setMessageConverters(messageConverters.toArray(new HttpMessageConverter<?>[messageConverters.size()]));
//		handlerAdapter.setApplicationContext(context);

		exceptionResolver.setMessageConverters(messageConverters);
		exceptionResolver.afterPropertiesSet();
//		exceptionResolver.setMessageConverters(messageConverters.toArray(new HttpMessageConverter<?>[messageConverters.size()]));
	}

	@Test
	public void testRequestMappingMethod() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/path1/path2");
		request.setParameter("datePattern", "MM:dd:yyyy");
		request.addHeader("dateA", "11:01:2011");
		request.addHeader("dateB", "11:02:2011");
		request.addHeader("Accept", "application/json");
		
		HandlerExecutionChain chain = handlerMapping.getHandler(request);
		assertNotNull(chain);

		MockHttpServletResponse response = new MockHttpServletResponse();
		handlerAdapter.handle(request, response, chain.getHandler());
		assertEquals("application/json", response.getHeader("Content-Type"));
		assertEquals("{\"dateA\":1320105600000,\"dateB\":1320192000000}", response.getContentAsString());

		response = new MockHttpServletResponse();
		exceptionResolver.resolveException(request, response, chain.getHandler(), new Exception("failure"));
		assertEquals("application/json", response.getHeader("Content-Type"));
		assertEquals("\"failure\"", response.getContentAsString());
	}

	
	/**
	 * SIMPLE CASE
	 */
	@Controller
	static class SimpleController {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@ModelAttribute
		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
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
		public abstract void initBinder(WebDataBinder dataBinder, String thePattern);

		@ModelAttribute
		public abstract void initModel(Date date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		public abstract Map<String, Object> handle(Date date, Model model) throws Exception;
		
		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}	

	/**
	 * CONTROLLER WITH ABSTRACT CLASS
	 * 
	 * <p>All annotations can be on methods in the abstract class except parameter annotations.
	 */
	static class AbstractClassController extends MappingAbstractClass {

		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
		}
		
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}
	
	
	@Controller
	static interface MappingInterface {

		@InitBinder
		void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern);

		@ModelAttribute
		void initModel(@RequestHeader("dateA") Date date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception;
		
		@ExceptionHandler(Exception.class)
		@ResponseBody
		String handleException(Exception exception);
	}	

	/**
	 * CONTROLLER WITH INTERFACE
	 * 
	 * No AOP:
	 * All annotations can be on interface methods except parameter annotations.
	 *
	 * JDK Dynamic proxy:
	 * All annotations must be on the interface.
	 */
	static class InterfaceController implements MappingInterface {

		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
		}
		
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}


	@Controller
	static abstract class MappingParameterizedAbstractClass<A, B, C> {

		@InitBinder
		public abstract void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		public abstract void initModel(B date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		public abstract Map<String, Object> handle(C date, Model model) throws Exception;
		
		@ExceptionHandler(Exception.class)
		@ResponseBody
		public abstract String handleException(Exception exception);
	}	

	/**
	 * CONTROLLER WITH PARAMETERIZED BASE CLASS
	 * 
	 * <p>All annotations can be on methods in the abstract class except parameter annotations.
	 */
	static class ParameterizedAbstractClassController extends MappingParameterizedAbstractClass<String, Date, Date> {

		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
		}
		
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}

	
	@Controller
	static interface MappingParameterizedInterface<A, B, C> {

		@InitBinder
		void initBinder(WebDataBinder dataBinder, A thePattern);

		@ModelAttribute
		void initModel(B date, Model model);

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		Map<String, Object> handle(C date, Model model) throws Exception;
		
		@ExceptionHandler(Exception.class)
		@ResponseBody
		String handleException(Exception exception);
	}	

	/**
	 * CONTROLLER WITH PARAMETERIZED INTERFACE
	 * 
	 * <p>All annotations can be on interface except parameter annotations.
	 * 
	 * <p>Cannot be used as JDK dynamic proxy since parameterized interface does not contain type information.
	 */
	static class ParameterizedInterfaceController implements MappingParameterizedInterface<String, Date, Date> {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String thePattern) {
			CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(thePattern), false);
			dataBinder.registerCustomEditor(Date.class, dateEditor);
		}

		@ModelAttribute
		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		@RequestMapping(value="/path1/path2", method=RequestMethod.POST, produces="application/json")
		@ResponseBody
		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
		}
		
		@ExceptionHandler(Exception.class)
		@ResponseBody
		public String handleException(Exception exception) {
			return exception.getMessage();
		}
	}	
	
	
	/**
	 * SPR-8248
	 * 
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
		public void initModel(@RequestHeader("dateA") Date date, Model model) {
			model.addAttribute("dateA", date);
		}

		@ResponseBody
		@RequestMapping(value="/path2", method=RequestMethod.POST, produces="application/json")
		public Map<String, Object> handle(@RequestHeader("dateB") Date date, Model model) throws Exception {
			model.addAttribute("dateB", date);
			return model.asMap();
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

}
