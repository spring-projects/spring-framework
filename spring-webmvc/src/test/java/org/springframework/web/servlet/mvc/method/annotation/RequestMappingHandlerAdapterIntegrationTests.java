/*
 * Copyright 2002-2020 the original author or authors.
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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

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


	@BeforeEach
	public void setup() throws Exception {
		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setValidator(new StubValidator());

		List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>();
		customResolvers.add(new ServletWebArgumentResolverAdapter(new ColorArgumentResolver()));
		customResolvers.add(new CustomPrincipalArgumentResolver());

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

	@AfterEach
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	public void handle() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {int.class, String.class, String.class, String.class, Map.class,
				Date.class, Map.class, String.class, String.class, TestBean.class, Errors.class, TestBean.class,
				Color.class, HttpServletRequest.class, HttpServletResponse.class, TestBean.class, TestBean.class,
				User.class, OtherUser.class, Principal.class, Model.class, UriComponentsBuilder.class};

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

		assertThat(mav.getViewName()).isEqualTo("viewName");
		assertThat(model.get("cookie")).isEqualTo(99);
		assertThat(model.get("pathvar")).isEqualTo("pathvarValue");
		assertThat(model.get("header")).isEqualTo("headerValue");
		assertThat(model.get("dateParam")).isEqualTo(date);

		Map<?, ?> map = (Map<?, ?>) model.get("headerMap");
		assertThat(map.get("header")).isEqualTo("headerValue");
		assertThat(map.get("anotherHeader")).isEqualTo("anotherHeaderValue");
		assertThat(model.get("systemHeader")).isEqualTo("systemHeaderValue");

		map = (Map<?, ?>) model.get("paramMap");
		assertThat(map.get("dateParam")).isEqualTo(formattedDate);
		assertThat(map.get("paramByConvention")).isEqualTo("paramByConventionValue");

		assertThat(model.get("value")).isEqualTo("/contextPath");

		TestBean modelAttr = (TestBean) model.get("modelAttr");
		assertThat(modelAttr.getAge()).isEqualTo(25);
		assertThat(modelAttr.getName()).isEqualTo("Set by model method [modelAttr]");
		assertThat(request.getSession().getAttribute("modelAttr")).isSameAs(modelAttr);

		BindingResult bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "modelAttr");
		assertThat(bindingResult.getTarget()).isSameAs(modelAttr);
		assertThat(bindingResult.getErrorCount()).isEqualTo(1);

		String conventionAttrName = "testBean";
		TestBean modelAttrByConvention = (TestBean) model.get(conventionAttrName);
		assertThat(modelAttrByConvention.getAge()).isEqualTo(25);
		assertThat(modelAttrByConvention.getName()).isEqualTo("Set by model method [modelAttrByConvention]");
		assertThat(request.getSession().getAttribute(conventionAttrName)).isSameAs(modelAttrByConvention);

		bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + conventionAttrName);
		assertThat(bindingResult.getTarget()).isSameAs(modelAttrByConvention);

		assertThat(model.get("customArg") instanceof Color).isTrue();
		assertThat(model.get("user").getClass()).isEqualTo(User.class);
		assertThat(model.get("otherUser").getClass()).isEqualTo(OtherUser.class);
		assertThat(((Principal) model.get("customUser")).getName()).isEqualTo("Custom User");

		assertThat(model.get("sessionAttribute")).isSameAs(sessionAttribute);
		assertThat(model.get("requestAttribute")).isSameAs(requestAttribute);

		assertThat(model.get("url")).isEqualTo(new URI("http://localhost/contextPath/main/path"));
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

		assertThat(mav.getViewName()).isEqualTo("viewName");
		assertThat(model.get("cookie")).isEqualTo(99);
		assertThat(model.get("pathvar")).isEqualTo("pathvarValue");
		assertThat(model.get("header")).isEqualTo("headerValue");
		assertThat(model.get("dateParam")).isEqualTo(date);

		Map<?, ?> map = (Map<?, ?>) model.get("headerMap");
		assertThat(map.get("header")).isEqualTo("headerValue");
		assertThat(map.get("anotherHeader")).isEqualTo("anotherHeaderValue");
		assertThat(model.get("systemHeader")).isEqualTo("systemHeaderValue");

		map = (Map<?, ?>) model.get("paramMap");
		assertThat(map.get("dateParam")).isEqualTo(formattedDate);
		assertThat(map.get("paramByConvention")).isEqualTo("paramByConventionValue");

		assertThat(model.get("value")).isEqualTo("/contextPath");

		TestBean modelAttr = (TestBean) model.get("modelAttr");
		assertThat(modelAttr.getAge()).isEqualTo(25);
		assertThat(modelAttr.getName()).isEqualTo("Set by model method [modelAttr]");
		assertThat(request.getSession().getAttribute("modelAttr")).isSameAs(modelAttr);

		BindingResult bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "modelAttr");
		assertThat(bindingResult.getTarget()).isSameAs(modelAttr);
		assertThat(bindingResult.getErrorCount()).isEqualTo(1);

		String conventionAttrName = "testBean";
		TestBean modelAttrByConvention = (TestBean) model.get(conventionAttrName);
		assertThat(modelAttrByConvention.getAge()).isEqualTo(25);
		assertThat(modelAttrByConvention.getName()).isEqualTo("Set by model method [modelAttrByConvention]");
		assertThat(request.getSession().getAttribute(conventionAttrName)).isSameAs(modelAttrByConvention);

		bindingResult = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + conventionAttrName);
		assertThat(bindingResult.getTarget()).isSameAs(modelAttrByConvention);

		assertThat(model.get("customArg") instanceof Color).isTrue();
		assertThat(model.get("user").getClass()).isEqualTo(User.class);
		assertThat(model.get("otherUser").getClass()).isEqualTo(OtherUser.class);

		assertThat(model.get("sessionAttribute")).isSameAs(sessionAttribute);
		assertThat(model.get("requestAttribute")).isSameAs(requestAttribute);

		assertThat(model.get("url")).isEqualTo(new URI("http://localhost/contextPath/main/path"));
	}

	@Test
	public void handleRequestBody() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {byte[].class};

		request.setMethod("POST");
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleRequestBody", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertThat(mav).isNull();
		assertThat(new String(response.getContentAsByteArray(), "UTF-8")).isEqualTo("Handled requestBody=[Hello Server]");
		assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
	}

	@Test
	public void handleAndValidateRequestBody() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {TestBean.class, Errors.class};

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleAndValidateRequestBody", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertThat(mav).isNull();
		assertThat(new String(response.getContentAsByteArray(), "UTF-8")).isEqualTo("Error count [1]");
		assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
	}

	@Test
	public void handleHttpEntity() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {HttpEntity.class};

		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleHttpEntity", parameterTypes);

		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertThat(mav).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
		assertThat(new String(response.getContentAsByteArray(), "UTF-8")).isEqualTo("Handled requestBody=[Hello Server]");
		assertThat(response.getHeader("header")).isEqualTo("headerValue");
		// set because of @SesstionAttributes
		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
	}

	// SPR-13867
	@Test
	public void handleHttpEntityWithCacheControl() throws Exception {
		Class<?>[] parameterTypes = new Class<?>[] {HttpEntity.class};
		request.addHeader("Content-Type", "text/plain; charset=utf-8");
		request.setContent("Hello Server".getBytes("UTF-8"));

		HandlerMethod handlerMethod = handlerMethod("handleHttpEntityWithCacheControl", parameterTypes);
		ModelAndView mav = handlerAdapter.handle(request, response, handlerMethod);

		assertThat(mav).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(new String(response.getContentAsByteArray(), "UTF-8")).isEqualTo("Handled requestBody=[Hello Server]");
		assertThat(response.getHeaderValues("Cache-Control")).containsExactly("max-age=3600");
	}

	@Test
	public void handleRequestPart() throws Exception {
		MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(new MockMultipartFile("requestPart", "", "text/plain", "content".getBytes("UTF-8")));

		HandlerMethod handlerMethod = handlerMethod("handleRequestPart", String.class, Model.class);
		ModelAndView mav = handlerAdapter.handle(multipartRequest, response, handlerMethod);

		assertThat(mav).isNotNull();
		assertThat(mav.getModelMap().get("requestPart")).isEqualTo("content");
	}

	@Test
	public void handleAndValidateRequestPart() throws Exception {
		MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(new MockMultipartFile("requestPart", "", "text/plain", "content".getBytes("UTF-8")));

		HandlerMethod handlerMethod = handlerMethod("handleAndValidateRequestPart", String.class, Errors.class, Model.class);
		ModelAndView mav = handlerAdapter.handle(multipartRequest, response, handlerMethod);

		assertThat(mav).isNotNull();
		assertThat(mav.getModelMap().get("error count")).isEqualTo(1);
	}

	@Test
	public void handleAndCompleteSession() throws Exception {
		HandlerMethod handlerMethod = handlerMethod("handleAndCompleteSession", SessionStatus.class);
		handlerAdapter.handle(request, response, handlerMethod);

		assertThat(request.getSession().getAttributeNames().hasMoreElements()).isFalse();
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
				@Nullable User user, // gh-26117, gh-26117 (for @Nullable)
				@ModelAttribute OtherUser otherUser,
				@AuthenticationPrincipal Principal customUser, // gh-25780
				Model model,
				UriComponentsBuilder builder) {

			model.addAttribute("cookie", cookieV)
					.addAttribute("pathvar", pathvarV)
					.addAttribute("header", headerV)
					.addAttribute("systemHeader", systemHeader)
					.addAttribute("headerMap", headerMap)
					.addAttribute("dateParam", dateParam)
					.addAttribute("paramMap", paramMap)
					.addAttribute("paramByConvention", paramByConvention)
					.addAttribute("value", value)
					.addAttribute("customArg", customArg)
					.addAttribute(user)
					.addAttribute("customUser", customUser)
					.addAttribute("sessionAttribute", sessionAttribute)
					.addAttribute("requestAttribute", requestAttribute)
					.addAttribute("url", builder.path("/path").build().toUri());

			assertThat(request).isNotNull();
			assertThat(response).isNotNull();

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

			model.addAttribute("cookie", cookieV)
					.addAttribute("pathvar", pathvarV)
					.addAttribute("header", headerV)
					.addAttribute("systemHeader", systemHeader)
					.addAttribute("headerMap", headerMap)
					.addAttribute("dateParam", dateParam)
					.addAttribute("paramMap", paramMap)
					.addAttribute("paramByConvention", paramByConvention)
					.addAttribute("value", value)
					.addAttribute("customArg", customArg).addAttribute(user)
					.addAttribute("sessionAttribute", sessionAttribute)
					.addAttribute("requestAttribute", requestAttribute)
					.addAttribute("url", builder.path("/path").build().toUri());

			assertThat(request).isNotNull();
			assertThat(response).isNotNull();

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

	private static class CustomPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return (Principal.class.isAssignableFrom(parameter.getParameterType()) &&
					parameter.hasParameterAnnotation(AuthenticationPrincipal.class));
		}

		@Nullable
		@Override
		public Object resolveArgument(
				MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

			return (Principal) () -> "Custom User";
		}
	}

	@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface AuthenticationPrincipal {}
}
