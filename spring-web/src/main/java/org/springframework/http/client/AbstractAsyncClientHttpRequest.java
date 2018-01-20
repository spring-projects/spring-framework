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
import java.io.OutputStream;

import com.codahale.metrics.MetricRegistry;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MetricConstants;
import org.springframework.http.MetricUtils;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * Abstract base for {@link AsyncClientHttpRequest} that makes sure that headers and body
 * are not written multiple times.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
abstract class AbstractAsyncClientHttpRequest implements AsyncClientHttpRequest {

	private final HttpHeaders headers = new HttpHeaders();

	private boolean executed = false;

	private MetricRegistry metricRegistry;

	@Override
	public final HttpHeaders getHeaders() {
		return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public final OutputStream getBody() throws IOException {
		assertNotExecuted();
		return getBodyInternal(this.headers);
	}

	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		// TODO
		// handle InterceptingAsyncClientHttpRequest
		final long prologue = System.currentTimeMillis();

		final MetricRegistry metricRegistry = this.getMetricRegistry();

		MetricUtils.mark(metricRegistry, MetricConstants.REQUESTS_PER_SECOND, 1);

		try {
			assertNotExecuted();
			ListenableFuture<ClientHttpResponse> result = executeInternal(this.headers);
			result.addCallback(new SuccessCallback<ClientHttpResponse>() {
				@Override
				public void onSuccess(ClientHttpResponse result) {
					long epilogue = System.currentTimeMillis();

					MetricUtils.mark(metricRegistry, MetricConstants.SUCCEEDED_REQUESTS_PER_SECOND, 1);
					MetricUtils.update(metricRegistry, MetricConstants.RESPONSE_TIME, epilogue - prologue);
				}
			}, new FailureCallback() {
				@Override
				public void onFailure(Throwable ex) {
					MetricUtils.mark(metricRegistry, MetricConstants.FAILED_REQUESTS_PER_SECOND, 1);
				}
			});
			this.executed = true;
			return result;
		} catch (IOException e) {
			MetricUtils.mark(metricRegistry, MetricConstants.FAILED_REQUESTS_PER_SECOND, 1);

			throw e;
		}
	}

	public MetricRegistry getMetricRegistry() {
		return this.metricRegistry;
	}

	public void setMetricRegistry(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	/**
	 * Asserts that this request has not been {@linkplain #executeAsync() executed} yet.
	 * @throws IllegalStateException if this request has been executed
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * Abstract template method that returns the body.
	 * @param headers the HTTP headers
	 * @return the body output stream
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * @param headers the HTTP headers
	 * @return the response object for the executed request
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers)
			throws IOException;

}
