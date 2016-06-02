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

package org.springframework.web.accept;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * A {@code ContentNegotiationStrategy} that resolves the file extension in the
 * request path to a key to be used to look up a media type.
 *
 * <p>If the file extension is not found in the explicit registrations provided
 * to the constructor, the Java Activation Framework (JAF) is used as a fallback
 * mechanism.
 *
 * <p>The presence of the JAF is detected and enabled automatically but the
 * {@link #setUseJaf(boolean)} property may be set to false.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class PathExtensionContentNegotiationStrategy
		extends AbstractMappingContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(PathExtensionContentNegotiationStrategy.class);

	private static final boolean JAF_PRESENT = ClassUtils.isPresent("javax.activation.FileTypeMap",
			PathExtensionContentNegotiationStrategy.class.getClassLoader());

	private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

	static {
		PATH_HELPER.setUrlDecode(false);
	}


	private boolean useJaf = true;

	private boolean ignoreUnknownExtensions = true;


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public PathExtensionContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}

	/**
	 * Create an instance without any mappings to start with. Mappings may be added
	 * later on if any extensions are resolved through the Java Activation framework.
	 */
	public PathExtensionContentNegotiationStrategy() {
		super(null);
	}


	/**
	 * Whether to use the Java Activation Framework to look up file extensions.
	 * <p>By default this is set to "true" but depends on JAF being present.
	 */
	public void setUseJaf(boolean useJaf) {
		this.useJaf = useJaf;
	}

	/**
	 * Whether to ignore requests with unknown file extension. Setting this to
	 * {@code false} results in {@code HttpMediaTypeNotAcceptableException}.
	 * <p>By default this is set to {@code true}.
	 */
	public void setIgnoreUnknownExtensions(boolean ignoreUnknownExtensions) {
		this.ignoreUnknownExtensions = ignoreUnknownExtensions;
	}


	@Override
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			logger.warn("An HttpServletRequest is required to determine the media type key");
			return null;
		}
		String path = PATH_HELPER.getLookupPathForRequest(request);
		String filename = WebUtils.extractFullFilenameFromUrlPath(path);
		String extension = StringUtils.getFilenameExtension(filename);
		return (StringUtils.hasText(extension)) ? extension.toLowerCase(Locale.ENGLISH) : null;
	}

	@Override
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension)
			throws HttpMediaTypeNotAcceptableException {

		if (this.useJaf && JAF_PRESENT) {
			MediaType mediaType = JafMediaTypeFactory.getMediaType("file." + extension);
			if (mediaType != null && !MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
				return mediaType;
			}
		}
		if (this.ignoreUnknownExtensions) {
			return null;
		}
		throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
	}


	/**
	 * Inner class to avoid hard-coded dependency on JAF.
	 */
	private static class JafMediaTypeFactory {

		private static final FileTypeMap fileTypeMap;

		static {
			fileTypeMap = initFileTypeMap();
		}

		/**
		 * Find extended mime.types from the spring-context-support module.
		 */
		private static FileTypeMap initFileTypeMap() {
			Resource resource = new ClassPathResource("org/springframework/mail/javamail/mime.types");
			if (resource.exists()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Loading JAF FileTypeMap from " + resource);
				}
				InputStream inputStream = null;
				try {
					inputStream = resource.getInputStream();
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
			if (logger.isTraceEnabled()) {
				logger.trace("Loading default Java Activation Framework FileTypeMap");
			}
			return FileTypeMap.getDefaultFileTypeMap();
		}

		public static MediaType getMediaType(String filename) {
			String mediaType = fileTypeMap.getContentType(filename);
			return (StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null);
		}
	}

}
