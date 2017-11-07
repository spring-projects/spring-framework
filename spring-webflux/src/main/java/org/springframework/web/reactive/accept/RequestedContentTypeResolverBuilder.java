/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Builder for a composite {@link RequestedContentTypeResolver} that delegates
 * to other resolvers each implementing a different strategy to determine the
 * requested content type -- e.g. Accept header, query parameter, or other.
 *
 * <p>Use builder methods to add resolvers in the desired order. For a given
 * request he first resolver to return a list that is not empty and does not
 * consist of just {@link MediaType#ALL}, will be used.
 *
 * <p>By default, if no resolvers are explicitly configured, the builder will
 * add {@link HeaderContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestedContentTypeResolverBuilder {

	private final List<Supplier<RequestedContentTypeResolver>> candidates = new ArrayList<>();


	/**
	 * Add a resolver to get the requested content type from a query parameter.
	 * By default the query parameter name is {@code "format"}.
	 */
	public ParameterResolverConfigurer parameterResolver() {
		ParameterResolverConfigurer parameterBuilder = new ParameterResolverConfigurer();
		this.candidates.add(parameterBuilder::createResolver);
		return parameterBuilder;
	}

	/**
	 * Add resolver to get the requested content type from the
	 * {@literal "Accept"} header.
	 */
	public void headerResolver() {
		this.candidates.add(HeaderContentTypeResolver::new);
	}

	/**
	 * Add resolver that returns a fixed set of media types.
	 * @param mediaTypes the media types to use
	 */
	public void fixedResolver(MediaType... mediaTypes) {
		this.candidates.add(() -> new FixedContentTypeResolver(Arrays.asList(mediaTypes)));
	}

	/**
	 * Add a custom resolver.
	 * @param resolver the resolver to add
	 */
	public void resolver(RequestedContentTypeResolver resolver) {
		this.candidates.add(() -> resolver);
	}

	/**
	 * Build a {@link RequestedContentTypeResolver} that delegates to the list
	 * of resolvers configured through this builder.
	 */
	public RequestedContentTypeResolver build() {

		List<RequestedContentTypeResolver> resolvers =
				this.candidates.isEmpty() ?
						Collections.singletonList(new HeaderContentTypeResolver()) :
						this.candidates.stream().map(Supplier::get).collect(Collectors.toList());

		return exchange -> {
			for (RequestedContentTypeResolver resolver : resolvers) {
				List<MediaType> type = resolver.resolveMediaTypes(exchange);
				if (type.isEmpty() || (type.size() == 1 && type.contains(MediaType.ALL))) {
					continue;
				}
				return type;
			}
			return Collections.emptyList();
		};
	}


	/**
	 * Helper to create and configure {@link ParameterContentTypeResolver}.
	 */
	public static class ParameterResolverConfigurer {

		private final Map<String, MediaType> mediaTypes = new HashMap<>();

		@Nullable
		private String parameterName;

		/**
		 * Configure a mapping between a lookup key (extracted from a query
		 * parameter value) and a corresponding {@code MediaType}.
		 * @param key the lookup key
		 * @param mediaType the MediaType for that key
		 */
		public ParameterResolverConfigurer mediaType(String key, MediaType mediaType) {
			this.mediaTypes.put(key, mediaType);
			return this;
		}

		/**
		 * Map-based variant of {@link #mediaType(String, MediaType)}.
		 * @param mediaTypes the mappings to copy
		 */
		public ParameterResolverConfigurer mediaType(Map<String, MediaType> mediaTypes) {
			this.mediaTypes.putAll(mediaTypes);
			return this;
		}

		/**
		 * Set the name of the parameter to use to determine requested media types.
		 * <p>By default this is set to {@literal "format"}.
		 */
		public ParameterResolverConfigurer parameterName(String parameterName) {
			this.parameterName = parameterName;
			return this;
		}

		/**
		 * Private factory method to create the resolver.
		 */
		private RequestedContentTypeResolver createResolver() {
			ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(this.mediaTypes);
			if (this.parameterName != null) {
				resolver.setParameterName(this.parameterName);
			}
			return resolver;
		}
	}

}
