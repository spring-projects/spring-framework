/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link org.springframework.web.accept.ApiVersionResolver} that extracts the version from a media type
 * parameter found in the Accept or Content-Type headers.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class MediaTypeParamApiVersionResolver implements ApiVersionResolver {

	private final MediaType compatibleMediaType;

	private final String parameterName;


	/**
	 * Create an instance.
	 * @param compatibleMediaType the media type to extract the parameter from with
	 * the match established via {@link MediaType#isCompatibleWith(MediaType)}
	 * @param paramName the name of the parameter
	 */
	public MediaTypeParamApiVersionResolver(MediaType compatibleMediaType, String paramName) {
		this.compatibleMediaType = compatibleMediaType;
		this.parameterName = paramName;
	}


	@Override
	public @Nullable String resolveVersion(ServerWebExchange exchange) {
		HttpHeaders headers = exchange.getRequest().getHeaders();
		for (MediaType mediaType : headers.getAccept()) {
			if (this.compatibleMediaType.isCompatibleWith(mediaType)) {
				return mediaType.getParameter(this.parameterName);
			}
		}
		MediaType mediaType = headers.getContentType();
		if (mediaType != null && this.compatibleMediaType.isCompatibleWith(mediaType)) {
			return mediaType.getParameter(this.parameterName);
		}
		return null;
	}

}
