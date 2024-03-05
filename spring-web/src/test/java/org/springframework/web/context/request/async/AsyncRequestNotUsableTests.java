/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.context.request.async;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

/**
 * {@link StandardServletAsyncWebRequest} tests related to response wrapping in
 * order to enforce thread safety and prevent use after errors.
 *
 * @author Rossen Stoyanchev
 */
public class AsyncRequestNotUsableTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final HttpServletResponse response = mock(HttpServletResponse.class);

	private final ServletOutputStream outputStream = mock(ServletOutputStream.class);

	private final PrintWriter writer = mock(PrintWriter.class);

	private StandardServletAsyncWebRequest asyncRequest;


	@BeforeEach
	void setup() throws IOException {
		this.request.setAsyncSupported(true);
		given(this.response.getOutputStream()).willReturn(this.outputStream);
		given(this.response.getWriter()).willReturn(this.writer);

		this.asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
	}

	@AfterEach
	void tearDown() {
		assertThat(this.asyncRequest.stateLock().isLocked()).isFalse();
	}


	@SuppressWarnings("DataFlowIssue")
	private ServletOutputStream getWrappedOutputStream() throws IOException {
		return this.asyncRequest.getResponse().getOutputStream();
	}

	@SuppressWarnings("DataFlowIssue")
	private PrintWriter getWrappedWriter() throws IOException {
		return this.asyncRequest.getResponse().getWriter();
	}


	@Nested
	class ResponseTests {

		@Test
		void notUsableAfterError() throws IOException {
			asyncRequest.startAsync();
			asyncRequest.onError(new AsyncEvent(new MockAsyncContext(request, response), new Exception()));

			HttpServletResponse wrapped = asyncRequest.getResponse();
			assertThat(wrapped).isNotNull();
			assertThatThrownBy(wrapped::getOutputStream).hasMessage("Response not usable after response errors.");
			assertThatThrownBy(wrapped::getWriter).hasMessage("Response not usable after response errors.");
			assertThatThrownBy(wrapped::flushBuffer).hasMessage("Response not usable after response errors.");
		}

		@Test
		void notUsableAfterCompletion() throws IOException {
			asyncRequest.startAsync();
			asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(request, response)));

			HttpServletResponse wrapped = asyncRequest.getResponse();
			assertThat(wrapped).isNotNull();
			assertThatThrownBy(wrapped::getOutputStream).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::getWriter).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::flushBuffer).hasMessage("Response not usable after async request completion.");
		}

		@Test
		void notUsableWhenRecreatedAfterCompletion() throws IOException {
			asyncRequest.startAsync();
			asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(request, response)));

			StandardServletAsyncWebRequest newWebRequest =
					new StandardServletAsyncWebRequest(request, response, asyncRequest);

			HttpServletResponse wrapped = newWebRequest.getResponse();
			assertThat(wrapped).isNotNull();
			assertThatThrownBy(wrapped::getOutputStream).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::getWriter).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::flushBuffer).hasMessage("Response not usable after async request completion.");
		}
	}


	@Nested
	class OutputStreamTests {

		@Test
		void use() throws IOException {
			testUseOutputStream();
		}

		@Test
		void useInAsyncState() throws IOException {
			asyncRequest.startAsync();
			testUseOutputStream();
		}

		private void testUseOutputStream() throws IOException {
			ServletOutputStream wrapped = getWrappedOutputStream();

			wrapped.write('a');
			wrapped.write(new byte[0], 1, 2);
			wrapped.flush();
			wrapped.close();

			verify(outputStream).write('a');
			verify(outputStream).write(new byte[0], 1, 2);
			verify(outputStream).flush();
			verify(outputStream).close();
		}

		@Test
		void notUsableAfterCompletion() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(request, response)));

			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(() -> wrapped.write(new byte[0])).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(() -> wrapped.write(new byte[0], 0, 0)).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::flush).hasMessage("Response not usable after async request completion.");
			assertThatThrownBy(wrapped::close).hasMessage("Response not usable after async request completion.");
		}

		@Test
		void lockingNotUsed() throws IOException {
			AtomicInteger count = new AtomicInteger(-1);
			doAnswer((Answer<Void>) invocation -> {
				count.set(asyncRequest.stateLock().getHoldCount());
				return null;
			}).when(outputStream).write('a');

			// Access ServletOutputStream in NEW state (no async handling) without locking
			getWrappedOutputStream().write('a');

			assertThat(count.get()).isEqualTo(0);
		}

		@Test
		void lockingUsedInAsyncState() throws IOException {
			AtomicInteger count = new AtomicInteger(-1);
			doAnswer((Answer<Void>) invocation -> {
				count.set(asyncRequest.stateLock().getHoldCount());
				return null;
			}).when(outputStream).write('a');

			// Access ServletOutputStream in ASYNC state with locking
			asyncRequest.startAsync();
			getWrappedOutputStream().write('a');

			assertThat(count.get()).isEqualTo(1);
		}
	}


	@Nested
	class OutputStreamErrorTests {

		@Test
		void writeInt() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			doThrow(new IOException("Broken pipe")).when(outputStream).write('a');
			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("ServletOutputStream failed to write: Broken pipe");
		}

		@Test
		void writeBytesFull() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			byte[] bytes = new byte[0];
			doThrow(new IOException("Broken pipe")).when(outputStream).write(bytes, 0, 0);
			assertThatThrownBy(() -> wrapped.write(bytes)).hasMessage("ServletOutputStream failed to write: Broken pipe");
		}

		@Test
		void writeBytes() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			byte[] bytes = new byte[0];
			doThrow(new IOException("Broken pipe")).when(outputStream).write(bytes, 0, 0);
			assertThatThrownBy(() -> wrapped.write(bytes, 0, 0)).hasMessage("ServletOutputStream failed to write: Broken pipe");
		}

		@Test
		void flush() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			doThrow(new IOException("Broken pipe")).when(outputStream).flush();
			assertThatThrownBy(wrapped::flush).hasMessage("ServletOutputStream failed to flush: Broken pipe");
		}

		@Test
		void close() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			doThrow(new IOException("Broken pipe")).when(outputStream).close();
			assertThatThrownBy(wrapped::close).hasMessage("ServletOutputStream failed to close: Broken pipe");
		}

		@Test
		void writeErrorPreventsFurtherWriting() throws IOException {
			ServletOutputStream wrapped = getWrappedOutputStream();

			doThrow(new IOException("Broken pipe")).when(outputStream).write('a');
			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("ServletOutputStream failed to write: Broken pipe");
			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("Response not usable after response errors.");
		}

		@Test
		void writeErrorInAsyncStatePreventsFurtherWriting() throws IOException {
			asyncRequest.startAsync();
			ServletOutputStream wrapped = getWrappedOutputStream();

			doThrow(new IOException("Broken pipe")).when(outputStream).write('a');
			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("ServletOutputStream failed to write: Broken pipe");
			assertThatThrownBy(() -> wrapped.write('a')).hasMessage("Response not usable after response errors.");
		}
	}


	@Nested
	class WriterTests {

		@Test
		void useWriter() throws IOException {
			testUseWriter();
		}

		@Test
		void useWriterInAsyncState() throws IOException {
			asyncRequest.startAsync();
			testUseWriter();
		}

		private void testUseWriter() throws IOException {
			PrintWriter wrapped = getWrappedWriter();

			wrapped.write('a');
			wrapped.write(new char[0], 1, 2);
			wrapped.write("abc", 1, 2);
			wrapped.flush();
			wrapped.close();

			verify(writer).write('a');
			verify(writer).write(new char[0], 1, 2);
			verify(writer).write("abc", 1, 2);
			verify(writer).flush();
			verify(writer).close();
		}

		@Test
		void writerNotUsableAfterCompletion() throws IOException {
			asyncRequest.startAsync();
			PrintWriter wrapped = getWrappedWriter();

			asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(request, response)));

			char[] chars = new char[0];
			wrapped.write('a');
			wrapped.write(chars, 1, 2);
			wrapped.flush();
			wrapped.close();

			verifyNoInteractions(writer);
		}

		@Test
		void lockingNotUsed() throws IOException {
			AtomicInteger count = new AtomicInteger(-1);

			doAnswer((Answer<Void>) invocation -> {
				count.set(asyncRequest.stateLock().getHoldCount());
				return null;
			}).when(writer).write('a');

			// Use Writer in NEW state (no async handling) without locking
			PrintWriter wrapped = getWrappedWriter();
			wrapped.write('a');

			assertThat(count.get()).isEqualTo(0);
		}

		@Test
		void lockingUsedInAsyncState() throws IOException {
			AtomicInteger count = new AtomicInteger(-1);

			doAnswer((Answer<Void>) invocation -> {
				count.set(asyncRequest.stateLock().getHoldCount());
				return null;
			}).when(writer).write('a');

			// Use Writer in ASYNC state with locking
			asyncRequest.startAsync();
			PrintWriter wrapped = getWrappedWriter();
			wrapped.write('a');

			assertThat(count.get()).isEqualTo(1);
		}
	}

}
