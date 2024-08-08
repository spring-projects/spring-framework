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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.FragmentsRendering;

/**
 * Handler for return values of type:
 * <ul>
 * <li>{@link ResponseBodyEmitter} including sub-class {@link SseEmitter} and others.
 * <li>Reactive return types known to {@link ReactiveAdapterRegistry}.
 * <li>Any of the above wrapped with {@link ResponseEntity}.
 * </ul>
 *
 * <p>Single-value reactive types are adapted to {@link DeferredResult}.
 * Multi-value reactive types are adapted to {@link ResponseBodyEmitter} or
 * {@link SseEmitter} as follows:
 * <ul>
 * <li>SSE stream, if the element type is
 * {@link org.springframework.http.codec.ServerSentEvent} or if negotiated by
 * content type.
 * <li>Text stream for a {@link org.reactivestreams.Publisher} of
 * {@link CharSequence}.
 * <li>A JSON stream if negotiated by content type to
 * {@link MediaType#APPLICATION_NDJSON}.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class ResponseBodyEmitterReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final List<HttpMessageConverter<?>> sseMessageConverters;

	private final ReactiveTypeHandler reactiveHandler;

	private final List<ViewResolver> viewResolvers;

	private final LocaleResolver localeResolver;


	/**
	 * Simple constructor with reactive type support based on a default instance of
	 * {@link ReactiveAdapterRegistry},
	 * {@link org.springframework.core.task.SyncTaskExecutor}, and
	 * {@link ContentNegotiationManager} with an Accept header strategy.
	 */
	public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
		this(messageConverters,
				ReactiveAdapterRegistry.getSharedInstance(), new SyncTaskExecutor(),
				new ContentNegotiationManager());
	}

	/**
	 * Constructor that with added arguments to customize "reactive" type support.
	 * @param messageConverters converters to write emitted objects with
	 * @param registry for reactive return value type support
	 * @param executor for blocking I/O writes of items emitted from reactive types
	 * @param manager for detecting streaming media types
	 * @since 5.0
	 */
	public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters,
			ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {

		this(messageConverters, registry, executor, manager, Collections.emptyList(), null);
	}

	/**
	 * Constructor that with added arguments for view rendering.
	 * @param messageConverters converters to write emitted objects with
	 * @param registry for reactive return value type support
	 * @param executor for blocking I/O writes of items emitted from reactive types
	 * @param manager for detecting streaming media types
	 * @param viewResolvers resolvers for fragment stream rendering
	 * @param localeResolver localeResolver for fragment stream rendering
	 * @since 6.2
	 */
	public ResponseBodyEmitterReturnValueHandler(
			List<HttpMessageConverter<?>> messageConverters,
			ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager,
			List<ViewResolver> viewResolvers, @Nullable LocaleResolver localeResolver) {

		Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
		this.sseMessageConverters = initSseConverters(messageConverters);
		this.reactiveHandler = new ReactiveTypeHandler(registry, executor, manager, null);
		this.viewResolvers = viewResolvers;
		this.localeResolver = (localeResolver != null ? localeResolver : new AcceptHeaderLocaleResolver());
	}

	private static List<HttpMessageConverter<?>> initSseConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter.canWrite(String.class, MediaType.TEXT_PLAIN)) {
				return converters;
			}
		}
		List<HttpMessageConverter<?>> result = new ArrayList<>(converters.size() + 1);
		result.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
		result.addAll(converters);
		return result;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> bodyType = ResponseEntity.class.isAssignableFrom(returnType.getParameterType()) ?
				ResolvableType.forMethodParameter(returnType).getGeneric().resolve() :
				returnType.getParameterType();

		return (bodyType != null && (ResponseBodyEmitter.class.isAssignableFrom(bodyType) ||
				this.reactiveHandler.isReactiveType(bodyType)));
	}

	@Override
	@SuppressWarnings("resource")
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Assert.state(response != null, "No HttpServletResponse");
		ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

		if (returnValue instanceof ResponseEntity<?> responseEntity) {
			response.setStatus(responseEntity.getStatusCode().value());
			outputMessage.getHeaders().putAll(responseEntity.getHeaders());
			returnValue = responseEntity.getBody();
			returnType = returnType.nested();
			if (returnValue == null) {
				mavContainer.setRequestHandled(true);
				outputMessage.flush();
				return;
			}
		}

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(request != null, "No ServletRequest");

		ResponseBodyEmitter emitter;
		if (returnValue instanceof ResponseBodyEmitter responseBodyEmitter) {
			emitter = responseBodyEmitter;
		}
		else {
			emitter = this.reactiveHandler.handleValue(returnValue, returnType, mavContainer, webRequest);
			if (emitter == null) {
				// We're not streaming; write headers without committing response
				outputMessage.getHeaders().forEach((headerName, headerValues) -> {
					for (String headerValue : headerValues) {
						response.addHeader(headerName, headerValue);
					}
				});
				return;
			}
		}
		emitter.extendResponse(outputMessage);

		// We are streaming
		ShallowEtagHeaderFilter.disableContentCaching(request);

		// Suppress header updates from message converters
		outputMessage = new StreamingServletServerHttpResponse(outputMessage);

		DefaultSseEmitterHandler emitterHandler;
		try {
			DeferredResult<?> result = new DeferredResult<>(emitter.getTimeout());
			WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(result, mavContainer);
			FragmentHandler handler = new FragmentHandler(request, response, this.viewResolvers, this.localeResolver);
			emitterHandler = new DefaultSseEmitterHandler(this.sseMessageConverters, handler, outputMessage, result);
		}
		catch (Throwable ex) {
			emitter.initializeWithError(ex);
			throw ex;
		}

		emitter.initialize(emitterHandler);
	}


	/**
	 * Wrap to silently ignore header changes HttpMessageConverter's that would
	 * otherwise cause HttpHeaders to raise exceptions.
	 */
	private static class StreamingServletServerHttpResponse extends DelegatingServerHttpResponse {

		private final HttpHeaders mutableHeaders = new HttpHeaders();

		public StreamingServletServerHttpResponse(ServerHttpResponse delegate) {
			super(delegate);
			this.mutableHeaders.putAll(delegate.getHeaders());
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.mutableHeaders;
		}
	}


	/**
	 * ResponseBodyEmitter.Handler that writes with HttpMessageConverter's.
	 */
	private static final class DefaultSseEmitterHandler implements ResponseBodyEmitter.Handler {

		private final List<HttpMessageConverter<?>> messageConverters;

		private final FragmentHandler fragmentHandler;

		private final ServerHttpResponse outputMessage;

		private final DeferredResult<?> deferredResult;

		public DefaultSseEmitterHandler(
				List<HttpMessageConverter<?>> messageConverters, FragmentHandler fragmentHandler,
				ServerHttpResponse outputMessage, DeferredResult<?> result) {

			this.messageConverters = messageConverters;
			this.fragmentHandler = fragmentHandler;
			this.outputMessage = outputMessage;
			this.deferredResult = result;
		}

		@Override
		public void send(Object data, @Nullable MediaType mediaType) throws IOException {
			sendInternal(data, mediaType);
			this.outputMessage.flush();
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
			for (ResponseBodyEmitter.DataWithMediaType item : items) {
				sendInternal(item.getData(), item.getMediaType());
			}
			this.outputMessage.flush();
		}

		@SuppressWarnings("unchecked")
		private <T> void sendInternal(T data, @Nullable MediaType mediaType) throws IOException {
			if (data instanceof ModelAndView mav) {
				this.fragmentHandler.handle(mav);
				return;
			}
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter.canWrite(data.getClass(), mediaType)) {
					((HttpMessageConverter<T>) converter).write(data, mediaType, this.outputMessage);
					return;
				}
			}
			throw new IllegalArgumentException("No suitable converter for " + data.getClass());
		}

		@Override
		public void complete() {
			try {
				this.outputMessage.flush();
				this.deferredResult.setResult(null);
			}
			catch (IOException ex) {
				this.deferredResult.setErrorResult(ex);
			}
		}

		@Override
		public void completeWithError(Throwable failure) {
			this.deferredResult.setErrorResult(failure);
		}

		@Override
		public void onTimeout(Runnable callback) {
			this.deferredResult.onTimeout(callback);
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
			this.deferredResult.onError(callback);
		}

		@Override
		public void onCompletion(Runnable callback) {
			this.deferredResult.onCompletion(callback);
		}
	}


	/**
	 * Handler that renders ModelAndView fragments via FragmentsRendering.
	 */
	private static final class FragmentHandler {

		private final HttpServletRequest request;

		private final HttpServletResponse response;

		private final List<ViewResolver> viewResolvers;

		private final Locale locale;

		private final Charset charset;

		private final ServletRequestAttributes requestAttributes;

		public FragmentHandler(
				HttpServletRequest request, HttpServletResponse response,
				List<ViewResolver> viewResolvers, LocaleResolver localeResolver) {

			this.request = request;
			this.response = response;
			this.viewResolvers = viewResolvers;
			this.charset = initCharset(response);
			this.locale = localeResolver.resolveLocale(request);
			this.requestAttributes = new ServletWebRequest(this.request);
		}

		private static Charset initCharset(HttpServletResponse response) {
			String s = response.getHeader("Content-Type");
			if (StringUtils.hasText(s)) {
				MediaType contentType = MediaType.valueOf(s);
				if (contentType.getCharset() != null) {
					return contentType.getCharset();
				}
			}
			return StandardCharsets.UTF_8;
		}

		public void handle(ModelAndView modelAndView) throws IOException {
			RequestContextHolder.setRequestAttributes(this.requestAttributes);
			try {
				FragmentHttpServletResponse fragmentResponse =
						new FragmentHttpServletResponse(this.response, this.charset);

				FragmentsRendering render = FragmentsRendering.with(List.of(modelAndView)).build();
				render.resolveNestedViews(this::resolveViewName, this.locale);
				render.render(modelAndView.getModel(), this.request, fragmentResponse);

				byte[] content = fragmentResponse.getFragmentContent();
				this.response.getOutputStream().write(content);
			}
			catch (IOException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to render " + modelAndView, ex);
			}
			finally {
				RequestContextHolder.resetRequestAttributes();
			}
		}

		@Nullable
		public View resolveViewName(String viewName, Locale locale) throws Exception {
			for (ViewResolver resolver : this.viewResolvers) {
				View view = resolver.resolveViewName(viewName, locale);
				if (view != null) {
					return view;
				}
			}
			return null;
		}
	}


	/**
	 * HttpServletResponse wrapper for fragment rendering.
	 * Ignores calls for setting content-type and content-length per fragment.
	 * Caches written content and replaces new lines.
	 */
	private static final class FragmentHttpServletResponse extends HttpServletResponseWrapper {

		private final FragmentServletOutputStream outputStream;

		private final PrintWriter writer;

		private final Charset charset;

		public FragmentHttpServletResponse(HttpServletResponse delegate, Charset charset) {
			super(delegate);
			this.outputStream = new FragmentServletOutputStream();
			this.writer = new PrintWriter(this.outputStream);
			this.charset = charset;
		}

		@Override
		public void setContentType(String type) {
			// ignore
		}

		@Override
		public void setCharacterEncoding(String charset) {
			// ignore
		}

		@Override
		public void setContentLength(int len) {
			// ignore
		}

		@Override
		public ServletOutputStream getOutputStream() {
			return this.outputStream;
		}

		@Override
		public PrintWriter getWriter() {
			return this.writer;
		}

		public byte[] getFragmentContent() {
			this.writer.flush();
			String content = this.outputStream.toString(this.charset);
			content = content.replace("\n", "\ndata:");
			return content.getBytes(this.charset);
		}
	}


	/**
	 * ServletOutputStream that caches written fragment content.
	 */
	private static final class FragmentServletOutputStream extends ServletOutputStream {

		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		@Override
		public void write(int b) {
			this.outputStream.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.outputStream.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			this.outputStream.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException();
		}

		public String toString(Charset charset) {
			return this.outputStream.toString(charset);
		}
	}

}
