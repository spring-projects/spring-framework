/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.AsyncHttpAccessor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * <strong>Spring's central class for asynchronous client-side HTTP access.</strong>
 * Exposes similar methods as {@link RestTemplate}, but returns {@link Future} wrappers
 * as opposed to concrete results.
 *
 * <p>The {@code AsyncRestTemplate} exposes a synchronous {@link RestTemplate} via the
 * {@link #getRestOperations()} method, and it shares its
 * {@linkplain #setErrorHandler(ResponseErrorHandler) error handler} and
 * {@linkplain #setMessageConverters(List) message converters} with this
 * {@code RestTemplate}.
 *
 * <p>For more information, please refer to the {@link RestTemplate} API documentation.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see RestTemplate
 */
public class AsyncRestTemplate extends AsyncHttpAccessor implements AsyncRestOperations {

	private final RestTemplate syncTemplate;


	/**
	 * Create a new instance of the {@link AsyncRestTemplate} using default settings.
	 * <p>This constructor uses a {@link SimpleClientHttpRequestFactory} in combination
	 * with a {@link SimpleAsyncTaskExecutor} for asynchronous execution.
	 */
	public AsyncRestTemplate() {
		this(new SimpleAsyncTaskExecutor());
	}

	/**
	 * Create a new instance of the {@link AsyncRestTemplate} using the given
	 * {@link AsyncTaskExecutor}.
	 * <p>This constructor uses a {@link SimpleClientHttpRequestFactory} in combination
	 * with the given {@code AsyncTaskExecutor} for asynchronous execution.
	 */
	public AsyncRestTemplate(AsyncTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "AsyncTaskExecutor must not be null");
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setTaskExecutor(taskExecutor);
		this.syncTemplate = new RestTemplate(requestFactory);
		setAsyncRequestFactory(requestFactory);
	}

	/**
	 * Create a new instance of the {@link AsyncRestTemplate} using the given
	 * {@link AsyncClientHttpRequestFactory}.
	 * <p>This constructor will cast the given asynchronous
	 * {@code AsyncClientHttpRequestFactory} to a {@link ClientHttpRequestFactory}. Since
	 * all implementations of {@code ClientHttpRequestFactory} provided in Spring also
	 * implement {@code AsyncClientHttpRequestFactory}, this should not result in a
	 * {@code ClassCastException}.
	 */
	public AsyncRestTemplate(AsyncClientHttpRequestFactory asyncRequestFactory) {
		this(asyncRequestFactory, (ClientHttpRequestFactory) asyncRequestFactory);
	}

	/**
	 * Creates a new instance of the {@link AsyncRestTemplate} using the given
	 * asynchronous and synchronous request factories.
	 * @param asyncRequestFactory the asynchronous request factory
	 * @param syncRequestFactory the synchronous request factory
	 */
	public AsyncRestTemplate(AsyncClientHttpRequestFactory asyncRequestFactory, ClientHttpRequestFactory syncRequestFactory) {
		this(asyncRequestFactory, new RestTemplate(syncRequestFactory));
	}

	/**
	 * Create a new instance of the {@link AsyncRestTemplate} using the given
	 * {@link AsyncClientHttpRequestFactory} and synchronous {@link RestTemplate}.
	 * @param requestFactory the asynchronous request factory to use
	 * @param restTemplate the synchronous template to use
	 */
	public AsyncRestTemplate(AsyncClientHttpRequestFactory requestFactory, RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.syncTemplate = restTemplate;
		setAsyncRequestFactory(requestFactory);
	}


	/**
	 * Set the error handler.
	 * <p>By default, AsyncRestTemplate uses a
	 * {@link org.springframework.web.client.DefaultResponseErrorHandler}.
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.syncTemplate.setErrorHandler(errorHandler);
	}

	/** Return the error handler. */
	public ResponseErrorHandler getErrorHandler() {
		return this.syncTemplate.getErrorHandler();
	}

	@Override
	public RestOperations getRestOperations() {
		return this.syncTemplate;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.syncTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * Return the message body converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return syncTemplate.getMessageConverters();
	}


	// GET

	@Override
	public <T> Future<ResponseEntity<T>> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, urlVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType) throws RestClientException {
		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}

	// HEAD

	@Override
	public Future<HttpHeaders> headForHeaders(String url, Object... uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public Future<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public Future<HttpHeaders> headForHeaders(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor);
	}

	// POST

	@Override
	public Future<URI> postForLocation(String url, HttpEntity<?> request,
			Object... uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture =
				execute(url, HttpMethod.POST, requestCallback, headersExtractor,
						uriVariables);
		return extractLocationHeader(headersFuture);
	}

	@Override
	public Future<URI> postForLocation(String url, HttpEntity<?> request,
			Map<String, ?> uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture =
				execute(url, HttpMethod.POST, requestCallback, headersExtractor,
						uriVariables);
		return extractLocationHeader(headersFuture);
	}

	@Override
	public Future<URI> postForLocation(URI url, HttpEntity<?> request)
			throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture =
				execute(url, HttpMethod.POST, requestCallback, headersExtractor);
		return extractLocationHeader(headersFuture);
	}

	private static Future<URI> extractLocationHeader(final Future<HttpHeaders> headersFuture) {
		return new Future<URI>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return headersFuture.cancel(mayInterruptIfRunning);
			}
			@Override
			public boolean isCancelled() {
				return headersFuture.isCancelled();
			}
			@Override
			public boolean isDone() {
				return headersFuture.isDone();
			}
			@Override
			public URI get() throws InterruptedException, ExecutionException {
				HttpHeaders headers = headersFuture.get();
				return headers.getLocation();
			}
			@Override
			public URI get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				HttpHeaders headers = headersFuture.get(timeout, unit);
				return headers.getLocation();
			}
		};
	}

	@Override
	public <T> Future<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor,
				uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor,
				uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> postForEntity(URI url, HttpEntity<?> request,
			Class<T> responseType) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}

	// PUT

	@Override
	public Future<Void> put(String url, HttpEntity<?> request, Object... uriVariables)
			throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public Future<Void> put(String url, HttpEntity<?> request,
			Map<String, ?> uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public Future<Void> put(URI url, HttpEntity<?> request) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null);
	}

	// DELETE

	@Override
	public Future<Void> delete(String url, Object... urlVariables)
			throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	@Override
	public Future<Void> delete(String url, Map<String, ?> urlVariables)
			throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	@Override
	public Future<Void> delete(URI url) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null);
	}

	// OPTIONS

	@Override
	public Future<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		return extractAllowHeader(headersFuture);
	}

	@Override
	public Future<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
		return extractAllowHeader(headersFuture);
	}

	@Override
	public Future<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		Future<HttpHeaders> headersFuture = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
		return extractAllowHeader(headersFuture);
	}

	private static Future<Set<HttpMethod>> extractAllowHeader(final Future<HttpHeaders> headersFuture) {
		return new Future<Set<HttpMethod>>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return headersFuture.cancel(mayInterruptIfRunning);
			}
			@Override
			public boolean isCancelled() {
				return headersFuture.isCancelled();
			}
			@Override
			public boolean isDone() {
				return headersFuture.isDone();
			}
			@Override
			public Set<HttpMethod> get() throws InterruptedException, ExecutionException {
				HttpHeaders headers = headersFuture.get();
				return headers.getAllow();
			}
			@Override
			public Set<HttpMethod> get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				HttpHeaders headers = headersFuture.get(timeout, unit);
				return headers.getAllow();
			}
		};
	}


	// exchange

	@Override
	public <T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException {
		AsyncRequestCallback requestCallback =
				httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback =
				httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException {
		AsyncRequestCallback requestCallback =
				httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... uriVariables) throws RestClientException {
		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {
		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> Future<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {
		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor =
				responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor);
	}


	// general execution

	@Override
	public <T> Future<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Object... urlVariables) throws RestClientException {

		URI expanded = new UriTemplate(url).expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> Future<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Map<String, ?> urlVariables) throws RestClientException {

		URI expanded = new UriTemplate(url).expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> Future<T> execute(URI url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor)
			throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * Execute the given method on the provided URI. The
	 * {@link org.springframework.http.client.ClientHttpRequest}
	 * is processed using the {@link RequestCallback}; the response with
	 * the {@link ResponseExtractor}.
	 * @param url the fully-expanded URL to connect to
	 * @param method the HTTP method to execute (GET, POST, etc.)
	 * @param requestCallback object that prepares the request (can be {@code null})
	 * @param responseExtractor object that extracts the return value from the response (can
	 * be {@code null})
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	@SuppressWarnings("unchecked")
	protected <T> Future<T> doExecute(URI url, HttpMethod method, AsyncRequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "'url' must not be null");
		Assert.notNull(method, "'method' must not be null");
		try {
			AsyncClientHttpRequest request = createAsyncRequest(url, method);
			if (requestCallback != null) {
				requestCallback.doWithRequest(request);
			}
			Future<ClientHttpResponse> responseFuture = request.executeAsync();
			if (responseExtractor != null) {
				return new ResponseExtractorFuture<T>(method, url, responseFuture, responseExtractor);
			}
			else {
				return (Future<T>) new VoidResponseFuture(method, url, responseFuture);
			}
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + url + "\":" + ex.getMessage(), ex);
		}
	}

	private void logResponseStatus(HttpMethod method, URI url, ClientHttpResponse response) {
		if (logger.isDebugEnabled()) {
			try {
				logger.debug("Async " + method.name() + " request for \"" + url +
						"\" resulted in " + response.getStatusCode() + " (" +
						response.getStatusText() + ")");
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	private void handleResponseError(HttpMethod method, URI url, ClientHttpResponse response) throws IOException {
		if (logger.isWarnEnabled()) {
			try {
				logger.warn("Async " + method.name() + " request for \"" + url +
						"\" resulted in " + response.getStatusCode() + " (" +
						response.getStatusText() + "); invoking error handler");
			}
			catch (IOException ex) {
				// ignore
			}
		}
		getErrorHandler().handleError(response);
	}

	/**
	 * Returns a request callback implementation that prepares the request {@code Accept}
	 * headers based on the given response type and configured {@linkplain
	 * #getMessageConverters() message converters}.
	 */
	protected <T> AsyncRequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.acceptHeaderRequestCallback(responseType));
	}

	/**
	 * Returns a request callback implementation that writes the given object to the
	 * request stream.
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(HttpEntity<T> requestBody) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(requestBody));
	}

	/**
	 * Returns a request callback implementation that writes the given object to the
	 * request stream.
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(HttpEntity<T> request, Type responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(request, responseType));
	}

	/**
	 * Returns a response extractor for {@link ResponseEntity}.
	 */
	protected <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return this.syncTemplate.responseEntityExtractor(responseType);
	}

	/**
	 * Returns a response extractor for {@link HttpHeaders}.
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.syncTemplate.headersExtractor();
	}


	private abstract class ResponseFuture<T> implements Future<T> {

		private final HttpMethod method;

		private final URI url;

		private final Future<ClientHttpResponse> responseFuture;

		public ResponseFuture(HttpMethod method, URI url, Future<ClientHttpResponse> responseFuture) {
			this.method = method;
			this.url = url;
			this.responseFuture = responseFuture;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return this.responseFuture.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return this.responseFuture.isCancelled();
		}

		@Override
		public boolean isDone() {
			return this.responseFuture.isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			return getInternal(this.responseFuture.get());
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return getInternal(this.responseFuture.get(timeout, unit));
		}

		private T getInternal(ClientHttpResponse response) throws ExecutionException {
			try {
				if (!getErrorHandler().hasError(response)) {
					logResponseStatus(this.method, this.url, response);
				}
				else {
					handleResponseError(this.method, this.url, response);
				}
				return extractData(response);
			}
			catch (IOException ex) {
				throw new ExecutionException(ex);
			}
			finally {
				if (response != null) {
					response.close();
				}
			}
		}

		protected abstract T extractData(ClientHttpResponse response)
				throws IOException;

	}


	private class ResponseExtractorFuture<T> extends ResponseFuture<T> {

		private final ResponseExtractor<T> responseExtractor;

		public ResponseExtractorFuture(HttpMethod method, URI url, Future<ClientHttpResponse> responseFuture,
				ResponseExtractor<T> responseExtractor) {
			super(method, url, responseFuture);
			this.responseExtractor = responseExtractor;
		}

		@Override
		protected T extractData(ClientHttpResponse response) throws IOException {
				return responseExtractor.extractData(response);
		}
	}


	private class VoidResponseFuture extends ResponseFuture<Void> {

		public VoidResponseFuture(HttpMethod method, URI url, Future<ClientHttpResponse> responseFuture) {
			super(method, url, responseFuture);
		}

		@Override
		protected Void extractData(ClientHttpResponse response) throws IOException {
			return null;
		}
	}


	/**
	 * Adapts a {@link RequestCallback} to the {@link AsyncRequestCallback} interface.
	 */
	private static class AsyncRequestCallbackAdapter implements AsyncRequestCallback {

		private final RequestCallback adaptee;

		/**
		 * Create a new {@code AsyncRequestCallbackAdapter} from the given
		 * {@link RequestCallback}.
		 * @param requestCallback the callback to base this adapter on
		 */
		public AsyncRequestCallbackAdapter(RequestCallback requestCallback) {
			this.adaptee = requestCallback;
		}

		@Override
		public void doWithRequest(final AsyncClientHttpRequest request) throws IOException {
			if (this.adaptee != null) {
				this.adaptee.doWithRequest(new ClientHttpRequest() {
					@Override
					public ClientHttpResponse execute() throws IOException {
						throw new UnsupportedOperationException("execute not supported");
					}
					@Override
					public OutputStream getBody() throws IOException {
						return request.getBody();
					}
					@Override
					public HttpMethod getMethod() {
						return request.getMethod();
					}
					@Override
					public URI getURI() {
						return request.getURI();
					}
					@Override
					public HttpHeaders getHeaders() {
						return request.getHeaders();
					}
				});
			}
		}
	}

}
