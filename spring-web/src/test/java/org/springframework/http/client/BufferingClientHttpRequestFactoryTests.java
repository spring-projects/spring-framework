/*
 * Copyright 2002-2025 the original author or authors.
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

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BufferingClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());
	}

	@Test
	void repeatableRead() throws Exception {

		ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/echo"), HttpMethod.PUT);
		assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT);

		String header = "MyHeader";
		request.getHeaders().add(header, "value1");
		request.getHeaders().add(header, "value2");

		byte[] body = "Hello World".getBytes(StandardCharsets.UTF_8);
		FileCopyUtils.copy(body, request.getBody());
		request.getHeaders().setContentLength(body.length);

		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

			assertThat(response.getHeaders().get(header)).containsExactly("value1", "value2");
			assertThat(response.getHeaders().get(header)).containsExactly("value1", "value2");

			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(result).isEqualTo(body);

			result = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(result).isEqualTo(body);
		}
	}

}
