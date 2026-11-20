/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver that always resolves to a fixed list of media types. This can be
 * used as the "last in line" strategy providing a fallback for when the client
 * has not requested any media types.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FixedContentTypeResolver implements RequestedContentTypeResolver {

	private final List<MediaType> contentTypes;


	/**
	 * Constructor with a single default {@code MediaType}.
	 */
	public FixedContentTypeResolver(MediaType mediaType) {
		this(Collections.singletonList(mediaType));
	}

	/**
	 * Constructor with an ordered List of default {@code MediaType}'s to return
	 * for use in applications that support a variety of content types.
	 * <p>Consider appending {@link MediaType#ALL} at the end if destinations
	 * are present which do not support any of the other default media types.
	 */
	public FixedContentTypeResolver(List<MediaType> contentTypes) {
		Assert.notNull(contentTypes, "'contentTypes' must not be null");
		this.contentTypes = Collections.unmodifiableList(contentTypes);
	}


	/**
	 * Return the configured list of media types.
	 */
	public List<MediaType> getContentTypes() {
		return this.contentTypes;
	}


	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) {
		return this.contentTypes;
	}

}
