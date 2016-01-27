/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.method.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.codec.Encoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;


/**
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private final List<Encoder<?>> encoders;

	private final ConversionService conversionService;

	private final List<MediaType> allMediaTypes;

	private final Map<Encoder<?>, List<MediaType>> mediaTypesByEncoder;

	private int order = 0;


	public ResponseBodyResultHandler(List<Encoder<?>> encoders, ConversionService service) {
		Assert.notEmpty(encoders, "At least one encoders is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.encoders = encoders;
		this.conversionService = service;
		this.allMediaTypes = getAllMediaTypes(encoders);
		this.mediaTypesByEncoder = getMediaTypesByEncoder(encoders);
	}

	private static List<MediaType> getAllMediaTypes(List<Encoder<?>> encoders) {
		Set<MediaType> set = new LinkedHashSet<>();
		encoders.forEach(encoder -> set.addAll(toMediaTypes(encoder.getSupportedMimeTypes())));
		List<MediaType> result = new ArrayList<>(set);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
	}

	private static Map<Encoder<?>, List<MediaType>> getMediaTypesByEncoder(List<Encoder<?>> encoders) {
		Map<Encoder<?>, List<MediaType>> result = new HashMap<>(encoders.size());
		encoders.forEach(encoder -> result.put(encoder, toMediaTypes(encoder.getSupportedMimeTypes())));
		return Collections.unmodifiableMap(result);
	}

	/**
	 * TODO: MediaType static method
	 */
	private static List<MediaType> toMediaTypes(List<MimeType> mimeTypes) {
		return mimeTypes.stream().map(ResponseBodyResultHandler::toMediaType).collect(Collectors.toList());
	}

	/**
	 * TODO: MediaType constructor
	 */
	private static MediaType toMediaType(MimeType mimeType) {
		return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
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

		Object value = result.getResult();
		if (value == null) {
			return Mono.empty();
		}

		Publisher<?> publisher;
		ResolvableType elementType;
		ResolvableType returnType = result.getResultType();
		if (this.conversionService.canConvert(returnType.getRawClass(), Publisher.class)) {
			publisher = this.conversionService.convert(value, Publisher.class);
			elementType = returnType.getGeneric(0);
			if (Void.class.equals(elementType.getRawClass())) {
				return (Mono<Void>)Mono.from(publisher);
			}
		}
		else {
			publisher = Mono.just(value);
			elementType = returnType;
		}

		List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(exchange.getRequest());
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(elementType);

		if (producibleMediaTypes.isEmpty()) {
			producibleMediaTypes.add(MediaType.ALL);
		}

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType requestedType : requestedMediaTypes) {
			for (MediaType producibleType : producibleMediaTypes) {
				if (requestedType.isCompatibleWith(producibleType)) {
					compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
				}
			}
		}
		if (compatibleMediaTypes.isEmpty()) {
			return Mono.error(new HttpMediaTypeNotAcceptableException(producibleMediaTypes));
		}

		List<MediaType> mediaTypes = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(mediaTypes);

		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.isConcrete()) {
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selectedMediaType != null) {
			Encoder<?> encoder = resolveEncoder(elementType, selectedMediaType);
			if (encoder != null) {
				ServerHttpResponse response = exchange.getResponse();
				response.getHeaders().setContentType(selectedMediaType);
				return response.setBody(encoder.encode((Publisher) publisher, elementType, selectedMediaType));
			}
		}

		return Mono.error(new HttpMediaTypeNotAcceptableException(this.allMediaTypes));
	}

	private List<MediaType> getAcceptableMediaTypes(ServerHttpRequest request) {
		List<MediaType> mediaTypes = request.getHeaders().getAccept();
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	private List<MediaType> getProducibleMediaTypes(ResolvableType type) {
		return this.encoders.stream()
				.filter(encoder -> encoder.canEncode(type, null))
				.flatMap(encoder -> this.mediaTypesByEncoder.get(encoder).stream())
				.collect(Collectors.toList());
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

	private Encoder<?> resolveEncoder(ResolvableType type, MediaType mediaType, Object... hints) {
		for (Encoder<?> encoder : this.encoders) {
			if (encoder.canEncode(type, mediaType, hints)) {
				return encoder;
			}
		}
		return null;
	}

}
