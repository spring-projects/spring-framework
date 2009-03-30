/*
 * Copyright 2002-2009 the original author or authors.
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * <strong>The central class for client-side HTTP access.</strong>. It simplifies communication with HTTP servers, and
 * enforces RESTful principles. It handles HTTP connections, leaving application code to provide URLs (with possible
 * template variables) and extract results.
 *
 * <p>The main entry points of this template are the methods named after the six main HTTP methods:
 * <table>
 * <tr><th>HTTP method</th><th>RestTemplate methods</th></tr>
 * <tr><td>DELETE</td><td>{@link #delete}</td></tr>
 * <tr><td>GET</td><td>{@link #getForObject}</td></tr>
 * <tr><td>HEAD</td><td>{@link #headForHeaders}</td></tr>
 * <tr><td>OPTIONS</td><td>{@link #optionsForAllow}</td></tr>
 * <tr><td>POST</td><td>{@link #postForLocation}</td></tr>
 * <tr><td>PUT</td><td>{@link #put}</td></tr>
 * <tr><td>any</td><td>{@link #execute}</td></tr>
 * </table>
 *
 * <p>Each of these methods takes {@linkplain UriTemplate uri template} arguments in two forms: as a {@code String}
 * variable arguments array, or as a {@code Map<String, String>}. The string varargs variant expands the given template
 * variables in order, so that
 * <pre>
 * String result = restTemplate.getForObject("http://example.com/hotels/{hotel}/bookings/{booking}", String.class,"42", "21");
 * </pre>
 * will perform a GET on {@code http://example.com/hotels/42/bookings/21}. The map variant expands the template
 * based on variable name, and is therefore more useful when using many variables, or when a single variable is used
 * multiple times. For example:
 * <pre>
 * Map&lt;String, String&gt; vars = Collections.singletonMap("hotel", "42");
 * String result = restTemplate.getForObject("http://example.com/hotels/{hotel}/rooms/{hotel}", String.class, vars);
 * </pre>
 * will perform a GET on {@code http://example.com/hotels/42/rooms/42}.
 *
 * <p>Objects passed to and returned from these methods are converted to and from HTTP messages by {@link
 * HttpMessageConverter} instances. Converters for the main mime types are registered by default, but you can also write
 * your own converter and register it via the {@link #setMessageConverters(HttpMessageConverter[]) messageConverters}
 * bean property.
 *
 * <p>This template uses a {@link org.springframework.http.client.SimpleClientHttpRequestFactory} and a {@link
 * DefaultResponseErrorHandler} as default strategies for creating HTTP connections or handling HTTP errors, respectively.
 * These defaults can be overridden through the {@link #setRequestFactory(ClientHttpRequestFactory) requestFactory} and
 * {@link #setErrorHandler(ResponseErrorHandler) errorHandler} bean properties.
 *
 * @author Arjen Poutsma
 * @see HttpMessageConverter
 * @see RequestCallback
 * @see ResponseExtractor
 * @see ResponseErrorHandler
 * @since 3.0
 */
public class RestTemplate extends HttpAccessor implements RestOperations {

	private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();

	private HttpMessageConverter<?>[] messageConverters =
			new HttpMessageConverter[]{new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
					new FormHttpMessageConverter(), new SourceHttpMessageConverter()};

	private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

	/** Create a new instance of the {@link RestTemplate} using default settings. */
	public RestTemplate() {
	}

	/**
	 * Create a new instance of the {@link RestTemplate} based on the given {@link ClientHttpRequestFactory}.
	 *
	 * @param requestFactory HTTP request factory to use
	 * @see org.springframework.http.client.SimpleClientHttpRequestFactory
	 * @see org.springframework.http.client.CommonsClientHttpRequestFactory
	 */
	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		setRequestFactory(requestFactory);
	}

	/**
	 * Set the message body converters to use. These converters are used to convert
	 * from and to HTTP requests and responses.
	 */
	public void setMessageConverters(HttpMessageConverter<?>[] messageConverters) {
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		this.messageConverters = messageConverters;
	}

	/**
	 * Returns the message body converters. These converters are used to convert
	 * from and to HTTP requests and responses.
	 */
	public HttpMessageConverter<?>[] getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Returns the message body converters that support a particular type.
	 *
	 * @param type the type to return converters for
	 * @return converters that support the given type
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<HttpMessageConverter<T>> getSupportedMessageConverters(Class<T> type) {
		HttpMessageConverter[] converters = getMessageConverters();
		List<HttpMessageConverter<T>> result = new ArrayList<HttpMessageConverter<T>>(converters.length);
		for (HttpMessageConverter converter : converters) {
			if (converter.supports(type)) {
				result.add((HttpMessageConverter<T>) converter);
			}
		}
		return result;
	}

	/** Set the error handler. */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		this.errorHandler = errorHandler;
	}

	/** Return the error handler. By default, this is the {@link DefaultResponseErrorHandler}. */
	public ResponseErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	// GET

	public <T> T getForObject(String url, Class<T> responseType, String... urlVariables) throws RestClientException {

		checkForSupportedMessageConverter(responseType);
		return execute(url, HttpMethod.GET, new GetCallback<T>(responseType),
				new HttpMessageConverterExtractor<T>(responseType), urlVariables);
	}

	public <T> T getForObject(String url, Class<T> responseType, Map<String, String> urlVariables)
			throws RestClientException {

		checkForSupportedMessageConverter(responseType);
		return execute(url, HttpMethod.GET, new GetCallback<T>(responseType),
				new HttpMessageConverterExtractor<T>(responseType), urlVariables);
	}

	// HEAD

	public HttpHeaders headForHeaders(String url, String... urlVariables) throws RestClientException {
		return execute(url, HttpMethod.HEAD, null, this.headersExtractor, urlVariables);
	}

	public HttpHeaders headForHeaders(String url, Map<String, String> urlVariables) throws RestClientException {
		return execute(url, HttpMethod.HEAD, null, this.headersExtractor, urlVariables);
	}

	// POST

	public URI postForLocation(String url, Object request, String... urlVariables) throws RestClientException {
		if (request != null) {
			checkForSupportedMessageConverter(request.getClass());
		}
		HttpHeaders headers =
				execute(url, HttpMethod.POST, new PostPutCallback(request), this.headersExtractor, urlVariables);
		return headers.getLocation();
	}

	public URI postForLocation(String url, Object request, Map<String, String> urlVariables)
			throws RestClientException {
		if (request != null) {
			checkForSupportedMessageConverter(request.getClass());
		}
		HttpHeaders headers =
				execute(url, HttpMethod.POST, new PostPutCallback(request), this.headersExtractor, urlVariables);
		return headers.getLocation();
	}

	// PUT

	public void put(String url, Object request, String... urlVariables) throws RestClientException {
		if (request != null) {
			checkForSupportedMessageConverter(request.getClass());
		}
		execute(url, HttpMethod.PUT, new PostPutCallback(request), null, urlVariables);
	}

	public void put(String url, Object request, Map<String, String> urlVariables) throws RestClientException {
		if (request != null) {
			checkForSupportedMessageConverter(request.getClass());
		}
		execute(url, HttpMethod.PUT, new PostPutCallback(request), null, urlVariables);
	}

	// DELETE

	public void delete(String url, String... urlVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	public void delete(String url, Map<String, String> urlVariables) throws RestClientException {
		execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	// OPTIONS

	public Set<HttpMethod> optionsForAllow(String url, String... urlVariables) throws RestClientException {

		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, this.headersExtractor, urlVariables);
		return headers.getAllow();
	}

	public Set<HttpMethod> optionsForAllow(String url, Map<String, String> urlVariables) throws RestClientException {

		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, this.headersExtractor, urlVariables);
		return headers.getAllow();
	}

	// general execution

	public <T> T execute(String url,
			HttpMethod method,
			RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor,
			String... urlVariables) throws RestClientException {

		UriTemplate uriTemplate = new UriTemplate(url);
		URI expanded = uriTemplate.expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	public <T> T execute(String url,
			HttpMethod method,
			RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor,
			Map<String, String> urlVariables) throws RestClientException {

		UriTemplate uriTemplate = new UriTemplate(url);
		URI expanded = uriTemplate.expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * Execute the given method on the provided URI. The {@link ClientHttpRequest} is processed using the {@link
	 * RequestCallback}; the response with the {@link ResponseExtractor}.
	 *
	 * @param url the fully-expanded URL to connect to
	 * @param method the HTTP method to execute (GET, POST, etc.)
	 * @param requestCallback object that prepares the request (can be <code>null</code>)
	 * @param responseExtractor object that extracts the return value from the response (can be <code>null</code>)
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	protected <T> T doExecute(URI url,
			HttpMethod method,
			RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "'url' must not be null");
		Assert.notNull(method, "'method' must not be null");
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = createRequest(url, method);
			if (requestCallback != null) {
				requestCallback.doWithRequest(request);
			}
			response = request.execute();
			if (getErrorHandler().hasError(response)) {
				getErrorHandler().handleError(response);
			}
			if (responseExtractor != null) {
				return responseExtractor.extractData(response);
			}
			else {
				return null;
			}
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error: " + ex.getMessage(), ex);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * Check whether any of the registered {@linkplain #setMessageConverters(HttpMessageConverter[]) message body
	 * converters} can convert the given type.
	 *
	 * @param type the type to check for
	 * @throws IllegalArgumentException if no supported entity converter can be found
	 * @see HttpMessageConverter#supports(Class)
	 */
	private void checkForSupportedMessageConverter(Class type) {
		for (HttpMessageConverter<?> entityConverter : getMessageConverters()) {
			if (entityConverter.supports(type)) {
				return;
			}
		}
		throw new IllegalArgumentException("Could not resolve HttpMessageConverter for [" + type.getName() + "]");
	}

	/** Request callback implementation that prepares the request's accept headers. */
	private class GetCallback<T> implements RequestCallback {

		private final Class<T> responseType;

		private GetCallback(Class<T> responseType) {
			this.responseType = responseType;
		}

		public void doWithRequest(ClientHttpRequest request) throws IOException {
			List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
			for (HttpMessageConverter<?> entityConverter : getSupportedMessageConverters(this.responseType)) {
				List<MediaType> supportedMediaTypes = entityConverter.getSupportedMediaTypes();
				for (MediaType supportedMediaType : supportedMediaTypes) {
					if (supportedMediaType.getCharSet() != null) {
						supportedMediaType =
								new MediaType(supportedMediaType.getType(), supportedMediaType.getSubtype());
					}
					allSupportedMediaTypes.add(supportedMediaType);
				}
			}
			Collections.sort(allSupportedMediaTypes);
			request.getHeaders().setAccept(allSupportedMediaTypes);
		}
	}

	/** Request callback implementation that writes the given object to the request stream. */
	private class PostPutCallback implements RequestCallback {

		private final Object request;

		private PostPutCallback(Object request) {
			this.request = request;
		}

		@SuppressWarnings("unchecked")
		public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
			if (request != null) {
				HttpMessageConverter entityConverter = getSupportedMessageConverters(this.request.getClass()).get(0);
				entityConverter.write(this.request, httpRequest);
			}
			else {
				httpRequest.getHeaders().setContentLength(0L);
			}
		}
	}

	/**
	 * Response extractor that uses the registered {@linkplain HttpMessageConverter entity converters}
	 * to convert the response into a type <code>T</code>.
	 */
	private class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {

		private final Class<T> responseType;

		private HttpMessageConverterExtractor(Class<T> responseType) {
			this.responseType = responseType;
		}

		public T extractData(ClientHttpResponse response) throws IOException {
			MediaType contentType = response.getHeaders().getContentType();
			if (contentType == null) {
				throw new RestClientException("Cannot extract response: no Content-Type found");
			}
			for (HttpMessageConverter<T> messageConverter : getSupportedMessageConverters(this.responseType)) {
				for (MediaType supportedMediaType : messageConverter.getSupportedMediaTypes()) {
					if (supportedMediaType.includes(contentType)) {
						return messageConverter.read(this.responseType, response);
					}
				}
			}
			throw new RestClientException(
					"Could not extract response: no suitable HttpMessageConverter found for response type [" +
							this.responseType.getName() + "] and content type [" + contentType + "]");
		}

	}

	/** Response extractor that extracts the response {@link HttpHeaders}. */
	private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {

		public HttpHeaders extractData(ClientHttpResponse response) throws IOException {
			return response.getHeaders();
		}
	}

}
