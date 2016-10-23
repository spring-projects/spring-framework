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
package org.springframework.web.reactive.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for {@link org.springframework.web.reactive.HandlerResultHandler
 * HandlerResultHandler} implementations that perform content negotiation.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractHandlerResultHandler implements Ordered {

	private static final MediaType MEDIA_TYPE_APPLICATION_ALL = new MediaType("application");


	private final RequestedContentTypeResolver contentTypeResolver;

	private final ReactiveAdapterRegistry adapterRegistry;

	private int order = LOWEST_PRECEDENCE;


	protected AbstractHandlerResultHandler(RequestedContentTypeResolver contentTypeResolver) {
		this(contentTypeResolver, new ReactiveAdapterRegistry());
	}

	protected AbstractHandlerResultHandler(RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		Assert.notNull(contentTypeResolver, "'contentTypeResolver' is required.");
		Assert.notNull(adapterRegistry, "'adapterRegistry' is required.");
		this.contentTypeResolver = contentTypeResolver;
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * Return the configured {@link ReactiveAdapterRegistry}.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}

	/**
	 * Return the configured {@link RequestedContentTypeResolver}.
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * Set the order for this result handler relative to others.
	 * <p>By default set to {@link Ordered#LOWEST_PRECEDENCE}, however see
	 * Javadoc of sub-classes which may change this default.
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * Select the best media type for the current request through a content
	 * negotiation algorithm.
	 * @param exchange the current request
	 * @param producibleTypes the media types that can be produced for the current request
	 * @return the selected media type or {@code null}
	 */
	protected MediaType selectMediaType(ServerWebExchange exchange, List<MediaType> producibleTypes) {

		List<MediaType> acceptableTypes = getAcceptableTypes(exchange);
		producibleTypes = getProducibleTypes(exchange, producibleTypes);

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptable : acceptableTypes) {
			for (MediaType producible : producibleTypes) {
				if (acceptable.isCompatibleWith(producible)) {
					compatibleMediaTypes.add(selectMoreSpecificMediaType(acceptable, producible));
				}
			}
		}

		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(result);

		for (MediaType mediaType : compatibleMediaTypes) {
			if (mediaType.isConcrete()) {
				return mediaType;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION_ALL)) {
				return MediaType.APPLICATION_OCTET_STREAM;
			}
		}

		return null;
	}

	private List<MediaType> getAcceptableTypes(ServerWebExchange exchange) {
		List<MediaType> mediaTypes = getContentTypeResolver().resolveMediaTypes(exchange);
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleTypes(ServerWebExchange exchange, List<MediaType> mediaTypes) {
		Optional<Object> optional = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (optional.isPresent()) {
			Set<MediaType> set = (Set<MediaType>) optional.get();
			return new ArrayList<>(set);
		}
		return mediaTypes;
	}

	private MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		producible = producible.copyQualityValue(acceptable);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptable, producible) <= 0 ? acceptable : producible);
	}

	/**
	 * Optionally set the response status using the information provided by {@code @ResponseStatus}.
	 * @param methodParameter the controller method return parameter
	 * @param exchange the server exchange being handled
	 */
	protected void updateResponseStatus(MethodParameter methodParameter, ServerWebExchange exchange) {
		ResponseStatus annotation = methodParameter.getMethodAnnotation(ResponseStatus.class);
		if (annotation != null) {
			annotation = AnnotationUtils.synthesizeAnnotation(annotation, methodParameter.getMethod());
			exchange.getResponse().setStatusCode(annotation.code());
		}
	}

}
