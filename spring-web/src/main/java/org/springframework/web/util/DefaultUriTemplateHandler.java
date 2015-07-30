/*
 * Copyright 2002-2015 the original author or authors.
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
 * Default implementation of {@link UriTemplateHandler} that relies on
 * {@link UriComponentsBuilder} internally.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultUriTemplateHandler implements UriTemplateHandler {

	private String baseUrl;

	private boolean parsePath;


	/**
	 * Configure a base URL to prepend URI templates with. The base URL should
	 * have a scheme and host but may also contain a port and a partial path.
	 * Individual URI templates then may provide the remaining part of the URL
	 * including additional path, query and fragment.
	 * <p><strong>Note: </strong>Individual URI templates are expanded and
	 * encoded before being appended to the base URL. Therefore the base URL is
	 * expected to be fully expanded and encoded, which can be done with the help
	 * of {@link UriComponentsBuilder}.
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
	 * <p>If set to {@code true} the path of parsed URI templates is decomposed
	 * into path segments so that URI variables expanded into the path are
	 * treated according to path segment encoding rules. In effect that means the
	 * "/" character is percent encoded.
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
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = uriComponentsBuilder.build().expand(uriVariables).encode();
		return insertBaseUrl(uriComponents);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariableValues) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = uriComponentsBuilder.build().expand(uriVariableValues).encode();
		return insertBaseUrl(uriComponents);
	}

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
