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

package org.springframework.web.client.core;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.MediaType;
import org.springframework.web.client.HttpClientException;
import org.springframework.web.client.HttpIOException;
import org.springframework.web.client.support.HttpAccessor;
import org.springframework.web.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.converter.HttpMessageConverter;
import org.springframework.web.converter.StringHttpMessageConverter;
import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;
import org.springframework.web.http.client.ClientHttpRequest;
import org.springframework.web.http.client.ClientHttpRequestFactory;
import org.springframework.web.http.client.ClientHttpResponse;
import org.springframework.web.util.UriTemplate;

/**
 * <strong>The central class for client-side HTTP access.</strong>. It simplifies communication with HTTP servers, and
 * enforces RESTful principles. It handles HTTP connections, leaving application code to provide URLs (with possible
 * template variables) and extract results.
 *
 * <p>The main entry points of this template are the methods named after the five main HTTP methods:
 * <table>
 * <tr><th>HTTP method<th><th>RestTemplate methods</th></tr>
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
 * will perform a GET on {@code http://example.com/hotels/42/bookings/21}. The map variant is explands the template
 * based on variable name, and is therefore more useful when using many variables, or when a single variable is used
 * multiple times. For example:
 * <pre>
 * Map&lt;String, String&gt; vars = Collections.singletonMap("hotel", 42);
 * String result = restTemplate.getForObject("http://example.com/hotels/{hotel}/rooms/{hotel}", String.class, vars);
 * </pre>
 * will perform a GET on {@code http://example.com/hotels/42/rooms/42}.
 *
 * <p>Objects passed to and returned from these methods are converted to and from HTTP messages by {@link
 * HttpMessageConverter} instances. Converters for the main mime types are registered by default, but you can also write
 * your own converter and register it via the {@link #setMessageConverters(HttpMessageConverter[]) messageConverters}
 * bean property.
 *
 * <p>This template uses a {@link org.springframework.web.http.client.SimpleClientHttpRequestFactory} and a {@link
 * SimpleHttpErrorHandler} as default strategies for for creating HTTP connections or handling HTTP errors, respectively.
 * These defaults can be overridden through the {@link #setRequestFactory(ClientHttpRequestFactory) requestFactory} and
 * {@link #setErrorHandler(HttpErrorHandler) errorHandler} bean properties.
 *
 * @author Arjen Poutsma
 * @see HttpMessageConverter
 * @see HttpRequestCallback
 * @see HttpResponseExtractor
 * @see HttpErrorHandler
 * @since 3.0
 */
public class RestTemplate extends HttpAccessor implements RestOperations {

	private final HttpResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();

	private HttpMessageConverter<?>[] messageConverters;

	private HttpErrorHandler errorHandler;

	/**
	 * Creates a new instance of the {@link RestTemplate} using default settings.
	 *
	 * @see #initDefaultStrategies()
	 */
	public RestTemplate() {
		initDefaultStrategies();
	}

	/**
	 * Creates a new instance of the {@link RestTemplate} based on the given {@link ClientHttpRequestFactory}.
	 *
	 * @param requestFactory HTTP request factory to use
	 * @see org.springframework.web.http.client.SimpleClientHttpRequestFactory
	 * @see org.springframework.web.http.client.commons.CommonsClientHttpRequestFactory
	 */
	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		initDefaultStrategies();
		setRequestFactory(requestFactory);
	}

	/**
	 * Initializes the default stragegies for this template.
	 *
	 * <p>Default implementation sets up the {@link SimpleHttpErrorHandler} and the {@link ByteArrayHttpMessageConverter} and
	 * {@link StringHttpMessageConverter}.
	 */
	protected void initDefaultStrategies() {
		errorHandler = new SimpleHttpErrorHandler();
		messageConverters =
				new HttpMessageConverter[]{new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter()};
	}

	/**
	 * Returns the array of message body converters. These converters are used to covert from and to HTTP requests and
	 * responses.
	 */
	public HttpMessageConverter<?>[] getMessageConverters() {
		return messageConverters;
	}

	/**
	 * Returns the list of message body converters that support a particular type.
	 *
	 * @param type the type to return converters for
	 * @return converts that support the given type
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

	/**
	 * Sets the array of message body converters to use. These converters are used to covert from and to HTTP requests and
	 * responses.
	 *
	 * <strong>Note</strong> that setting this property overrides the {@linkplain #initDefaultStrategies() default strategies}.
	 */
	public void setMessageConverters(HttpMessageConverter<?>[] messageConverters) {
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		this.messageConverters = messageConverters;
	}

	/**
	 * Returns the error handler. By default, this is the {@link SimpleHttpErrorHandler}.
	 */
	public HttpErrorHandler getErrorHandler() {
		return errorHandler;
	}

	/**
	 * Sets the error handler.
	 */
	public void setErrorHandler(HttpErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		this.errorHandler = errorHandler;
	}

	// GET

	public <T> T getForObject(String url, Class<T> responseType, String... urlVariables) {
		checkForSupportedEntityConverter(responseType);
		return execute(url, HttpMethod.GET, new GetCallback<T>(responseType),
				new HttpMessageConverterExtractor<T>(responseType), urlVariables);
	}

	public <T> T getForObject(String url, Class<T> responseType, Map<String, String> urlVariables) {
		checkForSupportedEntityConverter(responseType);
		return execute(url, HttpMethod.GET, new GetCallback<T>(responseType),
				new HttpMessageConverterExtractor<T>(responseType), urlVariables);
	}

	// POST

	public URI postForLocation(String url, Object request, String... urlVariables) {
		checkForSupportedEntityConverter(request.getClass());
		HttpHeaders headers =
				execute(url, HttpMethod.POST, new PostPutCallback(request), headersExtractor, urlVariables);
		return headers.getLocation();
	}

	public URI postForLocation(String url, Object request, Map<String, String> urlVariables) {
		checkForSupportedEntityConverter(request.getClass());
		HttpHeaders headers =
				execute(url, HttpMethod.POST, new PostPutCallback(request), headersExtractor, urlVariables);
		return headers.getLocation();
	}

	// PUT

	public void put(String url, Object request, String... urlVariables) {
		checkForSupportedEntityConverter(request.getClass());
		execute(url, HttpMethod.PUT, new PostPutCallback(request), null, urlVariables);
	}

	public void put(String url, Object request, Map<String, String> urlVariables) {
		checkForSupportedEntityConverter(request.getClass());
		execute(url, HttpMethod.PUT, new PostPutCallback(request), null, urlVariables);
	}

	// HEAD

	public HttpHeaders headForHeaders(String url, String... urlVariables) {
		return execute(url, HttpMethod.HEAD, null, headersExtractor, urlVariables);
	}

	public HttpHeaders headForHeaders(String url, Map<String, String> urlVariables) {
		return execute(url, HttpMethod.HEAD, null, headersExtractor, urlVariables);
	}

	// DELETE

	public void delete(String url, String... urlVariables) {
		execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	public void delete(String url, Map<String, String> urlVariables) {
		execute(url, HttpMethod.DELETE, null, null, urlVariables);
	}

	// OPTIONS

	public EnumSet<HttpMethod> optionsForAllow(String url, String... urlVariables) {
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, urlVariables);
		return headers.getAllow();
	}

	public EnumSet<HttpMethod> optionsForAllow(String url, Map<String, String> urlVariables) {
		HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, urlVariables);
		return headers.getAllow();
	}

	// execute

	public <T> T execute(String url,
			HttpMethod method,
			HttpRequestCallback requestCallback,
			HttpResponseExtractor<T> responseExtractor,
			String... urlVariables) {
		UriTemplate uriTemplate = new UriTemplate(url);
		URI expanded = uriTemplate.expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	public <T> T execute(String url,
			HttpMethod method,
			HttpRequestCallback requestCallback,
			HttpResponseExtractor<T> responseExtractor,
			Map<String, String> urlVariables) {
		UriTemplate uriTemplate = new UriTemplate(url);
		URI expanded = uriTemplate.expand(urlVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	/**
	 * Execute the given method on the provided URI. The {@link ClientHttpRequest} is processed using the {@link
	 * HttpRequestCallback}; the response with the {@link HttpResponseExtractor}.
	 *
	 * @param url			   the fully-expanded URL to connect to
	 * @param method			the HTTP method to execute (GET, POST, etc.)
	 * @param requestCallback   object that prepares the request. Can be <code>null</code>.
	 * @param responseExtractor object that extracts the return value from the response. Can be <code>null</code>.
	 * @return an arbitrary object, as returned by the {@link HttpResponseExtractor}
	 */
	protected <T> T doExecute(URI url,
			HttpMethod method,
			HttpRequestCallback requestCallback,
			HttpResponseExtractor<T> responseExtractor) {
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
			throw new HttpIOException("I/O error: " + ex.getMessage(), ex);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * Checks whether any of the registered {@linkplain #setMessageConverters(HttpMessageConverter[]) message body
	 * converters} can convert the given type.
	 *
	 * @param type the type to check for
	 * @throws IllegalArgumentException if no supported entity converter can be found
	 * @see HttpMessageConverter#supports(Class)
	 */
	private void checkForSupportedEntityConverter(Class type) {
		for (HttpMessageConverter<?> entityConverter : getMessageConverters()) {
			if (entityConverter.supports(type)) {
				return;
			}
		}
		throw new IllegalArgumentException("Could not resolve HttpMessageConverter for [" + type.getName() + "]");
	}

	/**
	 * Request callback implementation that sets the <code>Accept</code> header based on the registered {@linkplain
	 * HttpMessageConverter entity converters}.
	 */
	private class AcceptHeaderCallback implements HttpRequestCallback {

		public void doWithRequest(ClientHttpRequest request) throws IOException {
			List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
			for (HttpMessageConverter<?> entityConverter : getMessageConverters()) {
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

	private class GetCallback<T> implements HttpRequestCallback {

		private final Class<T> responseType;

		private GetCallback(Class<T> responseType) {
			this.responseType = responseType;
		}

		public void doWithRequest(ClientHttpRequest request) throws IOException {
			List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
			for (HttpMessageConverter<?> entityConverter : getSupportedMessageConverters(responseType)) {
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

	/**
	 * Extension of {@link AcceptHeaderCallback} that writes the given object to the request stream.
	 */
	private class PostPutCallback implements HttpRequestCallback {

		private final Object request;

		private PostPutCallback(Object request) {
			this.request = request;
		}

		@SuppressWarnings("unchecked")
		public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
			for (HttpMessageConverter entityConverter : getSupportedMessageConverters(request.getClass())) {
				entityConverter.write(request, httpRequest);
				break;
			}
		}

	}

	/**
	 * Response extractor that uses the registered {@linkplain HttpMessageConverter entity converters} to convert the
	 * response into a type <code>T</code>.
	 */
	private class HttpMessageConverterExtractor<T> implements HttpResponseExtractor<T> {

		private final Class<T> responseType;

		private HttpMessageConverterExtractor(Class<T> responseType) {
			this.responseType = responseType;
		}

		public T extractData(ClientHttpResponse response) throws IOException {
			MediaType contentType = response.getHeaders().getContentType();
			if (contentType == null) {
				throw new HttpClientException("Cannot extract response: no Content-Type found");
			}
			for (HttpMessageConverter<T> messageConverter : getSupportedMessageConverters(responseType)) {
				for (MediaType supportedMediaType : messageConverter.getSupportedMediaTypes()) {
					if (supportedMediaType.includes(contentType)) {
						return messageConverter.read(responseType, response);
					}
				}
			}
			throw new HttpClientException(
					"Could not extract response: no suitable HttpMessageConverter found for " + "response type [" +
							responseType.getName() + "] and content type [" + contentType + "]");
		}

	}

	/**
	 * Response extractor that extracts the response {@link HttpHeaders}.
	 */
	private static class HeadersExtractor implements HttpResponseExtractor<HttpHeaders> {

		public HttpHeaders extractData(ClientHttpResponse response) throws IOException {
			return response.getHeaders();
		}
	}
}
