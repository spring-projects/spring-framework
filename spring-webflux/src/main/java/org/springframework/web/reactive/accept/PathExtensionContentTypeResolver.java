/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;

/**
 * A {@link RequestedContentTypeResolver} that extracts the file extension from
 * the request path and uses that as the media type lookup key.
 *
 * <p>If the file extension is not found in the explicit registrations provided
 * to the constructor, the {@link MediaTypeFactory} is used as a fallback
 * mechanism.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class PathExtensionContentTypeResolver extends AbstractMappingContentTypeResolver {

	private boolean useRegisteredExtensionsOnly = false;

	private boolean ignoreUnknownExtensions = true;


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public PathExtensionContentTypeResolver(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}

	/**
	 * Create an instance without any mappings to start with. Mappings may be added
	 * later on if any extensions are resolved through the Java Activation framework.
	 */
	public PathExtensionContentTypeResolver() {
		super(null);
	}


	/**
	 * Whether to only use the registered mappings to look up file extensions, or also refer to
	 * defaults.
	 * <p>By default this is set to {@code false}, meaning that defaults are used.
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
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
	protected String extractKey(ServerWebExchange exchange) {
		String path = exchange.getRequest().getURI().getRawPath();
		String extension = UriUtils.extractFileExtension(path);
		return (StringUtils.hasText(extension)) ? extension.toLowerCase(Locale.ENGLISH) : null;
	}

	@Override
	protected MediaType handleNoMatch(String key) throws NotAcceptableStatusException {
		if (!this.useRegisteredExtensionsOnly) {
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType("file." + key);
			if (mediaType.isPresent()) {
				return mediaType.get();
			}
		}
		if (this.ignoreUnknownExtensions) {
			return null;
		}
		throw new NotAcceptableStatusException(getAllMediaTypes());
	}

	/**
	 * A public method exposing the knowledge of the path extension resolver to
	 * determine the media type for a given {@link Resource}. First it checks
	 * the explicitly registered mappings and then falls back on {@link MediaTypeFactory}.
	 * @param resource the resource
	 * @return the MediaType for the extension, or {@code null} if none determined
	 */
	public MediaType resolveMediaTypeForResource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		MediaType mediaType = null;
		String filename = resource.getFilename();
		String extension = StringUtils.getFilenameExtension(filename);
		if (extension != null) {
			mediaType = getMediaType(extension);
		}
		if (mediaType == null) {
			mediaType = MediaTypeFactory.getMediaType(filename).orElse(null);
		}
		return mediaType;
	}

}
