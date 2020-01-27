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

package org.springframework.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
public class SimpleClientHttpResponseTests {

	private final HttpURLConnection connection = mock(HttpURLConnection.class);

	private final SimpleClientHttpResponse response = new SimpleClientHttpResponse(this.connection);


	@Test  // SPR-14040
	public void shouldNotCloseConnectionWhenResponseClosed() throws Exception {
		TestByteArrayInputStream is = new TestByteArrayInputStream("Spring".getBytes(StandardCharsets.UTF_8));
		given(this.connection.getErrorStream()).willReturn(null);
		given(this.connection.getInputStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		assertThat(StreamUtils.copyToString(responseStream, StandardCharsets.UTF_8)).isEqualTo("Spring");

		this.response.close();
		assertThat(is.isClosed()).isTrue();
		verify(this.connection, never()).disconnect();
	}

	@Test  // SPR-14040
	public void shouldDrainStreamWhenResponseClosed() throws Exception {
		byte[] buf = new byte[6];
		TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
		given(this.connection.getErrorStream()).willReturn(null);
		given(this.connection.getInputStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		responseStream.read(buf);
		assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo("Spring");
		assertThat(is.available()).isEqualTo(6);

		this.response.close();
		assertThat(is.available()).isEqualTo(0);
		assertThat(is.isClosed()).isTrue();
		verify(this.connection, never()).disconnect();
	}

	@Test  // SPR-14040
	public void shouldDrainErrorStreamWhenResponseClosed() throws Exception {
		byte[] buf = new byte[6];
		TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
		given(this.connection.getErrorStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		responseStream.read(buf);
		assertThat(new String(buf, StandardCharsets.UTF_8)).isEqualTo("Spring");
		assertThat(is.available()).isEqualTo(6);

		this.response.close();
		assertThat(is.available()).isEqualTo(0);
		assertThat(is.isClosed()).isTrue();
		verify(this.connection, never()).disconnect();
	}

	@Test  // SPR-16773
	public void shouldNotDrainWhenErrorStreamClosed() throws Exception {
		InputStream is = mock(InputStream.class);
		given(this.connection.getErrorStream()).willReturn(is);
		willDoNothing().given(is).close();
		given(is.read(any())).willThrow(new NullPointerException("from HttpURLConnection#ErrorStream"));

		InputStream responseStream = this.response.getBody();
		responseStream.close();
		this.response.close();

		verify(is).close();
	}

	@Test // SPR-17181
	public void shouldDrainResponseEvenIfResponseNotRead() throws Exception {
		TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(StandardCharsets.UTF_8));
		given(this.connection.getErrorStream()).willReturn(null);
		given(this.connection.getInputStream()).willReturn(is);

		this.response.close();
		assertThat(is.available()).isEqualTo(0);
		assertThat(is.isClosed()).isTrue();
		verify(this.connection, never()).disconnect();
	}


	private static class TestByteArrayInputStream extends ByteArrayInputStream {

		private boolean closed;

		public TestByteArrayInputStream(byte[] buf) {
			super(buf);
			this.closed = false;
		}

		public boolean isClosed() {
			return closed;
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.closed = true;
		}
	}

}
