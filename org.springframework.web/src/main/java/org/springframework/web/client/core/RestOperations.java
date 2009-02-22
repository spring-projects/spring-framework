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

import java.net.URI;
import java.util.EnumSet;
import java.util.Map;

import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;

/**
 * Interface specifying a basic set of RESTful operations. Implemented by {@link RestTemplate}. Not often used directly,
 * but a useful option to enhance testability, as it can easily be mocked or stubbed.
 *
 * @author Arjen Poutsma
 * @see RestTemplate
 * @since 3.0
 */
public interface RestOperations {

	// GET

	/**
	 * Retrieves a representation by doing a GET on the specified URL. URI Template variables are expanded using the
	 * given URI variables, if any.
	 *
	 * @param uri		  the URI to GET
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @return the converted object
	 */
	<T> T getForObject(String uri, Class<T> responseType, String... uriVariables);

	/**
	 * Retrieves a representation by doing a GET on the URI template. URI Template variables are expanded using the
	 * given map.
	 *
	 * @param uri		  the URI to GET
	 * @param responseType the type of the return value
	 * @param uriVariables the map containing variables for the URI template
	 * @return the converted object
	 */
	<T> T getForObject(String uri, Class<T> responseType, Map<String, String> uriVariables);

	// HEAD

	/**
	 * Retrieves all headers of the resource specified by the URI template. URI Template variables are expanded using
	 * the given URI variables, if any.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the variables to expand the template
	 * @return all HTTP headers of that resource
	 */
	HttpHeaders headForHeaders(String uri, String... uriVariables);

	/**
	 * Retrieves all headers of the resource specified by the URI template. URI Template variables are expanded using
	 * the given map.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource
	 */
	HttpHeaders headForHeaders(String uri, Map<String, String> uriVariables);

	// POST

	/**
	 * Creates a new resource by POSTing the given object to the URI template. The value of the <code>Location</code>,
	 * indicating where the new resource is stored, is returned. URI Template variables are expanded using the given URI
	 * variables, if any.
	 *
	 * @param uri	 the URI
	 * @param request the Object to be POSTED
	 * @return the value for the <code>Location</code> header
	 */
	URI postForLocation(String uri, Object request, String... uriVariables);

	/**
	 * Creates a new resource by POSTing the given object to URI template. The value of the <code>Location</code>,
	 * indicating where the new resource is stored, is returned. URI Template variables are expanded using the given
	 * map.
	 *
	 * @param uri		  the URI
	 * @param request	  the Object to be POSTed
	 * @param uriVariables the variables to expand the template
	 * @return the value for the <code>Location</code> header
	 */
	URI postForLocation(String uri, Object request, Map<String, String> uriVariables);

	// PUT

	/**
	 * Creates or updates a resource by PUTting the given object to the URI. URI Template variables are expanded using
	 * the given URI variables, if any.
	 *
	 * @param uri		  the URI
	 * @param request	  the Object to be POSTed
	 * @param uriVariables the variables to expand the template
	 */
	void put(String uri, Object request, String... uriVariables);

	/**
	 * Creates a new resource by PUTting the given object to URI template. URI Template variables are expanded using the
	 * given map.
	 *
	 * @param uri		  the URI
	 * @param request	  the Object to be POSTed
	 * @param uriVariables the variables to expand the template
	 */
	void put(String uri, Object request, Map<String, String> uriVariables);

	// DELETE

	/**
	 * Deletes the resources at the specified URI. URI Template variables are expanded using the given URI variables, if
	 * any.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the variables to expand in the template
	 */
	void delete(String uri, String... uriVariables);

	/**
	 * Deletes the resources at the specified URI. URI Template variables are expanded using the given map.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the variables to expand the template
	 */
	void delete(String uri, Map<String, String> uriVariables);

	//OPTIONS

	/**
	 * Returns value of the Allow header for the given URI. URI Template variables are expanded using the given URI
	 * variables, if any.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header
	 */
	EnumSet<HttpMethod> optionsForAllow(String uri, String... uriVariables);

	/**
	 * Returns value of the Allow header for the given URI.  URI Template variables are expanded using the given map.
	 *
	 * @param uri		  the URI
	 * @param uriVariables the variables to expand in the template
	 * @return the value of the allow header
	 */
	EnumSet<HttpMethod> optionsForAllow(String uri, Map<String, String> uriVariables);

	/**
	 * Executes the HTTP methods to the given URI, preparing the request with the {@link HttpRequestCallback}, and
	 * reading the response with a {@link HttpResponseExtractor}. URI Template variables are expanded using the
	 * given URI variables, if any.
	 *
	 * @param uri			   the URI
	 * @param method			the HTTP method (GET, POST, etc)
	 * @param requestCallback   object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables	  the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link HttpResponseExtractor}
	 */
	<T> T execute(String uri,
			HttpMethod method,
			HttpRequestCallback requestCallback,
			HttpResponseExtractor<T> responseExtractor,
			String... uriVariables);

	/**
	 * Executes the HTTP methods to the given URI, preparing the request with the {@link HttpRequestCallback}, and
	 * reading the response with a {@link HttpResponseExtractor}. URI Template variables are expanded using the
	 * given URI variables map.
	 *
	 * @param uri			   the URI
	 * @param method			the HTTP method (GET, POST, etc)
	 * @param requestCallback   object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param uriVariables	  the variables to expand in the template
	 * @return an arbitrary object, as returned by the {@link HttpResponseExtractor}
	 */
	<T> T execute(String uri,
			HttpMethod method,
			HttpRequestCallback requestCallback,
			HttpResponseExtractor<T> responseExtractor,
			Map<String, String> uriVariables);


}
