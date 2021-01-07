/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * A factory delegate for resolving {@link MediaType} objects
 * from {@link Resource} handles or filenames.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 5.0
 */
public final class MediaTypeFactory {

	private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";

	private static final MultiValueMap<String, MediaType> fileExtensionToMediaTypes = parseMimeTypes();


	private MediaTypeFactory() {
	}


	/**
	 * Parse the {@code mime.types} file found in the resources. Format is:
	 * <code>
	 * # comments begin with a '#'<br>
	 * # the format is &lt;mime type> &lt;space separated file extensions><br>
	 * # for example:<br>
	 * text/plain    txt text<br>
	 * # this would map file.txt and file.text to<br>
	 * # the mime type "text/plain"<br>
	 * </code>
	 * @return a multi-value map, mapping media types to file extensions.
	 */
	private static MultiValueMap<String, MediaType> parseMimeTypes() {
		InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
			MultiValueMap<String, MediaType> result = new LinkedMultiValueMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
				MediaType mediaType = MediaType.parseMediaType(tokens[0]);
				for (int i = 1; i < tokens.length; i++) {
					String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
					result.add(fileExtension, mediaType);
				}
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + MIME_TYPES_FILE_NAME + "'", ex);
		}
	}

	/**
	 * Determine a media type for the given resource, if possible.
	 * @param resource the resource to introspect
	 * @return the corresponding media type, or {@code null} if none found
	 */
	public static Optional<MediaType> getMediaType(@Nullable Resource resource) {
		return Optional.ofNullable(resource)
				.map(Resource::getFilename)
				.flatMap(MediaTypeFactory::getMediaType);
	}

	/**
	 * Determine a media type for the given file name, if possible.
	 * @param filename the file name plus extension
	 * @return the corresponding media type, or {@code null} if none found
	 */
	public static Optional<MediaType> getMediaType(@Nullable String filename) {
		return getMediaTypes(filename).stream().findFirst();
	}

	/**
	 * Determine the media types for the given file name, if possible.
	 * @param filename the file name plus extension
	 * @return the corresponding media types, or an empty list if none found
	 */
	public static List<MediaType> getMediaTypes(@Nullable String filename) {
		List<MediaType> mediaTypes = null;
		String ext = StringUtils.getFilenameExtension(filename);
		if (ext != null) {
			mediaTypes = fileExtensionToMediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
		}
		return (mediaTypes != null ? mediaTypes : Collections.emptyList());
	}

}
