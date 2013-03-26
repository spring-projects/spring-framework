/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.web.client.response;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.FileCopyUtils;

/**
 * Tests for the {@link MockRestResponseCreators} static factory methods.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseCreatorsTests {

	@Test
	public void success() throws Exception {
		MockClientHttpResponse response = (MockClientHttpResponse) MockRestResponseCreators.withSuccess().createResponse(null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

	@Test
	public void successWithContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", MediaType.TEXT_PLAIN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
		assertArrayEquals("foo".getBytes(), FileCopyUtils.copyToByteArray(response.getBody()));
	}

	@Test
	public void successWithContentWithoutContentType() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withSuccess("foo", null);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getHeaders().getContentType());
		assertArrayEquals("foo".getBytes(), FileCopyUtils.copyToByteArray(response.getBody()));
	}

	@Test
	public void created() throws Exception {
		URI location = new URI("/foo");
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withCreatedEntity(location);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(location, response.getHeaders().getLocation());
		assertNull(response.getBody());
	}

	@Test
	public void noContent() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withNoContent();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

	@Test
	public void badRequest() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withBadRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

	@Test
	public void unauthorized() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withUnauthorizedRequest();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

	@Test
	public void serverError() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withServerError();
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

	@Test
	public void withStatus() throws Exception {
		DefaultResponseCreator responseCreator = MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN);
		MockClientHttpResponse response = (MockClientHttpResponse) responseCreator.createResponse(null);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertTrue(response.getHeaders().isEmpty());
		assertNull(response.getBody());
	}

}
