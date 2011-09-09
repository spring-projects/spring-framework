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
 * <p>This mapping does not contain mappings for {@link org.springframework.web.util.UriComponents.Type#PATH_SEGMENT} or nor {@link
 * org.springframework.web.util.UriComponents.Type#QUERY_PARAM}, since those components can occur multiple times in the URI. Instead, one can use {@link
 * #getPathSegments()} or {@link #getQueryParams()} respectively.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class UriComponents implements Map<UriComponents.Type, String> {

	private static final String PATH_DELIMITER = "/";

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)=?([^&=]+)?");

	private final Map<Type, String> uriComponents;

	/** Constructs a new, empty instance of the {@code UriComponents} object. */
	public UriComponents() {
		this.uriComponents = new EnumMap<Type, String>(Type.class);
	}

	/**
	 * Creates an instance of the {@code UriComponents} object that contains the given component.
	 *
	 * @param uriComponents the component to initialize with
	 */
	public UriComponents(Map<Type, String> uriComponents) {
		Assert.notNull(uriComponents, "'uriComponents' must not be null");
		this.uriComponents = new EnumMap<Type, String>(uriComponents);
	}

	// convenience properties

	/**
	 * Returns the scheme.
	 *
	 * @return the scheme. Can be {@code null}.
	 */
	public String getScheme() {
		return get(Type.SCHEME);
	}

	/**
	 * Sets the scheme.
	 *
	 * @param scheme the scheme. Can be {@code null}.
	 */
	public void setScheme(String scheme) {
		put(Type.SCHEME, scheme);
	}

	/**
	 * Returns the authority.
	 *
	 * @return the authority. Can be {@code null}.
	 */
	public String getAuthority() {
		return get(Type.AUTHORITY);
	}

	/**
	 * Sets the authority.
	 *
	 * @param authority the authority. Can be {@code null}.
	 */
	public void setAuthority(String authority) {
		put(Type.AUTHORITY, authority);
	}

	/**
	 * Returns the user info.
	 *
	 * @return the user info. Can be {@code null}.
	 */
	public String getUserInfo() {
		return get(Type.USER_INFO);
	}

	/**
	 * Sets the user info
	 *
	 * @param userInfo the user info. Can be {@code null}
	 */
	public void setUserInfo(String userInfo) {
		put(Type.USER_INFO, userInfo);
	}

	/**
	 * Returns the host.
	 *
	 * @return the host. Can be {@code null}.
	 */
	public String getHost() {
		return get(Type.HOST);
	}

	/**
	 * Sets the host.
	 *
	 * @param host the host. Can be {@code null}.
	 */
	public void setHost(String host) {
		put(Type.HOST, host);
	}

	/**
	 * Returns the port as string.
	 *
	 * @return the port as string. Can be {@code null}.
	 */
	public String getPort() {
		return get(Type.PORT);
	}

	/**
	 * Sets the port as string.
	 *
	 * @param port the port as string. Can be {@code null}.
	 */
	public void setPort(String port) {
		put(Type.PORT, port);
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
		put(Type.PORT, portString);
	}

	/**
	 * Returns the path.
	 *
	 * @return the path. Can be {@code null}.
	 */
	public String getPath() {
		return get(Type.PATH);
	}

	/**
	 * Sets the path.
	 *
	 * @param path the path. Can be {@code null}.
	 */
	public void setPath(String path) {
		put(Type.PATH, path);
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
		return get(Type.QUERY);
	}

	/**
	 * Sets the query.
	 *
	 * @param query the query. Can be {@code null}.
	 */
	public void setQuery(String query) {
		put(Type.QUERY, query);
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
		return get(Type.FRAGMENT);
	}

	/**
	 * Sets the fragment.
	 *
	 * @param fragment the fragment. Can be {@code null}.
	 */
	public void setFragment(String fragment) {
		put(Type.FRAGMENT, fragment);
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

	public String put(Type key, String value) {
		return this.uriComponents.put(key, value);
	}

	public String remove(Object key) {
		return this.uriComponents.remove(key);
	}

	public void putAll(Map<? extends Type, ? extends String> m) {
		this.uriComponents.putAll(m);
	}

	public void clear() {
		this.uriComponents.clear();
	}

	public Set<Type> keySet() {
		return this.uriComponents.keySet();
	}

	public Collection<String> values() {
		return this.uriComponents.values();
	}

	public Set<Entry<Type, String>> entrySet() {
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

	// inner types

	/**
	 * Enumeration used to identify the parts of a URI.
	 *
	 * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
	 *
	 * @author Arjen Poutsma
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
	 */
	public static enum Type {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		AUTHORITY {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
			}
		},
		HOST {
			@Override
			public boolean isAllowed(int c) {
				return isUnreserved(c) || isSubDelimiter(c);
			}
		},
		PORT {
			@Override
			public boolean isAllowed(int c) {
				return isDigit(c);
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c;
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '+' == c || '&' == c) {
					return false;
				}
				else {
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c) || '/' == c || '?' == c;
			}
		};

		/**
		 * Indicates whether the given character is allowed in this URI component.
		 *
		 * @param c the character
		 * @return {@code true} if the character is allowed; {@code false} otherwise
		 */
		public abstract boolean isAllowed(int c);

		/**
		 * Indicates whether the given character is in the {@code ALPHA} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isAlpha(int c) {
			return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
		}

		/**
		 * Indicates whether the given character is in the {@code DIGIT} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isDigit(int c) {
			return c >= '0' && c <= '9';
		}

		/**
		 * Indicates whether the given character is in the {@code gen-delims} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isGenericDelimiter(int c) {
			return ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c;
		}

		/**
		 * Indicates whether the given character is in the {@code sub-delims} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isSubDelimiter(int c) {
			return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c;
		}

		/**
		 * Indicates whether the given character is in the {@code reserved} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isReserved(char c) {
			return isGenericDelimiter(c) || isReserved(c);
		}

		/**
		 * Indicates whether the given character is in the {@code unreserved} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isUnreserved(int c) {
			return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
		}

		/**
		 * Indicates whether the given character is in the {@code pchar} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isPchar(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}

	}
}
