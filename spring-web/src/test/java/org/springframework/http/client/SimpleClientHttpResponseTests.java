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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.StreamUtils;

/**
 * @author Brian Clozel
 */
public class SimpleClientHttpResponseTests {

	private final Charset UTF8 = Charset.forName("UTF-8");

	private SimpleClientHttpResponse response;

	private HttpURLConnection connection;

	@Before
	public void setup() throws Exception {
		this.connection = mock(HttpURLConnection.class);
		this.response = new SimpleClientHttpResponse(this.connection);
	}

	// SPR-14040
	@Test
	public void shouldNotCloseConnectionWhenResponseClosed() throws Exception {
		TestByteArrayInputStream is = new TestByteArrayInputStream("Spring".getBytes(UTF8));
		given(this.connection.getErrorStream()).willReturn(null);
		given(this.connection.getInputStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		assertThat(StreamUtils.copyToString(responseStream, UTF8), is("Spring"));

		this.response.close();
		assertTrue(is.isClosed());
		verify(this.connection, never()).disconnect();
	}

	// SPR-14040
	@Test
	public void shouldDrainStreamWhenResponseClosed() throws Exception {
		byte[] buf = new byte[6];
		TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(UTF8));
		given(this.connection.getErrorStream()).willReturn(null);
		given(this.connection.getInputStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		responseStream.read(buf);
		assertThat(new String(buf, UTF8), is("Spring"));
		assertThat(is.available(), is(6));

		this.response.close();
		assertThat(is.available(), is(0));
		assertTrue(is.isClosed());
		verify(this.connection, never()).disconnect();
	}

	// SPR-14040
	@Test
	public void shouldDrainErrorStreamWhenResponseClosed() throws Exception {
		byte[] buf = new byte[6];
		TestByteArrayInputStream is = new TestByteArrayInputStream("SpringSpring".getBytes(UTF8));
		given(this.connection.getErrorStream()).willReturn(is);

		InputStream responseStream = this.response.getBody();
		responseStream.read(buf);
		assertThat(new String(buf, UTF8), is("Spring"));
		assertThat(is.available(), is(6));

		this.response.close();
		assertThat(is.available(), is(0));
		assertTrue(is.isClosed());
		verify(this.connection, never()).disconnect();
	}


	class TestByteArrayInputStream extends ByteArrayInputStream {

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
