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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;


/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;

	private final List<MediaType> allMediaTypes;

	private final Map<HttpMessageConverter<?>, List<MediaType>> mediaTypesByEncoder;

	private int order = 0; // TODO: should be MAX_VALUE

	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> messageConverters,
			ConversionService service) {
		Assert.notEmpty(messageConverters, "At least one message converter is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.messageConverters = messageConverters;
		this.conversionService = service;
		this.allMediaTypes = getAllMediaTypes(messageConverters);
		this.mediaTypesByEncoder = getMediaTypesByConverter(messageConverters);
	}

	private static List<MediaType> getAllMediaTypes(
			List<HttpMessageConverter<?>> messageConverters) {
		Set<MediaType> set = new LinkedHashSet<>();
		messageConverters.forEach(
				converter -> set.addAll(converter.getWritableMediaTypes()));
		List<MediaType> result = new ArrayList<>(set);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
	}

	private static Map<HttpMessageConverter<?>, List<MediaType>> getMediaTypesByConverter(
			List<HttpMessageConverter<?>> converters) {
		Map<HttpMessageConverter<?>, List<MediaType>> result =
				new HashMap<>(converters.size());
		converters.forEach(converter -> result
				.put(converter, converter.getWritableMediaTypes()));
		return Collections.unmodifiableMap(result);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
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

		Optional<Object> value = result.getReturnValue();
		if (!value.isPresent()) {
			return Mono.empty();
		}

		Publisher<?> publisher;
		ResolvableType elementType;
		ResolvableType returnType = result.getReturnValueType();
		if (this.conversionService.canConvert(returnType.getRawClass(), Publisher.class)) {
			publisher = this.conversionService.convert(value.get(), Publisher.class);
			elementType = returnType.getGeneric(0);
			if (Void.class.equals(elementType.getRawClass())) {
				return Mono.from((Publisher<Void>)publisher);
			}
		}
		else {
			publisher = Mono.just(value.get());
			elementType = returnType;
		}

		List<MediaType> compatibleMediaTypes =
				getCompatibleMediaTypes(exchange.getRequest(), elementType);
		if (compatibleMediaTypes.isEmpty()) {
			return Mono.error(new NotAcceptableStatusException(
					getProducibleMediaTypes(elementType)));
		}

		Optional<MediaType> selectedMediaType = selectBestMediaType(compatibleMediaTypes);

		if (selectedMediaType.isPresent()) {
			HttpMessageConverter<?> converter =
					resolveEncoder(elementType, selectedMediaType.get());
			if (converter != null) {
				ServerHttpResponse response = exchange.getResponse();
				return converter.write((Publisher) publisher, elementType,
						selectedMediaType.get(),
								response);
			}
		}

		return Mono.error(new NotAcceptableStatusException(this.allMediaTypes));
	}

	private List<MediaType> getCompatibleMediaTypes(ServerHttpRequest request,
			ResolvableType elementType) {

		List<MediaType> acceptableMediaTypes = getAcceptableMediaTypes(request);
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(elementType);

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptableMediaType : acceptableMediaTypes) {
			compatibleMediaTypes.addAll(producibleMediaTypes.stream().
					filter(acceptableMediaType::isCompatibleWith).
					map(producibleType -> getMostSpecificMediaType(acceptableMediaType,
							producibleType)).collect(Collectors.toList()));
		}

		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(result);
		return result;
	}

	private List<MediaType> getAcceptableMediaTypes(ServerHttpRequest request) {
		List<MediaType> mediaTypes = request.getHeaders().getAccept();
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	private Optional<MediaType> selectBestMediaType(
			List<MediaType> compatibleMediaTypes) {
		for (MediaType mediaType : compatibleMediaTypes) {
			if (mediaType.isConcrete()) {
				return Optional.of(mediaType);
			}
			else if (mediaType.equals(MediaType.ALL) ||
					mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				return Optional.of(MediaType.APPLICATION_OCTET_STREAM);
			}
		}
		return Optional.empty();
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType type) {
		List<MediaType> result = this.messageConverters.stream()
				.filter(converter -> converter.canWrite(type, null))
				.flatMap(encoder -> this.mediaTypesByEncoder.get(encoder).stream())
				.collect(Collectors.toList());
		if (result.isEmpty()) {
			result.add(MediaType.ALL);
		}

		return result;
	}

	/**
	 * Return the more specific of the acceptable and the producible media types
	 * with the q-value of the former.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptType, produceType) <= 0 ? acceptType : produceType);
	}

	private HttpMessageConverter<?> resolveEncoder(ResolvableType type,
			MediaType mediaType) {
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canWrite(type, mediaType)) {
				return converter;
			}
		}
		return null;
	}

}
