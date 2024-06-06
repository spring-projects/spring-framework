/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ExceptionHandlerExceptionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 * @author Rodolphe Lecocq
 */
@SuppressWarnings("unused")
class ExceptionHandlerExceptionResolverTests {

	//TODO

	private static int DEFAULT_RESOLVER_COUNT;

	private static int DEFAULT_HANDLER_COUNT;

	private ExceptionHandlerExceptionResolver resolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeAll
	static void setupOnce() {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.afterPropertiesSet();
		DEFAULT_RESOLVER_COUNT = resolver.getArgumentResolvers().getResolvers().size();
		DEFAULT_HANDLER_COUNT = resolver.getReturnValueHandlers().getHandlers().size();
	}

	@BeforeEach
	void setup() throws Exception {
		this.resolver = new ExceptionHandlerExceptionResolver();
		this.resolver.setWarnLogCategory(this.resolver.getClass().getName());
		this.request = new MockHttpServletRequest("GET", "/");
		this.request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		this.response = new MockHttpServletResponse();
	}


	@Test
	void nullHandler() {
		Object handler = null;
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handler, null);
		assertThat(mav).as("Exception can be resolved only if there is a HandlerMethod").isNull();
	}

	@Test
	void setCustomArgumentResolvers() {
		HandlerMethodArgumentResolver argumentResolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setCustomArgumentResolvers(Collections.singletonList(argumentResolver));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getArgumentResolvers().getResolvers()).contains(argumentResolver);
		assertMethodProcessorCount(DEFAULT_RESOLVER_COUNT + 1, DEFAULT_HANDLER_COUNT);
	}

	@Test
	void setArgumentResolvers() {
		HandlerMethodArgumentResolver argumentResolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setArgumentResolvers(Collections.singletonList(argumentResolver));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(1, DEFAULT_HANDLER_COUNT);
	}

	@Test
	void setCustomReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
		this.resolver.setCustomReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getReturnValueHandlers().getHandlers()).contains(handler);
		assertMethodProcessorCount(DEFAULT_RESOLVER_COUNT, DEFAULT_HANDLER_COUNT + 1);
	}

	@Test
	void setResponseBodyAdvice() {
		this.resolver.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		assertThat(this.resolver).extracting("responseBodyAdvice").asInstanceOf(LIST).hasSize(1);
		this.resolver.setResponseBodyAdvice(Collections.singletonList(new CustomResponseBodyAdvice()));
		assertThat(this.resolver).extracting("responseBodyAdvice").asInstanceOf(LIST).hasSize(2);
	}

	@Test
	void setReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
		this.resolver.setReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(DEFAULT_RESOLVER_COUNT, 1);
	}

	@Test
	void resolveNoExceptionHandlerForException() throws NoSuchMethodException {
		Exception npe = new NullPointerException();
		HandlerMethod handlerMethod = new HandlerMethod(new IoExceptionController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, npe);

		assertThat(mav).as("NPE should not have been handled").isNull();
	}

	@Test
	void resolveExceptionModelAndView() throws NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
		HandlerMethod handlerMethod = new HandlerMethod(new ModelAndViewController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isFalse();
		assertThat(mav.getViewName()).isEqualTo("errorView");
		assertThat(mav.getModel().get("detail")).isEqualTo("Bad argument");
	}

	@Test
	void resolveExceptionResponseBody() throws UnsupportedEncodingException, NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "IllegalArgumentException");
	}

	@Test  // gh-26317
	void resolveExceptionResponseBodyMatchingCauseLevel2() throws UnsupportedEncodingException, NoSuchMethodException {
		Exception ex = new Exception(new Exception(new IllegalArgumentException()));
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "IllegalArgumentException");
	}

	@Test
	void resolveExceptionResponseWriter() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "IllegalArgumentException");
	}

	@Test  // SPR-13546
	void resolveExceptionModelAtArgument() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getModelMap()).hasSize(1);
		assertThat(mav.getModelMap().get("exceptionClassName")).isEqualTo("IllegalArgumentException");
	}

	@Test  // SPR-14651
	void resolveRedirectAttributesAtArgument() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new RedirectAttributesController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getViewName()).isEqualTo("redirect:/");
		FlashMap flashMap = (FlashMap) this.request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
		assertThat((Object) flashMap).as("output FlashMap should exist").isNotNull();
		assertThat(flashMap.get("exceptionClassName")).isEqualTo("IllegalArgumentException");
	}

	@Test
	void resolveExceptionGlobalHandler() throws Exception {
		loadConfiguration(MyConfig.class);

		IllegalAccessException ex = new IllegalAccessException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "AnotherTestExceptionResolver: IllegalAccessException");
	}

	@Test
	void resolveExceptionGlobalHandlerForHandlerFunction() throws Exception {
		loadConfiguration(MyConfig.class);

		IllegalAccessException ex = new IllegalAccessException();
		HandlerFunction<ServerResponse> handlerFunction = req -> {
			throw new IllegalAccessException();
		};
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerFunction, ex);

		assertExceptionHandledAsBody(mav, "AnotherTestExceptionResolver: IllegalAccessException");
	}

	@Test
	void resolveExceptionGlobalHandlerOrdered() throws Exception {
		loadConfiguration(MyConfig.class);

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "TestExceptionResolver: IllegalStateException");
	}

	@Test  // gh-26317
	void resolveExceptionGlobalHandlerOrderedMatchingCauseLevel2() throws Exception {
		loadConfiguration(MyConfig.class);

		Exception ex = new Exception(new Exception(new IllegalStateException()));
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "TestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-12605
	void resolveExceptionWithHandlerMethodArg() throws Exception {
		loadConfiguration(MyConfig.class);

		ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "HandlerMethod: handle");
	}

	@Test
	void resolveExceptionWithAssertionError() throws Exception {
		loadConfiguration(MyConfig.class);

		AssertionError err = new AssertionError("argh");
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod,
				new ServletException("Handler dispatch failed", err));

		assertExceptionHandledAsBody(mav, err.toString());
	}

	@Test
	void resolveExceptionWithAssertionErrorAsRootCause() throws Exception {
		loadConfiguration(MyConfig.class);

		AssertionError rootCause = new AssertionError("argh");
		FatalBeanException cause = new FatalBeanException("wrapped", rootCause);
		Exception ex = new Exception(cause);  // gh-26317
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, rootCause.toString());
	}

	@Test //gh-27156
	void resolveExceptionWithReasonResolvedByMessageSource() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		StaticApplicationContext context = new StaticApplicationContext(ctx);
		Locale locale = Locale.ENGLISH;
		context.addMessage("gateway.timeout", locale, "Gateway Timeout");
		context.refresh();
		LocaleContextHolder.setLocale(locale);
		this.resolver.setApplicationContext(context);
		this.resolver.afterPropertiesSet();

		SocketTimeoutException ex = new SocketTimeoutException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "");
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
		assertThat(this.response.getErrorMessage()).isEqualTo("Gateway Timeout");
	}

	@Test
	void resolveExceptionControllerAdviceHandler() throws Exception {
		loadConfiguration(MyControllerAdviceConfig.class);

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test  // gh-26317
	void resolveExceptionControllerAdviceHandlerMatchingCauseLevel2() throws Exception {
		loadConfiguration(MyControllerAdviceConfig.class);

		Exception ex = new Exception(new IllegalStateException());
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test
	void resolveExceptionControllerAdviceNoHandler() throws Exception {
		loadConfiguration(MyControllerAdviceConfig.class);

		IllegalStateException ex = new IllegalStateException();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, null, ex);

		assertExceptionHandledAsBody(mav, "DefaultTestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-16496
	void resolveExceptionControllerAdviceAgainstProxy() throws Exception {
		loadConfiguration(MyControllerAdviceConfig.class);

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test // gh-22619
	void resolveExceptionViaMappedHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setMappedHandlerClasses(HttpRequestHandler.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handler, ex);

		assertExceptionHandledAsBody(mav, "DefaultTestExceptionResolver: IllegalStateException");
	}

	@Test // gh-26772
	void resolveExceptionViaMappedHandlerPredicate() throws Exception {
		Object handler = new Object();

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setMappedHandlerPredicate(h -> h == handler);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		ModelAndView mav = this.resolver.resolveException(
				this.request, this.response, handler, new IllegalStateException());

		assertExceptionHandledAsBody(mav, "DefaultTestExceptionResolver: IllegalStateException");
	}

	@Test
	void resolveExceptionAsyncRequestNotUsable() throws Exception {
		HttpServletResponse response = mock();
		given(response.getOutputStream()).willThrow(new AsyncRequestNotUsableException("Simulated I/O failure"));

		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
	}

	@Test
	void resolveExceptionJsonMediaType() throws UnsupportedEncodingException, NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
		this.resolver.afterPropertiesSet();
		this.request.addHeader("Accept", "application/json");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertExceptionHandledAsBody(mav, "jsonBody");
	}

	@Test
	void resolveExceptionHtmlMediaType() throws NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
		this.resolver.afterPropertiesSet();
		this.request.addHeader("Accept", "text/html");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getViewName()).isEqualTo("htmlView");
	}

	@Test
	void resolveExceptionDefaultMediaType() throws NoSuchMethodException {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
		this.resolver.afterPropertiesSet();
		this.request.addHeader("Accept", "*/*");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getViewName()).isEqualTo("htmlView");
	}


	private void assertMethodProcessorCount(int resolverCount, int handlerCount) {
		assertThat(this.resolver.getArgumentResolvers().getResolvers()).hasSize(resolverCount);
		assertThat(this.resolver.getReturnValueHandlers().getHandlers()).hasSize(handlerCount);
	}

	private void assertExceptionHandledAsBody(ModelAndView mav, String expectedBody) throws UnsupportedEncodingException {
		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo(expectedBody);
	}

	private void loadConfiguration(Class<?> configClass) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(configClass);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();
	}


	@Controller
	static class ModelAndViewController {

		public void handle() {}

		@ExceptionHandler
		public ModelAndView handle(Exception ex) throws IOException {
			return new ModelAndView("errorView", "detail", ex.getMessage());
		}
	}


	@Controller
	static class ResponseWriterController {

		public void handle() {}

		@ExceptionHandler
		public void handleException(Exception ex, Writer writer) throws IOException {
			writer.write(ClassUtils.getShortName(ex.getClass()));
		}
	}


	interface ResponseBodyInterface {

		void handle();

		@ExceptionHandler
		@ResponseBody
		String handleException(IllegalArgumentException ex);
	}


	@Controller
	static class ResponseBodyController extends WebApplicationObjectSupport implements ResponseBodyInterface {

		@Override
		public void handle() {}

		@Override
		@ExceptionHandler
		@ResponseBody
		public String handleException(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	static class IoExceptionController {

		public void handle() {}

		@ExceptionHandler(value = IOException.class)
		public void handleException() {
		}
	}


	@Controller
	static class ModelArgumentController {

		public void handle() {}

		@ExceptionHandler
		public void handleException(Exception ex, Model model) {
			model.addAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
		}
	}

	@Controller
	static class RedirectAttributesController {

		public void handle() {}

		@ExceptionHandler
		public String handleException(Exception ex, RedirectAttributes redirectAttributes) {
			redirectAttributes.addFlashAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
			return "redirect:/";
		}
	}


	@RestControllerAdvice
	@Order(1)
	static class TestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "TestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler(ArrayIndexOutOfBoundsException.class)
		public String handleWithHandlerMethod(HandlerMethod handlerMethod) {
			return "HandlerMethod: " + handlerMethod.getMethod().getName();
		}

		@ExceptionHandler(AssertionError.class)
		public String handleAssertionError(Error err) {
			return err.toString();
		}
	}


	@RestControllerAdvice
	@Order(2)
	static class AnotherTestExceptionResolver {

		@ExceptionHandler({IllegalStateException.class, IllegalAccessException.class})
		public String handleException(Exception ex) {
			return "AnotherTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}

	@RestControllerAdvice
	@Order(3)
	static class ResponseStatusTestExceptionResolver {

		@ExceptionHandler(SocketTimeoutException.class)
		@ResponseStatus(code = HttpStatus.GATEWAY_TIMEOUT, reason = "gateway.timeout")
		public void handleException(SocketTimeoutException ex) {

		}
	}

	@Configuration
	static class MyConfig {

		@Bean
		public TestExceptionResolver testExceptionResolver() {
			return new TestExceptionResolver();
		}

		@Bean
		public AnotherTestExceptionResolver anotherTestExceptionResolver() {
			return new AnotherTestExceptionResolver();
		}

		@Bean
		public ResponseStatusTestExceptionResolver responseStatusTestExceptionResolver() {
			return new ResponseStatusTestExceptionResolver();
		}
	}


	@RestControllerAdvice("java.lang")
	@Order(1)
	static class NotCalledTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "NotCalledTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@RestControllerAdvice(assignableTypes = WebApplicationObjectSupport.class)
	@Order(2)
	static class BasePackageTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "BasePackageTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@RestControllerAdvice
	@Order(3)
	static class DefaultTestExceptionResolver {

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "DefaultTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
		}
	}


	@Configuration
	static class MyControllerAdviceConfig {

		@Bean
		public NotCalledTestExceptionResolver notCalledTestExceptionResolver() {
			return new NotCalledTestExceptionResolver();
		}

		@Bean
		public BasePackageTestExceptionResolver basePackageTestExceptionResolver() {
			return new BasePackageTestExceptionResolver();
		}

		@Bean
		public DefaultTestExceptionResolver defaultTestExceptionResolver() {
			return new DefaultTestExceptionResolver();
		}
	}

	static class CustomResponseBodyAdvice implements ResponseBodyAdvice<Object> {

		@Override
		public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
			return false;
		}

		@Override
		public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
				Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
			return null;
		}
	}

	@Controller
	static class MediaTypeController {

		public void handle() {}

		@ExceptionHandler(exception = IllegalArgumentException.class, produces = "application/json")
		@ResponseBody
		public String handleExceptionJson() {
			return "jsonBody";
		}

		@ExceptionHandler(exception = IllegalArgumentException.class, produces = {"text/html", "*/*"})
		public String handleExceptionHtml() {
			return "htmlView";
		}
	}

}
