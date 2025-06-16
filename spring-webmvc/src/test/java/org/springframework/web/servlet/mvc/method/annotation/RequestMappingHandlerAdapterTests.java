/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.AsyncEvent;
import org.apache.groovy.util.Maps;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see ServletAnnotationControllerHandlerMethodTests
 * @see HandlerMethodAnnotationDetectionTests
 * @see RequestMappingHandlerAdapterIntegrationTests
 */
class RequestMappingHandlerAdapterTests {

	private static int RESOLVER_COUNT;

	private static int INIT_BINDER_RESOLVER_COUNT;

	private static int HANDLER_COUNT;

	private RequestMappingHandlerAdapter handlerAdapter;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private StaticWebApplicationContext webAppContext;


	@BeforeAll
	static void setupOnce() {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setApplicationContext(new StaticWebApplicationContext());
		adapter.afterPropertiesSet();

		RESOLVER_COUNT = adapter.getArgumentResolvers().size();
		INIT_BINDER_RESOLVER_COUNT = adapter.getInitBinderArgumentResolvers().size();
		HANDLER_COUNT = adapter.getReturnValueHandlers().size();
	}

	@BeforeEach
	void setup() throws Exception {
		this.webAppContext = new StaticWebApplicationContext();
		this.handlerAdapter = new RequestMappingHandlerAdapter();
		this.handlerAdapter.setApplicationContext(this.webAppContext);
		this.request = new MockHttpServletRequest("GET", "/");
		this.response = new MockHttpServletResponse();
	}


	@Test
	void cacheControlWithoutSessionAttributes() throws Exception {
		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
		this.handlerAdapter.setCacheSeconds(100);
		this.handlerAdapter.afterPropertiesSet();

		this.handlerAdapter.handle(this.request, this.response, handlerMethod);
		assertThat(response.getHeader("Cache-Control")).contains("max-age");
	}

	@Test
	void cacheControlWithSessionAttributes() throws Exception {
		SessionAttributeController handler = new SessionAttributeController();
		this.handlerAdapter.setCacheSeconds(100);
		this.handlerAdapter.afterPropertiesSet();

		this.handlerAdapter.handle(this.request, this.response, handlerMethod(handler, "handle"));
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
	}

	@Test
	void setAlwaysUseRedirectAttributes() throws Exception {
		HandlerMethodArgumentResolver redirectAttributesResolver = new RedirectAttributesMethodArgumentResolver();
		HandlerMethodArgumentResolver modelResolver = new ModelMethodProcessor();
		HandlerMethodReturnValueHandler viewHandler = new ViewNameMethodReturnValueHandler();

		this.handlerAdapter.setArgumentResolvers(Arrays.asList(redirectAttributesResolver, modelResolver));
		this.handlerAdapter.setReturnValueHandlers(Collections.singletonList(viewHandler));
		this.handlerAdapter.afterPropertiesSet();

		this.request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());

		HandlerMethod handlerMethod = handlerMethod(new RedirectAttributeController(), "handle", Model.class);
		ModelAndView mav = this.handlerAdapter.handle(request, response, handlerMethod);

		assertThat(mav.getModel().isEmpty()).as("Without RedirectAttributes arg, model should be empty").isTrue();
	}

	@Test
	void setCustomArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setCustomArgumentResolvers(Collections.singletonList(resolver));
		this.handlerAdapter.afterPropertiesSet();

		assertThat(this.handlerAdapter.getArgumentResolvers()).contains(resolver);
		assertMethodProcessorCount(RESOLVER_COUNT + 1, INIT_BINDER_RESOLVER_COUNT + 1, HANDLER_COUNT);
	}

	@Test
	void setArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setArgumentResolvers(Collections.singletonList(resolver));
		this.handlerAdapter.afterPropertiesSet();

		assertMethodProcessorCount(1, INIT_BINDER_RESOLVER_COUNT, HANDLER_COUNT);
	}

	@Test
	void setInitBinderArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setInitBinderArgumentResolvers(Collections.singletonList(resolver));
		this.handlerAdapter.afterPropertiesSet();

		assertMethodProcessorCount(RESOLVER_COUNT, 1, HANDLER_COUNT);
	}

	@Test
	void setCustomReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
		this.handlerAdapter.setCustomReturnValueHandlers(Collections.singletonList(handler));
		this.handlerAdapter.afterPropertiesSet();

		assertThat(this.handlerAdapter.getReturnValueHandlers()).contains(handler);
		assertMethodProcessorCount(RESOLVER_COUNT, INIT_BINDER_RESOLVER_COUNT, HANDLER_COUNT + 1);
	}

	@Test
	void setReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
		this.handlerAdapter.setReturnValueHandlers(Collections.singletonList(handler));
		this.handlerAdapter.afterPropertiesSet();

		assertMethodProcessorCount(RESOLVER_COUNT, INIT_BINDER_RESOLVER_COUNT, 1);
	}

	@Test
	void modelAttributeAdvice() throws Exception {
		this.webAppContext.registerSingleton("maa", ModelAttributeAdvice.class);
		this.webAppContext.refresh();

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
		this.handlerAdapter.afterPropertiesSet();
		ModelAndView mav = this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertThat(mav.getModel().get("attr1")).isEqualTo("lAttr1");
		assertThat(mav.getModel().get("attr2")).isEqualTo("gAttr2");
	}

	@Test
	void prototypeControllerAdvice() throws Exception {
		this.webAppContext.registerPrototype("maa", ModelAttributeAdvice.class);
		this.webAppContext.refresh();

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
		this.handlerAdapter.afterPropertiesSet();
		Map<String, Object> model1 = this.handlerAdapter.handle(this.request, this.response, handlerMethod).getModel();
		Map<String, Object> model2 = this.handlerAdapter.handle(this.request, this.response, handlerMethod).getModel();

		assertThat(model1.get("instance")).isNotSameAs(model2.get("instance"));
	}

	@Test
	void modelAttributeAdviceInParentContext() throws Exception {
		StaticWebApplicationContext parent = new StaticWebApplicationContext();
		parent.registerSingleton("maa", ModelAttributeAdvice.class);
		parent.refresh();
		this.webAppContext.setParent(parent);
		this.webAppContext.refresh();

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
		this.handlerAdapter.afterPropertiesSet();
		ModelAndView mav = this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertThat(mav.getModel().get("attr1")).isEqualTo("lAttr1");
		assertThat(mav.getModel().get("attr2")).isEqualTo("gAttr2");
	}

	@Test
	void modelAttributePackageNameAdvice() throws Exception {
		this.webAppContext.registerSingleton("mapa", ModelAttributePackageAdvice.class);
		this.webAppContext.registerSingleton("manupa", ModelAttributeNotUsedPackageAdvice.class);
		this.webAppContext.refresh();

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handle");
		this.handlerAdapter.afterPropertiesSet();
		ModelAndView mav = this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertThat(mav.getModel().get("attr1")).isEqualTo("lAttr1");
		assertThat(mav.getModel().get("attr2")).isEqualTo("gAttr2");
		assertThat(mav.getModel().get("attr3")).isNull();
	}

	@Test // gh-15486
	@SuppressWarnings("removal")
	// TODO Migrate from MappingJackson2HttpMessageConverter and MappingJacksonValue to JacksonJsonHttpMessageConverter.
	public void responseBodyAdvice() throws Exception {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		this.handlerAdapter.setMessageConverters(converters);

		this.webAppContext.registerSingleton("rba", ResponseCodeSuppressingAdvice.class);
		this.webAppContext.refresh();

		this.request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		this.request.setParameter("c", "callback");

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handleBadRequest");
		this.handlerAdapter.afterPropertiesSet();
		this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("{\"status\":400,\"message\":\"body\"}");
	}

	@Test // gh-30522
	public void responseBodyAdviceWithEmptyBody() throws Exception {
		this.webAppContext.registerBean("rba", EmptyBodyAdvice.class);
		this.webAppContext.refresh();

		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handleBody", Map.class);
		this.handlerAdapter.afterPropertiesSet();
		this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("Body: {foo=bar}");
	}

	@Test
	void asyncRequestNotUsable() throws Exception {

		// Put AsyncWebRequest in ERROR state
		StandardServletAsyncWebRequest asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		asyncRequest.onError(new AsyncEvent(new MockAsyncContext(this.request, this.response), new Exception()));

		// Set it as the current AsyncWebRequest, from the initial REQUEST dispatch
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(this.request);
		asyncManager.setAsyncWebRequest(asyncRequest);

		// AsyncWebRequest created for current dispatch should inherit state
		HandlerMethod handlerMethod = handlerMethod(new TestController(), "handleOutputStream", OutputStream.class);
		this.handlerAdapter.afterPropertiesSet();

		// Use of response should be rejected
		assertThatThrownBy(() -> this.handlerAdapter.handle(this.request, this.response, handlerMethod))
				.isInstanceOf(AsyncRequestNotUsableException.class);
	}

	private HandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes) throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(handler, method);
	}

	private void assertMethodProcessorCount(int resolverCount, int initBinderResolverCount, int handlerCount) {
		assertThat(this.handlerAdapter.getArgumentResolvers()).hasSize(resolverCount);
		assertThat(this.handlerAdapter.getInitBinderArgumentResolvers()).hasSize(initBinderResolverCount);
		assertThat(this.handlerAdapter.getReturnValueHandlers()).hasSize(handlerCount);
	}


	@SuppressWarnings("unused")
	private static class TestController {

		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr1", "lAttr1");
		}

		public String handle() {
			return null;
		}

		public ResponseEntity<Map<String, String>> handleWithResponseEntity() {
			return new ResponseEntity<>(Collections.singletonMap("foo", "bar"), HttpStatus.OK);
		}

		public ResponseEntity<String> handleBadRequest() {
			return new ResponseEntity<>("body", HttpStatus.BAD_REQUEST);
		}

		@ResponseBody
		public String handleBody(@Nullable @RequestBody Map<String, String> body) {
			return "Body: " + body;
		}

		public void handleOutputStream(OutputStream outputStream) throws IOException {
			outputStream.write("body".getBytes(StandardCharsets.UTF_8));
		}
	}


	@SessionAttributes("attr1")
	private static class SessionAttributeController {

		@SuppressWarnings("unused")
		public void handle() {
		}
	}


	@SuppressWarnings("unused")
	private static class RedirectAttributeController {

		public String handle(Model model) {
			model.addAttribute("someAttr", "someAttrValue");
			return "redirect:/path";
		}
	}


	@ControllerAdvice
	private static class ModelAttributeAdvice {

		@SuppressWarnings("unused")
		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr1", "gAttr1");
			model.addAttribute("attr2", "gAttr2");
			model.addAttribute("instance", this);
		}
	}


	@ControllerAdvice({"org.springframework.web.servlet.mvc.method.annotation", "java.lang"})
	private static class ModelAttributePackageAdvice {

		@SuppressWarnings("unused")
		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr2", "gAttr2");
		}
	}


	@ControllerAdvice("java.lang")
	private static class ModelAttributeNotUsedPackageAdvice {

		@SuppressWarnings("unused")
		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr3", "gAttr3");
		}
	}


	/**
	 * This class additionally implements {@link RequestBodyAdvice} solely for the purpose
	 * of verifying that controller advice implementing both {@link ResponseBodyAdvice}
	 * and {@link RequestBodyAdvice} does not get registered twice.
	 *
	 * @see <a href="https://github.com/spring-projects/spring-framework/pull/22638">gh-22638</a>
	 */
	@ControllerAdvice
	private static class ResponseCodeSuppressingAdvice
			extends AbstractMappingJacksonResponseBodyAdvice implements RequestBodyAdvice {

		@Override
		@SuppressWarnings("removal")
		protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
				MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

			int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
			response.setStatusCode(HttpStatus.OK);

			Map<String, Object> map = new LinkedHashMap<>();
			map.put("status", status);
			map.put("message", bodyContainer.getValue());
			bodyContainer.setValue(map);
		}

		@Override
		public boolean supports(MethodParameter methodParameter, Type targetType,
				Class<? extends HttpMessageConverter<?>> converterType) {

			return StringHttpMessageConverter.class.equals(converterType);
		}

		@Override
		public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return inputMessage;
		}

		@Override
		public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return body;
		}

		@Override
		public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return "default value for empty body";
		}
	}


	@ControllerAdvice
	private static class EmptyBodyAdvice implements RequestBodyAdvice {

		@Override
		public boolean supports(MethodParameter param, Type targetType, Class<? extends HttpMessageConverter<?>> type) {
			return true;
		}

		@Override
		public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter param,
				Type targetType, Class<? extends HttpMessageConverter<?>> type) {

			throw new UnsupportedOperationException();
		}

		@Override
		public Object afterBodyRead(Object body, HttpInputMessage message, MethodParameter param,
				Type targetType, Class<? extends HttpMessageConverter<?>> type) {

			throw new UnsupportedOperationException();
		}

		@Override
		public Object handleEmptyBody(Object body, HttpInputMessage message, MethodParameter param,
				Type targetType, Class<? extends HttpMessageConverter<?>> type) {

			return Maps.of("foo", "bar");
		}
	}

}
