/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
