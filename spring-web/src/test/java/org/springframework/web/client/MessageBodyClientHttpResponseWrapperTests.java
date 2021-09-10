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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MessageBodyClientHttpResponseWrapper}.
 *
 * @since 5.3.10
 * @author Yin-Jui Liao
 */
class MessageBodyClientHttpResponseWrapperTests {

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);

	private final MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);


	@Test
	void messageBodyDoesNotExist() throws Exception {
		given(response.getBody()).willReturn(null);
		assertThat(responseWrapper.hasEmptyMessageBody()).isTrue();
	}

	@Test
	void messageBodyExists() throws Exception {
		InputStream stream = new ByteArrayInputStream("content".getBytes());
		given(response.getBody()).willReturn(stream);
		assertThat(responseWrapper.hasEmptyMessageBody()).isFalse();
	}

}
