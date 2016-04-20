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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * TODO: merge into {@link MimeTypeUtils}, and use wherever we still have a runtime check
 * to see if JAF is available (i.e. jafPresent). Since JAF has been included in the JDK
 * since 1.6, we don't
 * need that check anymore. (i.e. {@link org.springframework.http.converter.ResourceHttpMessageConverter}
 * @author Arjen Poutsma
 */
public abstract class MimeTypeUtils2 extends MimeTypeUtils {

	private static final FileTypeMap fileTypeMap;

	static {
		fileTypeMap = loadFileTypeMapFromContextSupportModule();
	}

	private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
		// See if we can find the extended mime.types from the context-support module...
		Resource mappingLocation =
				new ClassPathResource("org/springframework/mail/javamail/mime.types");
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
	 * Returns the {@code MimeType} of the given file name, using the Java Activation
	 * Framework.
	 * @param filename the filename whose mime type is to be found
	 * @return the mime type, if any
	 */
	public static Optional<MimeType> getMimeType(String filename) {
		if (filename != null) {
			String mimeType = fileTypeMap.getContentType(filename);
			if (StringUtils.hasText(mimeType)) {
				return Optional.of(parseMimeType(mimeType));
			}
		}
		return Optional.empty();
	}


}
