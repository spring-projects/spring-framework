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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link ClientHttpRequestFactory} implementation that uses standard JDK facilities.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.net.HttpURLConnection
 * @see HttpComponentsClientHttpRequestFactory
 */
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

	private static final int DEFAULT_CHUNK_SIZE = 4096;


	private Proxy proxy;

	private boolean bufferRequestBody = true;

	private int chunkSize = DEFAULT_CHUNK_SIZE;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	private boolean outputStreaming = true;

	private AsyncListenableTaskExecutor taskExecutor;


	/**
	 * Set the {@link Proxy} to use for this request factory.
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Indicates whether this request factory should buffer the {@linkplain ClientHttpRequest#getBody() request body}
	 * internally.
	 * <p>Default is {@code true}. When sending large amounts of data via POST or PUT, it is recommended
	 * to change this property to {@code false}, so as not to run out of memory. This will result in a
	 * {@link ClientHttpRequest} that either streams directly to the underlying {@link HttpURLConnection}
	 * (if the {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length} is known in advance),
	 * or that will use "Chunked transfer encoding" (if the {@code Content-Length} is not known in advance).
	 * @see #setChunkSize(int)
	 * @see HttpURLConnection#setFixedLengthStreamingMode(int)
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * Sets the number of bytes to write in each chunk when not buffering request bodies locally.
	 * <p>Note that this parameter is only used when {@link #setBufferRequestBody(boolean) bufferRequestBody} is set
	 * to {@code false}, and the {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}
	 * is not known in advance.
	 * @see #setBufferRequestBody(boolean)
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * Set the underlying URLConnection's connect timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the underlying URLConnection's read timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Set if the underlying URLConnection can be set to 'output streaming' mode.
	 * Default is {@code true}.
	 * <p>When output streaming is enabled, authentication and redirection cannot be handled automatically.
	 * If output streaming is disabled, the {@link HttpURLConnection#setFixedLengthStreamingMode} and
	 * {@link HttpURLConnection#setChunkedStreamingMode} methods of the underlying connection will never
	 * be called.
	 * @param outputStreaming if output streaming is enabled
	 */
	public void setOutputStreaming(boolean outputStreaming) {
		this.outputStreaming = outputStreaming;
	}

	/**
	 * Set the task executor for this request factory. Setting this property is required
	 * for {@linkplain #createAsyncRequest(URI, HttpMethod) creating asynchronous requests}.
	 * @param taskExecutor the task executor
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingClientHttpRequest(connection, this.outputStreaming);
		}
		else {
			return new SimpleStreamingClientHttpRequest(connection, this.chunkSize, this.outputStreaming);
		}
	}


	/**
	 * {@inheritDoc}
	 * <p>Setting the {@link #setTaskExecutor taskExecutor} property is required before calling this method.
	 */
	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		Assert.state(this.taskExecutor != null,
				"Asynchronous execution requires an AsyncTaskExecutor to be set");

		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingAsyncClientHttpRequest(
					connection, this.outputStreaming, this.taskExecutor);
		}
		else {
			return new SimpleStreamingAsyncClientHttpRequest(
					connection, this.chunkSize, this.outputStreaming, this.taskExecutor);
		}
	}

	/**
	 * Opens and returns a connection to the given URL.
	 * <p>The default implementation uses the given {@linkplain #setProxy(java.net.Proxy) proxy} -
	 * if any - to open a connection.
	 * @param url the URL to open a connection to
	 * @param proxy the proxy to use, may be {@code null}
	 * @return the opened connection
	 * @throws IOException in case of I/O errors
	 */
	protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
		URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
		Assert.isInstanceOf(HttpURLConnection.class, urlConnection);
		return (HttpURLConnection) urlConnection;
	}

	/**
	 * Template method for preparing the given {@link HttpURLConnection}.
	 * <p>The default implementation prepares the connection for input and output, and sets the HTTP method.
	 * @param connection the connection to prepare
	 * @param httpMethod the HTTP request method ({@code GET}, {@code POST}, etc.)
	 * @throws IOException in case of I/O errors
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoInput(true);

		if ("GET".equals(httpMethod)) {
			connection.setInstanceFollowRedirects(true);
		}
		else {
			connection.setInstanceFollowRedirects(false);
		}

		if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
				"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod)) {
			connection.setDoOutput(true);
		}
		else {
			connection.setDoOutput(false);
		}

		connection.setRequestMethod(httpMethod);
	}

	/**
	 * Add the given headers to the given HTTP connection.
	 * @param connection the connection to add the headers to
	 * @param headers the headers to add
	 */
	static void addHeaders(HttpURLConnection connection, HttpHeaders headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)) {  // RFC 6265
				String headerValue = StringUtils
						.collectionToDelimitedString(entry.getValue(), "; ");
				connection.setRequestProperty(headerName, headerValue);
			}
			else {
				for (String headerValue : entry.getValue()) {
					String actualHeaderValue = headerValue != null ? headerValue : "";
					connection.addRequestProperty(headerName, actualHeaderValue);
				}
			}
		}
	}


	private static class HttpURLConnectionHelper {

		protected final HttpURLConnection connection;

		public HttpURLConnectionHelper(HttpURLConnection connection) {
			this.connection = connection;
		}

		public HttpMethod getMethod() {
			return HttpMethod.resolve(this.connection.getRequestMethod());
		}

		public URI getURI() {
			try {
				return this.connection.getURL().toURI();
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException(
						"Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
			}
		}

		public SimpleClientHttpResponse executeBuffered(HttpHeaders headers,
				byte[] bufferedOutput, boolean outputStreaming) throws IOException {
			addHeaders(this.connection, headers);

			// JDK <1.8 doesn't support getOutputStream with HTTP DELETE
			if (HttpMethod.DELETE == getMethod() && bufferedOutput.length == 0) {
				this.connection.setDoOutput(false);
			}

			if (this.connection.getDoOutput() && outputStreaming) {
				this.connection.setFixedLengthStreamingMode(bufferedOutput.length);
			}
			this.connection.connect();
			if (this.connection.getDoOutput()) {
				FileCopyUtils.copy(bufferedOutput, this.connection.getOutputStream());
			}

			return new SimpleClientHttpResponse(this.connection);
		}

		public void setFixedLengthStreamingMode(int contentLength) {
			this.connection.setFixedLengthStreamingMode(contentLength);
		}

		public void setChunkedStreamingMode(int chunkSize) {
			this.connection.setChunkedStreamingMode(chunkSize);
		}
	}

	private static class StreamingHttpURLConnectionHelper
			extends HttpURLConnectionHelper {

		private OutputStream body;

		public StreamingHttpURLConnectionHelper(HttpURLConnection connection) {
			super(connection);
		}

		public OutputStream getBody(HttpHeaders headers, boolean outputStreaming,
				int chunkSize) throws IOException {
			if (this.body == null) {
				if (outputStreaming) {
					int contentLength = (int) headers.getContentLength();
					if (contentLength >= 0) {
						this.connection.setFixedLengthStreamingMode(contentLength);
					}
					else {
						this.connection.setChunkedStreamingMode(chunkSize);
					}
				}
				addHeaders(this.connection, headers);
				this.connection.connect();
				this.body = this.connection.getOutputStream();
			}
			return StreamUtils.nonClosing(this.body);
		}

		public SimpleClientHttpResponse executeStreaming(HttpHeaders headers)
				throws IOException {
			try {
				if (body != null) {
					body.close();
				}
				else {
					addHeaders(this.connection, headers);
					this.connection.connect();
				}
			}
			catch (IOException ex) {
				// ignore
			}
			return new SimpleClientHttpResponse(this.connection);
		}

	}

	static class SimpleBufferingClientHttpRequest
			extends AbstractBufferingClientHttpRequest {

		private final HttpURLConnectionHelper connectionHelper;

		private final boolean outputStreaming;

		SimpleBufferingClientHttpRequest(HttpURLConnection connection,
				boolean outputStreaming) {
			this.connectionHelper = new HttpURLConnectionHelper(connection);
			this.outputStreaming = outputStreaming;
		}

		@Override
		public HttpMethod getMethod() {
			return this.connectionHelper.getMethod();
		}

		@Override
		public URI getURI() {
			return this.connectionHelper.getURI();
		}

		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers,
				byte[] bufferedOutput) throws IOException {
			return this.connectionHelper
					.executeBuffered(headers, bufferedOutput, outputStreaming);
		}

	}

	private static class SimpleStreamingClientHttpRequest
			extends AbstractClientHttpRequest {

		private final StreamingHttpURLConnectionHelper connectionHelper;

		private final int chunkSize;

		private final boolean outputStreaming;

		SimpleStreamingClientHttpRequest(HttpURLConnection connection, int chunkSize,
				boolean outputStreaming) {
			this.connectionHelper = new StreamingHttpURLConnectionHelper(connection);
			this.chunkSize = chunkSize;
			this.outputStreaming = outputStreaming;
		}

		@Override
		public HttpMethod getMethod() {
			return this.connectionHelper.getMethod();
		}

		@Override
		public URI getURI() {
			return this.connectionHelper.getURI();
		}

		@Override
		protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
			return this.connectionHelper.getBody(headers, outputStreaming, chunkSize);
		}

		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers)
				throws IOException {
			return this.connectionHelper.executeStreaming(headers);
		}

	}

	private static final class SimpleBufferingAsyncClientHttpRequest
			extends AbstractBufferingAsyncClientHttpRequest {

		private final HttpURLConnectionHelper connectionHelper;

		private final boolean outputStreaming;

		private final AsyncListenableTaskExecutor taskExecutor;

		SimpleBufferingAsyncClientHttpRequest(HttpURLConnection connection,
				boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

			this.connectionHelper = new HttpURLConnectionHelper(connection);
			this.outputStreaming = outputStreaming;
			this.taskExecutor = taskExecutor;
		}

		@Override
		public HttpMethod getMethod() {
			return this.connectionHelper.getMethod();
		}

		@Override
		public URI getURI() {
			return this.connectionHelper.getURI();
		}

		@Override
		protected ListenableFuture<ClientHttpResponse> executeInternal(
				final HttpHeaders headers, final byte[] bufferedOutput)
				throws IOException {
			return this.taskExecutor.submitListenable(new Callable<ClientHttpResponse>() {
				@Override
				public ClientHttpResponse call() throws Exception {
					return connectionHelper
							.executeBuffered(headers, bufferedOutput, outputStreaming);
				}
			});
		}


	}

	private static final class SimpleStreamingAsyncClientHttpRequest
			extends AbstractAsyncClientHttpRequest {

		private StreamingHttpURLConnectionHelper connectionHelper;

		private final int chunkSize;

		private final boolean outputStreaming;

		private final AsyncListenableTaskExecutor taskExecutor;

		SimpleStreamingAsyncClientHttpRequest(HttpURLConnection connection, int chunkSize,
				boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {
			this.connectionHelper = new StreamingHttpURLConnectionHelper(connection);

			this.chunkSize = chunkSize;
			this.outputStreaming = outputStreaming;
			this.taskExecutor = taskExecutor;
		}

		@Override
		public HttpMethod getMethod() {
			return this.connectionHelper.getMethod();
		}

		@Override
		public URI getURI() {
			return this.connectionHelper.getURI();
		}

		@Override
		protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
			return this.connectionHelper.getBody(headers, outputStreaming, chunkSize);
		}

		@Override
		protected ListenableFuture<ClientHttpResponse> executeInternal(
				final HttpHeaders headers) throws IOException {
			return this.taskExecutor.submitListenable(new Callable<ClientHttpResponse>() {
				@Override
				public ClientHttpResponse call() throws Exception {
					return connectionHelper.executeStreaming(headers);
				}
			});
		}

	}

	private static final class SimpleClientHttpResponse
			extends AbstractClientHttpResponse {

		private final HttpURLConnection connection;

		private HttpHeaders headers;

		SimpleClientHttpResponse(HttpURLConnection connection) {
			this.connection = connection;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return this.connection.getResponseCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.connection.getResponseMessage();
		}

		@Override
		public HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
				// Header field 0 is the status line for most HttpURLConnections, but not on GAE
				String name = this.connection.getHeaderFieldKey(0);
				if (StringUtils.hasLength(name)) {
					this.headers.add(name, this.connection.getHeaderField(0));
				}
				int i = 1;
				while (true) {
					name = this.connection.getHeaderFieldKey(i);
					if (!StringUtils.hasLength(name)) {
						break;
					}
					this.headers.add(name, this.connection.getHeaderField(i));
					i++;
				}
			}
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			InputStream errorStream = this.connection.getErrorStream();
			return (errorStream != null ? errorStream : this.connection.getInputStream());
		}

		@Override
		public void close() {
			this.connection.disconnect();
		}

	}
}
