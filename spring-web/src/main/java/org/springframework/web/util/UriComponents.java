/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Represents an immutable collection of URI components, mapping component type to string
 * values. Contains convenience getters for all components. Effectively similar to {@link
 * java.net.URI}, but with more powerful encoding options and support for URI template
 * variables.
 *
 * @author Arjen Poutsma
 * @see UriComponentsBuilder
 * @since 3.1
 */
public abstract class UriComponents {

	private static final String DEFAULT_ENCODING = "UTF-8";

	/** Captures URI template variable names. */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	private final String scheme;

	private final String fragment;

	protected UriComponents(String scheme, String fragment) {
		this.scheme = scheme;
		this.fragment = fragment;
	}

	// component getters

	/**
	 * Returns the scheme.
	 *
	 * @return the scheme. Can be {@code null}.
	 */
	public final String getScheme() {
		return scheme;
	}

	/**
	 * Returns the scheme specific part.
	 *
	 * @retur the scheme specific part. Can be {@code null}.
	 */
	public abstract String getSchemeSpecificPart();

	/**
	 * Returns the user info.
	 *
	 * @return the user info. Can be {@code null}.
	 */
	public abstract String getUserInfo();

	/**
	 * Returns the host.
	 *
	 * @return the host. Can be {@code null}.
	 */
	public abstract String getHost();

	/**
	 * Returns the port. Returns {@code -1} if no port has been set.
	 *
	 * @return the port
	 */
	public abstract int getPort();

	/**
	 * Returns the path.
	 *
	 * @return the path. Can be {@code null}.
	 */
	public abstract String getPath();

	/**
	 * Returns the list of path segments.
	 *
	 * @return the path segments. Empty if no path has been set.
	 */
	public abstract List<String> getPathSegments();

	/**
	 * Returns the query.
	 *
	 * @return the query. Can be {@code null}.
	 */
	public abstract String getQuery();

	/**
	 * Returns the map of query parameters.
	 *
	 * @return the query parameters. Empty if no query has been set.
	 */
	public abstract MultiValueMap<String, String> getQueryParams();

	/**
	 * Returns the fragment.
	 *
	 * @return the fragment. Can be {@code null}.
	 */
	public final String getFragment() {
		return fragment;
	}

	// encoding

	/**
	 * Encodes all URI components using their specific encoding rules, and returns the result
	 * as a new {@code UriComponents} instance. This method uses UTF-8 to encode.
	 *
	 * @return the encoded uri components
	 */
	public final UriComponents encode() {
		try {
			return encode(DEFAULT_ENCODING);
		}
		catch (UnsupportedEncodingException e) {
			throw new InternalError("\"" + DEFAULT_ENCODING + "\" not supported");
		}
	}

	/**
	 * Encodes all URI components using their specific encoding rules, and
	 * returns the result as a new {@code UriComponents} instance.
	 *
	 * @param encoding the encoding of the values contained in this map
	 * @return the encoded uri components
	 * @throws UnsupportedEncodingException if the given encoding is not supported
	 */
	public abstract UriComponents encode(String encoding) throws UnsupportedEncodingException;

	// expanding

	/**
	 * Replaces all URI template variables with the values from a given map. The map keys
	 * represent variable names; the values variable values. The order of variables is not
	 * significant.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the expanded uri components
	 */
	public final UriComponents expand(Map<String, ?> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");

		return expandInternal(new MapTemplateVariables(uriVariables));
	}

	/**
	 * Replaces all URI template variables with the values from a given array. The array
	 * represent variable values. The order of variables is significant.
	 *
	 * @param uriVariableValues URI variable values
	 * @return the expanded uri components
	 */
	public final UriComponents expand(Object... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");

		return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
	}

	/**
	 * Replaces all URI template variables with the values from the given {@link
	 * UriTemplateVariables}
	 *
	 * @param uriVariables URI template values
	 * @return the expanded uri components
	 */
	abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

	static String expandUriComponent(String source, UriTemplateVariables uriVariables) {
		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		Matcher matcher = NAMES_PATTERN.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group(1);
			String variableName = getVariableName(match);
			Object variableValue = uriVariables.getValue(variableName);
			String variableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(variableValueString);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String getVariableName(String match) {
		int colonIdx = match.indexOf(':');
		return colonIdx == -1 ? match : match.substring(0, colonIdx);
	}

	private static String getVariableValueAsString(Object variableValue) {
		return variableValue != null ? variableValue.toString() : "";
	}

	/**
	 * Returns a URI string from this {@code UriComponents} instance.
	 *
	 * @return the URI string
	 */
	public abstract String toUriString();

	/**
	 * Returns a {@code URI} from this {@code UriComponents} instance.
	 *
	 * @return the URI
	 */
	public abstract URI toUri();

	@Override
	public final String toString() {
		return toUriString();
	}

	/**
	 * Normalize the path removing sequences like "path/..".
	 *
	 * @see org.springframework.util.StringUtils#cleanPath(String)
	 */
	public abstract UriComponents normalize();

	/**
	 * Defines the contract for URI Template variables
	 *
	 * @see HierarchicalUriComponents#expand
	 */
	interface UriTemplateVariables {

		Object getValue(String name);

	}

	/**
	 * URI template variables backed by a map.
	 */
	private static class MapTemplateVariables implements UriTemplateVariables {

		private final Map<String, ?> uriVariables;

		public MapTemplateVariables(Map<String, ?> uriVariables) {
			this.uriVariables = uriVariables;
		}

		public Object getValue(String name) {
			if (!this.uriVariables.containsKey(name)) {
				throw new IllegalArgumentException("Map has no value for '" + name + "'");
			}
			return this.uriVariables.get(name);
		}
	}

	/**
	 * URI template variables backed by a variable argument array.
	 */
	private static class VarArgsTemplateVariables implements UriTemplateVariables {

		private final Iterator<Object> valueIterator;

		public VarArgsTemplateVariables(Object... uriVariableValues) {
			this.valueIterator = Arrays.asList(uriVariableValues).iterator();
		}

		public Object getValue(String name) {
			if (!valueIterator.hasNext()) {
				throw new IllegalArgumentException(
						"Not enough variable values available to expand '" + name + "'");
			}
			return valueIterator.next();
		}
	}


}
