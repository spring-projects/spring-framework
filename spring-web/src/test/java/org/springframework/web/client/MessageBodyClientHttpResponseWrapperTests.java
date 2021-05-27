/*
 * Copyright 2002-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for {MessageBodyClientHttpResponseWrapper}.
 *
 * @author Yin-Jui Liao
 */
class MessageBodyClientHttpResponseWrapperTests {

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);

	@Test
	void testMessageBodyNotExist() throws IOException {
		given(response.getBody()).willReturn(null);
		MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
		assertThat(responseWrapper.hasEmptyMessageBody()).isTrue();
	}

	@Test
	void testMessageBodyExist() throws IOException {
		String body = "Accepted request";
		InputStream stream = new ByteArrayInputStream(body.getBytes());
		given(response.getBody()).willReturn(stream);
		MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
		assertThat(responseWrapper.hasEmptyMessageBody()).isFalse();
	}
}
