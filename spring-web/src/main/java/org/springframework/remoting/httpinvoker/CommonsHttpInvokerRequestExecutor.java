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

package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HttpInvokerRequestExecutor} implementation that uses
 * <a href="http://jakarta.apache.org/commons/httpclient">Jakarta Commons HttpClient</a>
 * to execute POST requests. Requires Commons HttpClient 3.0 or higher.
 *
 * <p>Allows to use a pre-configured {@link org.apache.commons.httpclient.HttpClient}
 * instance, potentially with authentication, HTTP connection pooling, etc.
 * Also designed for easy subclassing, providing specific template methods.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 1.1
 * @see SimpleHttpInvokerRequestExecutor
 */
public class CommonsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	/**
	 * Default timeout value if no HttpClient is explicitly provided.
	 */
	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

	private HttpClient httpClient;


	/**
	 * Create a new CommonsHttpInvokerRequestExecutor with a default
	 * HttpClient that uses a default MultiThreadedHttpConnectionManager.
	 * Sets the socket read timeout to {@link #DEFAULT_READ_TIMEOUT_MILLISECONDS}.
	 * @see org.apache.commons.httpclient.HttpClient
	 * @see org.apache.commons.httpclient.MultiThreadedHttpConnectionManager
	 */
	public CommonsHttpInvokerRequestExecutor() {
		this.httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		setReadTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS);
	}

	/**
	 * Create a new CommonsHttpInvokerRequestExecutor with the given
	 * HttpClient instance. The socket read timeout of the provided
	 * HttpClient will not be changed.
	 * @param httpClient the HttpClient instance to use for this request executor
	 */
	public CommonsHttpInvokerRequestExecutor(HttpClient httpClient) {
		this.httpClient = httpClient;
	}


	/**
	 * Set the HttpClient instance to use for this request executor.
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Return the HttpClient instance that this request executor uses.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Set the connection timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setConnectionTimeout(int)
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
	}

	/**
	 * Set the socket read timeout for the underlying HttpClient.
	 * A timeout value of 0 specifies an infinite timeout.
	 * @param timeout the timeout value in milliseconds
	 * @see org.apache.commons.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)
	 * @see #DEFAULT_READ_TIMEOUT_MILLISECONDS
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);
	}


	/**
	 * Execute the given request through Commons HttpClient.
	 * <p>This method implements the basic processing workflow:
	 * The actual work happens in this class's template methods.
	 * @see #createPostMethod
	 * @see #setRequestBody
	 * @see #executePostMethod
	 * @see #validateResponse
	 * @see #getResponseBody
	 */
	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {

		PostMethod postMethod = createPostMethod(config);
		try {
			setRequestBody(config, postMethod, baos);
			executePostMethod(config, getHttpClient(), postMethod);
			validateResponse(config, postMethod);
			InputStream responseBody = getResponseBody(config, postMethod);
			return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
		}
		finally {
			// Need to explicitly release because it might be pooled.
			postMethod.releaseConnection();
		}
	}

	/**
	 * Create a PostMethod for the given configuration.
	 * <p>The default implementation creates a standard PostMethod with
	 * "application/x-java-serialized-object" as "Content-Type" header.
	 * @param config the HTTP invoker configuration that specifies the
	 * target service
	 * @return the PostMethod instance
	 * @throws IOException if thrown by I/O methods
	 */
	protected PostMethod createPostMethod(HttpInvokerClientConfiguration config) throws IOException {
		PostMethod postMethod = new PostMethod(config.getServiceUrl());
		LocaleContext locale = LocaleContextHolder.getLocaleContext();
		if (locale != null) {
			postMethod.addRequestHeader(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale.getLocale()));
		}
		if (isAcceptGzipEncoding()) {
			postMethod.addRequestHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
		return postMethod;
	}

	/**
	 * Set the given serialized remote invocation as request body.
	 * <p>The default implementation simply sets the serialized invocation as the
	 * PostMethod's request body. This can be overridden, for example, to write a
	 * specific encoding and to potentially set appropriate HTTP request headers.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param postMethod the PostMethod to set the request body on
	 * @param baos the ByteArrayOutputStream that contains the serialized
	 * RemoteInvocation object
	 * @throws IOException if thrown by I/O methods
	 * @see org.apache.commons.httpclient.methods.PostMethod#setRequestBody(java.io.InputStream)
	 * @see org.apache.commons.httpclient.methods.PostMethod#setRequestEntity
	 * @see org.apache.commons.httpclient.methods.InputStreamRequestEntity
	 */
	protected void setRequestBody(
			HttpInvokerClientConfiguration config, PostMethod postMethod, ByteArrayOutputStream baos)
			throws IOException {

		postMethod.setRequestEntity(new ByteArrayRequestEntity(baos.toByteArray(), getContentType()));
	}

	/**
	 * Execute the given PostMethod instance.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param httpClient the HttpClient to execute on
	 * @param postMethod the PostMethod to execute
	 * @throws IOException if thrown by I/O methods
	 * @see org.apache.commons.httpclient.HttpClient#executeMethod(org.apache.commons.httpclient.HttpMethod)
	 */
	protected void executePostMethod(
			HttpInvokerClientConfiguration config, HttpClient httpClient, PostMethod postMethod)
			throws IOException {

		httpClient.executeMethod(postMethod);
	}

	/**
	 * Validate the given response as contained in the PostMethod object,
	 * throwing an exception if it does not correspond to a successful HTTP response.
	 * <p>Default implementation rejects any HTTP status code beyond 2xx, to avoid
	 * parsing the response body and trying to deserialize from a corrupted stream.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param postMethod the executed PostMethod to validate
	 * @throws IOException if validation failed
	 * @see org.apache.commons.httpclient.methods.PostMethod#getStatusCode()
	 * @see org.apache.commons.httpclient.HttpException
	 */
	protected void validateResponse(HttpInvokerClientConfiguration config, PostMethod postMethod)
			throws IOException {

		if (postMethod.getStatusCode() >= 300) {
			throw new HttpException(
					"Did not receive successful HTTP response: status code = " + postMethod.getStatusCode() +
					", status message = [" + postMethod.getStatusText() + "]");
		}
	}

	/**
	 * Extract the response body from the given executed remote invocation request.
	 * <p>The default implementation simply fetches the PostMethod's response body stream.
	 * If the response is recognized as GZIP response, the InputStream will get wrapped
	 * in a GZIPInputStream.
	 * @param config the HTTP invoker configuration that specifies the target service
	 * @param postMethod the PostMethod to read the response body from
	 * @return an InputStream for the response body
	 * @throws IOException if thrown by I/O methods
	 * @see #isGzipResponse
	 * @see java.util.zip.GZIPInputStream
	 * @see org.apache.commons.httpclient.methods.PostMethod#getResponseBodyAsStream()
	 * @see org.apache.commons.httpclient.methods.PostMethod#getResponseHeader(String)
	 */
	protected InputStream getResponseBody(HttpInvokerClientConfiguration config, PostMethod postMethod)
			throws IOException {

		if (isGzipResponse(postMethod)) {
			return new GZIPInputStream(postMethod.getResponseBodyAsStream());
		}
		else {
			return postMethod.getResponseBodyAsStream();
		}
	}

	/**
	 * Determine whether the given response indicates a GZIP response.
	 * <p>The default implementation checks whether the HTTP "Content-Encoding"
	 * header contains "gzip" (in any casing).
	 * @param postMethod the PostMethod to check
	 * @return whether the given response indicates a GZIP response
	 */
	protected boolean isGzipResponse(PostMethod postMethod) {
		Header encodingHeader = postMethod.getResponseHeader(HTTP_HEADER_CONTENT_ENCODING);
		return (encodingHeader != null && encodingHeader.getValue() != null &&
				encodingHeader.getValue().toLowerCase().contains(ENCODING_GZIP));
	}

}
