/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.client.response;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link MockRestResponseCreators} static factory methods.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseCreatorsTests {

	@Test
	public void success() throws Exception {
		MockClientHttpResponse response = (MockClientHttpResponse) MockRestResponseCreators.withSuccess().createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void successWithContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", MediaType.TEXT_PLAIN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
	}

	@Test
	public void successWithContentWithoutContentType() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", null);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isNull();
		assertThat(StreamUtils.copyToByteArray(response.getBody())).isEqualTo("foo".getBytes());
	}

	@Test
	public void created() throws Exception {
		URI location = new URI("/foo");
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withCreatedEntity(location);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getLocation()).isEqualTo(location);
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void noContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withNoContent();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void badRequest() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void unauthorized() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withUnauthorizedRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void serverError() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withServerError();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

	@Test
	public void withStatus() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getHeaders().isEmpty()).isTrue();
		assertThat(StreamUtils.copyToByteArray(response.getBody()).length).isEqualTo(0);
	}

}
