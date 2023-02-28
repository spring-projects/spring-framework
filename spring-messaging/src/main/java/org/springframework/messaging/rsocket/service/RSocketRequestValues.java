/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * Container for RSocket request values extracted from an
 * {@link RSocketExchange @RSocketExchange}-annotated
 * method and argument values passed to it. This is then used to define a request
 * via {@link org.springframework.messaging.rsocket.RSocketRequester}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class RSocketRequestValues {

	@Nullable
	private final String route;

	private final Object[] routeVariables;

	private final Map<Object, MimeType> metadata;

	@Nullable
	private final Object payloadValue;

	@Nullable
	private final Publisher<?> payload;

	@Nullable
	private final ParameterizedTypeReference<?> payloadElementType;


	public RSocketRequestValues(
			@Nullable String route, @Nullable List<Object> routeVariables,  @Nullable MetadataHelper metadataHelper,
			@Nullable Object payloadValue, @Nullable Publisher<?> payload,
			@Nullable ParameterizedTypeReference<?> payloadElementType) {

		this.route = route;
		this.routeVariables = (routeVariables != null ? routeVariables.toArray() : new Object[0]);
		this.metadata = (metadataHelper != null ? metadataHelper.toMap() : Collections.emptyMap());
		this.payloadValue = payloadValue;
		this.payload = payload;
		this.payloadElementType = payloadElementType;
	}


	/**
	 * Return the route value for
	 * {@link org.springframework.messaging.rsocket.RSocketRequester#route(String, Object...) route}.
	 */
	@Nullable
	public String getRoute() {
		return this.route;
	}

	/**
	 * Return the route variables for
	 * {@link org.springframework.messaging.rsocket.RSocketRequester#route(String, Object...) route}.
	 */
	public Object[] getRouteVariables() {
		return this.routeVariables;
	}

	/**
	 * Return the metadata entries for
	 * {@link org.springframework.messaging.rsocket.RSocketRequester.RequestSpec#metadata(Object, MimeType)}.
	 */
	public Map<Object, MimeType> getMetadata() {
		return this.metadata;
	}

	/**
	 * Return the request payload as a value to be serialized, if set.
	 * <p>This is mutually exclusive with {@link #getPayload()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Object getPayloadValue() {
		return this.payloadValue;
	}

	/**
	 * Return the request payload as a Publisher.
	 * <p>This is mutually exclusive with {@link #getPayloadValue()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Publisher<?> getPayload() {
		return this.payload;
	}

	/**
	 * Return the element type for a {@linkplain #getPayload() Publisher payload}.
	 */
	@Nullable
	public ParameterizedTypeReference<?> getPayloadElementType() {
		return this.payloadElementType;
	}


	public static Builder builder(@Nullable String route) {
		return new Builder(route);
	}


	/**
	 * Builder for {@link RSocketRequestValues}.
	 */
	public final static class Builder {

		@Nullable
		private String route;

		@Nullable
		private List<Object> routeVariables;

		@Nullable
		private MetadataHelper metadataHelper;

		@Nullable
		private Object payloadValue;

		@Nullable
		private Publisher<?> payload;

		@Nullable
		private ParameterizedTypeReference<?> payloadElementType;

		Builder(@Nullable String route) {
			this.route = (StringUtils.hasText(route) ? route : null);
		}

		/**
		 * Set the route for the request.
		 */
		public Builder setRoute(String route) {
			this.route = route;
			this.routeVariables = null;
			return this;
		}

		/**
		 * Add a route variable.
		 */
		public Builder addRouteVariable(Object variable) {
			this.routeVariables = (this.routeVariables != null ? this.routeVariables : new ArrayList<>());
			this.routeVariables.add(variable);
			return this;
		}

		/**
		 * Add a metadata entry.
		 * This must be followed by a corresponding call to {@link #addMimeType(MimeType)}.
		 */
		public Builder addMetadata(Object metadata) {
			this.metadataHelper = (this.metadataHelper != null ? this.metadataHelper : new MetadataHelper());
			this.metadataHelper.addMetadata(metadata);
			return this;
		}

		/**
		 * Set the mime type for a metadata entry.
		 * This must be preceded by a call to {@link #addMetadata(Object)}.
		 */
		public Builder addMimeType(MimeType mimeType) {
			this.metadataHelper = (this.metadataHelper != null ? this.metadataHelper : new MetadataHelper());
			this.metadataHelper.addMimeType(mimeType);
			return this;
		}

		/**
		 * Set the request payload as a concrete value to be serialized.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@linkplain #setPayload(Publisher, ParameterizedTypeReference) payload Publisher}.
		 */
		public Builder setPayloadValue(Object payloadValue) {
			this.payloadValue = payloadValue;
			this.payload = null;
			this.payloadElementType = null;
			return this;
		}

		/**
		 * Set the request payload value to be serialized.
		 */
		public <T, P extends Publisher<T>> Builder setPayload(P payload, ParameterizedTypeReference<T> elementTye) {
			this.payload = payload;
			this.payloadElementType = elementTye;
			this.payloadValue = null;
			return this;
		}

		/**
		 * Build the {@link RSocketRequestValues} instance.
		 */
		public RSocketRequestValues build() {
			return new RSocketRequestValues(
					this.route, this.routeVariables, this.metadataHelper,
					this.payloadValue, this.payload, this.payloadElementType);
		}

	}


	/**
	 * Class that helps to collect a map of metadata entries as a series of calls
	 * to provide each metadata and mime type pair.
	 */
	private static class MetadataHelper {

		private final List<Object> metadata = new ArrayList<>();

		private final List<MimeType> mimeTypes = new ArrayList<>();

		public void addMetadata(Object metadata) {
			Assert.isTrue(this.metadata.size() == this.mimeTypes.size(), () -> "Invalid state: " + this);
			this.metadata.add(metadata);
		}

		public void addMimeType(MimeType mimeType) {
			Assert.isTrue(this.metadata.size() == (this.mimeTypes.size() + 1), () -> "Invalid state: " + this);
			this.mimeTypes.add(mimeType);
		}

		public Map<Object, MimeType> toMap() {
			Map<Object, MimeType> map = new LinkedHashMap<>(this.metadata.size());
			for (int i = 0; i < this.metadata.size(); i++) {
				map.put(this.metadata.get(i), this.mimeTypes.get(i));
			}
			return map;
		}

		@Override
		public String toString() {
			return "metadata=" + this.metadata + ", mimeTypes=" + this.mimeTypes;
		}

	}

}
