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

package org.springframework.web.util;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code UriBuilderFactory} that relies on {@link UriComponentsBuilder} for
 * the actual building of the URI.
 *
 * <p>Provides options to create {@link UriBuilder} instances with a common
 * base URI, alternative encoding mode strategies, among others.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see UriComponentsBuilder
 */
public class DefaultUriBuilderFactory implements UriBuilderFactory {

	/**
	 * Enum to represent multiple URI encoding strategies.
	 * @see #setEncodingMode
	 */
	public enum EncodingMode {

		/**
		 * Pre-encode the URI template first, then strictly encode URI variables
		 * when expanded, with the following rules:
		 * <ul>
		 * <li>For the URI template replace <em>only</em> non-ASCII and illegal
		 * (within a given URI component type) characters with escaped octets.
		 * <li>For URI variables do the same and also replace characters with
		 * reserved meaning.
		 * </ul>
		 * <p>For most cases, this mode is most likely to give the expected
		 * result because in treats URI variables as opaque data to be fully
		 * encoded, while {@link #URI_COMPONENT} by comparison is useful only
		 * if intentionally expanding URI variables with reserved characters.
		 * @since 5.0.8
		 * @see UriComponentsBuilder#encode()
		 */
		TEMPLATE_AND_VALUES,

		/**
		 * Does not encode the URI template and instead applies strict encoding
		 * to URI variables via {@link UriUtils#encodeUriVariables} prior to
		 * expanding them into the template.
		 * @see UriUtils#encodeUriVariables(Object...)
		 * @see UriUtils#encodeUriVariables(Map)
		 */
		VALUES_ONLY,

		/**
		 * Expand URI variables first, and then encode the resulting URI
		 * component values, replacing <em>only</em> non-ASCII and illegal
		 * (within a given URI component type) characters, but not characters
		 * with reserved meaning.
		 * @see UriComponents#encode()
		 */
		URI_COMPONENT,

		/**
		 * No encoding should be applied.
		 */
		NONE
	}


	@Nullable
	private final UriComponentsBuilder baseUri;

	private EncodingMode encodingMode = EncodingMode.TEMPLATE_AND_VALUES;

	private final Map<String, Object> defaultUriVariables = new HashMap<>();

	private boolean parsePath = true;


	/**
	 * Default constructor without a base URI.
	 * <p>The target address must be specified on each UriBuilder.
	 */
	public DefaultUriBuilderFactory() {
		this.baseUri = null;
	}

	/**
	 * Constructor with a base URI.
	 * <p>The given URI template is parsed via
	 * {@link UriComponentsBuilder#fromUriString} and then applied as a base URI
	 * to every UriBuilder via {@link UriComponentsBuilder#uriComponents} unless
	 * the UriBuilder itself was created with a URI template that already has a
	 * target address.
	 * @param baseUriTemplate the URI template to use a base URL
	 */
	public DefaultUriBuilderFactory(String baseUriTemplate) {
		this.baseUri = UriComponentsBuilder.fromUriString(baseUriTemplate);
	}

	/**
	 * Variant of {@link #DefaultUriBuilderFactory(String)} with a
	 * {@code UriComponentsBuilder}.
	 */
	public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
		this.baseUri = baseUri;
	}


	/**
	 * Set the encoding mode to use.
	 * <p>By default this is set to {@link EncodingMode#TEMPLATE_AND_VALUES
	 * EncodingMode.TEMPLATE_AND_VALUES}.
	 * <p><strong>Note:</strong> In 5.1 the default was changed from
	 * {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}.
	 * Consequently the {@code WebClient}, which relies on the built-in default
	 * has also been switched to the new default. The {@code RestTemplate}
	 * however sets this explicitly to {@link EncodingMode#URI_COMPONENT
	 * EncodingMode.URI_COMPONENT} explicitly for historic and backwards
	 * compatibility reasons.
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
	 * Provide default URI variable values to use when expanding URI templates
	 * with a Map of variables.
	 * @param defaultUriVariables default URI variable values
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
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
	 * Whether to parse the input path into path segments if the encoding mode
	 * is set to {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT},
	 * which ensures that URI variables in the path are encoded according to
	 * path segment rules and for example a '/' is encoded.
	 * <p>By default this is set to {@code true}.
	 * @param parsePath whether to parse the path into path segments
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * Whether to parse the path into path segments if the encoding mode is set
	 * to {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}.
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
			UriComponentsBuilder result;
			if (!StringUtils.hasLength(uriTemplate)) {
				result = (baseUri != null ? baseUri.cloneBuilder() : UriComponentsBuilder.newInstance());
			}
			else if (baseUri != null) {
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
				UriComponents uri = builder.build();
				result = (uri.getHost() == null ? baseUri.cloneBuilder().uriComponents(uri) : builder);
			}
			else {
				result = UriComponentsBuilder.fromUriString(uriTemplate);
			}
			if (encodingMode.equals(EncodingMode.TEMPLATE_AND_VALUES)) {
				result.encode();
			}
			parsePathIfNecessary(result);
			return result;
		}

		private void parsePathIfNecessary(UriComponentsBuilder result) {
			if (parsePath && encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				UriComponents uric = result.build();
				String path = uric.getPath();
				result.replacePath(null);
				for (String segment : uric.getPathSegments()) {
					result.pathSegment(segment);
				}
				if (path != null && path.endsWith("/")) {
					result.path("/");
				}
			}
		}


		@Override
		public DefaultUriBuilder scheme(@Nullable String scheme) {
			this.uriComponentsBuilder.scheme(scheme);
			return this;
		}

		@Override
		public DefaultUriBuilder userInfo(@Nullable String userInfo) {
			this.uriComponentsBuilder.userInfo(userInfo);
			return this;
		}

		@Override
		public DefaultUriBuilder host(@Nullable String host) {
			this.uriComponentsBuilder.host(host);
			return this;
		}

		@Override
		public DefaultUriBuilder port(int port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder port(@Nullable String port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder path(String path) {
			this.uriComponentsBuilder.path(path);
			return this;
		}

		@Override
		public DefaultUriBuilder replacePath(@Nullable String path) {
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
		public DefaultUriBuilder replaceQuery(@Nullable String query) {
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
		public DefaultUriBuilder fragment(@Nullable String fragment) {
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
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			return createUri(uric);
		}

		@Override
		public URI build(Object... uriVars) {
			if (ObjectUtils.isEmpty(uriVars) && !defaultUriVariables.isEmpty()) {
				return build(Collections.emptyMap());
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			return createUri(uric);
		}

		private URI createUri(UriComponents uric) {
			if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				uric = uric.encode();
			}
			return URI.create(uric.toString());
		}
	}

}
