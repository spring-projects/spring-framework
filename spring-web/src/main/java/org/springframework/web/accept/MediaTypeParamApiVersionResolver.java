/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * {@link ApiVersionResolver} that extracts the version from a media type
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
	public @Nullable String resolveVersion(HttpServletRequest request) {
		Enumeration<String> headers = request.getHeaders(HttpHeaders.ACCEPT);
		while (headers.hasMoreElements()) {
			String header = headers.nextElement();
			for (MediaType mediaType : MediaType.parseMediaTypes(header)) {
				if (this.compatibleMediaType.isCompatibleWith(mediaType)) {
					String version = mediaType.getParameter(this.parameterName);
					if (version != null) {
						return version;
					}
				}
			}
		}
		String header = request.getHeader(HttpHeaders.CONTENT_TYPE);
		for (MediaType mediaType : MediaType.parseMediaTypes(header)) {
			if (this.compatibleMediaType.isCompatibleWith(mediaType)) {
				return mediaType.getParameter(this.parameterName);
			}
		}
		return null;
	}

}
