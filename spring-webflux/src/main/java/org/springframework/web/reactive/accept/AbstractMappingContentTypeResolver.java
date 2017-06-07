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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for resolvers that extract a key from the request and look up a
 * mapping to a MediaType. The use case is URI-based content negotiation for
 * example based on query parameter or file extension in the request path.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractMappingContentTypeResolver implements RequestedContentTypeResolver {

	/** Primary lookup for media types by key (e.g. "json" -> "application/json") */
	private final Map<String, MediaType> mediaTypeLookup = new ConcurrentHashMap<>(64);


	public AbstractMappingContentTypeResolver(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((key, mediaType) ->
				this.mediaTypeLookup.put(key.toLowerCase(Locale.ENGLISH), mediaType));
	}


	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) {
		String key = getKey(exchange);
		if (StringUtils.hasText(key)) {
			MediaType mediaType = getMediaType(key);
			if (mediaType != null) {
				this.mediaTypeLookup.putIfAbsent(key, mediaType);
				return Collections.singletonList(mediaType);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Get the key to look up a MediaType with.
	 */
	@Nullable
	protected abstract String getKey(ServerWebExchange exchange);

	/**
	 * Get the MediaType for the given key.
	 */
	@Nullable
	protected MediaType getMediaType(String key) {
		key = key.toLowerCase(Locale.ENGLISH);
		MediaType mediaType = this.mediaTypeLookup.get(key);
		if (mediaType == null) {
			mediaType = MediaTypeFactory.getMediaType("filename." + key).orElse(null);
		}
		return mediaType;
	}

}
