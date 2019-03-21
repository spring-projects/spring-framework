/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;

/**
 * Base class for {@link org.springframework.web.client.RestTemplate} and other HTTP accessing gateway helpers, adding
 * interceptor-related properties to {@link HttpAccessor}'s common properties.
 *
 * <p>Not intended to be used directly. See {@link org.springframework.web.client.RestTemplate}.
 *
 * @author Arjen Poutsma
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {

	private List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();

	/**
	 * Sets the request interceptors that this accessor should use.
	 */
	public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * Return the request interceptor that this accessor uses.
	 */
	public List<ClientHttpRequestInterceptor> getInterceptors() {
		return interceptors;
	}

	@Override
	public ClientHttpRequestFactory getRequestFactory() {
		ClientHttpRequestFactory delegate = super.getRequestFactory();
		if (!CollectionUtils.isEmpty(getInterceptors())) {
			return new InterceptingClientHttpRequestFactory(delegate, getInterceptors());
		}
		else {
			return delegate;
		}
	}

}
