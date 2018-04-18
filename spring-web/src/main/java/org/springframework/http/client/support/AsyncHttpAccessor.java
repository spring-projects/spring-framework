/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for {@link org.springframework.web.client.AsyncRestTemplate}
 * and other HTTP accessing gateway helpers, defining common properties
 * such as the {@link org.springframework.http.client.AsyncClientHttpRequestFactory}
 * to operate on.
 *
 * <p>Not intended to be used directly. See
 * {@link org.springframework.web.client.AsyncRestTemplate}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see org.springframework.web.client.AsyncRestTemplate
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
public class AsyncHttpAccessor {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory;


	/**
	 * Set the request factory that this accessor uses for obtaining {@link
	 * org.springframework.http.client.ClientHttpRequest HttpRequests}.
	 */
	public void setAsyncRequestFactory(
			org.springframework.http.client.AsyncClientHttpRequestFactory asyncRequestFactory) {

		Assert.notNull(asyncRequestFactory, "AsyncClientHttpRequestFactory must not be null");
		this.asyncRequestFactory = asyncRequestFactory;
	}

	/**
	 * Return the request factory that this accessor uses for obtaining {@link
	 * org.springframework.http.client.ClientHttpRequest HttpRequests}.
	 */
	public org.springframework.http.client.AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		Assert.state(asyncRequestFactory != null, "No AsyncClientHttpRequestFactory set");
		return this.asyncRequestFactory;
	}

	/**
	 * Create a new {@link org.springframework.http.client.AsyncClientHttpRequest} via this template's
	 * {@link org.springframework.http.client.AsyncClientHttpRequestFactory}.
	 * @param url the URL to connect to
	 * @param method the HTTP method to execute (GET, POST, etc.)
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	protected org.springframework.http.client.AsyncClientHttpRequest createAsyncRequest(URI url, HttpMethod method)
			throws IOException {

		org.springframework.http.client.AsyncClientHttpRequest request =
				getAsyncRequestFactory().createAsyncRequest(url, method);
		if (logger.isDebugEnabled()) {
			logger.debug("Created asynchronous " + method.name() + " request for \"" + url + "\"");
		}
		return request;
	}

}
