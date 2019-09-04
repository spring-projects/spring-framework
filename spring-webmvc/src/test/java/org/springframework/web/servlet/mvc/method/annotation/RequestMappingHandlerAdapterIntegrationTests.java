/*
 * Copyright 2002-2018 the original author or authors.
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

import java.awt.Color;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.Assert.*;

/**
 * A test fixture with a controller with all supported method signature styles
 * and arguments. A convenient place to test or confirm a problem with a
 * specific argument or return value type.
 *
 * @author Rossen Stoyanchev
 * @see HandlerMethodAnnotationDetectionTests
 * @see ServletAnnotationControllerHandlerMethodTests
 */
public class RequestMappingHandlerAdapterIntegrationTests {

	private final Object handler = new Handler();

	private RequestMappingHandlerAdapter handlerAdapter;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setup() throws Exception {
		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setValidator(new StubValidator());

		List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>();
		customResolvers.add(new ServletWebArgumentResolverAdapter(new ColorArgumentResolver()));

		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();

		handlerAdapter = new RequestMappingHandlerAdapter();
		handlerAdapter.setWebBindingInitializer(bindingInitializer);
		handlerAdapter.setCustomArgumentResolvers(customResolvers);
		handlerAdapter.setApplicationContext(context);
		handlerAdapter.setBeanFactory(context.getBeanFactory());
		handlerAdapter.afterPropertiesSet();

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		request.setMethod("POST");

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(new ServletWebRequest(request));
	}

	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	public void handle() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {int.class, String.class, String.class, String.class, Map.class,
				Date.class, Map.class, String.class, String.class, TestBean.class, Errors.class, TestBean.class,
				Color.class, HttpServletRequest.class, HttpServletResponse.class, TestBean.class, TestBean.class,
				User.class, OtherUser.class, Model.class, UriComponentsBuilder.class};

		String datePattern = "yyyy.MM.dd";
		String formattedDate = "2011.03.16";
		Date date = new GregorianCalendar(2011, Calendar.MARCH, 16).getTime();
		TestBean sessionAttribute = new TestBean();
		TestBean requestAttribute = new TestBean();

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("header", "headerValue");
		request.addHeader("anotherHeader", "anotherHeaderValue");
		request.addParameter("datePattern", datePattern);
		request.addParameter("dateParam", formattedDate);
		request.addParameter("paramByConvention", "paramByConventionValue");
		request.addParameter("age", "25");
		request.setCookies(new Cookie("cookie", "99"));
		request.setContent("Hello World".getBytes("UTF-8"));
		request.setUserPrincipal(new User());
		request.setContextPath("/contextPath");
		request.setServletPath("/main");
		System.setProperty("systemHeader", "systemHeaderValue");
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("pathvar", "pathvarValue");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);
		request.getSession().setAttribute("sessionAttribute", sessionAttribute);
		request.setAttribute("requestAttribute", requestAttribute);

		HandlerMethod handlerMethod = handlerMethod("handle", parameterTypes);
		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);
		ModelMap model = mav.getModelMap();

		assertEquals("viewName", mav.getViewName());
		assertEquals(99, model.get("cookie"));
		assertEquals("pathvarValue", model.get("pathvar"));
		assertEquals("headerValue", model.get("header"));
		assertEquals(date, model.get("dateParam"));

		Map<?, ?> map = (Map<?, ?>) model.get("headerMap");
		assertEquals("headerValue", map.get("header"));
		assertEquals("anotherHeaderValue", map.get("anotherHeader"));
		assertEquals("systemHeaderValue", model.get("systemHeader"));

		map = (Map<?, ?>) model.get("paramMap");
		assertEquals(formattedDate, map.get("dateParam"));
		assertEquals("paramByConventionValue", map.get("paramByConvention"));

		assertEquals("/contextPath", model.get("value"));

		TestBean modelAttr = (TestBean) model.get("modelAttr");
		assertEquals(25, modelAttr.getAge());
		assertEquals("Set by model method [modelAttr]", modelAttr.getName());
		assertSame(modelAttr, request.getSession().getAttribute("modelAttr"));

		BindingResult bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "modelAttr");
		assertSame(modelAttr, bindingResult.getTarget());
		assertEquals(1, bindingResult.getErrorCount());

		String conventionAttrName = "testBean";
		TestBean modelAttrByConvention = (TestBean) model.get(conventionAttrName);
		assertEquals(25, modelAttrByConvention.getAge());
		assertEquals("Set by model method [modelAttrByConvention]", modelAttrByConvention.getName());
		assertSame(modelAttrByConvention, request.getSession().getAttribute(conventionAttrName));

		bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + conventionAttrName);
		assertSame(modelAttrByConvention, bindingResult.getTarget());

		assertTrue(model.get("customArg") instanceof Color);
		assertEquals(User.class, model.get("user").getClass());
		assertEquals(OtherUser.class, model.get("otherUser").getClass());

		assertSame(sessionAttribute, model.get("sessionAttribute"));
		assertSame(requestAttribute, model.get("requestAttribute"));

		assertEquals(new URI("http://localhost/contextPath/main/path"), model.get("url"));
	}

	@Test
	public void handleInInterface() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {int.class, String.class, String.class, String.class, Map.class,
				Date.class, Map.class, String.class, String.class, TestBean.class, Errors.class, TestBean.class,
				Color.class, HttpServletRequest.class, HttpServletResponse.class, TestBean.class, TestBean.class,
				User.class, OtherUser.class, Model.class, UriComponentsBuilder.class};

		String datePattern = "yyyy.MM.dd";
		String formattedDate = "2011.03.16";
		Date date = new GregorianCalendar(2011, Calendar.MARCH, 16).getTime();
		TestBean sessionAttribute = new TestBean();
		TestBean requestAttribute = new TestBean();

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.addHeader("header", "headerValue");
		request.addHeader("anotherHeader", "anotherHeaderValue");
		request.addParameter("datePattern", datePattern);
		request.addParameter("dateParam", formattedDate);
		request.addParameter("paramByConvention", "paramByConventionValue");
		request.addParameter("age", "25");
		request.setCookies(new Cookie("cookie", "99"));
		request.setContent("Hello World".getBytes("UTF-8"));
		request.setUserPrincipal(new User());
		request.setContextPath("/contextPath");
		request.setServletPath("/main");
		System.setProperty("systemHeader", "systemHeaderValue");
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("pathvar", "pathvarValue");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);
		request.getSession().setAttribute("sessionAttribute", sessionAttribute);
		request.setAttribute("requestAttribute", requestAttribute);

		HandlerMethod handlerMethod = handlerMethod("handleInInterface", parameterTypes);
		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);
		ModelMap model = mav.getModelMap();

		assertEquals("viewName", mav.getViewName());
		assertEquals(99, model.get("cookie"));
		assertEquals("pathvarValue", model.get("pathvar"));
		assertEquals("headerValue", model.get("header"));
		assertEquals(date, model.get("dateParam"));

		Map<?, ?> map = (Map<?, ?>) model.get("headerMap");
		assertEquals("headerValue", map.get("header"));
		assertEquals("anotherHeaderValue", map.get("anotherHeader"));
		assertEquals("systemHeaderValue", model.get("systemHeader"));

		map = (Map<?, ?>) model.get("paramMap");
		assertEquals(formattedDate, map.get("dateParam"));
		assertEquals("paramByConventionValue", map.get("paramByConvention"));

		assertEquals("/contextPath", model.get("value"));

		TestBean modelAttr = (TestBean) model.get("modelAttr");
		assertEquals(25, modelAttr.getAge());
		assertEquals("Set by model method [modelAttr]", modelAttr.getName());
		assertSame(modelAttr, request.getSession().getAttribute("modelAttr"));

		BindingResult bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "modelAttr");
		assertSame(modelAttr, bindingResult.getTarget());
		assertEquals(1, bindingResult.getErrorCount());

		String conventionAttrName = "testBean";
		TestBean modelAttrByConvention = (TestBean) model.get(conventionAttrName);
		assertEquals(25, modelAttrByConvention.getAge());
		assertEquals("Set by model method [modelAttrByConvention]", modelAttrByConvention.getName());
		assertSame(modelAttrByConvention, request.getSession().getAttribute(conventionAttrName));

		bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + conventionAttrName);
		assertSame(modelAttrByConvention, bindingResult.getTarget());

		assertTrue(model.get("customArg") instanceof Color);
		assertEquals(User.class, model.get("user").getClass());
		assertEquals(OtherUser.class, model.get("otherUser").getClass());

		assertSame(sessionAttribute, model.get("sessionAttribute"));
		assertSame(requestAttribute, model.get("requestAttribute"));

		assertEquals(new URI("http://localhost/contextPath/main/path"), model.get("url"));
	}

	@Test
	public void handleRequestBody() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {byte[].class};

		request.setMethod("POST");
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleRequestBody", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertNull(mav);
		assertEquals("Handled requestBody=[Hello Server]", new String(response.getContentAsByteArray(), "UTF-8"));
		assertEquals(HttpStatus.ACCEPTED.value(), response.getStatus());
	}

	@Test
	public void handleAndValidateRequestBody() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {TestBean.class, Errors.class};

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleAndValidateRequestBody", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertNull(mav);
		assertEquals("Error count [1]", new String(response.getContentAsByteArray(), "UTF-8"));
		assertEquals(HttpStatus.ACCEPTED.value(), response.getStatus());
	}

	@Test
	public void handleHttpEntity() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {HttpEntity.class};

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleHttpEntity", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertNull(mav);
		assertEquals(HttpStatus.ACCEPTED.value(), response.getStatus());
		assertEquals("Handled requestBody=[Hello Server]", new String(response.getContentAsByteArray(), "UTF-8"));
		assertEquals("headerValue", response.getHeader("header"));
		// set because of @SesstionAttributes
		assertEquals("no-store", response.getHeader("Cache-Control"));
	}

	// SPR-13867
	@Test
	public void handleHttpEntityWithCacheControl() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {HttpEntity.class};
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleHttpEntityWithCacheControl", parameterTypes);
		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertNull(mav);
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEquals("Handled requestBody=[Hello Server]", new String(response.getContentAsByteArray(), "UTF-8"));
		assertThat(response.getHeaderValues("Cache-Control"), Matchers.contains("max-age=3600"));
	}

	@Test
	public void handleRequestPart() throws Exception {
		MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(new MockMultipartFile("requestPart", "", "text/plain", "content".getBytes("UTF-8")));

		HandlerMethod handlerMethod = handlerMethod("handleRequestPart", String.class, Model.class);
		ModelAndView mav = handlerAdapter.handle(multipartRequest, response, handlerMethod);

		assertNotNull(mav);
		assertEquals("content", mav.getModelMap().get("requestPart"));
	}

	@Test
	public void handleAndValidateRequestPart() throws Exception {
		MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(new MockMultipartFile("requestPart", "", "text/plain", "content".getBytes("UTF-8")));

		HandlerMethod handlerMethod = handlerMethod("handleAndValidateRequestPart", String.class, Errors.class, Model.class);
		ModelAndView mav = handlerAdapter.handle(multipartRequest, response, handlerMethod);

		assertNotNull(mav);
		assertEquals(1, mav.getModelMap().get("error count"));
	}

	@Test
	public void handleAndCompleteSession() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("handleAndCompleteSession", SessionStatus.class);
		handlerAdapter.handle(request, response, handlerMethod);

		assertFalse(request.getSession().getAttributeNames().hasMoreElements());
	}

	private HandlerMethod handlerMethod(String methodName, Class<?>... paramTypes) throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(handler, method);
	}


	private interface HandlerIfc {

		String handleInInterface(
				@CookieValue("cookie") int cookieV,
				@PathVariable("pathvar") String pathvarV,
				@RequestHeader("header") String headerV,
				@RequestHeader(defaultValue = "#{systemProperties.systemHeader}") String systemHeader,
				@RequestHeader Map<String, Object> headerMap,
				@RequestParam("dateParam") Date dateParam,
				@RequestParam Map<String, Object> paramMap,
				String paramByConvention,
				@Value("#{request.contextPath}") String value,
				@ModelAttribute("modelAttr") @Valid TestBean modelAttr,
				Errors errors,
				TestBean modelAttrByConvention,
				Color customArg,
				HttpServletRequest request,
				HttpServletResponse response,
				@SessionAttribute TestBean sessionAttribute,
				@RequestAttribute TestBean requestAttribute,
				User user,
				@ModelAttribute OtherUser otherUser,
				Model model,
				UriComponentsBuilder builder);
	}


	@SuppressWarnings("unused")
	@SessionAttributes(types = TestBean.class)
	private static class Handler implements HandlerIfc {

		@InitBinder("dateParam")
		public void initBinder(WebDataBinder dataBinder, @RequestParam("datePattern") String datePattern) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
			dataBinder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@ModelAttribute
		public void model(Model model) {
			TestBean modelAttr = new TestBean();
			modelAttr.setName("Set by model method [modelAttr]");
			model.addAttribute("modelAttr", modelAttr);

			modelAttr = new TestBean();
			modelAttr.setName("Set by model method [modelAttrByConvention]");
			model.addAttribute(modelAttr);

			model.addAttribute(new OtherUser());
		}

		public String handle(
				@CookieValue("cookie") int cookieV,
				@PathVariable("pathvar") String pathvarV,
				@RequestHeader("header") String headerV,
				@RequestHeader(defaultValue = "#{systemProperties.systemHeader}") String systemHeader,
				@RequestHeader Map<String, Object> headerMap,
				@RequestParam("dateParam") Date dateParam,
				@RequestParam Map<String, Object> paramMap,
				String paramByConvention,
				@Value("#{request.contextPath}") String value,
				@ModelAttribute("modelAttr") @Valid TestBean modelAttr,
				Errors errors,
				TestBean modelAttrByConvention,
				Color customArg,
				HttpServletRequest request,
				HttpServletResponse response,
				@SessionAttribute TestBean sessionAttribute,
				@RequestAttribute TestBean requestAttribute,
				User user,
				@ModelAttribute OtherUser otherUser,
				Model model,
				UriComponentsBuilder builder) {

			model.addAttribute("cookie", cookieV).addAttribute("pathvar", pathvarV).addAttribute("header", headerV)
					.addAttribute("systemHeader", systemHeader).addAttribute("headerMap", headerMap)
					.addAttribute("dateParam", dateParam).addAttribute("paramMap", paramMap)
					.addAttribute("paramByConvention", paramByConvention).addAttribute("value", value)
					.addAttribute("customArg", customArg).addAttribute(user)
					.addAttribute("sessionAttribute", sessionAttribute)
					.addAttribute("requestAttribute", requestAttribute)
					.addAttribute("url", builder.path("/path").build().toUri());

			assertNotNull(request);
			assertNotNull(response);

			return "viewName";
		}

		@Override
		public String handleInInterface(
				int cookieV,
				String pathvarV,
				String headerV,
				String systemHeader,
				Map<String, Object> headerMap,
				Date dateParam,
				Map<String, Object> paramMap,
				String paramByConvention,
				String value,
				TestBean modelAttr,
				Errors errors,
				TestBean modelAttrByConvention,
				Color customArg,
				HttpServletRequest request,
				HttpServletResponse response,
				TestBean sessionAttribute,
				TestBean requestAttribute,
				User user,
				OtherUser otherUser,
				Model model,
				UriComponentsBuilder builder) {

			model.addAttribute("cookie", cookieV).addAttribute("pathvar", pathvarV).addAttribute("header", headerV)
					.addAttribute("systemHeader", systemHeader).addAttribute("headerMap", headerMap)
					.addAttribute("dateParam", dateParam).addAttribute("paramMap", paramMap)
					.addAttribute("paramByConvention", paramByConvention).addAttribute("value", value)
					.addAttribute("customArg", customArg).addAttribute(user)
					.addAttribute("sessionAttribute", sessionAttribute)
					.addAttribute("requestAttribute", requestAttribute)
					.addAttribute("url", builder.path("/path").build().toUri());

			assertNotNull(request);
			assertNotNull(response);

			return "viewName";
		}

		@ResponseStatus(HttpStatus.ACCEPTED)
		@ResponseBody
		public String handleRequestBody(@RequestBody byte[] bytes) throws Exception {
			String requestBody = new String(bytes, "UTF-8");
			return "Handled requestBody=[" + requestBody + "]";
		}

		@ResponseStatus(code = HttpStatus.ACCEPTED)
		@ResponseBody
		public String handleAndValidateRequestBody(@Valid TestBean modelAttr, Errors errors) {
			return "Error count [" + errors.getErrorCount() + "]";
		}

		public ResponseEntity<String> handleHttpEntity(HttpEntity<byte[]> httpEntity) throws Exception {
			String responseBody = "Handled requestBody=[" + new String(httpEntity.getBody(), "UTF-8") + "]";
			return ResponseEntity.accepted()
					.header("header", "headerValue")
					.body(responseBody);
		}

		public ResponseEntity<String> handleHttpEntityWithCacheControl(HttpEntity<byte[]> httpEntity) throws Exception {
			String responseBody = "Handled requestBody=[" + new String(httpEntity.getBody(), "UTF-8") + "]";
			return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)).body(responseBody);
		}

		public void handleRequestPart(@RequestPart String requestPart, Model model) {
			model.addAttribute("requestPart", requestPart);
		}

		public void handleAndValidateRequestPart(@RequestPart @Valid String requestPart,
				Errors errors, Model model) throws Exception {

			model.addAttribute("error count", errors.getErrorCount());
		}

		public void handleAndCompleteSession(SessionStatus sessionStatus) {
			sessionStatus.setComplete();
		}
	}


	private static class StubValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
			errors.reject("error");
		}
	}


	private static class ColorArgumentResolver implements WebArgumentResolver {

		@Override
		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
			return new Color(0);
		}
	}


	private static class User implements Principal {

		@Override
		public String getName() {
			return "user";
		}
	}


	private static class OtherUser implements Principal {

		@Override
		public String getName() {
			return "other user";
		}
	}

}
