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

package org.springframework.web.reactive.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for {@link org.springframework.web.reactive.HandlerResultHandler
 * HandlerResultHandler} with support for content negotiation and access to a
 * {@code ReactiveAdapter} registry.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class HandlerResultHandlerSupport implements Ordered {

	private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES =
			Arrays.asList(MediaType.ALL, new MediaType("application"));


	protected final Log logger = LogFactory.getLog(getClass());

	private final RequestedContentTypeResolver contentTypeResolver;

	private final ReactiveAdapterRegistry adapterRegistry;

	private int order = LOWEST_PRECEDENCE;


	protected HandlerResultHandlerSupport(RequestedContentTypeResolver contentTypeResolver,
			ReactiveAdapterRegistry adapterRegistry) {

		Assert.notNull(contentTypeResolver, "RequestedContentTypeResolver is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
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
	 * Get a {@code ReactiveAdapter} for the top-level return value type.
	 * @return the matching adapter, or {@code null} if none
	 */
	@Nullable
	protected ReactiveAdapter getAdapter(HandlerResult result) {
		return getAdapterRegistry().getAdapter(result.getReturnType().resolve(), result.getReturnValue());
	}

	/**
	 * Select the best media type for the current request through a content negotiation algorithm.
	 * @param exchange the current request
	 * @param producibleTypesSupplier the media types that can be produced for the current request
	 * @return the selected media type, or {@code null} if none
	 */
	@Nullable
	protected MediaType selectMediaType(
			ServerWebExchange exchange, Supplier<List<MediaType>> producibleTypesSupplier) {

		MediaType contentType = exchange.getResponse().getHeaders().getContentType();
		if (contentType != null && contentType.isConcrete()) {
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Found 'Content-Type:" + contentType + "' in response");
			}
			return contentType;
		}

		List<MediaType> acceptableTypes = getAcceptableTypes(exchange);
		List<MediaType> producibleTypes = getProducibleTypes(exchange, producibleTypesSupplier);

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptable : acceptableTypes) {
			for (MediaType producible : producibleTypes) {
				if (acceptable.isCompatibleWith(producible)) {
					compatibleMediaTypes.add(selectMoreSpecificMediaType(acceptable, producible));
				}
			}
		}

		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MimeTypeUtils.sortBySpecificity(result);

		MediaType selected = null;
		for (MediaType mediaType : result) {
			if (mediaType.isConcrete()) {
				selected = mediaType;
				break;
			}
			else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
				selected = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selected != null) {
			selected = selected.removeQualityValue();
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Using '" + selected + "' given " + acceptableTypes +
						" and supported " + producibleTypes);
			}
		}
		else if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() +
					"No match for " + acceptableTypes + ", supported: " + producibleTypes);
		}

		return selected;
	}

	private List<MediaType> getAcceptableTypes(ServerWebExchange exchange) {
		return getContentTypeResolver().resolveMediaTypes(exchange);
	}

	private List<MediaType> getProducibleTypes(
			ServerWebExchange exchange, Supplier<List<MediaType>> producibleTypesSupplier) {

		Set<MediaType> mediaTypes = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return (mediaTypes != null ? new ArrayList<>(mediaTypes) : producibleTypesSupplier.get());
	}

	private MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		producible = producible.copyQualityValue(acceptable);
		if (acceptable.isLessSpecific(producible)) {
			return producible;
		}
		else {
			return acceptable;
		}
	}

}
