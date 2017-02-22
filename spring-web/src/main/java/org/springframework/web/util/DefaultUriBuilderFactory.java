/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link UriBuilderFactory} using
 * {@link UriComponentsBuilder} for building, encoding, and expanding URI
 * templates.
 *
 * <p>Exposes configuration properties that customize the creation of all URIs
 * built through this factory instance including a base URI, default URI
 * variables, and an encoding mode.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultUriBuilderFactory implements UriBuilderFactory {

	public enum EncodingMode {URI_COMPONENT, VALUES_ONLY, NONE };


	private final UriComponentsBuilder baseUri;

	private final Map<String, Object> defaultUriVariables = new HashMap<>();

	private EncodingMode encodingMode = EncodingMode.URI_COMPONENT;

	private boolean parsePath = true;


	/**
	 * Default constructor without a base URI.
	 */
	public DefaultUriBuilderFactory() {
		this(UriComponentsBuilder.fromPath(null));
	}

	/**
	 * Constructor with a String "base URI".
	 * <p>The String given here is used to create a single "base"
	 * {@code UriComponentsBuilder}. Each time a new URI is prepared via
	 * {@link #uriString(String)} a new {@code UriComponentsBuilder} is created and
	 * merged with a clone of the "base" {@code UriComponentsBuilder}.
	 * <p>Note that the base URI may contain any or all components of a URI and
	 * those will apply to every URI.
	 */
	public DefaultUriBuilderFactory(String baseUri) {
		this(UriComponentsBuilder.fromUriString(baseUri));
	}

	/**
	 * Alternate constructor with a {@code UriComponentsBuilder} as the base URI.
	 */
	public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
		Assert.notNull(baseUri, "'baseUri' is required.");
		this.baseUri = baseUri;
	}


	/**
	 * Configure default URI variable values to use when expanding a URI with a
	 * Map of values. The map supplied when expanding a given URI can override
	 * default values.
	 * @param defaultUriVariables the default URI variables
	 */
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * Return the configured default URI variable values.
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}

	/**
	 * Specify the encoding mode to use when building URIs:
	 * <ul>
	 * <li>URI_COMPONENT -- expand the URI variables first and then encode all URI
	 * component (e.g. host, path, query, etc) according to the encoding rules
	 * for each individual component.
	 * <li>VALUES_ONLY -- encode URI variable values only, prior to expanding
	 * them, using a "strict" encoding mode, i.e. encoding all characters
	 * outside the unreserved set as defined in
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 Section 2</a>.
	 * This ensures a URI variable value will not contain any characters with a
	 * reserved purpose.
	 * <li>NONE -- in this mode no encoding is performed.
	 * </ul>
	 * <p>By default this is set to {@code "URI_COMPONENT"}.
	 * @param encodingMode the encoding mode to use
	 */
	public void setEncodingMode(EncodingMode encodingMode) {
		this.encodingMode = encodingMode;
	}

	/**
	 * Return the configured encoding mode.
	 */
	public EncodingMode getEncodingMode() {
		return this.encodingMode;
	}

	/**
	 * Whether to parse the path into path segments for the URI string passed
	 * into {@link #uriString(String)} or one of the expand methods.
	 * <p>Setting this property to {@code true} ensures that URI variables
	 * expanded into the path are subject to path segment encoding rules and
	 * "/" characters are percent-encoded. If set to {@code false} the path is
	 * kept as a full path and expanded URI variables will have "/" characters
	 * preserved.
	 * <p>By default this is set to {@code true}.
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


	// UriTemplateHandler

	public URI expand(String uriTemplate, Map<String, ?> uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	public URI expand(String uriTemplate, Object... uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	// UriBuilderFactory

	public UriBuilder uriString(String uriTemplate) {
		return new DefaultUriBuilder(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return new DefaultUriBuilder("");
	}


	/**
	 * {@link DefaultUriBuilderFactory} specific implementation of UriBuilder.
	 */
	private class DefaultUriBuilder implements UriBuilder {

		private final UriComponentsBuilder uriComponentsBuilder;


		public DefaultUriBuilder(String uriTemplate) {
			this.uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		}

		private UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {

			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(uriTemplate);
			UriComponents uriComponents = uriComponentsBuilder.build();

			UriComponentsBuilder result = (uriComponents.getHost() == null ?
					baseUri.cloneBuilder().uriComponents(uriComponents) : uriComponentsBuilder);

			if (shouldParsePath()) {
				UriComponents uric = result.build();
				String path = uric.getPath();
				List<String> pathSegments = uric.getPathSegments();

				result.replacePath(null);
				result.pathSegment(pathSegments.toArray(new String[0]));

				if (path != null && path.endsWith("/")) {
					result.path("/");
				}
			}

			return result;
		}

		@Override
		public DefaultUriBuilder scheme(String scheme) {
			this.uriComponentsBuilder.scheme(scheme);
			return this;
		}

		@Override
		public DefaultUriBuilder userInfo(String userInfo) {
			this.uriComponentsBuilder.userInfo(userInfo);
			return this;
		}

		@Override
		public DefaultUriBuilder host(String host) {
			this.uriComponentsBuilder.host(host);
			return this;
		}

		@Override
		public DefaultUriBuilder port(int port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder port(String port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder path(String path) {
			this.uriComponentsBuilder.path(path);
			return this;
		}

		@Override
		public DefaultUriBuilder replacePath(String path) {
			this.uriComponentsBuilder.replacePath(path);
			return this;
		}

		@Override
		public DefaultUriBuilder pathSegment(String... pathSegments) {
			this.uriComponentsBuilder.pathSegment(pathSegments);
			return this;
		}

		@Override
		public DefaultUriBuilder query(String query) {
			this.uriComponentsBuilder.query(query);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQuery(String query) {
			this.uriComponentsBuilder.replaceQuery(query);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParam(String name, Object... values) {
			this.uriComponentsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParam(String name, Object... values) {
			this.uriComponentsBuilder.replaceQueryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.queryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.replaceQueryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder fragment(String fragment) {
			this.uriComponentsBuilder.fragment(fragment);
			return this;
		}

		@Override
		public URI build(Map<String, ?> uriVars) {
			if (!defaultUriVariables.isEmpty()) {
				Map<String, Object> map = new HashMap<>();
				map.putAll(defaultUriVariables);
				map.putAll(uriVars);
				uriVars = map;
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			UriComponents uriComponents = this.uriComponentsBuilder.build().expand(uriVars);
			if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				uriComponents = uriComponents.encode();
			}
			return URI.create(uriComponents.toString());
		}

		@Override
		public URI build(Object... uriVars) {
			if (ObjectUtils.isEmpty(uriVars) && !defaultUriVariables.isEmpty()) {
				return build(Collections.emptyMap());
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			UriComponents uriComponents = this.uriComponentsBuilder.build().expand(uriVars);
			if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				uriComponents = uriComponents.encode();
			}
			return URI.create(uriComponents.toString());
		}
	}

}
