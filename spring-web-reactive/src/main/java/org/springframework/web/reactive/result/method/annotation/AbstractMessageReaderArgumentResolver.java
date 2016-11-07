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
package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerHttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebExchangeDataBinder;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.bind.WebExchangeBindException;

/**
 * Abstract base class for argument resolvers that resolve method arguments
 * by reading the request body with an {@link HttpMessageReader}.
 *
 * <p>Applies validation if the method argument is annotated with
 * {@code @javax.validation.Valid} or
 * {@link org.springframework.validation.annotation.Validated}. Validation
 * failure results in an {@link ServerWebInputException}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractMessageReaderArgumentResolver {

	private final List<HttpMessageReader<?>> messageReaders;

	private final ReactiveAdapterRegistry adapterRegistry;

	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor with {@link HttpMessageReader}'s and a {@link Validator}.
	 * @param readers readers to convert from the request body
	 */
	protected AbstractMessageReaderArgumentResolver(List<HttpMessageReader<?>> readers) {
		this(readers, new ReactiveAdapterRegistry());
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry}.
	 * @param messageReaders readers to convert from the request body
	 * @param adapterRegistry for adapting to other reactive types from Flux and Mono
	 */
	protected AbstractMessageReaderArgumentResolver(List<HttpMessageReader<?>> messageReaders,
			ReactiveAdapterRegistry adapterRegistry) {

		Assert.notEmpty(messageReaders, "At least one HttpMessageReader is required.");
		Assert.notNull(adapterRegistry, "'adapterRegistry' is required");
		this.messageReaders = messageReaders;
		this.adapterRegistry = adapterRegistry;
		this.supportedMediaTypes = messageReaders.stream()
				.flatMap(converter -> converter.getReadableMediaTypes().stream())
				.collect(Collectors.toList());
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}

	/**
	 * Return the configured {@link ReactiveAdapterRegistry}.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	protected Mono<Object> readBody(MethodParameter bodyParameter, boolean isBodyRequired,
			BindingContext bindingContext, ServerWebExchange exchange) {

		ResolvableType bodyType = ResolvableType.forMethodParameter(bodyParameter);
		ReactiveAdapter adapter = getAdapterRegistry().getAdapterTo(bodyType.resolve());

		ResolvableType elementType = ResolvableType.forMethodParameter(bodyParameter);
		if (adapter != null) {
			elementType = elementType.getGeneric(0);
		}

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		MediaType mediaType = request.getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		for (HttpMessageReader<?> reader : getMessageReaders()) {

			if (reader.canRead(elementType, mediaType)) {
				Map<String, Object> readHints = Collections.emptyMap();
				if (adapter != null && adapter.getDescriptor().isMultiValue()) {
					Flux<?> flux;
					if (reader instanceof ServerHttpMessageReader) {
						ServerHttpMessageReader<?> serverReader = ((ServerHttpMessageReader<?>) reader);
						flux = serverReader.read(bodyType, elementType, request, response, readHints);
					}
					else {
						flux = reader.read(elementType, request, readHints);
					}
					flux = flux.onErrorResumeWith(ex -> Flux.error(wrapReadError(ex, bodyParameter)));
					if (checkRequired(adapter, isBodyRequired)) {
						flux = flux.switchIfEmpty(Flux.error(getRequiredBodyError(bodyParameter)));
					}
					Object[] hints = extractValidationHints(bodyParameter);
					if (hints != null) {
						flux = flux.doOnNext(target ->
								validate(target, hints, bodyParameter, bindingContext, exchange));
					}
					return Mono.just(adapter.fromPublisher(flux));
				}
				else {
					Mono<?> mono;
					if (reader instanceof ServerHttpMessageReader) {
						ServerHttpMessageReader<?> serverReader = (ServerHttpMessageReader<?>) reader;
						mono = serverReader.readMono(bodyType, elementType, request, response, readHints);
					}
					else {
						mono = reader.readMono(elementType, request, readHints);
					}
					mono = mono.otherwise(ex -> Mono.error(wrapReadError(ex, bodyParameter)));
					if (checkRequired(adapter, isBodyRequired)) {
						mono = mono.otherwiseIfEmpty(Mono.error(getRequiredBodyError(bodyParameter)));
					}
					Object[] hints = extractValidationHints(bodyParameter);
					if (hints != null) {
						mono = mono.doOnNext(target ->
								validate(target, hints, bodyParameter, bindingContext, exchange));
					}
					if (adapter != null) {
						return Mono.just(adapter.fromPublisher(mono));
					}
					else {
						return Mono.from(mono);
					}
				}
			}
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(mediaType, this.supportedMediaTypes));
	}

	protected ServerWebInputException wrapReadError(Throwable ex, MethodParameter parameter) {
		return new ServerWebInputException("Failed to read HTTP message", parameter, ex);
	}

	protected boolean checkRequired(ReactiveAdapter adapter, boolean isBodyRequired) {
		return adapter != null && !adapter.getDescriptor().supportsEmpty() || isBodyRequired;
	}

	protected ServerWebInputException getRequiredBodyError(MethodParameter parameter) {
		return new ServerWebInputException("Required request body is missing: " +
				parameter.getMethod().toGenericString());
	}

	/**
	 * Check if the given MethodParameter requires validation and if so return
	 * a (possibly empty) Object[] with validation hints. A return value of
	 * {@code null} indicates that validation is not required.
	 */
	protected Object[] extractValidationHints(MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validAnnot = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validAnnot != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validAnnot != null ? validAnnot.value() : AnnotationUtils.getValue(ann));
				return (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
			}
		}
		return null;
	}

	protected void validate(Object target, Object[] validationHints,
			MethodParameter param, BindingContext binding, ServerWebExchange exchange) {

		String name = Conventions.getVariableNameForParameter(param);
		WebExchangeDataBinder binder = binding.createDataBinder(exchange, target, name);
		binder.validate(validationHints);
		if (binder.getBindingResult().hasErrors()) {
			throw new WebExchangeBindException(param, binder.getBindingResult());
		}
	}

}
