/*
 * Copyright 2002-2018 the original author or authors.
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
