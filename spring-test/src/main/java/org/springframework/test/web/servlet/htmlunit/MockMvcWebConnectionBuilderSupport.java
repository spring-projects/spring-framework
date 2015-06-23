/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.test.web.servlet.htmlunit;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebConnection;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Makes it easy to create a WebConnection that uses MockMvc and optionally delegates to
 * a real WebConnection for specific requests. The default is to use MockMvc for any host
 * that is "localhost" and otherwise use a real WebConnection.
 *
 * @author Rob Winch
 * @since 4.2
 */
public abstract class MockMvcWebConnectionBuilderSupport<T extends MockMvcWebConnectionBuilderSupport<T>> {
	private String contextPath = "";

	private final MockMvc mockMvc;

	private List<WebRequestMatcher> mockMvcRequestMatchers = new ArrayList<WebRequestMatcher>();

	private boolean alwaysUseMockMvc;

	/**
	 * Creates a new instance using a MockMvc instance
	 *
	 * @param mockMvc the MockMvc instance to use. Cannot be null.
	 */
	protected MockMvcWebConnectionBuilderSupport(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "mockMvc cannot be null");
		this.mockMvc = mockMvc;
		this.mockMvcRequestMatchers.add(new HostRequestMatcher("localhost"));
	}

	/**
	 * Creates a new instance using a WebApplicationContext
	 * @param context the WebApplicationContext to create a MockMvc instance from.
	 * Cannot be null.
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context) {
		this(MockMvcBuilders.webAppContextSetup(context).build());
	}

	/**
	 * Creates a new instance using a WebApplicationContext
	 * @param context the WebApplicationContext to create a MockMvc instance from.
	 * @param configurer the MockMvcConfigurer to apply
	 * Cannot be null.
	 */
	protected MockMvcWebConnectionBuilderSupport(WebApplicationContext context, MockMvcConfigurer configurer) {
		this(MockMvcBuilders.webAppContextSetup(context).apply(configurer).build());
	}

	/**
	 * The context path to use. Default is "". If the value is null, then the first path
	 * segment of the request URL is assumed to be the context path.
	 *
	 * @param contextPath the context path to use.
	 * @return the builder for further customization
	 */
	@SuppressWarnings("unchecked")
	public T contextPath(String contextPath) {
		this.contextPath = contextPath;
		return (T) this;
	}

	/**
	 * Always use MockMvc no matter what the request looks like.
	 *
	 * @return the builder for further customization
	 */
	@SuppressWarnings("unchecked")
	public T alwaysUseMockMvc() {
		this.alwaysUseMockMvc = true;
		return (T) this;
	}

	/**
	 * Add additional WebRequestMatcher instances that if return true will ensure MockMvc
	 * is used.
	 *
	 * @param matchers the WebRequestMatcher instances that if true will ensure MockMvc
	 * processes the request.
	 * @return the builder for further customization
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvc(WebRequestMatcher... matchers) {
		for(WebRequestMatcher matcher : matchers) {
			this.mockMvcRequestMatchers.add(matcher);
		}
		return (T) this;
	}

	/**
	 * Add additional WebRequestMatcher instances that will return true if the host matches.
	 *
	 * @param hosts the additional hosts that will ensure MockMvc gets invoked (i.e. example.com or example.com:8080).
	 * @return the builder for further customization
	 */
	@SuppressWarnings("unchecked")
	public T useMockMvcForHosts(String... hosts) {
		this.mockMvcRequestMatchers.add(new HostRequestMatcher(hosts));
		return (T) this;
	}

	/**
	 * Creates a new WebConnection that will use a MockMvc instance if one of the
	 * specified WebRequestMatcher matches.
	 *
	 * @param defaultConnection the default WebConnection to use if none of the specified
	 * WebRequestMatcher instances match. Cannot be null.
	 * @return a new WebConnection that will use a MockMvc instance if one of the
	 * specified WebRequestMatcher matches.
	 *
	 * @see #alwaysUseMockMvc
	 * @see #useMockMvc(WebRequestMatcher...)
	 * @see #useMockMvcForHosts(String...)
	 */
	protected final WebConnection createConnection(WebConnection defaultConnection) {
		Assert.notNull(defaultConnection, "defaultConnection cannot be null");
		MockMvcWebConnection mockMvcWebConnection = new MockMvcWebConnection(mockMvc, contextPath);

		if(alwaysUseMockMvc) {
			return mockMvcWebConnection;
		}

		List<DelegatingWebConnection.DelegateWebConnection> delegates = new ArrayList<DelegatingWebConnection.DelegateWebConnection>(mockMvcRequestMatchers.size());
		for(WebRequestMatcher matcher : mockMvcRequestMatchers) {
			delegates.add(new DelegatingWebConnection.DelegateWebConnection(matcher, mockMvcWebConnection));
		}

		return new DelegatingWebConnection(defaultConnection, delegates);
	}
}