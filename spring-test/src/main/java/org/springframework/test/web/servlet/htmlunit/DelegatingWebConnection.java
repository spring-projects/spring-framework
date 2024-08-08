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

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;

import org.springframework.util.Assert;

/**
 * Implementation of {@link WebConnection} that allows delegating to various
 * {@code WebConnection} implementations.
 *
 * <p>For example, if you host your JavaScript on the domain {@code code.jquery.com},
 * you might want to use the following.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 *
 * MockMvc mockMvc = ...
 * MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);
 *
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * WebConnection httpConnection = new HttpWebConnection(webClient);
 * WebConnection webConnection = new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection));
 *
 * webClient.setWebConnection(webConnection);
 *
 * WebClient webClient = new WebClient();
 * webClient.setWebConnection(webConnection);
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public final class DelegatingWebConnection implements WebConnection {

	private final List<DelegateWebConnection> connections;

	private final WebConnection defaultConnection;


	public DelegatingWebConnection(WebConnection defaultConnection, List<DelegateWebConnection> connections) {
		Assert.notNull(defaultConnection, "Default WebConnection must not be null");
		Assert.notEmpty(connections, "Connections List must not be empty");
		this.connections = connections;
		this.defaultConnection = defaultConnection;
	}

	public DelegatingWebConnection(WebConnection defaultConnection, DelegateWebConnection... connections) {
		this(defaultConnection, Arrays.asList(connections));
	}


	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		for (DelegateWebConnection connection : this.connections) {
			if (connection.getMatcher().matches(request)) {
				return connection.getDelegate().getResponse(request);
			}
		}
		return this.defaultConnection.getResponse(request);
	}

	@Override
	public void close() {
	}


	/**
	 * The delegate web connection.
	 */
	public static final class DelegateWebConnection {

		private final WebRequestMatcher matcher;

		private final WebConnection delegate;

		public DelegateWebConnection(WebRequestMatcher matcher, WebConnection delegate) {
			this.matcher = matcher;
			this.delegate = delegate;
		}

		private WebRequestMatcher getMatcher() {
			return this.matcher;
		}

		private WebConnection getDelegate() {
			return this.delegate;
		}
	}

}
