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

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Interface specifying a basic set of asynchronous RESTful operations. Implemented by
 * {@link AsyncRestTemplate}. Not often used directly, but a useful option to enhance
 * testability, as it can easily be mocked or stubbed.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface AsyncRestOperations {

	/**
	 * Expose the synchronous Spring RestTemplate to allow synchronous invocation.
	 */
	RestOperations getRestOperations();


	// GET

	/**
	 * Asynchronously retrieve an entity by doing a GET on the specified URL. The response is
	 * converted and stored in an {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * Asynchronously retrieve a representation by doing a GET on the URI template. The
	 * response is converted and stored in an {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param uriVariables the map containing variables for the URI template
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * Asynchronously retrieve a representation by doing a GET on the URL.
	 * The response is converted and stored in an {@link ResponseEntity}.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @return the entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType)
			throws RestClientException;

	// HEAD

	/**
	 * Asynchronously retrieve all headers of the resource specified by the URI template.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param uriVariables the variables to expand the template
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	Future<HttpHeaders> headForHeaders(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * Asynchronously retrieve all headers of the resource specified by the URI template.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param uriVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	Future<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * Asynchronously retrieve all headers of the resource specified by the URL.
	 * @param url the URL
	 * @return all HTTP headers of that resource wrapped in a {@link Future}
	 */
	Future<HttpHeaders> headForHeaders(URI url) throws RestClientException;

	// POST

	/**
	 * Create a new resource by POSTing the given object to the URI template, and
	 * asynchronously returns the value of the {@code Location} header. This header
	 * typically indicates where the new resource is stored.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	Future<URI> postForLocation(String url, HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * Create a new resource by POSTing the given object to the URI template, and
	 * asynchronously returns the value of the {@code Location} header. This header
	 * typically indicates where the new resource is stored.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	Future<URI> postForLocation(String url, HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * Create a new resource by POSTing the given object to the URL, and asynchronously
	 * returns the value of the {@code Location} header. This header typically indicates
	 * where the new resource is stored.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @return the value for the {@code Location} header wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	Future<URI> postForLocation(URI url, HttpEntity<?> request) throws RestClientException;

	/**
	 * Create a new resource by POSTing the given object to the URI template,
	 * and asynchronously returns the response as {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> Future<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException;

	/**
	 * Create a new resource by POSTing the given object to the URI template,
	 * and asynchronously returns the response as {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> Future<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * Create a new resource by POSTing the given object to the URL,
	 * and asynchronously returns the response as {@link ResponseEntity}.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @return the entity wrapped in a {@link Future}
	 * @see org.springframework.http.HttpEntity
	 */
	<T> Future<ResponseEntity<T>> postForEntity(URI url, HttpEntity<?> request,
			Class<T> responseType) throws RestClientException;

	// PUT

	/**
	 * Create or update a resource by PUTting the given object to the URI.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	Future<Void> put(String url, HttpEntity<?> request, Object... uriVariables)
			throws RestClientException;

	/**
	 * Creates a new resource by PUTting the given object to URI template.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param uriVariables the variables to expand the template
	 * @see HttpEntity
	 */
	Future<Void> put(String url, HttpEntity<?> request, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * Creates a new resource by PUTting the given object to URL.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @see HttpEntity
	 */
	Future<Void> put(URI url, HttpEntity<?> request) throws RestClientException;

	// DELETE

	/**
	 * Asynchronously delete the resources at the specified URI.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 */
	Future<Void> delete(String url, Object... uriVariables) throws RestClientException;

	/**
	 * Asynchronously delete the resources at the specified URI.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 */
	Future<Void> delete(String url, Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * Asynchronously delete the resources at the specified URI.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 */
	Future<Void> delete(URI url) throws RestClientException;

	// OPTIONS

	/**
	 * Asynchronously return the value of the Allow header for the given URI.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	Future<Set<HttpMethod>> optionsForAllow(String url, Object... uriVariables)
			throws RestClientException;

	/**
	 * Asynchronously return the value of the Allow header for the given URI.
	 * <p>URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	Future<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVariables)
			throws RestClientException;

	/**
	 * Asynchronously return the value of the Allow header for the given URL.
	 * @param url the URL
	 * @return the value of the allow header wrapped in a {@link Future}
	 */
	Future<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException;


	// exchange

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the
	 * given request entity to the request, and returns the response as
	 * {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables)
			throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the
	 * given request entity to the request, and returns the response as
	 * {@link ResponseEntity}.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the
	 * given request entity to the request, and returns the response as
	 * {@link ResponseEntity}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request
	 * (may be {@code null})
	 * @param responseType the type of the return value
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the given
	 * request entity to the request, and returns the response as {@link ResponseEntity}.
	 * The given {@link ParameterizedTypeReference} is used to pass generic type
	 * information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the
	 * request, may be {@code null}
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... uriVariables) throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the given
	 * request entity to the request, and returns the response as {@link ResponseEntity}.
	 * The given {@link ParameterizedTypeReference} is used to pass generic type
	 * information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may be {@code null}
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand in the template
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, writing the given
	 * request entity to the request, and returns the response as {@link ResponseEntity}.
	 * The given {@link ParameterizedTypeReference} is used to pass generic type
	 * information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may be {@code null}
	 * @param responseType the type of the return value
	 * @return the response as entity wrapped in a {@link Future}
	 */
	<T> Future<ResponseEntity<T>> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException;


	// general execution

	/**
	 * Asynchronously execute the HTTP method to the given URI template, preparing the
	 * request with the {@link AsyncRequestCallback}, and reading the response with a
	 * {@link ResponseExtractor}.
	 * <p>URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> Future<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Object... uriVariables) throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URI template, preparing the
	 * request with the {@link AsyncRequestCallback}, and reading the response with a
	 * {@link ResponseExtractor}.
	 * <p>URI Template variables are expanded using the given URI variables map.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> Future<T> execute(String url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor,
			Map<String, ?> uriVariables) throws RestClientException;

	/**
	 * Asynchronously execute the HTTP method to the given URL, preparing the request
	 * with the {@link AsyncRequestCallback}, and reading the response with a
	 * {@link ResponseExtractor}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 */
	<T> Future<T> execute(URI url, HttpMethod method,
			AsyncRequestCallback requestCallback, ResponseExtractor<T> responseExtractor)
			throws RestClientException;

}
