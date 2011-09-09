/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Represents the components that make up a URI, mapping component type to string values. Contains convenience getters
 * and setters for all components, as well as the regular {@link Map} implementation.
 *
 * <p>This mapping does not contain mappings for {@link UriComponent#PATH_SEGMENT} or nor {@link
 * UriComponent#QUERY_PARAM}, since those components can occur multiple times in the URI. Instead, one can use {@link
 * #getPathSegments()} or {@link #getQueryParams()} respectively.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class UriComponents implements Map<UriComponent, String> {

	private static final String PATH_DELIMITER = "/";

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)=?([^&=]+)?");

	private final Map<UriComponent, String> uriComponents;

	/** Constructs a new, empty instance of the {@code UriComponents} object. */
	public UriComponents() {
		this.uriComponents = new EnumMap<UriComponent, String>(UriComponent.class);
	}

	/**
	 * Creates an instance of the {@code UriComponents} object that contains the given component.
	 *
	 * @param uriComponents the component to initialize with
	 */
	public UriComponents(Map<UriComponent, String> uriComponents) {
		Assert.notNull(uriComponents, "'uriComponents' must not be null");
		this.uriComponents = new EnumMap<UriComponent, String>(uriComponents);
	}

	// convenience properties

	/**
	 * Returns the scheme.
	 *
	 * @return the scheme. Can be {@code null}.
	 */
	public String getScheme() {
		return get(UriComponent.SCHEME);
	}

	/**
	 * Sets the scheme.
	 *
	 * @param scheme the scheme. Can be {@code null}.
	 */
	public void setScheme(String scheme) {
		put(UriComponent.SCHEME, scheme);
	}

	/**
	 * Returns the authority.
	 *
	 * @return the authority. Can be {@code null}.
	 */
	public String getAuthority() {
		return get(UriComponent.AUTHORITY);
	}

	/**
	 * Sets the authority.
	 *
	 * @param authority the authority. Can be {@code null}.
	 */
	public void setAuthority(String authority) {
		put(UriComponent.AUTHORITY, authority);
	}

	/**
	 * Returns the user info.
	 *
	 * @return the user info. Can be {@code null}.
	 */
	public String getUserInfo() {
		return get(UriComponent.USER_INFO);
	}

	/**
	 * Sets the user info
	 *
	 * @param userInfo the user info. Can be {@code null}
	 */
	public void setUserInfo(String userInfo) {
		put(UriComponent.USER_INFO, userInfo);
	}

	/**
	 * Returns the host.
	 *
	 * @return the host. Can be {@code null}.
	 */
	public String getHost() {
		return get(UriComponent.HOST);
	}

	/**
	 * Sets the host.
	 *
	 * @param host the host. Can be {@code null}.
	 */
	public void setHost(String host) {
		put(UriComponent.HOST, host);
	}

	/**
	 * Returns the port as string.
	 *
	 * @return the port as string. Can be {@code null}.
	 */
	public String getPort() {
		return get(UriComponent.PORT);
	}

	/**
	 * Sets the port as string.
	 *
	 * @param port the port as string. Can be {@code null}.
	 */
	public void setPort(String port) {
		put(UriComponent.PORT, port);
	}

	/**
	 * Returns the port as integer. Returns {@code -1} if no port has been set.
	 *
	 * @return the port the port as integer
	 */
	public int getPortAsInteger() {
		String port = getPort();
		return port != null ? Integer.parseInt(port) : -1;
	}

	/**
	 * Sets the port as integer. A value < 0 resets the port to an empty value.
	 *
	 * @param port the port as integer
	 */
	public void setPortAsInteger(int port) {
		String portString = port > -1 ? Integer.toString(port) : null;
		put(UriComponent.PORT, portString);
	}

	/**
	 * Returns the path.
	 *
	 * @return the path. Can be {@code null}.
	 */
	public String getPath() {
		return get(UriComponent.PATH);
	}

	/**
	 * Sets the path.
	 *
	 * @param path the path. Can be {@code null}.
	 */
	public void setPath(String path) {
		put(UriComponent.PATH, path);
	}

	/**
	 * Returns the list of path segments.
	 *
	 * @return the path segments. Empty if no path has been set.
	 */
	public List<String> getPathSegments() {
		String path = getPath();
		if (path != null) {
			return Arrays.asList(StringUtils.tokenizeToStringArray(path, PATH_DELIMITER));
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Sets the path segments. An empty or {@code null} value resets the path to an empty value.
	 *
	 * @param pathSegments the path segments
	 */
	public void setPathSegments(List<String> pathSegments) {
		if (!CollectionUtils.isEmpty(pathSegments)) {
			StringBuilder pathBuilder = new StringBuilder("/");
			for (Iterator<String> iterator = pathSegments.iterator(); iterator.hasNext(); ) {
				String pathSegment = iterator.next();
				pathBuilder.append(pathSegment);
				if (iterator.hasNext()) {
					pathBuilder.append('/');
				}
			}
			setPath(pathBuilder.toString());
		}
		else {
			setPath(null);
		}
	}

	/**
	 * Returns the query.
	 *
	 * @return the query. Can be {@code null}.
	 */
	public String getQuery() {
		return get(UriComponent.QUERY);
	}

	/**
	 * Sets the query.
	 *
	 * @param query the query. Can be {@code null}.
	 */
	public void setQuery(String query) {
		put(UriComponent.QUERY, query);
	}

	/**
	 * Returns the map of query parameters.
	 *
	 * @return the query parameters. Empty if no query has been set.
	 */
	public MultiValueMap<String, String> getQueryParams() {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
		String query = getQuery();
		if (query != null) {
			Matcher m = QUERY_PARAM_PATTERN.matcher(query);
			while (m.find()) {
				String name = m.group(1);
				String value = m.group(2);
				result.add(name, value);
			}
		}
		return result;
	}

	/**
	 * Sets the query parameters. An empty or {@code null} value resets the query to an empty value.
	 *
	 * @param queryParams the query parameters
	 */
	public void setQueryParams(MultiValueMap<String, String> queryParams) {
		if (!CollectionUtils.isEmpty(queryParams)) {
			StringBuilder queryBuilder = new StringBuilder();
			for (Iterator<Entry<String, List<String>>> entryIterator = queryParams.entrySet().iterator();
					entryIterator.hasNext(); ) {
				Entry<String, List<String>> entry = entryIterator.next();
				String name = entry.getKey();
				List<String> values = entry.getValue();
				if (CollectionUtils.isEmpty(values) || (values.size() == 1 && values.get(0) == null)) {
					queryBuilder.append(name);
				}
				else {
					for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
						String value = valueIterator.next();
						queryBuilder.append(name);
						queryBuilder.append('=');
						queryBuilder.append(value);
						if (valueIterator.hasNext()) {
							queryBuilder.append('&');
						}
					}
				}
				if (entryIterator.hasNext()) {
					queryBuilder.append('&');
				}
			}
			setQuery(queryBuilder.toString());
		}
		else {
			setQuery(null);
		}
	}

	/**
	 * Returns the fragment.
	 *
	 * @return the fragment. Can be {@code null}.
	 */
	public String getFragment() {
		return get(UriComponent.FRAGMENT);
	}

	/**
	 * Sets the fragment.
	 *
	 * @param fragment the fragment. Can be {@code null}.
	 */
	public void setFragment(String fragment) {
		put(UriComponent.FRAGMENT, fragment);
	}

	// Map implementation

	public int size() {
		return this.uriComponents.size();
	}

	public boolean isEmpty() {
		return this.uriComponents.isEmpty();
	}

	public boolean containsKey(Object key) {
		return this.uriComponents.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.uriComponents.containsValue(value);
	}

	public String get(Object key) {
		return this.uriComponents.get(key);
	}

	public String put(UriComponent key, String value) {
		return this.uriComponents.put(key, value);
	}

	public String remove(Object key) {
		return this.uriComponents.remove(key);
	}

	public void putAll(Map<? extends UriComponent, ? extends String> m) {
		this.uriComponents.putAll(m);
	}

	public void clear() {
		this.uriComponents.clear();
	}

	public Set<UriComponent> keySet() {
		return this.uriComponents.keySet();
	}

	public Collection<String> values() {
		return this.uriComponents.values();
	}

	public Set<Entry<UriComponent, String>> entrySet() {
		return this.uriComponents.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof UriComponents) {
			UriComponents other = (UriComponents) o;
			return this.uriComponents.equals(other.uriComponents);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.uriComponents.hashCode();
	}

	@Override
	public String toString() {
		return this.uriComponents.toString();
	}


}
