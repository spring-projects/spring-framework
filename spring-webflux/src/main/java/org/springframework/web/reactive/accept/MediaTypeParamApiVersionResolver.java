/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
				String version = mediaType.getParameter(this.parameterName);
				if (version != null) {
					return version;
				}
			}
		}
		MediaType mediaType = headers.getContentType();
		if (mediaType != null && this.compatibleMediaType.isCompatibleWith(mediaType)) {
			return mediaType.getParameter(this.parameterName);
		}
		return null;
	}

}
