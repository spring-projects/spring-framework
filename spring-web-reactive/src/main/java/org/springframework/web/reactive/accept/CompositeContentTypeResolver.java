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
package org.springframework.web.reactive.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ContentTypeResolver} that contains and delegates to a list of other
 * resolvers.
 *
 * <p>Also an implementation of {@link MappingContentTypeResolver} that delegates
 * to those resolvers from the list that are also of type
 * {@code MappingContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CompositeContentTypeResolver implements MappingContentTypeResolver {

	private final List<ContentTypeResolver> resolvers = new ArrayList<>();


	public CompositeContentTypeResolver(List<ContentTypeResolver> resolvers) {
		Assert.notEmpty(resolvers, "At least one resolver is expected.");
		this.resolvers.addAll(resolvers);
	}


	/**
	 * Return a read-only list of the configured resolvers.
	 */
	public List<ContentTypeResolver> getResolvers() {
		return Collections.unmodifiableList(this.resolvers);
	}

	/**
	 * Return the first {@link ContentTypeResolver} of the given type.
	 * @param resolverType the resolver type
	 * @return the first matching resolver or {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ContentTypeResolver> T findResolver(Class<T> resolverType) {
		for (ContentTypeResolver resolver : this.resolvers) {
			if (resolverType.isInstance(resolver)) {
				return (T) resolver;
			}
		}
		return null;
	}


	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) throws HttpMediaTypeNotAcceptableException {
		for (ContentTypeResolver resolver : this.resolvers) {
			List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);
			if (mediaTypes.isEmpty() || (mediaTypes.size() == 1 && mediaTypes.contains(MediaType.ALL))) {
				continue;
			}
			return mediaTypes;
		}
		return Collections.emptyList();
	}

	@Override
	public Set<String> getKeysFor(MediaType mediaType) {
		Set<String> result = new LinkedHashSet<>();
		for (ContentTypeResolver resolver : this.resolvers) {
			if (resolver instanceof MappingContentTypeResolver)
			result.addAll(((MappingContentTypeResolver) resolver).getKeysFor(mediaType));
		}
		return result;
	}

	@Override
	public Set<String> getKeys() {
		Set<String> result = new LinkedHashSet<>();
		for (ContentTypeResolver resolver : this.resolvers) {
			if (resolver instanceof MappingContentTypeResolver)
				result.addAll(((MappingContentTypeResolver) resolver).getKeys());
		}
		return result;
	}

}
