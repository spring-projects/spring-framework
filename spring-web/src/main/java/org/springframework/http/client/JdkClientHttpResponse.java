/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on the Java {@link HttpClient}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @since 6.1
 */
class JdkClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse<InputStream> response;

	private final HttpHeaders headers;

	private final InputStream body;


	public JdkClientHttpResponse(HttpResponse<InputStream> response) {
		this.response = response;
		this.headers = adaptHeaders(response);
		InputStream inputStream = response.body();
		this.body = (inputStream != null ? inputStream : InputStream.nullInputStream());
	}

	private static HttpHeaders adaptHeaders(HttpResponse<?> response) {
		Map<String, List<String>> rawHeaders = response.headers().map();
		Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ENGLISH);
		MultiValueMap<String, String> multiValueMap = CollectionUtils.toMultiValueMap(map);
		multiValueMap.putAll(rawHeaders);
		return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(this.response.statusCode());
	}

	@Override
	public String getStatusText() {
		// HttpResponse does not expose status text
		if (getStatusCode() instanceof HttpStatus status) {
			return status.getReasonPhrase();
		}
		else {
			return "";
		}
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public void close() {
		try {
			try {
				StreamUtils.drain(this.body);
			}
			finally {
				this.body.close();
			}
		}
		catch (IOException ignored) {
		}
	}

}
