/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * A base class for resolving method argument values by reading from the body of
 * a request with {@link HttpMessageConverter}s.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

	private static final Object NO_VALUE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	protected final List<HttpMessageConverter<?>> messageConverters;

	protected final List<MediaType> allSupportedMediaTypes;

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
			List<Object> requestResponseBodyAdvice) {

		Assert.notEmpty(converters, "'messageConverters' must not be empty");
		this.messageConverters = converters;
		this.allSupportedMediaTypes = getAllSupportedMediaTypes(converters);
		this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
	}


	/**
	 * Return the media types supported by all provided message converters sorted
	 * by specificity via {@link MediaType#sortBySpecificity(List)}.
	 */
	private static List<MediaType> getAllSupportedMediaTypes(List<HttpMessageConverter<?>> messageConverters) {
		Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<>();
		for (HttpMessageConverter<?> messageConverter : messageConverters) {
			allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
		}
		List<MediaType> result = new ArrayList<>(allSupportedMediaTypes);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
	}


	/**
	 * Return the configured {@link RequestBodyAdvice} and
	 * {@link RequestBodyAdvice} where each instance may be wrapped as a
	 * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}.
	 */
	protected RequestResponseBodyAdviceChain getAdvice() {
		return this.advice;
	}

	/**
	 * Create the method argument value of the expected parameter type by
	 * reading from the given request.
	 * @param <T> the expected type of the argument value to be created
	 * @param webRequest the current request
	 * @param methodParam the method argument
	 * @param paramType the type of the argument value to be created
	 * @return the created method argument value
	 * @throws IOException if the reading from the request fails
	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
	 */
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter methodParam,
			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		HttpInputMessage inputMessage = createInputMessage(webRequest);
		return readWithMessageConverters(inputMessage, methodParam, paramType);
	}

	/**
	 * Create the method argument value of the expected parameter type by reading
	 * from the given HttpInputMessage.
	 * @param <T> the expected type of the argument value to be created
	 * @param inputMessage the HTTP input message representing the current request
	 * @param param the method parameter descriptor (may be {@code null})
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @return the created method argument value
	 * @throws IOException if the reading from the request fails
	 * @throws HttpMediaTypeNotSupportedException if no suitable message converter is found
	 */
	@SuppressWarnings("unchecked")
	protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter param,
			Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		MediaType contentType;
		boolean noContentType = false;
		try {
			contentType = inputMessage.getHeaders().getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			throw new HttpMediaTypeNotSupportedException(ex.getMessage());
		}
		if (contentType == null) {
			noContentType = true;
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		Class<?> contextClass = (param != null ? param.getContainingClass() : null);
		Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
		if (targetClass == null) {
			ResolvableType resolvableType = (param != null ?
					ResolvableType.forMethodParameter(param) : ResolvableType.forType(targetType));
			targetClass = (Class<T>) resolvableType.resolve();
		}

		HttpMethod httpMethod = ((HttpRequest) inputMessage).getMethod();
		Object body = NO_VALUE;

		try {
			inputMessage = new EmptyBodyCheckingHttpInputMessage(inputMessage);

			for (HttpMessageConverter<?> converter : this.messageConverters) {
				Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
				if (converter instanceof GenericHttpMessageConverter) {
					GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
					if (genericConverter.canRead(targetType, contextClass, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
						}
						if (inputMessage.getBody() != null) {
							inputMessage = getAdvice().beforeBodyRead(inputMessage, param, targetType, converterType);
							body = genericConverter.read(targetType, contextClass, inputMessage);
							body = getAdvice().afterBodyRead(body, inputMessage, param, targetType, converterType);
						}
						else {
							body = null;
							body = getAdvice().handleEmptyBody(body, inputMessage, param, targetType, converterType);
						}
						break;
					}
				}
				else if (targetClass != null) {
					if (converter.canRead(targetClass, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
						}
						if (inputMessage.getBody() != null) {
							inputMessage = getAdvice().beforeBodyRead(inputMessage, param, targetType, converterType);
							body = ((HttpMessageConverter<T>) converter).read(targetClass, inputMessage);
							body = getAdvice().afterBodyRead(body, inputMessage, param, targetType, converterType);
						}
						else {
							body = null;
							body = getAdvice().handleEmptyBody(body, inputMessage, param, targetType, converterType);
						}
						break;
					}
				}
			}
		}
		catch (IOException ex) {
			throw new HttpMessageNotReadableException("Could not read document: " + ex.getMessage(), ex);
		}

		if (body == NO_VALUE) {
			if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) ||
					(noContentType && inputMessage.getBody() == null)) {
				return null;
			}
			throw new HttpMediaTypeNotSupportedException(contentType, this.allSupportedMediaTypes);
		}

		return body;
	}

	/**
	 * Create a new {@link HttpInputMessage} from the given {@link NativeWebRequest}.
	 * @param webRequest the web request to create an input message from
	 * @return the input message
	 */
	protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * Validate the request part if applicable.
	 * <p>The default implementation checks for {@code @javax.validation.Valid},
	 * Spring's {@link org.springframework.validation.annotation.Validated},
	 * and custom annotations whose name starts with "Valid".
	 * @param binder the DataBinder to be used
	 * @param methodParam the method parameter
	 * @see #isBindExceptionRequired
	 * @since 4.1.5
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter methodParam) {
		Annotation[] annotations = methodParam.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * @param binder the data binder used to perform data binding
	 * @param methodParam the method argument
	 * @return {@code true} if the next method argument is not of type {@link Errors}
	 * @since 4.1.5
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter methodParam) {
		int i = methodParam.getParameterIndex();
		Class<?>[] paramTypes = methodParam.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}


	private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;

		private final InputStream body;

		private final HttpMethod method;


		public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
			this.headers = inputMessage.getHeaders();
			InputStream inputStream = inputMessage.getBody();
			if (inputStream == null) {
				this.body = null;
			}
			else if (inputStream.markSupported()) {
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
			this.method = ((HttpRequest) inputMessage).getMethod();
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.body;
		}

		public HttpMethod getMethod() {
			return this.method;
		}
	}

}
