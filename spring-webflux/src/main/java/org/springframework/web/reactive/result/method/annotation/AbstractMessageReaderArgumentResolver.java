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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Abstract base class for argument resolvers that resolve method arguments
 * by reading the request body with an {@link HttpMessageReader}.
 *
 * <p>Applies validation if the method argument is annotated with any
 * {@linkplain org.springframework.validation.annotation.ValidationAnnotationUtils#determineValidationHints
 * annotations that trigger validation}. Validation failure results in a
 * {@link ServerWebInputException}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class AbstractMessageReaderArgumentResolver extends HandlerMethodArgumentResolverSupport {

	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);


	private final List<HttpMessageReader<?>> messageReaders;


	/**
	 * Constructor with {@link HttpMessageReader}'s and a {@link Validator}.
	 * @param readers the readers to convert from the request body
	 */
	protected AbstractMessageReaderArgumentResolver(List<HttpMessageReader<?>> readers) {
		this(readers, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry}.
	 * @param messageReaders readers to convert from the request body
	 * @param adapterRegistry for adapting to other reactive types from Flux and Mono
	 */
	protected AbstractMessageReaderArgumentResolver(
			List<HttpMessageReader<?>> messageReaders, ReactiveAdapterRegistry adapterRegistry) {

		super(adapterRegistry);
		Assert.notEmpty(messageReaders, "At least one HttpMessageReader is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.messageReaders = messageReaders;
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}


	/**
	 * Read the body from a method argument with {@link HttpMessageReader}.
	 * @param bodyParameter the {@link MethodParameter} to read
	 * @param isBodyRequired true if the body is required
	 * @param bindingContext the binding context to use
	 * @param exchange the current exchange
	 * @return the body
	 * @see #readBody(MethodParameter, MethodParameter, boolean, BindingContext, ServerWebExchange)
	 */
	protected Mono<Object> readBody(MethodParameter bodyParameter, boolean isBodyRequired,
			BindingContext bindingContext, ServerWebExchange exchange) {

		return this.readBody(bodyParameter, null, isBodyRequired, bindingContext, exchange);
	}

	/**
	 * Read the body from a method argument with {@link HttpMessageReader}.
	 * @param bodyParam represents the element type for the body
	 * @param actualParam the actual method argument type; possibly different
	 * from {@code bodyParam}, e.g. for an {@code HttpEntity} argument
	 * @param isBodyRequired true if the body is required
	 * @param bindingContext the binding context to use
	 * @param exchange the current exchange
	 * @return a Mono with the value to use for the method argument
	 * @since 5.0.2
	 */
	protected Mono<Object> readBody(MethodParameter bodyParam, @Nullable MethodParameter actualParam,
			boolean isBodyRequired, BindingContext bindingContext, ServerWebExchange exchange) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParam);
		ResolvableType actualType = (actualParam != null ? ResolvableType.forMethodParameter(actualParam) : bodyType);
		Class<?> resolvedType = bodyType.resolve();
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		ResolvableType elementType = (adapter != null ? bodyType.getGeneric() : bodyType);
		isBodyRequired = isBodyRequired || (adapter != null && !adapter.supportsEmpty());

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		MediaType contentType;
		HttpHeaders headers = request.getHeaders();
		try {
			contentType = headers.getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			throw new UnsupportedMediaTypeStatusException(
					"Can't parse Content-Type [" + headers.getFirst("Content-Type") + "]: " + ex.getMessage());
		}

		MediaType mediaType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
		Object[] hints = extractValidationHints(bodyParam);

		if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Form data is accessed via ServerWebExchange.getFormData() in WebFlux.");
			}
			return Mono.error(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + (contentType != null ?
					"Content-Type:" + contentType :
					"No Content-Type, using " + MediaType.APPLICATION_OCTET_STREAM));
		}

		for (HttpMessageReader<?> reader : getMessageReaders()) {
			if (reader.canRead(elementType, mediaType)) {
				Map<String, Object> readHints = Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
				if (adapter != null && adapter.isMultiValue()) {
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..N [" + elementType + "]");
					}
					Flux<?> flux = reader.read(actualType, elementType, request, response, readHints);
					flux = flux.onErrorResume(ex -> Flux.error(handleReadError(bodyParam, ex)));
					if (isBodyRequired) {
						flux = flux.switchIfEmpty(Flux.error(() -> handleMissingBody(bodyParam)));
					}
					if (hints != null) {
						flux = flux.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}
					return Mono.just(adapter.fromPublisher(flux));
				}
				else {
					// Single-value (with or without reactive type wrapper)
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "0..1 [" + elementType + "]");
					}
					Mono<?> mono = reader.readMono(actualType, elementType, request, response, readHints);
					mono = mono.onErrorResume(ex -> Mono.error(handleReadError(bodyParam, ex)));
					if (isBodyRequired) {
						mono = mono.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
					}
					if (hints != null) {
						mono = mono.doOnNext(target ->
								validate(target, hints, bodyParam, bindingContext, exchange));
					}
					return (adapter != null ? Mono.just(adapter.fromPublisher(mono)) : Mono.from(mono));
				}
			}
		}

		// No compatible reader but body may be empty.

		HttpMethod method = request.getMethod();
		if (contentType == null && method != null && SUPPORTED_METHODS.contains(method)) {
			Flux<DataBuffer> body = request.getBody().doOnNext(buffer -> {
				DataBufferUtils.release(buffer);
				// Body not empty, back toy 415..
				throw new UnsupportedMediaTypeStatusException(
						mediaType, getSupportedMediaTypes(elementType), elementType);
			});
			if (isBodyRequired) {
				body = body.switchIfEmpty(Mono.error(() -> handleMissingBody(bodyParam)));
			}
			return (adapter != null ? Mono.just(adapter.fromPublisher(body)) : Mono.from(body));
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(
				mediaType, getSupportedMediaTypes(elementType), elementType));
	}

	private Throwable handleReadError(MethodParameter parameter, Throwable ex) {
		return (ex instanceof DecodingException ?
				new ServerWebInputException("Failed to read HTTP message", parameter, ex) : ex);
	}

	private ServerWebInputException handleMissingBody(MethodParameter parameter) {
		String paramInfo = parameter.getExecutable().toGenericString();
		return new ServerWebInputException("Request body is missing: " + paramInfo, parameter);
	}

	/**
	 * Check if the given MethodParameter requires validation and if so return
	 * a (possibly empty) Object[] with validation hints. A return value of
	 * {@code null} indicates that validation is not required.
	 */
	@Nullable
	private Object[] extractValidationHints(MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Object[] hints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (hints != null) {
				return hints;
			}
		}
		return null;
	}

	private void validate(Object target, Object[] validationHints, MethodParameter param,
			BindingContext binding, ServerWebExchange exchange) {

		String name = Conventions.getVariableNameForParameter(param);
		WebExchangeDataBinder binder = binding.createDataBinder(exchange, target, name);
		binder.validate(validationHints);
		if (binder.getBindingResult().hasErrors()) {
			throw new WebExchangeBindException(param, binder.getBindingResult());
		}
	}

	private List<MediaType> getSupportedMediaTypes(ResolvableType elementType) {
		List<MediaType> mediaTypes = new ArrayList<>();
		for (HttpMessageReader<?> reader : this.messageReaders) {
			mediaTypes.addAll(reader.getReadableMediaTypes(elementType));
		}
		return mediaTypes;
	}

}
