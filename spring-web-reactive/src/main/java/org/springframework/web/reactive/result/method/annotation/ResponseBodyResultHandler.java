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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.ContentNegotiatingResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;


/**
 * {@code HandlerResultHandler} that handles return values from methods annotated
 * with {@code @ResponseBody} writing to the body of the request or response with
 * an {@link HttpMessageConverter}.
 *
 * <p>By default the order for the result handler is set to 0. It is generally
 * safe and expected it will be ordered ahead of other result handlers since it
 * only gets involved based on the presence of an {@code @ResponseBody}
 * annotation.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class ResponseBodyResultHandler extends ContentNegotiatingResultHandlerSupport
		implements HandlerResultHandler, Ordered {

	private final List<HttpMessageConverter<?>> messageConverters;


	/**
	 * Constructor with message converters and a {@code ConversionService} only
	 * and creating a {@link HeaderContentTypeResolver}, i.e. using Accept header
	 * to determine the requested content type.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService) {

		this(converters, conversionService, new HeaderContentTypeResolver());
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting other reactive types (e.g.
	 * rx.Observable, rx.Single, etc.) to Flux or Mono
	 * @param contentTypeResolver for resolving the requested content type
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		super(conversionService, contentTypeResolver);
		Assert.notEmpty(converters, "At least one message converter is required.");
		this.messageConverters = converters;
		setOrder(0);
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			MethodParameter returnType = ((HandlerMethod) handler).getReturnType();
			Class<?> containingClass = returnType.getContainingClass();
			return (AnnotationUtils.findAnnotation(containingClass, ResponseBody.class) != null ||
					returnType.getMethodAnnotation(ResponseBody.class) != null);
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Publisher<?> publisher;
		ResolvableType elementType;
		ResolvableType returnType = result.getReturnValueType();

		if (getConversionService().canConvert(returnType.getRawClass(), Publisher.class)) {
			Optional<Object> optionalValue = result.getReturnValue();
			if (optionalValue.isPresent()) {
				publisher = getConversionService().convert(optionalValue.get(), Publisher.class);
			}
			else {
				publisher = Mono.empty();
			}
			elementType = returnType.getGeneric(0);
			if (Void.class.equals(elementType.getRawClass())) {
				return Mono.from((Publisher<Void>)publisher);
			}
		}
		else {
			publisher = Mono.justOrEmpty(result.getReturnValue());
			elementType = returnType;
		}

		List<MediaType> producibleTypes = getProducibleMediaTypes(elementType);
		MediaType bestMediaType = selectMediaType(exchange, producibleTypes);

		if (bestMediaType != null) {
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter.canWrite(elementType, bestMediaType)) {
					ServerHttpResponse response = exchange.getResponse();
					return converter.write((Publisher) publisher, elementType, bestMediaType, response);
				}
			}
		}

		return Mono.error(new NotAcceptableStatusException(producibleTypes));
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType type) {
		return this.messageConverters.stream()
				.filter(converter -> converter.canWrite(type, null))
				.flatMap(converter -> converter.getWritableMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
