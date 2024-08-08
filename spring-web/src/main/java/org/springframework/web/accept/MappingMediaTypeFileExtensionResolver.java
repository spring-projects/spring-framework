/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * An implementation of {@code MediaTypeFileExtensionResolver} that maintains
 * lookups between file extensions and MediaTypes in both directions.
 *
 * <p>Initially created with a map of file extensions and media types.
 * Subsequently, subclasses can use {@link #addMapping} to add more mappings.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

	private final ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);

	private final ConcurrentMap<MediaType, List<String>> fileExtensions = new ConcurrentHashMap<>(64);

	private final List<String> allFileExtensions = new CopyOnWriteArrayList<>();


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public MappingMediaTypeFileExtensionResolver(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			Set<String> allFileExtensions = CollectionUtils.newHashSet(mediaTypes.size());
			mediaTypes.forEach((extension, mediaType) -> {
				String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);
				this.mediaTypes.put(lowerCaseExtension, mediaType);
				addFileExtension(mediaType, lowerCaseExtension);
				allFileExtensions.add(lowerCaseExtension);
			});
			this.allFileExtensions.addAll(allFileExtensions);
		}
	}


	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	protected List<MediaType> getAllMediaTypes() {
		return new ArrayList<>(this.mediaTypes.values());
	}

	/**
	 * Map an extension to a MediaType. Ignore if extension already mapped.
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
		if (previous == null) {
			addFileExtension(mediaType, extension);
			this.allFileExtensions.add(extension);
		}
	}

	private void addFileExtension(MediaType mediaType, String extension) {
		this.fileExtensions.computeIfAbsent(mediaType, key -> new CopyOnWriteArrayList<>())
				.add(extension);
	}


	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		List<String> fileExtensions = this.fileExtensions.get(mediaType);
		return (fileExtensions != null ? fileExtensions : Collections.emptyList());
	}

	@Override
	public List<String> getAllFileExtensions() {
		return Collections.unmodifiableList(this.allFileExtensions);
	}

	/**
	 * Use this method for a reverse lookup from extension to MediaType.
	 * @return a MediaType for the extension, or {@code null} if none found
	 */
	@Nullable
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
	}

}
