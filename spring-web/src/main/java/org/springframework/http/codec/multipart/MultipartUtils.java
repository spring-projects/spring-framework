/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * Various static utility methods for dealing with multipart parsing.
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class MultipartUtils {

	/**
	 * Return the character set of the given headers, as defined in the
	 * {@link HttpHeaders#getContentType()} header.
	 */
	public static Charset charset(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset;
			}
		}
		return StandardCharsets.UTF_8;
	}

	@Nullable
	public static byte[] boundary(HttpMessage message, Charset headersCharset) {
		MediaType contentType = message.getHeaders().getContentType();
		if (contentType != null) {
			String boundary = contentType.getParameter("boundary");
			if (boundary != null) {
				int len = boundary.length();
				if (len > 2 && boundary.charAt(0) == '"' && boundary.charAt(len - 1) == '"') {
					boundary = boundary.substring(1, len - 1);
				}
				return boundary.getBytes(headersCharset);
			}
		}
		return null;
	}


	/**
	 * Concatenates the given array of byte arrays.
	 */
	public static byte[] concat(byte[]... byteArrays) {
		int len = 0;
		for (byte[] byteArray : byteArrays) {
			len += byteArray.length;
		}
		byte[] result = new byte[len];
		len = 0;
		for (byte[] byteArray : byteArrays) {
			System.arraycopy(byteArray, 0, result, len, byteArray.length);
			len += byteArray.length;
		}
		return result;
	}

	public static void closeChannel(Channel channel) {
		try {
			if (channel.isOpen()) {
				channel.close();
			}
		}
		catch (IOException ignore) {
		}
	}

	public static void deleteFile(Path file) {
		try {
			Files.delete(file);
		}
		catch (IOException ignore) {
		}
	}

	public static boolean isFormField(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType))
				&& headers.getContentDisposition().getFilename() == null;
	}
}
