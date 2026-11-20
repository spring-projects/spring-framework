/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;

/**
 * Internal methods shared between types in this package.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
abstract class RestClientUtils {

	public static byte[] getBody(HttpInputMessage message) {
		try {
			return FileCopyUtils.copyToByteArray(message.getBody());
		}
		catch (IOException ignore) {
		}
		return new byte[0];
	}

	public static @Nullable Charset getCharset(HttpMessage response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}
}
