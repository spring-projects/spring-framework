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

import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * An extension of {@code PathExtensionContentNegotiationStrategy} that uses
 * {@link ServletContext#getMimeType(String)} as a fallback mechanism when
 * matching a path extension to a media type.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ServletPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy {

	private final ServletContext servletContext;


	/**
	 * Create an instance with the given extension-to-MediaType lookup.
	 * @throws IllegalArgumentException if a media type string cannot be parsed
	 */
	public ServletPathExtensionContentNegotiationStrategy(
			ServletContext servletContext, Map<String, MediaType> mediaTypes) {

		super(mediaTypes);
		Assert.notNull(servletContext, "ServletContext is required!");
		this.servletContext = servletContext;
	}

	/**
	 * Create an instance without any mappings to start with. Mappings may be
	 * added later on if any extensions are resolved through
	 * {@link ServletContext#getMimeType(String)} or through the Java Activation
	 * framework.
	 */
	public ServletPathExtensionContentNegotiationStrategy(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * Look up the given extension via {@link ServletContext#getMimeType(String)}
	 * and if that doesn't help, delegate to the parent implementation.
	 */
	@Override
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension) {
		MediaType mediaType = null;
		if (this.servletContext != null) {
			String mimeType = this.servletContext.getMimeType("file." + extension);
			if (StringUtils.hasText(mimeType)) {
				mediaType = MediaType.parseMediaType(mimeType);
			}
		}
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			MediaType superMediaType = super.handleNoMatch(webRequest, extension);
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		return mediaType;
	}

}
