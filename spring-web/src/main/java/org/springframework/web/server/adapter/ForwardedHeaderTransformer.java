/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.server.adapter;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers to override
 * the request URI (i.e. {@link ServerHttpRequest#getURI()}) so it reflects
 * the client-originated protocol and address.
 *
 * <p>Alternatively if {@link #setRemoveOnly removeOnly} is set to "true",
 * then "Forwarded" and "X-Forwarded-*" headers are only removed, and not used.
 *
 * <p>An instance of this class is typically declared as a bean with the name
 * "forwardedHeaderTransformer" and detected by
 * {@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)}, or it
 * can also be registered directly via
 * {@link WebHttpHandlerBuilder#forwardedHeaderTransformer(ForwardedHeaderTransformer)}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
public class ForwardedHeaderTransformer implements Function<ServerHttpRequest, ServerHttpRequest> {

	static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
	}


	private boolean removeOnly;


	/**
	 * Enable mode in which any "Forwarded" or "X-Forwarded-*" headers are
	 * removed only and the information in them ignored.
	 * @param removeOnly whether to discard and ignore forwarded headers
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * Whether the "remove only" mode is on.
	 * @see #setRemoveOnly
	 */
	public boolean isRemoveOnly() {
		return this.removeOnly;
	}


	/**
	 * Apply and remove, or remove Forwarded type headers.
	 * @param request the request
	 */
	@Override
	public ServerHttpRequest apply(ServerHttpRequest request) {
		if (hasForwardedHeaders(request)) {
			ServerHttpRequest.Builder builder = request.mutate();
			if (!this.removeOnly) {
				URI uri = UriComponentsBuilder.fromHttpRequest(request).build(true).toUri();
				builder.uri(uri);
				String prefix = getForwardedPrefix(request);
				if (prefix != null) {
					builder.path(prefix + uri.getRawPath());
					builder.contextPath(prefix);
				}
				InetSocketAddress remoteAddress = request.getRemoteAddress();
				remoteAddress = UriComponentsBuilder.parseForwardedFor(request, remoteAddress);
				if (remoteAddress != null) {
					builder.remoteAddress(remoteAddress);
				}
			}
			removeForwardedHeaders(builder);
			request = builder.build();
		}
		return request;
	}

	/**
	 * Whether the request has any Forwarded headers.
	 * @param request the request
	 */
	protected boolean hasForwardedHeaders(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		for (String headerName : FORWARDED_HEADER_NAMES) {
			if (headers.containsKey(headerName)) {
				return true;
			}
		}
		return false;
	}

	private void removeForwardedHeaders(ServerHttpRequest.Builder builder) {
		builder.headers(map -> FORWARDED_HEADER_NAMES.forEach(map::remove));
	}


	@Nullable
	private static String getForwardedPrefix(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		String header = headers.getFirst("X-Forwarded-Prefix");
		if (header == null) {
			return null;
		}
		StringBuilder prefix = new StringBuilder(header.length());
		String[] rawPrefixes = StringUtils.tokenizeToStringArray(header, ",");
		for (String rawPrefix : rawPrefixes) {
			int endIndex = rawPrefix.length();
			while (endIndex > 1 && rawPrefix.charAt(endIndex - 1) == '/') {
				endIndex--;
			}
			prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
		}
		return prefix.toString();
	}

}
