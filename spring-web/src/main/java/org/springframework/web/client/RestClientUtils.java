/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
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

	@Nullable
	public static Charset getCharset(HttpMessage response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType.getCharset() : null);
	}
}
