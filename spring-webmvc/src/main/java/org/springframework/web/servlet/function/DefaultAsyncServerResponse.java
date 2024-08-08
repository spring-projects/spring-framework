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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link AsyncServerResponse} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
final class DefaultAsyncServerResponse extends ErrorHandlingServerResponse implements AsyncServerResponse {

	static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", DefaultAsyncServerResponse.class.getClassLoader());

	private final CompletableFuture<ServerResponse> futureResponse;

	@Nullable
	private final Duration timeout;


	DefaultAsyncServerResponse(CompletableFuture<ServerResponse> futureResponse, @Nullable Duration timeout) {
		this.futureResponse = futureResponse;
		this.timeout = timeout;
	}

	@Override
	public ServerResponse block() {
		try {
			if (this.timeout != null) {
				return this.futureResponse.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			else {
				return this.futureResponse.get();
			}
		}
		catch (InterruptedException | ExecutionException | TimeoutException ex) {
			throw new IllegalStateException("Failed to get future response", ex);
		}
	}

	@Override
	public HttpStatusCode statusCode() {
		return delegate(ServerResponse::statusCode);
	}

	@Override
	@Deprecated
	public int rawStatusCode() {
		return delegate(ServerResponse::rawStatusCode);
	}

	@Override
	public HttpHeaders headers() {
		return delegate(ServerResponse::headers);
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return delegate(ServerResponse::cookies);
	}

	private <R> R delegate(Function<ServerResponse, R> function) {
		ServerResponse response = this.futureResponse.getNow(null);
		if (response != null) {
			return function.apply(response);
		}
		else {
			throw new IllegalStateException("Future ServerResponse has not yet completed");
		}
	}

	@Nullable
	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException {

		writeAsync(request, response, createDeferredResult(request));
		return null;
	}

	static void writeAsync(HttpServletRequest request, HttpServletResponse response, DeferredResult<?> deferredResult)
			throws ServletException, IOException {

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
		asyncManager.setAsyncWebRequest(asyncWebRequest);
		try {
			asyncManager.startDeferredResultProcessing(deferredResult);
		}
		catch (IOException | ServletException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ServletException("Async processing failed", ex);
		}

	}

	private DeferredResult<ServerResponse> createDeferredResult(HttpServletRequest request) {
		DeferredResult<ServerResponse> result;
		if (this.timeout != null) {
			result = new DeferredResult<>(this.timeout.toMillis());
		}
		else {
			result = new DeferredResult<>();
		}
		this.futureResponse.whenComplete((value, ex) -> {
			if (ex != null) {
				if (ex instanceof CompletionException && ex.getCause() != null) {
					ex = ex.getCause();
				}
				ServerResponse errorResponse = errorResponse(ex, request);
				if (errorResponse != null) {
					result.setResult(errorResponse);
				}
				else {
					result.setErrorResult(ex);
				}
			}
			else {
				result.setResult(value);
			}
		});
		return result;
	}
}
