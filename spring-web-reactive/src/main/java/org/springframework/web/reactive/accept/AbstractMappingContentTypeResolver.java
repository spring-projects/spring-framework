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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Abstract base class for {@link MappingContentTypeResolver} implementations.
 * Maintains the actual mappings and pre-implements the overall algorithm with
 * sub-classes left to provide a way to extract the lookup key (e.g. file
 * extension, query parameter, etc) for a given exchange.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractMappingContentTypeResolver implements MappingContentTypeResolver {

	/** Primary lookup for media types by key (e.g. "json" -> "application/json") */
	private final ConcurrentMap<String, MediaType> mediaTypeLookup = new ConcurrentHashMap<>(64);

	/** Reverse lookup for keys associated with a media type */
	private final MultiValueMap<MediaType, String> keyLookup = new LinkedMultiValueMap<>(64);


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public AbstractMappingContentTypeResolver(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
				String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
				MediaType mediaType = entry.getValue();
				this.mediaTypeLookup.put(extension, mediaType);
				this.keyLookup.add(mediaType, extension);
			}
		}
	}


	/**
	 * Sub-classes can use this method to look up a MediaType by key.
	 * @param key the key converted to lower case
	 * @return a MediaType or {@code null}
	 */
	protected MediaType getMediaType(String key) {
		return this.mediaTypeLookup.get(key.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Sub-classes can use this method get all mapped media types.
	 */
	protected List<MediaType> getMediaTypes() {
		return new ArrayList<>(this.mediaTypeLookup.values());
	}


	// ContentTypeResolver implementation

	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange)
			throws NotAcceptableStatusException {

		String key = extractKey(exchange);
		return resolveMediaTypes(key);
	}

	/**
	 * An overloaded resolve method with a pre-resolved lookup key.
	 * @param key the key for looking up media types
	 * @return a list of resolved media types or an empty list
	 * @throws NotAcceptableStatusException
	 */
	public List<MediaType> resolveMediaTypes(String key) throws NotAcceptableStatusException {
		if (StringUtils.hasText(key)) {
			MediaType mediaType = getMediaType(key);
			if (mediaType != null) {
				handleMatch(key, mediaType);
				return Collections.singletonList(mediaType);
			}
			mediaType = handleNoMatch(key);
			if (mediaType != null) {
				MediaType previous = this.mediaTypeLookup.putIfAbsent(key, mediaType);
				if (previous == null) {
					this.keyLookup.add(mediaType, key);
				}
				return Collections.singletonList(mediaType);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Extract the key to use to look up a media type from the given exchange,
	 * e.g. file extension, query parameter, etc.
	 * @return the key or {@code null}
	 */
	protected abstract String extractKey(ServerWebExchange exchange);

	/**
	 * Override to provide handling when a key is successfully resolved via
	 * {@link #getMediaType(String)}.
	 */
	@SuppressWarnings("UnusedParameters")
	protected void handleMatch(String key, MediaType mediaType) {
	}

	/**
	 * Override to provide handling when a key is not resolved via.
	 * {@link #getMediaType(String)}. If a MediaType is returned from
	 * this method it will be added to the mappings.
	 */
	@SuppressWarnings("UnusedParameters")
	protected MediaType handleNoMatch(String key) throws NotAcceptableStatusException {
		return null;
	}

	// MappingContentTypeResolver implementation

	@Override
	public Set<String> getKeysFor(MediaType mediaType) {
		List<String> keys = this.keyLookup.get(mediaType);
		return (keys != null ? new HashSet<>(keys) : Collections.emptySet());
	}

	@Override
	public Set<String> getKeys() {
		return new HashSet<>(this.mediaTypeLookup.keySet());
	}

}
