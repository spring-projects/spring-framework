/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;

import reactor.netty.http.server.HttpServerRequest;

import org.springframework.util.Assert;

/**
 * Helper class for creating a {@link URI} from a reactor {@link HttpServerRequest}.
 *
 * @author Arjen Poutsma
 * @since 6.0.8
 */
abstract class ReactorUriHelper {

	public static URI createUri(HttpServerRequest request) throws URISyntaxException {
		Assert.notNull(request, "HttpServerRequest must not be null");

		StringBuilder builder = new StringBuilder();
		String scheme = request.scheme();
		builder.append(scheme);
		builder.append("://");

		appendHostName(request, builder);

		int port = request.hostPort();
		if ((scheme.equals("http") || scheme.equals("ws")) && port != 80 ||
				(scheme.equals("https") || scheme.equals("wss")) && port != 443) {
			builder.append(':');
			builder.append(port);
		}

		appendRequestUri(request, builder);

		return new URI(builder.toString());
	}

	private static void appendHostName(HttpServerRequest request, StringBuilder builder) {
		String hostName = request.hostName();
		boolean ipv6 = hostName.indexOf(':') != -1;
		boolean brackets = ipv6 && !hostName.startsWith("[") && !hostName.endsWith("]");
		if (brackets) {
			builder.append('[');
		}
		if (encoded(hostName, ipv6)) {
			builder.append(hostName);
		}
		else {
			for (int i=0; i < hostName.length(); i++) {
				char c = hostName.charAt(i);
				if (isAllowedInHost(c, ipv6)) {
					builder.append(c);
				}
				else {
					builder.append('%');
					char hex1 = Character.toUpperCase(Character.forDigit((c >> 4) & 0xF, 16));
					char hex2 = Character.toUpperCase(Character.forDigit(c & 0xF, 16));
					builder.append(hex1);
					builder.append(hex2);
				}
			}
		}
		if (brackets) {
			builder.append(']');
		}
	}

	private static boolean encoded(String hostName, boolean ipv6) {
		int length = hostName.length();
		for (int i = 0; i < length; i++) {
			char c = hostName.charAt(i);
			if (c == '%') {
				if ((i + 2) < length) {
					char hex1 = hostName.charAt(i + 1);
					char hex2 = hostName.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						return false;
					}
					i += 2;
				}
				else {
					return false;
				}
			}
			else if (!isAllowedInHost(c, ipv6)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isAllowedInHost(char c, boolean ipv6) {
		return (c >= 'a' && c <= 'z') || // alpha
				(c >= 'A' && c <= 'Z') || // alpha
				(c >= '0' && c <= '9') || // digit
				'-' == c || '.' == c || '_' == c || '~' == c || // unreserved
				'!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || // sub-delims
				'*' == c || '+' == c || ',' == c || ';' == c || '=' == c ||
				(ipv6 && ('[' == c || ']' == c || ':' == c)); // ipv6
	}

	private static void appendRequestUri(HttpServerRequest request, StringBuilder builder) {
		String uri = request.uri();
		int length = uri.length();
		for (int i = 0; i < length; i++) {
			char c = uri.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				break;
			}
			if (c == ':' && (i + 2 < length)) {
				if (uri.charAt(i + 1) == '/' && uri.charAt(i + 2) == '/') {
					for (int j = i + 3; j < length; j++) {
						c = uri.charAt(j);
						if (c == '/' || c == '?' || c == '#') {
							builder.append(uri, j, length);
							return;
						}
					}
					return;
				}
			}
		}
		builder.append(uri);
	}
}
