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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * An implementation of {@link MediaTypeFileExtensionResolver} that maintains a lookup
 * from extension to MediaType.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

	private final ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>(64);

	private final MultiValueMap<MediaType, String> fileExtensions = new LinkedMultiValueMap<MediaType, String>();

	private final List<String> allFileExtensions = new LinkedList<String>();


	/**
	 * Create an instance with the given mappings between extensions and media types.
	 * @throws IllegalArgumentException if a media type string cannot be parsed
	 */
	public MappingMediaTypeFileExtensionResolver(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			for (Entry<String, MediaType> entries : mediaTypes.entrySet()) {
				String extension = entries.getKey().toLowerCase(Locale.ENGLISH);
				MediaType mediaType = entries.getValue();
				addMapping(extension, mediaType);
			}
		}
	}


	/**
	 * Find the file extensions mapped to the given MediaType.
	 * @return 0 or more extensions, never {@code null}
	 */
	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		List<String> fileExtensions = this.fileExtensions.get(mediaType);
		return (fileExtensions != null) ? fileExtensions : Collections.<String>emptyList();
	}

	@Override
	public List<String> getAllFileExtensions() {
		return Collections.unmodifiableList(this.allFileExtensions);
	}

	protected List<MediaType> getAllMediaTypes() {
		return new ArrayList<MediaType>(this.mediaTypes.values());
	}

	/**
	 * Return the MediaType mapped to the given extension.
	 * @return a MediaType for the key or {@code null}
	 */
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension);
	}

	/**
	 * Map a MediaType to an extension or ignore if the extensions is already mapped.
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
		if (previous == null) {
			this.fileExtensions.add(mediaType, extension);
			this.allFileExtensions.add(extension);
		}
	}

}
