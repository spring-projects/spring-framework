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

package org.springframework.test.web.servlet.request;

import java.net.URI;

import jakarta.servlet.DispatcherType;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Static factory methods for {@link RequestBuilder RequestBuilders}.
 *
 * <h3>Integration with the Spring TestContext Framework</h3>
 * <p>Methods in this class will reuse a
 * {@link org.springframework.mock.web.MockServletContext MockServletContext}
 * that was created by the Spring TestContext Framework.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Greg Turnquist
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 3.2
 */
public abstract class MockMvcRequestBuilders {

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a GET request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder get(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a GET request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder get(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a POST request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder post(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a POST request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder post(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder put(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder put(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder patch(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PATCH request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder patch(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.PATCH).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder delete(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder delete(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder options(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder options(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a HEAD request.
	 * @param uri the URI
	 * @since 4.1
	 */
	public static MockHttpServletRequestBuilder head(URI uri) {
		return new MockHttpServletRequestBuilder(HttpMethod.HEAD).uri(uri);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod method, String uriTemplate, Object... uriVariables) {
		return new MockHttpServletRequestBuilder(method).uri(uriTemplate, uriVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 * @param uri the URI
	 * @since 4.0.3
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, URI uri) {
		return new MockHttpServletRequestBuilder(httpMethod).uri(uri);
	}

	/**
	 * Alternative factory method that allows for custom HTTP verbs (e.g. WebDAV).
	 * @param httpMethod the HTTP method
	 * @param uri the URI
	 * @since 4.3
	 * @deprecated in favor of {@link #request(HttpMethod, URI)}
	 */
	@Deprecated(since = "6.2")
	public static MockHttpServletRequestBuilder request(String httpMethod, URI uri) {
		return request(HttpMethod.valueOf(httpMethod), uri);
	}

	/**
	 * Create a {@link MockMultipartHttpServletRequestBuilder} for a multipart request,
	 * using POST as the HTTP method.
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 5.0
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(String uriTemplate, Object... uriVariables) {
		MockMultipartHttpServletRequestBuilder builder = new MockMultipartHttpServletRequestBuilder();
		builder.uri(uriTemplate, uriVariables);
		return builder;
	}

	/**
	 * Variant of {@link #multipart(String, Object...)} that also accepts an
	 * {@link HttpMethod}.
	 * @param httpMethod the HTTP method to use
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 * @since 5.3.22
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, String uriTemplate, Object... uriVariables) {
		MockMultipartHttpServletRequestBuilder builder = new MockMultipartHttpServletRequestBuilder(httpMethod);
		builder.uri(uriTemplate, uriVariables);
		return builder;
	}

	/**
	 * Variant of {@link #multipart(String, Object...)} with a {@link URI}.
	 * @param uri the URI
	 * @since 5.0
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(URI uri) {
		MockMultipartHttpServletRequestBuilder builder = new MockMultipartHttpServletRequestBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Variant of {@link #multipart(String, Object...)} with a {@link URI} and
	 * an {@link HttpMethod}.
	 * @param httpMethod the HTTP method to use
	 * @param uri the URI
	 * @since 5.3.21
	 */
	public static MockMultipartHttpServletRequestBuilder multipart(HttpMethod httpMethod, URI uri) {
		MockMultipartHttpServletRequestBuilder builder = new MockMultipartHttpServletRequestBuilder(httpMethod);
		builder.uri(uri);
		return builder;
	}

	/**
	 * Create a {@link RequestBuilder} for an async dispatch from the
	 * {@link MvcResult} of the request that started async processing.
	 * <p>Usage involves performing a request that starts async processing first:
	 * <pre class="code">
	 * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
	 *	.andExpect(request().asyncStarted())
	 *	.andReturn();
	 *  </pre>
	 * <p>And then performing the async dispatch re-using the {@code MvcResult}:
	 * <pre class="code">
	 * this.mockMvc.perform(asyncDispatch(mvcResult))
	 * 	.andExpect(status().isOk())
	 * 	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 * 	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	 * </pre>
	 * @param mvcResult the result from the request that started async processing
	 */
	public static RequestBuilder asyncDispatch(MvcResult mvcResult) {

		// There must be an async result before dispatching
		mvcResult.getAsyncResult();

		return servletContext -> {
			MockHttpServletRequest request = mvcResult.getRequest();
			request.setDispatcherType(DispatcherType.ASYNC);
			request.setAsyncStarted(false);
			return request;
		};
	}

}
