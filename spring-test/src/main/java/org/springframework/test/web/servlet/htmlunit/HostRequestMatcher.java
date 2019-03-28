/*
 * Copyright 2002-2019 the original author or authors.
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

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * A {@link WebRequestMatcher} that allows matching on the host and optionally
 * the port of {@code WebRequest#getUrl()}.
 *
 * <p>For example, the following would match any request to the host
 * {@code "code.jquery.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com");</pre>
 *
 * <p>Multiple hosts can also be passed in. For example, the following would
 * match any request to the host {@code "code.jquery.com"} or the host
 * {@code "cdn.com"} without regard for the port.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com", "cdn.com");</pre>
 *
 * <p>Alternatively, one can also specify the port. For example, the following would match
 * any request to the host {@code "code.jquery.com"} with the port of {@code 80}.
 *
 * <pre class="code">WebRequestMatcher cdnMatcher = new HostMatcher("code.jquery.com:80");</pre>
 *
 * <p>The above {@code cdnMatcher} would match {@code "http://code.jquery.com/jquery.js"}
 * which has a default port of {@code 80} and {@code "http://code.jquery.com:80/jquery.js"}.
 * However, it would not match {@code "https://code.jquery.com/jquery.js"}
 * which has a default port of {@code 443}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see UrlRegexRequestMatcher
 * @see org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection
 */
public final class HostRequestMatcher implements WebRequestMatcher {

	private final Set<String> hosts = new HashSet<>();


	/**
	 * Create a new {@code HostRequestMatcher} for the given hosts &mdash;
	 * for example: {@code "localhost"}, {@code "example.com:443"}, etc.
	 * @param hosts the hosts to match on
	 */
	public HostRequestMatcher(String... hosts) {
		Collections.addAll(this.hosts, hosts);
	}


	@Override
	public boolean matches(WebRequest request) {
		URL url = request.getUrl();
		String host = url.getHost();

		if (this.hosts.contains(host)) {
			return true;
		}

		int port = url.getPort();
		if (port == -1) {
			port = url.getDefaultPort();
		}
		String hostAndPort = host + ":" + port;

		return this.hosts.contains(hostAndPort);
	}

}
