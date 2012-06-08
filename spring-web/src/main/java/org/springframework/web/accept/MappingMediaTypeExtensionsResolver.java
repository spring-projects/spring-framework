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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.MediaType;

/**
 * An implementation of {@link MediaTypeExtensionsResolver} that maintains a lookup
 * from extension to MediaType.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MappingMediaTypeExtensionsResolver implements MediaTypeExtensionsResolver {

	private ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>();

	/**
	 * Create an instance with the given mappings between extensions and media types.
	 * @throws IllegalArgumentException if a media type string cannot be parsed
	 */
	public MappingMediaTypeExtensionsResolver(Map<String, String> mediaTypes) {
		if (mediaTypes != null) {
			for (Map.Entry<String, String> entry : mediaTypes.entrySet()) {
				String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
				MediaType mediaType = MediaType.parseMediaType(entry.getValue());
				this.mediaTypes.put(extension, mediaType);
			}
		}
	}

	/**
	 * Find the extensions applicable to the given MediaType.
	 * @return 0 or more extensions, never {@code null}
	 */
	public List<String> resolveExtensions(MediaType mediaType) {
		List<String> result = new ArrayList<String>();
		for (Entry<String, MediaType> entry : this.mediaTypes.entrySet()) {
			if (mediaType.includes(entry.getValue())) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	/**
	 * Return the MediaType mapped to the given extension.
	 * @return a MediaType for the key or {@code null}
	 */
	public MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension);
	}

	/**
	 * Map a MediaType to an extension or ignore if the extensions is already mapped.
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		this.mediaTypes.putIfAbsent(extension, mediaType);
	}

}