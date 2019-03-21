/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Abstract base class for {@link UriTemplateHandler} implementations.
 *
 * <p>Support {@link #setBaseUrl} and {@link #setDefaultUriVariables} properties
 * that should be relevant regardless of the URI template expand and encode
 * mechanism used in sub-classes.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public abstract class AbstractUriTemplateHandler implements UriTemplateHandler {

	private String baseUrl;

	private final Map<String, Object> defaultUriVariables = new HashMap<String, Object>();


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
	 * Configure default URI variable values to use with every expanded URI
	 * template. These default values apply only when expanding with a Map, and
	 * not with an array, where the Map supplied to {@link #expand(String, Map)}
	 * can override the default values.
	 * @param defaultUriVariables the default URI variable values
	 * @since 4.3
	 */
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * Return a read-only copy of the configured default URI variables.
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		if (!getDefaultUriVariables().isEmpty()) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.putAll(getDefaultUriVariables());
			map.putAll(uriVariables);
			uriVariables = map;
		}
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}


	/**
	 * Actually expand and encode the URI template.
	 */
	protected abstract URI expandInternal(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * Actually expand and encode the URI template.
	 */
	protected abstract URI expandInternal(String uriTemplate, Object... uriVariables);


	/**
	 * Insert a base URL (if configured) unless the given URL has a host already.
	 */
	private URI insertBaseUrl(URI url) {
		try {
			String baseUrl = getBaseUrl();
			if (baseUrl != null && url.getHost() == null) {
				url = new URI(baseUrl + url.toString());
			}
			return url;
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
		}
	}

}
