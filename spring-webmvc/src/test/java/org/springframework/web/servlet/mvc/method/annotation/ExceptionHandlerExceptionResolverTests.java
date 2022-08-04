/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ExceptionHandlerExceptionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 * @author Brian Clozel
 * @author Rodolphe Lecocq
 * @since 3.1
 */
@SuppressWarnings("unused")
public class ExceptionHandlerExceptionResolverTests {

	private static int RESOLVER_COUNT;

	private static int HANDLER_COUNT;

	private ExceptionHandlerExceptionResolver resolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeAll
	public static void setupOnce() {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.afterPropertiesSet();
		RESOLVER_COUNT = resolver.getArgumentResolvers().getResolvers().size();
		HANDLER_COUNT = resolver.getReturnValueHandlers().getHandlers().size();
	}

	@BeforeEach
	public void setup() throws Exception {
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
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setCustomArgumentResolvers(Collections.singletonList(resolver));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getArgumentResolvers().getResolvers().contains(resolver)).isTrue();
		assertMethodProcessorCount(RESOLVER_COUNT + 1, HANDLER_COUNT);
	}

	@Test
	void setArgumentResolvers() {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.resolver.setArgumentResolvers(Collections.singletonList(resolver));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(1, HANDLER_COUNT);
	}

	@Test
	void setCustomReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
		this.resolver.setCustomReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertThat(this.resolver.getReturnValueHandlers().getHandlers().contains(handler)).isTrue();
		assertMethodProcessorCount(RESOLVER_COUNT, HANDLER_COUNT + 1);
	}

	@Test
	void setResponseBodyAdvice() {
		this.resolver.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		assertThat(this.resolver).extracting("responseBodyAdvice").asList().hasSize(1);
		this.resolver.setResponseBodyAdvice(Collections.singletonList(new CustomResponseBodyAdvice()));
		assertThat(this.resolver).extracting("responseBodyAdvice").asList().hasSize(2);
	}

	@Test
	void setReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
		this.resolver.setReturnValueHandlers(Collections.singletonList(handler));
		this.resolver.afterPropertiesSet();

		assertMethodProcessorCount(RESOLVER_COUNT, 1);
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

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
	}

	@Test  // gh-26317
	void resolveExceptionResponseBodyMatchingCauseLevel2() throws UnsupportedEncodingException, NoSuchMethodException {
		Exception ex = new Exception(new Exception(new IllegalArgumentException()));
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
	}

	@Test
	void resolveExceptionResponseWriter() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
	}

	@Test  // SPR-13546
	void resolveExceptionModelAtArgument() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
		this.resolver.afterPropertiesSet();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).isNotNull();
		assertThat(mav.getModelMap().size()).isEqualTo(1);
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
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalAccessException ex = new IllegalAccessException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
	}

	@Test
	void resolveExceptionGlobalHandlerOrdered() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("TestExceptionResolver: IllegalStateException");
	}

	@Test  // gh-26317
	void resolveExceptionGlobalHandlerOrderedMatchingCauseLevel2() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		Exception ex = new Exception(new Exception(new IllegalStateException()));
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("TestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-12605
	void resolveExceptionWithHandlerMethodArg() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("HandlerMethod: handle");
	}

	@Test
	void resolveExceptionWithAssertionError() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		AssertionError err = new AssertionError("argh");
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod,
				new NestedServletException("Handler dispatch failed", err));

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo(err.toString());
	}

	@Test
	void resolveExceptionWithAssertionErrorAsRootCause() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		AssertionError rootCause = new AssertionError("argh");
		FatalBeanException cause = new FatalBeanException("wrapped", rootCause);
		Exception ex = new Exception(cause);  // gh-26317
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo(rootCause.toString());
	}

	@Test //gh-27156
	void resolveExceptionWithReasonResovledByMessageSource() throws Exception {
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

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
		assertThat(this.response.getErrorMessage()).isEqualTo("Gateway Timeout");
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	@Test
	void resolveExceptionControllerAdviceHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test  // gh-26317
	void resolveExceptionControllerAdviceHandlerMatchingCauseLevel2() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		Exception ex = new Exception(new IllegalStateException());
		HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
	}

	@Test
	void resolveExceptionControllerAdviceNoHandler() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, null, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
	}

	@Test  // SPR-16496
	void resolveExceptionControllerAdviceAgainstProxy() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
		this.resolver.setApplicationContext(ctx);
		this.resolver.afterPropertiesSet();

		IllegalStateException ex = new IllegalStateException();
		HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
		ModelAndView mav = this.resolver.resolveException(this.request, this.response, handlerMethod, ex);

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
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

		assertThat(mav).as("Exception was not handled").isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
	}


	private void assertMethodProcessorCount(int resolverCount, int handlerCount) {
		assertThat(this.resolver.getArgumentResolvers().getResolvers().size()).isEqualTo(resolverCount);
		assertThat(this.resolver.getReturnValueHandlers().getHandlers().size()).isEqualTo(handlerCount);
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

}
