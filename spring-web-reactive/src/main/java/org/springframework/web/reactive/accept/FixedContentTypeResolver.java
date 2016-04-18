/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ContentTypeResolver} that resolves to a fixed list of media types.
 *
 * @author Rossen Stoyanchev
 */
public class FixedContentTypeResolver implements ContentTypeResolver {

	private final List<MediaType> mediaTypes;


	/**
	 * Create an instance with the given content type.
	 */
	public FixedContentTypeResolver(MediaType mediaTypes) {
		this.mediaTypes = Collections.singletonList(mediaTypes);
	}


	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) {
		return this.mediaTypes;
	}

}
