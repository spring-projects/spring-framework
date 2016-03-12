/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;

import com.squareup.okhttp.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpResponse} implementation based on OkHttp.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @since 4.2
 */
class OkHttpClientHttpResponse extends AbstractClientHttpResponse {

	private final Response response;

	private HttpHeaders headers;


	public OkHttpClientHttpResponse(Response response) {
		Assert.notNull(response, "Response must not be null");
		this.response = response;
	}


	@Override
	public int getRawStatusCode() {
		return this.response.code();
	}

	@Override
	public String getStatusText() {
		return this.response.message();
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.response.body().byteStream();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			HttpHeaders headers = new HttpHeaders();
			for (String headerName : this.response.headers().names()) {
				for (String headerValue : this.response.headers(headerName)) {
					headers.add(headerName, headerValue);
				}
			}
			this.headers = headers;
		}
		return this.headers;
	}

	@Override
	public void close() {
		try {
			this.response.body().close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}
