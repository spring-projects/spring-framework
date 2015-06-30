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

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * <p>
 * An implementation of WebRequestMatcher that allows matching on the host and optionally
 * the port of WebRequest#getUrl(). For example, the following would match any request to
 * the host "code.jquery.com" without regard for the port:
 * </p>
 *
 * <pre>
 * WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");
 * </pre>
 *
 * Multiple hosts can also be passed in. For example, the following would match an request
 * to the host "code.jquery.com" or the host "cdn.com" without regard for the port:
 *
 * <pre>
 * WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");
 * </pre>
 *
 * <p>
 * Alternatively, one can also specify the port. For example, the following would match
 * any request to the host "code.jquery.com" with the port of 80.
 * </p>
 *
 * <pre>
 * WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com:80");
 * </pre>
 *
 * <p>
 * The above cdnMatcher would match: "http://code.jquery.com/jquery.js" (default port of
 * 80) and "http://code.jquery.com:80/jquery.js". However, it would not match
 * "https://code.jquery.com/jquery.js" (default port of 443).
 * </p>
 *
 * @author Rob Winch
 * @since 4.2
 * @see UrlRegexRequestMatcher
 * @see org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection
 */
public final class HostRequestMatcher implements WebRequestMatcher {
	private final Set<String> hosts = new HashSet<String>();

	/**
	 * Creates a new instance
	 *
	 * @param hosts the hosts to match on (i.e. "localhost", "example.com:443")
	 */
	public HostRequestMatcher(String... hosts) {
		this.hosts.addAll(Arrays.asList(hosts));
	}

	@Override
	public boolean matches(WebRequest request) {
		URL url = request.getUrl();
		String host = url.getHost();

		if(hosts.contains(host)) {
			return true;
		}

		int port = url.getPort();
		if(port == -1) {
			port = url.getDefaultPort();
		}
		String hostAndPort = host + ":" + port;

		return hosts.contains(hostAndPort);
	}
}
