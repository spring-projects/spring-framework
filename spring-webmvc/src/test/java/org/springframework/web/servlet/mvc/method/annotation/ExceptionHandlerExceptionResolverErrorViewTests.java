/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ErrorResponseViewResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExceptionHandlerExceptionResolver} with an
 * {@link ErrorResponseViewResolver}.
 *
 * @author Brian Clozel
 */
class ExceptionHandlerExceptionResolverErrorViewTests {

	private static final String BROWSER_ACCEPT =
			"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8";

	private static final URI PROBLEM_INSTANCE = URI.create("/orders/42/errors/7f3c2a91");


	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/42");

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final List<ProblemDetail> resolvedDetails = new ArrayList<>();

	private final List<ErrorResponse> resolvedErrorResponses = new ArrayList<>();

	private final ErrorResponseViewResolver viewResolver = (request, problemDetail, errorResponse) -> {
		this.resolvedDetails.add(problemDetail);
		if (errorResponse != null) {
			this.resolvedErrorResponses.add(errorResponse);
		}
		return new ModelAndView("error/" + problemDetail.getStatus(), Map.of("problem", problemDetail));
	};


	@Test
	void browserGetsView() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

		ModelAndView mav = resolve(createResolver(), ex);
		assertThat(mav).isNotNull();
		assertThat(mav.getViewName()).isEqualTo("error/405");
		assertThat(mav.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(mav.getModel()).containsKey("problem");
		assertThat(this.response.getStatus()).isEqualTo(405);
		assertThat(this.response.getHeader(HttpHeaders.ALLOW)).isEqualTo("GET");
		assertThat(this.response.getContentAsString()).isEmpty();
		assertThat(this.resolvedErrorResponses).containsExactly(ex);
	}

	@Test
	void interceptorsAreAppliedBeforeViewResolution() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		ExceptionHandlerExceptionResolver resolver = createResolver();
		resolver.setErrorResponseInterceptors(List.of((detail, errorResponse) -> {
			detail.setInstance(PROBLEM_INSTANCE);
			detail.setProperty("traceId", "123");
		}));

		resolve(resolver, new IllegalStateException());
		assertThat(this.resolvedDetails).singleElement().satisfies(detail -> {
			assertThat(detail.getInstance()).isEqualTo(PROBLEM_INSTANCE);
			assertThat(detail.getProperties()).isEqualTo(Map.of("traceId", "123"));
		});
	}

	@Test
	void applicationInstanceIsUsedForView() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InstanceAdvice.class);

		resolve(createResolver(context), new IllegalStateException());
		assertThat(this.resolvedDetails).singleElement()
				.extracting(ProblemDetail::getInstance).isEqualTo(PROBLEM_INSTANCE);
	}

	@Test
	void serverErrorRenderedAsViewIsRecorded() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		ServerRequestObservationContext context = new ServerRequestObservationContext(this.request, this.response);
		this.request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, context);
		IllegalStateException ex = new IllegalStateException();

		ModelAndView mav = resolve(createResolver(), ex);
		assertThat(mav.getViewName()).isEqualTo("error/500");
		assertThat(context.getError()).isSameAs(ex);
	}

	@Test
	void anyMediaTypeGetsProblemJson() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
		assertProblemJson(resolve(createResolver(), new IllegalStateException()));
	}

	@Test
	void noAcceptHeaderGetsProblemJson() throws Exception {
		assertProblemJson(resolve(createResolver(), new IllegalStateException()));
	}

	@Test
	void jsonGetsProblemJson() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		assertProblemJson(resolve(createResolver(), new IllegalStateException()));
	}

	@Test
	void qualityFactorsAreHonored() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, "text/html;q=0.5, application/json");
		assertProblemJson(resolve(createResolver(), new IllegalStateException()));
	}

	@Test
	void htmlWithLowerQualityWildcard() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, "*/*;q=0.8, text/html");
		assertThat(resolve(createResolver(), new IllegalStateException()).getViewName()).isEqualTo("error/500");
	}

	@Test
	void producibleTypesNotCompatibleWithHtml() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JsonOnlyAdvice.class);

		ModelAndView mav = resolve(createResolver(context), new IllegalStateException());
		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.resolvedDetails).isEmpty();
	}

	@Test
	void viewResolverReturnsNull() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		ExceptionHandlerExceptionResolver resolver = createResolver();
		resolver.setErrorResponseViewResolver((request, problemDetail, errorResponse) -> null);

		ModelAndView mav = resolve(resolver, new IllegalStateException());
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test
	void viewStatusHasPrecedence() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		ExceptionHandlerExceptionResolver resolver = createResolver();
		resolver.setErrorResponseViewResolver((request, problemDetail, errorResponse) ->
				new ModelAndView("error", HttpStatus.SERVICE_UNAVAILABLE));

		ModelAndView mav = resolve(resolver, new IllegalStateException());
		assertThat(mav.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(this.response.getStatus()).isEqualTo(503);
	}

	@Test
	void applicationAdviceReturningProblemDetail() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProblemDetailAdvice.class);

		ModelAndView mav = resolve(createResolver(context), new IllegalStateException());
		assertThat(mav.getViewName()).isEqualTo("error/409");
		assertThat(this.response.getStatus()).isEqualTo(409);
	}

	@Test
	void applicationAdviceReturningResponseEntity() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ResponseEntityAdvice.class);

		ModelAndView mav = resolve(createResolver(context), new IllegalStateException());
		assertThat(mav.getViewName()).isEqualTo("error/422");
		assertThat(mav.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(this.response.getHeader("X-Error")).isEqualTo("true");
	}

	@Test
	void applicationAdviceWithExplicitContentType() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ContentTypeAdvice.class);

		ModelAndView mav = resolve(createResolver(context), new IllegalStateException());
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.resolvedDetails).isEmpty();
	}

	@Test
	void applicationAdviceReturningResponseBody() throws Exception {
		this.request.addHeader(HttpHeaders.ACCEPT, BROWSER_ACCEPT);
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ResponseBodyAdvice.class);

		ModelAndView mav = resolve(createResolver(context), new IllegalStateException());
		assertThat(mav.getViewName()).isEqualTo("error/400");
		assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(this.resolvedErrorResponses).isEmpty();
	}

	private void assertProblemJson(ModelAndView mav) throws Exception {
		assertThat(mav).isNotNull();
		assertThat(mav.isEmpty()).isTrue();
		assertThat(this.response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(this.response.getContentAsString()).contains("\"status\":500");
		assertThat(this.resolvedDetails).isEmpty();
	}

	private ExceptionHandlerExceptionResolver createResolver() {
		return createResolver(refreshed(new StaticApplicationContext()));
	}

	private ExceptionHandlerExceptionResolver createResolver(ApplicationContext context) {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setRenderUnhandledExceptionsAsProblemDetails(true);
		resolver.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
		resolver.setErrorResponseViewResolver(this.viewResolver);
		resolver.setApplicationContext(context);
		return resolver;
	}

	private ModelAndView resolve(ExceptionHandlerExceptionResolver resolver, Exception ex) throws Exception {
		resolver.afterPropertiesSet();
		HandlerMethod handlerMethod = new HandlerMethod(new PlainController(), "handle");
		return resolver.resolveException(this.request, this.response, handlerMethod, ex);
	}

	private static StaticApplicationContext refreshed(StaticApplicationContext context) {
		context.refresh();
		return context;
	}


	@Controller
	static class PlainController {

		public void handle() {
		}
	}

	@ControllerAdvice
	static class JsonOnlyAdvice {

		@ExceptionHandler(produces = MediaType.APPLICATION_JSON_VALUE)
		public ProblemDetail handle(IllegalStateException ex) {
			return ProblemDetail.forStatus(HttpStatus.CONFLICT);
		}
	}

	@ControllerAdvice
	static class ProblemDetailAdvice {

		@ExceptionHandler
		public ProblemDetail handle(IllegalStateException ex) {
			return ProblemDetail.forStatus(HttpStatus.CONFLICT);
		}
	}

	@ControllerAdvice
	static class InstanceAdvice {

		@ExceptionHandler
		public ProblemDetail handle(IllegalStateException ex) {
			ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Order 42 is already cancelled");
			detail.setType(URI.create("https://example.org/problems/order-already-cancelled"));
			detail.setTitle("Order already cancelled");
			detail.setInstance(PROBLEM_INSTANCE);
			return detail;
		}
	}

	@ControllerAdvice
	static class ResponseEntityAdvice {

		@ExceptionHandler
		public ResponseEntity<ProblemDetail> handle(IllegalStateException ex) {
			return ResponseEntity.unprocessableContent().header("X-Error", "true")
					.body(ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT));
		}
	}

	@ControllerAdvice
	static class ContentTypeAdvice {

		@ExceptionHandler
		public ResponseEntity<ProblemDetail> handle(IllegalStateException ex) {
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON)
					.body(ProblemDetail.forStatus(HttpStatus.BAD_REQUEST));
		}
	}

	@ControllerAdvice
	static class ResponseBodyAdvice {

		@ExceptionHandler
		@ResponseBody
		public Object handle(IllegalStateException ex) {
			return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		}
	}

}
