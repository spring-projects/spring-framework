/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * Implementation of {@link ServerResponse} based on a {@link CompletableFuture}.
 *
 * @author Arjen Poutsma
 * @since 5.3
 * @see ServerResponse#async(Object)
 */
final class AsyncServerResponse extends ErrorHandlingServerResponse {

	static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", AsyncServerResponse.class.getClassLoader());


	private final CompletableFuture<ServerResponse> futureResponse;


	private AsyncServerResponse(CompletableFuture<ServerResponse> futureResponse) {
		this.futureResponse = futureResponse;
	}

	@Override
	public HttpStatus statusCode() {
		return delegate(ServerResponse::statusCode);
	}

	@Override
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
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context) {

		SharedAsyncContextHttpServletRequest sharedRequest = new SharedAsyncContextHttpServletRequest(request);
		AsyncContext asyncContext = sharedRequest.startAsync(request, response);
		this.futureResponse.whenComplete((futureResponse, futureThrowable) -> {
			try {
				if (futureResponse != null) {
					ModelAndView mav = futureResponse.writeTo(sharedRequest, response, context);
					Assert.state(mav == null, "Asynchronous, rendering ServerResponse implementations are not " +
							"supported in WebMvc.fn. Please use WebFlux.fn instead.");
				}
				else if (futureThrowable != null) {
					handleError(futureThrowable, request, response, context);
				}
			}
			catch (Throwable throwable) {
				try {
					handleError(throwable, request, response, context);
				}
				catch (ServletException | IOException ex) {
					logger.warn("Asynchronous execution resulted in exception", ex);
				}
			}
			finally {
				asyncContext.complete();
			}
		});
		return null;
	}


	@SuppressWarnings({"unchecked"})
	public static ServerResponse create(Object o) {
		Assert.notNull(o, "Argument to async must not be null");

		if (o instanceof CompletableFuture) {
			CompletableFuture<ServerResponse> futureResponse = (CompletableFuture<ServerResponse>) o;
			return new AsyncServerResponse(futureResponse);
		}
		else if (reactiveStreamsPresent) {
			ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
			ReactiveAdapter publisherAdapter = registry.getAdapter(o.getClass());
			if (publisherAdapter != null) {
				Publisher<ServerResponse> publisher = publisherAdapter.toPublisher(o);
				ReactiveAdapter futureAdapter = registry.getAdapter(CompletableFuture.class);
				if (futureAdapter != null) {
					CompletableFuture<ServerResponse> futureResponse =
							(CompletableFuture<ServerResponse>) futureAdapter.fromPublisher(publisher);
					return new AsyncServerResponse(futureResponse);
				}
			}
		}
		throw new IllegalArgumentException("Asynchronous type not supported: " + o.getClass());
	}


	/**
	 * HttpServletRequestWrapper that shares its AsyncContext between this
	 * AsyncServerResponse class and other, subsequent ServerResponse
	 * implementations, keeping track of how many contexts where
	 * started with startAsync(). This way, we make sure that
	 * {@link AsyncContext#complete()} only completes for the response that
	 * finishes last, and is not closed prematurely.
	 */
	private static final class SharedAsyncContextHttpServletRequest extends HttpServletRequestWrapper {

		private final AsyncContext asyncContext;

		private final AtomicInteger startedContexts;

		public SharedAsyncContextHttpServletRequest(HttpServletRequest request) {
			super(request);
			this.asyncContext = request.startAsync();
			this.startedContexts = new AtomicInteger(0);
		}

		private SharedAsyncContextHttpServletRequest(HttpServletRequest request, AsyncContext asyncContext,
				AtomicInteger startedContexts) {
			super(request);
			this.asyncContext = asyncContext;
			this.startedContexts = startedContexts;
		}

		@Override
		public AsyncContext startAsync() throws IllegalStateException {
			this.startedContexts.incrementAndGet();
			return new SharedAsyncContext(this.asyncContext, this, this.asyncContext.getResponse(),
					this.startedContexts);
		}

		@Override
		public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IllegalStateException {
			this.startedContexts.incrementAndGet();
			SharedAsyncContextHttpServletRequest sharedRequest;
			if (servletRequest instanceof SharedAsyncContextHttpServletRequest) {
				sharedRequest = (SharedAsyncContextHttpServletRequest) servletRequest;
			}
			else {
				sharedRequest = new SharedAsyncContextHttpServletRequest((HttpServletRequest) servletRequest,
						this.asyncContext, this.startedContexts);
			}
			return new SharedAsyncContext(this.asyncContext, sharedRequest, servletResponse, this.startedContexts);
		}

		@Override
		public AsyncContext getAsyncContext() {
			return new SharedAsyncContext(this.asyncContext, this, this.asyncContext.getResponse(), this.startedContexts);
		}


		private static final class SharedAsyncContext implements AsyncContext {

			private final AsyncContext delegate;

			private final AtomicInteger openContexts;

			private final ServletRequest request;

			private final ServletResponse response;


			public SharedAsyncContext(AsyncContext delegate, SharedAsyncContextHttpServletRequest request,
					ServletResponse response, AtomicInteger usageCount) {

				this.delegate = delegate;
				this.request = request;
				this.response = response;
				this.openContexts = usageCount;
			}

			@Override
			public void complete() {
				if (this.openContexts.decrementAndGet() == 0) {
					this.delegate.complete();
				}
			}

			@Override
			public ServletRequest getRequest() {
				return this.request;
			}

			@Override
			public ServletResponse getResponse() {
				return this.response;
			}

			@Override
			public boolean hasOriginalRequestAndResponse() {
				return this.delegate.hasOriginalRequestAndResponse();
			}

			@Override
			public void dispatch() {
				this.delegate.dispatch();
			}

			@Override
			public void dispatch(String path) {
				this.delegate.dispatch(path);
			}

			@Override
			public void dispatch(ServletContext context, String path) {
				this.delegate.dispatch(context, path);
			}

			@Override
			public void start(Runnable run) {
				this.delegate.start(run);
			}

			@Override
			public void addListener(AsyncListener listener) {
				this.delegate.addListener(listener);
			}

			@Override
			public void addListener(AsyncListener listener,
					ServletRequest servletRequest,
					ServletResponse servletResponse) {

				this.delegate.addListener(listener, servletRequest, servletResponse);
			}

			@Override
			public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
				return this.delegate.createListener(clazz);
			}

			@Override
			public void setTimeout(long timeout) {
				this.delegate.setTimeout(timeout);
			}

			@Override
			public long getTimeout() {
				return this.delegate.getTimeout();
			}
		}
	}
}
