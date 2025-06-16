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
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * A base class for resolving method argument values by reading from the body of
 * a request with {@link HttpMessageConverter HttpMessageConverters}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {

	protected enum ConverterType { BASE, GENERIC, SMART };


	private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

	private static final Object NO_VALUE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	protected final List<HttpMessageConverter<?>> messageConverters;

	private final RequestResponseBodyAdviceChain advice;


	/**
	 * Basic constructor with converters only.
	 */
	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
		this(converters, null);
	}

	/**
	 * Constructor with converters and {@code Request~} and {@code ResponseBodyAdvice}.
	 * @since 4.2
	 */
	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters,
			@Nullable List<Object> requestResponseBodyAdvice) {

		Assert.notEmpty(converters, "'messageConverters' must not be empty");
		this.messageConverters = converters;
		this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
	}

	/**
	 * Return the configured {@link RequestBodyAdvice} and
	 * {@link RequestBodyAdvice} where each instance may be wrapped as a
	 * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}.
	 */
	RequestResponseBodyAdviceChain getAdvice() {
		return this.advice;
	}

	/**
	 * Create the method argument value of the expected parameter type by
	 * reading from the given request.
	 * @param webRequest the current request
	 * @param parameter the method parameter descriptor (may be {@code null})
	 * @param paramType the type of the argument value to be created
	 * @return the created method argument value
	 * @throws IOException if the reading from the request fails
	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
	 */
	protected @Nullable Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		HttpInputMessage inputMessage = createInputMessage(webRequest);
		return readWithMessageConverters(inputMessage, parameter, paramType);
	}

	/**
	 * Create the method argument value of the expected parameter type by reading
	 * from the given HttpInputMessage.
	 * @param <T> the expected type of the argument value to be created
	 * @param inputMessage the HTTP input message representing the current request
	 * @param parameter the method parameter descriptor
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, for example, for {@code HttpEntity<String>}.
	 * @return the created method argument value
	 * @throws IOException if the reading from the request fails
	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
	 */
	@SuppressWarnings({"rawtypes", "unchecked", "NullAway"})
	protected <T> @Nullable Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		Class<?> contextClass = parameter.getContainingClass();
		Class<T> targetClass = (targetType instanceof Class clazz ? clazz : null);
		ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
		if (targetClass == null) {
			targetClass = (Class<T>) resolvableType.resolve();
		}

		MediaType contentType;
		boolean noContentType = false;
		try {
			contentType = inputMessage.getHeaders().getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			throw new HttpMediaTypeNotSupportedException(
					ex.getMessage(), getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
		}
		if (contentType == null) {
			noContentType = true;
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		HttpMethod httpMethod = (inputMessage instanceof HttpRequest httpRequest ? httpRequest.getMethod() : null);
		Object body = NO_VALUE;

		EmptyBodyCheckingHttpInputMessage message = null;
		try {
			ResolvableType targetResolvableType = null;
			message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				Class<? extends HttpMessageConverter<?>> converterClass = (Class<? extends HttpMessageConverter<?>>) converter.getClass();
				ConverterType converterTypeToUse = null;
				if (converter instanceof GenericHttpMessageConverter<?> genericConverter) {
					if (genericConverter.canRead(targetType, contextClass, contentType)) {
						converterTypeToUse = ConverterType.GENERIC;
					}
				}
				else if (converter instanceof SmartHttpMessageConverter<?> smartConverter) {
					if (targetResolvableType == null) {
						targetResolvableType = getNestedTypeIfNeeded(resolvableType);
					}
					if (smartConverter.canRead(targetResolvableType, contentType)) {
						converterTypeToUse = ConverterType.SMART;
					}
				}
				else if (targetClass != null && converter.canRead(targetClass, contentType)) {
					converterTypeToUse = ConverterType.BASE;
				}
				if (converterTypeToUse != null) {
					if (message.hasBody()) {
						HttpInputMessage msgToUse = this.advice.beforeBodyRead(message, parameter, targetType, converterClass);
						body = switch (converterTypeToUse) {
							case BASE -> ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse);
							case GENERIC -> ((GenericHttpMessageConverter<?>) converter).read(targetType, contextClass, msgToUse);
							case SMART -> ((SmartHttpMessageConverter<?>) converter).read(targetResolvableType, msgToUse,
									this.advice.determineReadHints(parameter, targetType, (Class<SmartHttpMessageConverter<?>>) converterClass));
						};
						body = this.advice.afterBodyRead(body, msgToUse, parameter, targetType, converterClass);
					}
					else {
						body = this.advice.handleEmptyBody(null, message, parameter, targetType, converterClass);
					}
					break;
				}

			}

			if (body == NO_VALUE && noContentType && !message.hasBody()) {
				body = this.advice.handleEmptyBody(
						null, message, parameter, targetType, NoContentTypeHttpMessageConverter.class);
			}
		}
		catch (IOException ex) {
			throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
		}
		finally {
			if (message != null && message.hasBody()) {
				closeStreamIfNecessary(message.getBody());
			}
		}

		if (body == NO_VALUE) {
			if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) || (noContentType && !message.hasBody())) {
				return null;
			}
			throw new HttpMediaTypeNotSupportedException(contentType,
					getSupportedMediaTypes(targetClass != null ? targetClass : Object.class), httpMethod);
		}

		MediaType selectedContentType = contentType;
		Object theBody = body;
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
			return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
		});

		return body;
	}

	/**
	 * Return the generic type of the {@code returnType} (or of the nested type
	 * if it is an {@link HttpEntity} or/and an {@link Optional}).
	 */
	protected ResolvableType getNestedTypeIfNeeded(ResolvableType type) {
		ResolvableType genericType = type;
		if (Optional.class.isAssignableFrom(genericType.toClass())) {
			genericType = genericType.getNested(2);
		}
		if (HttpEntity.class.isAssignableFrom(genericType.toClass())) {
			genericType = genericType.getNested(2);
		}
		return genericType;
	}


	/**
	 * Create a new {@link HttpInputMessage} from the given {@link NativeWebRequest}.
	 * @param webRequest the web request to create an input message from
	 * @return the input message
	 */
	protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * Validate the binding target if applicable.
	 * <p>The default implementation checks for {@code @jakarta.validation.Valid},
	 * Spring's {@link org.springframework.validation.annotation.Validated},
	 * and custom annotations whose name starts with "Valid".
	 * @param binder the DataBinder to be used
	 * @param parameter the method parameter descriptor
	 * @since 4.1.5
	 * @see #isBindExceptionRequired
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * @param binder the data binder used to perform data binding
	 * @param parameter the method parameter descriptor
	 * @return {@code true} if the next method argument is not of type {@link Errors}
	 * @since 4.1.5
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

	/**
	 * Return the media types supported by all provided message converters sorted
	 * by specificity via {@link MimeTypeUtils#sortBySpecificity(List)}.
	 * @since 5.3.4
	 */
	protected List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		Set<MediaType> mediaTypeSet = new LinkedHashSet<>();
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			mediaTypeSet.addAll(converter.getSupportedMediaTypes(clazz));
		}
		List<MediaType> result = new ArrayList<>(mediaTypeSet);
		MimeTypeUtils.sortBySpecificity(result);
		return result;
	}

	/**
	 * Adapt the given argument against the method parameter, if necessary.
	 * @param arg the resolved argument
	 * @param parameter the method parameter descriptor
	 * @return the adapted argument, or the original resolved argument as-is
	 * @since 4.3.5
	 */
	protected @Nullable Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
		if (parameter.getParameterType() == Optional.class) {
			if (arg == null || (arg instanceof Collection<?> collection && collection.isEmpty()) ||
					(arg instanceof Object[] array && array.length == 0)) {
				return Optional.empty();
			}
			else {
				return Optional.of(arg);
			}
		}
		return arg;
	}

	/**
	 * Allow for closing the body stream if necessary,
	 * for example, for part streams in a multipart request.
	 */
	void closeStreamIfNecessary(InputStream body) {
		// No-op by default: A standard HttpInputMessage exposes the HTTP request stream
		// (ServletRequest#getInputStream), with its lifecycle managed by the container.
	}


	private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;

		private final @Nullable InputStream body;

		public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
			this.headers = inputMessage.getHeaders();
			InputStream inputStream = inputMessage.getBody();
			if (inputStream.markSupported()) {
				inputStream.mark(1);
				this.body = (inputStream.read() != -1 ? inputStream : null);
				inputStream.reset();
			}
			else {
				PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
				int b = pushbackInputStream.read();
				if (b == -1) {
					this.body = null;
				}
				else {
					this.body = pushbackInputStream;
					pushbackInputStream.unread(b);
				}
			}
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() {
			return (this.body != null ? this.body : InputStream.nullInputStream());
		}

		public boolean hasBody() {
			return (this.body != null);
		}
	}


	/**
	 * Placeholder HttpMessageConverter type to pass to RequestBodyAdvice if there
	 * is no content-type and no content. In that case, we may not find a converter,
	 * but RequestBodyAdvice have a chance to provide it via handleEmptyBody.
	 */
	private static class NoContentTypeHttpMessageConverter implements HttpMessageConverter<String> {

		@Override
		public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
			return false;
		}

		@Override
		public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
			return false;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return Collections.emptyList();
		}

		@Override
		public String read(Class<? extends String> clazz, HttpInputMessage inputMessage) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(String s, @Nullable MediaType contentType, HttpOutputMessage outputMessage) {
			throw new UnsupportedOperationException();
		}
	}

}
