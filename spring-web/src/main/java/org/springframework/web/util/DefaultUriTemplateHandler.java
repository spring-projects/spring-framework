/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link UriTemplateHandler} that uses
 * {@link UriComponentsBuilder} to expand and encode variables.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultUriTemplateHandler implements UriTemplateHandler {

	private String baseUrl;

	private boolean parsePath;


	/**
	 * Configure a base URL to prepend URI templates with. The base URL must
	 * have a scheme and host but may optionally contain a port and a path.
	 * The base URL must be fully expanded and encoded which can be done via
	 * {@link UriComponentsBuilder}.
	 * @param baseUrl the base URL.
	 */
	public void setBaseUrl(String baseUrl) {
		if (baseUrl != null) {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).build();
			Assert.hasText(uriComponents.getScheme(), "'baseUrl' must have a scheme");
			Assert.hasText(uriComponents.getHost(), "'baseUrl' must have a host");
			Assert.isNull(uriComponents.getQuery(), "'baseUrl' cannot have a query");
			Assert.isNull(uriComponents.getFragment(), "'baseUrl' cannot have a fragment");
		}
		this.baseUrl = baseUrl;
	}

	/**
	 * Return the configured base URL.
	 */
	public String getBaseUrl() {
		return this.baseUrl;
	}

	/**
	 * Whether to parse the path of a URI template string into path segments.
	 * <p>If set to {@code true} the URI template path is immediately decomposed
	 * into path segments any URI variables expanded into it are then subject to
	 * path segment encoding rules. In effect URI variables in the path have any
	 * "/" characters percent encoded.
	 * <p>By default this is set to {@code false} in which case the path is kept
	 * as a full path and expanded URI variables will preserve "/" characters.
	 * @param parsePath whether to parse the path into path segments
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * Whether the handler is configured to parse the path into path segments.
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		UriComponentsBuilder builder = initUriComponentsBuilder(uriTemplate);
		UriComponents url = builder.build().expand(uriVariables).encode();
		return insertBaseUrl(url);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		UriComponentsBuilder builder = initUriComponentsBuilder(uriTemplate);
		UriComponents url = builder.build().expand(uriVariables).encode();
		return insertBaseUrl(url);
	}

	/**
	 * Create a {@code UriComponentsBuilder} from the UriTemplate string. The
	 * default implementation also parses the path into path segments if
	 * {@link #setParsePath parsePath} is enabled.
	 */
	protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		if (shouldParsePath()) {
			List<String> pathSegments = builder.build().getPathSegments();
			builder.replacePath(null);
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		return builder;
	}

	/**
	 * Invoked after the URI template has been expanded and encoded to prepend
	 * the configured {@link #setBaseUrl(String) baseUrl} if any.
	 * @param uriComponents the expanded and encoded URI
	 * @return the final URI
	 */
	protected URI insertBaseUrl(UriComponents uriComponents) {
		if (getBaseUrl() == null || uriComponents.getHost() != null) {
			return uriComponents.toUri();
		}
		String url = getBaseUrl() + uriComponents.toUriString();
		try {
			return new URI(url);
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
		}
	}

}
