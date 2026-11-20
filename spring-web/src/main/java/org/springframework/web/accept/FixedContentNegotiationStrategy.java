/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A {@code ContentNegotiationStrategy} that returns a fixed content type.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class FixedContentNegotiationStrategy implements ContentNegotiationStrategy {

	private final List<MediaType> contentTypes;


	/**
	 * Constructor with a single default {@code MediaType}.
	 */
	public FixedContentNegotiationStrategy(MediaType contentType) {
		this(Collections.singletonList(contentType));
	}

	/**
	 * Constructor with an ordered List of default {@code MediaType}'s to return
	 * for use in applications that support a variety of content types.
	 * <p>Consider appending {@link MediaType#ALL} at the end if destinations
	 * are present which do not support any of the other default media types.
	 * @since 5.0
	 */
	public FixedContentNegotiationStrategy(List<MediaType> contentTypes) {
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
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) {
		return this.contentTypes;
	}

}
