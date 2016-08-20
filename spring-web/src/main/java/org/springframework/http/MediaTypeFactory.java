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

package org.springframework.http;

import java.io.IOException;
import java.io.InputStream;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * A factory delegate for resolving {@link MediaType} objects
 * from {@link Resource} handles or filenames.
 *
 * <p>This implementation is based on the Java Activation Framework,
 * sharing the MIME type definitions with Spring's JavaMail support.
 * However, JAF is an implementation detail and not leaking out.
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public class MediaTypeFactory {

	private static final FileTypeMap fileTypeMap;

	static {
		fileTypeMap = loadFileTypeMapFromContextSupportModule();
	}


	private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
		// See if we can find the extended mime.types from the context-support module...
		Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
		if (mappingLocation.exists()) {
			InputStream inputStream = null;
			try {
				inputStream = mappingLocation.getInputStream();
				return new MimetypesFileTypeMap(inputStream);
			}
			catch (IOException ex) {
				// ignore
			}
			finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					}
					catch (IOException ex) {
						// ignore
					}
				}
			}
		}
		return FileTypeMap.getDefaultFileTypeMap();
	}


	/**
	 * Determine a media type for the given resource, if possible.
	 * @param resource the resource to introspect
	 * @return the corresponding media type, or {@code null} if none found
	 */
	public static MediaType getMediaType(Resource resource) {
		String filename = resource.getFilename();
		return (filename != null ? getMediaType(filename) : null);
	}

	/**
	 * Determine a media type for the given file name, if possible.
	 * @param filename the file name plus extension
	 * @return the corresponding media type, or {@code null} if none found
	 */
	public static MediaType getMediaType(String filename) {
		String mediaType = fileTypeMap.getContentType(filename);
		return (StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null);
	}

}
