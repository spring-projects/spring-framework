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
package org.springframework.test.web.servlet.request;

import java.lang.reflect.Method;

import javax.servlet.ServletContext;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.util.ReflectionUtils;

/**
 * Static factory methods for {@link RequestBuilder}s.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java
 * editor favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class MockMvcRequestBuilders {

	private MockMvcRequestBuilders() {
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a GET request.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockHttpServletRequestBuilder get(String urlTemplate, Object... urlVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.GET, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a POST request.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockHttpServletRequestBuilder post(String urlTemplate, Object... urlVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.POST, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a PUT request.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockHttpServletRequestBuilder put(String urlTemplate, Object... urlVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.PUT, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a DELETE request.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockHttpServletRequestBuilder delete(String urlTemplate, Object... urlVariables) {
		return new MockHttpServletRequestBuilder(HttpMethod.DELETE, urlTemplate, urlVariables);
	}

	/**
     * Create a {@link MockHttpServletRequestBuilder} for an OPTIONS request.
     *
     * @param urlTemplate a URL template; the resulting URL will be encoded
     * @param urlVariables zero or more URL variables
     */
    public static MockHttpServletRequestBuilder options(String urlTemplate, Object... urlVariables) {
        return new MockHttpServletRequestBuilder(HttpMethod.OPTIONS, urlTemplate, urlVariables);
    }


	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a request with the given HTTP method.
	 *
	 * @param httpMethod the HTTP method
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockHttpServletRequestBuilder request(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) {
		return new MockHttpServletRequestBuilder(httpMethod, urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link MockHttpServletRequestBuilder} for a multipart request.
	 *
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param urlVariables zero or more URL variables
	 */
	public static MockMultipartHttpServletRequestBuilder fileUpload(String urlTemplate, Object... urlVariables) {
		return new MockMultipartHttpServletRequestBuilder(urlTemplate, urlVariables);
	}

	/**
	 * Create a {@link RequestBuilder} for an async dispatch from the
	 * {@link MvcResult} of the request that started async processing.
	 *
	 * <p>Usage involves performing one request first that starts async processing:
	 * <pre class="code">
	 * MvcResult mvcResult = this.mockMvc.perform(get("/1"))
	 *	.andExpect(request().asyncStarted())
	 *	.andReturn();
	 *  </pre>
	 *
	 * <p>And then performing the async dispatch re-using the {@code MvcResult}:
	 * <pre class="code">
	 * this.mockMvc.perform(asyncDispatch(mvcResult))
	 * 	.andExpect(status().isOk())
	 * 	.andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 * 	.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	 * </pre>
	 *
	 * @param mvcResult the result from the request that started async processing
	 */
	public static RequestBuilder asyncDispatch(final MvcResult mvcResult) {
		return new RequestBuilder() {
			@Override
			public MockHttpServletRequest buildRequest(ServletContext servletContext) {
				MockHttpServletRequest request = mvcResult.getRequest();
				Method method = ReflectionUtils.findMethod(request.getClass(), "setAsyncStarted", boolean.class);
				method.setAccessible(true);
				ReflectionUtils.invokeMethod(method, request, false);
				return request;
			}
		};
	}

}
