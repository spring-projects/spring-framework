/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A base class for ContentNegotiationStrategy types that maintain a map with keys
 * such as "json" and media types such as "application/json".
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class AbstractMappingContentNegotiationStrategy extends MappingMediaTypeExtensionsResolver
		implements ContentNegotiationStrategy, MediaTypeExtensionsResolver {

	/**
	 * Create an instance with the given extension-to-MediaType lookup.
	 * @throws IllegalArgumentException if a media type string cannot be parsed
	 */
	public AbstractMappingContentNegotiationStrategy(Map<String, String> mediaTypes) {
		super(mediaTypes);
	}

	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
		String key = getMediaTypeKey(webRequest);
		if (StringUtils.hasText(key)) {
			MediaType mediaType = lookupMediaType(key);
			if (mediaType != null) {
				handleMatch(key, mediaType);
				return Collections.singletonList(mediaType);
			}
			mediaType = handleNoMatch(webRequest, key);
			if (mediaType != null) {
				addMapping(key, mediaType);
				return Collections.singletonList(mediaType);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Sub-classes must extract the key to use to look up a media type.
	 * @return the lookup key or {@code null} if the key cannot be derived
	 */
	protected abstract String getMediaTypeKey(NativeWebRequest request);

	/**
	 * Invoked when a matching media type is found in the lookup map.
	 */
	protected void handleMatch(String mappingKey, MediaType mediaType) {
	}

	/**
	 * Invoked when no matching media type is found in the lookup map.
	 * Sub-classes can take further steps to determine the media type.
	 */
	protected MediaType handleNoMatch(NativeWebRequest request, String mappingKey) {
		return null;
	}

}