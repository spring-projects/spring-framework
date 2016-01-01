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

package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Base class for {@link org.springframework.web.client.RestTemplate}
 * and other HTTP accessing gateway helpers, defining common properties
 * such as the {@link ClientHttpRequestFactory} to operate on.
 *
 * <p>Not intended to be used directly. See {@link org.springframework.web.client.RestTemplate}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.springframework.web.client.RestTemplate
 */
public abstract class HttpAccessor {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();


	/**
	 * Set the request factory that this accessor uses for obtaining
	 * {@link ClientHttpRequest HttpRequests}.
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.requestFactory = requestFactory;
	}

	/**
	 * Return the request factory that this accessor uses for obtaining {@link ClientHttpRequest HttpRequests}.
	 */
	public ClientHttpRequestFactory getRequestFactory() {
		return this.requestFactory;
	}


	/**
	 * Create a new {@link ClientHttpRequest} via this template's {@link ClientHttpRequestFactory}.
	 * @param url the URL to connect to
	 * @param method the HTTP method to exectute (GET, POST, etc.)
	 * @return the created request
	 * @throws IOException in case of I/O errors
	 */
	protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
		ClientHttpRequest request = getRequestFactory().createRequest(url, method);
		if (logger.isDebugEnabled()) {
			logger.debug("Created " + method.name() + " request for \"" + url + "\"");
		}
		return request;
	}

}
