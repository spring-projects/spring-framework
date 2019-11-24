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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture with {@link ServletInvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
public class ServletInvocableHandlerMethodTests {

	private final List<HttpMessageConverter<?>> converters =
			Collections.singletonList(new StringHttpMessageConverter());

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final HandlerMethodReturnValueHandlerComposite returnValueHandlers =
			new HandlerMethodReturnValueHandlerComposite();

	private final ModelAndViewContainer mavContainer = new ModelAndViewContainer();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final ServletWebRequest webRequest = new ServletWebRequest(this.request, this.response);


	@Test
	public void invokeAndHandle_VoidWithResponseStatus() throws Exception {
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "responseStatus");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled()).as("Null return value + @ResponseStatus should result in 'request handled'").isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	public void invokeAndHandle_VoidWithComposedResponseStatus() throws Exception {
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "composedResponseStatus");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled()).as("Null return value + @ComposedResponseStatus should result in 'request handled'").isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	public void invokeAndHandle_VoidWithTypeLevelResponseStatus() throws Exception {
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new ResponseStatusHandler(), "handle");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled()).isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	public void invokeAndHandle_VoidWithHttpServletResponseArgument() throws Exception {
		this.argumentResolvers.addResolver(new ServletResponseMethodArgumentResolver());

		ServletInvocableHandlerMethod handlerMethod =
				getHandlerMethod(new Handler(), "httpServletResponse", HttpServletResponse.class);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled()).as("Null return value + HttpServletResponse arg should result in 'request handled'").isTrue();
	}

	@Test
	public void invokeAndHandle_VoidRequestNotModified() throws Exception {
		this.request.addHeader("If-Modified-Since", 10 * 1000 * 1000);
		int lastModifiedTimestamp = 1000 * 1000;
		this.webRequest.checkNotModified(lastModifiedTimestamp);

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "notModified");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled())
				.as("Null return value + 'not modified' request should result in 'request handled'")
				.isTrue();
	}

	@Test // gh-23775
	public void invokeAndHandle_VoidNotModifiedWithEtag() throws Exception {
		String etag = "\"deadb33f8badf00d\"";
		this.request.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		this.webRequest.checkNotModified(etag);

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "notModified");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled())
				.as("Null return value + 'not modified' request should result in 'request handled'")
				.isTrue();

		assertThat(this.request.getAttribute(ShallowEtagHeaderFilter.class.getName() + ".STREAMING"))
				.isEqualTo(true);
	}

	@Test  // SPR-9159
	public void invokeAndHandle_NotVoidWithResponseStatusAndReason() throws Exception {
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "responseStatusWithReason");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.isRequestHandled()).as("When a status reason w/ used, the request is handled").isTrue();
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(this.response.getErrorMessage()).isEqualTo("400 Bad Request");
	}

	@Test
	public void invokeAndHandle_Exception() throws Exception {
		this.returnValueHandlers.addHandler(new ExceptionRaisingReturnValueHandler());

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "handle");
		assertThatExceptionOfType(HttpMessageNotWritableException.class).isThrownBy(() ->
				handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer));
	}

	@Test
	public void invokeAndHandle_DynamicReturnValue() throws Exception {
		this.argumentResolvers.addResolver(new RequestParamMethodArgumentResolver(null, false));
		this.returnValueHandlers.addHandler(new ViewMethodReturnValueHandler());
		this.returnValueHandlers.addHandler(new ViewNameMethodReturnValueHandler());

		// Invoke without a request parameter (String return value)
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new Handler(), "dynamicReturnValue", String.class);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.getView()).isNotNull();
		assertThat(this.mavContainer.getView().getClass()).isEqualTo(RedirectView.class);

		// Invoke with a request parameter (RedirectView return value)
		this.request.setParameter("param", "value");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.mavContainer.getViewName()).isEqualTo("view");
	}

	@Test
	public void wrapConcurrentResult_MethodLevelResponseBody() throws Exception {
		wrapConcurrentResult_ResponseBody(new MethodLevelResponseBodyHandler(), "bar", String.class);
	}

	@Test
	public void wrapConcurrentResult_MethodLevelResponseBodyEmpty() throws Exception {
		wrapConcurrentResult_ResponseBody(new MethodLevelResponseBodyHandler(), null, String.class);
	}

	@Test
	public void wrapConcurrentResult_TypeLevelResponseBody() throws Exception {
		wrapConcurrentResult_ResponseBody(new TypeLevelResponseBodyHandler(), "bar", String.class);
	}

	@Test
	public void wrapConcurrentResult_TypeLevelResponseBodyEmpty() throws Exception {
		wrapConcurrentResult_ResponseBody(new TypeLevelResponseBodyHandler(), null, String.class);
	}

	@Test
	public void wrapConcurrentResult_DeferredResultSubclass() throws Exception {
		wrapConcurrentResult_ResponseBody(new DeferredResultSubclassHandler(), "bar", String.class);
	}

	@Test
	public void wrapConcurrentResult_DeferredResultSubclassEmpty() throws Exception {
		wrapConcurrentResult_ResponseBody(new DeferredResultSubclassHandler(), null, CustomDeferredResult.class);
	}

	private void wrapConcurrentResult_ResponseBody(Object handler, Object result, Class<?> expectedReturnType)
			throws Exception {

		this.returnValueHandlers.addHandler(new ModelAndViewMethodReturnValueHandler());
		this.returnValueHandlers.addHandler(new RequestResponseBodyMethodProcessor(this.converters));
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(handler, "handle");

		handlerMethod = handlerMethod.wrapConcurrentResult(result);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);
		Object expected = (result != null ? result.toString() : "");
		assertThat(this.response.getContentAsString()).isEqualTo(expected);
		assertThat(handlerMethod.getReturnValueType(result).getParameterType()).isEqualTo(expectedReturnType);
	}

	@Test
	public void wrapConcurrentResult_ResponseEntity() throws Exception {
		this.returnValueHandlers.addHandler(new HttpEntityMethodProcessor(this.converters));
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new ResponseEntityHandler(), "handleDeferred");
		handlerMethod = handlerMethod.wrapConcurrentResult(new ResponseEntity<>("bar", HttpStatus.OK));
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getContentAsString()).isEqualTo("bar");
	}

	@Test  // SPR-12287
	public void wrapConcurrentResult_ResponseEntityNullBody() throws Exception {
		this.returnValueHandlers.addHandler(new HttpEntityMethodProcessor(this.converters));
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new ResponseEntityHandler(), "handleDeferred");
		handlerMethod = handlerMethod.wrapConcurrentResult(new ResponseEntity<>(HttpStatus.OK));
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	@Test
	public void wrapConcurrentResult_ResponseEntityNullReturnValue() throws Exception {
		this.returnValueHandlers.addHandler(new HttpEntityMethodProcessor(this.converters));
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new ResponseEntityHandler(), "handleDeferred");
		handlerMethod = handlerMethod.wrapConcurrentResult(null);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	@Test
	public void wrapConcurrentResult_ResponseBodyEmitter() throws Exception {

		this.returnValueHandlers.addHandler(new ResponseBodyEmitterReturnValueHandler(this.converters));

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new StreamingHandler(), "handleEmitter");
		handlerMethod = handlerMethod.wrapConcurrentResult(null);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	@Test
	public void wrapConcurrentResult_StreamingResponseBody() throws Exception {
		this.returnValueHandlers.addHandler(new StreamingResponseBodyReturnValueHandler());
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new StreamingHandler(), "handleStreamBody");
		handlerMethod = handlerMethod.wrapConcurrentResult(null);
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	@Test
	public void wrapConcurrentResult_CollectedValuesList() throws Exception {
		List<HttpMessageConverter<?>> converters = Collections.singletonList(new MappingJackson2HttpMessageConverter());
		ResolvableType elementType = ResolvableType.forClass(List.class);
		ReactiveTypeHandler.CollectedValuesList result = new ReactiveTypeHandler.CollectedValuesList(elementType);
		result.add(Arrays.asList("foo1", "bar1"));
		result.add(Arrays.asList("foo2", "bar2"));

		ContentNegotiationManager manager = new ContentNegotiationManager();
		this.returnValueHandlers.addHandler(new RequestResponseBodyMethodProcessor(converters, manager));
		ServletInvocableHandlerMethod hm = getHandlerMethod(new MethodLevelResponseBodyHandler(), "handleFluxOfLists");
		hm = hm.wrapConcurrentResult(result);
		hm.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("[[\"foo1\",\"bar1\"],[\"foo2\",\"bar2\"]]");
	}

	@Test // SPR-15478
	public void wrapConcurrentResult_CollectedValuesListWithResponseEntity() throws Exception {
		List<HttpMessageConverter<?>> converters = Collections.singletonList(new MappingJackson2HttpMessageConverter());
		ResolvableType elementType = ResolvableType.forClass(Bar.class);
		ReactiveTypeHandler.CollectedValuesList result = new ReactiveTypeHandler.CollectedValuesList(elementType);
		result.add(new Bar("foo"));
		result.add(new Bar("bar"));

		ContentNegotiationManager manager = new ContentNegotiationManager();
		this.returnValueHandlers.addHandler(new RequestResponseBodyMethodProcessor(converters, manager));
		ServletInvocableHandlerMethod hm = getHandlerMethod(new ResponseEntityHandler(), "handleFlux");
		hm = hm.wrapConcurrentResult(result);
		hm.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("[{\"value\":\"foo\"},{\"value\":\"bar\"}]");
	}

	@Test  // SPR-12287 (16/Oct/14 comments)
	public void responseEntityRawTypeWithNullBody() throws Exception {
		this.returnValueHandlers.addHandler(new HttpEntityMethodProcessor(this.converters));
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod(new ResponseEntityHandler(), "handleRawType");
		handlerMethod.invokeAndHandle(this.webRequest, this.mavContainer);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentAsString()).isEqualTo("");
	}

	private ServletInvocableHandlerMethod getHandlerMethod(Object controller,
			String methodName, Class<?>... argTypes) throws NoSuchMethodException {

		Method method = controller.getClass().getDeclaredMethod(methodName, argTypes);
		ServletInvocableHandlerMethod handlerMethod = new ServletInvocableHandlerMethod(controller, method);
		handlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		handlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
		return handlerMethod;
	}


	@SuppressWarnings("unused")
	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@interface ComposedResponseStatus {

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus responseStatus() default HttpStatus.INTERNAL_SERVER_ERROR;
	}


	@SuppressWarnings("unused")
	private static class Handler {

		public String handle() {
			return "view";
		}

		@ResponseStatus(HttpStatus.BAD_REQUEST)
		public void responseStatus() {
		}

		@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "400 Bad Request")
		public String responseStatusWithReason() {
			return "foo";
		}

		@ComposedResponseStatus(responseStatus = HttpStatus.BAD_REQUEST)
		public void composedResponseStatus() {
		}

		public void httpServletResponse(HttpServletResponse response) {
		}

		public void notModified() {
		}

		public Object dynamicReturnValue(@RequestParam(required=false) String param) {
			return (param != null) ? "view" : new RedirectView("redirectView");
		}
	}


	@SuppressWarnings("unused")
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	private static class ResponseStatusHandler {

		public void handle() { }
	}


	private static class MethodLevelResponseBodyHandler {

		@ResponseBody
		public DeferredResult<String> handle() { return null; }

		// Unusual but legal return type
		// Properly test generic type handling of Flux values collected to a List

		@ResponseBody
		public Flux<List<String>> handleFluxOfLists() { return null; }
	}


	@SuppressWarnings("unused")
	@ResponseBody
	private static class TypeLevelResponseBodyHandler {

		public DeferredResult<String> handle() { return null; }
	}


	private static class DeferredResultSubclassHandler {

		@ResponseBody
		public CustomDeferredResult handle() { return null; }
	}


	private static class CustomDeferredResult extends DeferredResult<String> {
	}


	@SuppressWarnings("unused")
	private static class ResponseEntityHandler {

		public DeferredResult<ResponseEntity<String>> handleDeferred() { return null; }

		public ResponseEntity<Void> handleRawType() { return null; }

		public ResponseEntity<Flux<Bar>> handleFlux() { return null; }
	}


	private static class ExceptionRaisingReturnValueHandler implements HandlerMethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return true;
		}

		@Override
		public void handleReturnValue(Object returnValue, MethodParameter returnType,
				ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
			throw new HttpMessageNotWritableException("oops, can't write");
		}
	}


	@SuppressWarnings("unused")
	private static class StreamingHandler {

		public ResponseBodyEmitter handleEmitter() { return null; }

		public StreamingResponseBody handleStreamBody() { return null; }

	}

	private static class Bar {

		private final String value;

		public Bar(String value) {
			this.value = value;
		}

		@SuppressWarnings("unused")
		public String getValue() {
			return this.value;
		}
	}

}
