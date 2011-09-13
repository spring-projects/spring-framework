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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Represents an immutable collection of URI components, mapping component type to string values. Contains convenience
 * getters for all components, as well as the regular {@link Map} implementation. Effectively similar to {@link URI},
 * but with more powerful encoding options.
 * <p/>
 * <strong>Note</strong> that this {@code Map} does not contain entries for {@link Type#PATH_SEGMENT}
 * nor {@link Type#QUERY_PARAM}, since those components can occur multiple
 * times in the URI. Instead, one can use {@link #getPathSegments()} or {@link #getQueryParams()} respectively.
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see UriComponentsBuilder
 */
public final class UriComponents {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final char PATH_DELIMITER = '/';

	/** Captures URI template variable names. */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	private final String scheme;

	private final String userInfo;

	private final String host;

	private final int port;

	private final List<String> pathSegments;

	private final MultiValueMap<String, String> queryParams;

	private final String fragment;

    private final boolean encoded;

	public UriComponents(String scheme,
						 String userInfo,
						 String host,
						 int port,
						 List<String> pathSegments,
						 MultiValueMap<String, String> queryParams,
						 String fragment,
						 boolean encoded) {
		this.scheme = scheme;
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		if (pathSegments == null) {
			pathSegments = Collections.emptyList();
		}
		this.pathSegments = Collections.unmodifiableList(pathSegments);
		if (queryParams == null) {
			queryParams = new LinkedMultiValueMap<String, String>(0);
		}
		this.queryParams = CollectionUtils.unmodifiableMultiValueMap(queryParams);
		this.fragment = fragment;
		this.encoded = encoded;
	}

    // component getters

    /**
     * Returns the scheme.
     *
     * @return the scheme. Can be {@code null}.
     */
    public String getScheme() {
		return scheme;
    }

    /**
     * Returns the user info.
     *
     * @return the user info. Can be {@code null}.
     */
    public String getUserInfo() {
		return userInfo;
    }

    /**
     * Returns the host.
     *
     * @return the host. Can be {@code null}.
     */
    public String getHost() {
		return host;
    }

    /**
     * Returns the port. Returns {@code -1} if no port has been set.
     *
     * @return the port
     */
    public int getPort() {
		return port;
    }

	/**
	 * Returns the path.
	 *
	 * @return the path. Can be {@code null}.
	 */
	public String getPath() {
		if (!pathSegments.isEmpty()) {
			StringBuilder pathBuilder = new StringBuilder();
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasLength(pathSegment)) {
					boolean startsWithSlash = pathSegment.charAt(0) == PATH_DELIMITER;
					boolean endsWithSlash =
							pathBuilder.length() > 0 && pathBuilder.charAt(pathBuilder.length() - 1) == PATH_DELIMITER;

					if (!endsWithSlash && !startsWithSlash) {
						pathBuilder.append('/');
					}
					else if (endsWithSlash && startsWithSlash) {
						pathSegment = pathSegment.substring(1);
					}
					pathBuilder.append(pathSegment);
				}
			}
			return pathBuilder.toString();
		}
		else {
			return null;
		}
	}

    /**
     * Returns the list of path segments.
     *
     * @return the path segments. Empty if no path has been set.
     */
    public List<String> getPathSegments() {
		return pathSegments;
    }

	/**
	 * Returns the query.
	 *
	 * @return the query. Can be {@code null}.
	 */
	public String getQuery() {
		if (!queryParams.isEmpty()) {
			StringBuilder queryBuilder = new StringBuilder();
			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				String name = entry.getKey();
				List<String> values = entry.getValue();
				if (CollectionUtils.isEmpty(values)) {
					if (queryBuilder.length() != 0) {
						queryBuilder.append('&');
					}
					queryBuilder.append(name);
				}
				else {
					for (Object value : values) {
						if (queryBuilder.length() != 0) {
							queryBuilder.append('&');
						}
						queryBuilder.append(name);

						if (value != null) {
							queryBuilder.append('=');
							queryBuilder.append(value.toString());
						}
					}
				}
			}
			return queryBuilder.toString();
		}
		else {
			return null;
		}
	}

	/**
     * Returns the map of query parameters.
     *
     * @return the query parameters. Empty if no query has been set.
     */
    public MultiValueMap<String, String> getQueryParams() {
		return queryParams;
    }

    /**
     * Returns the fragment.
     *
     * @return the fragment. Can be {@code null}.
     */
    public String getFragment() {
		return fragment;
    }

    // encoding

	/**
	 * Encodes all URI components using their specific encoding rules, and returns the result as a new
	 * {@code UriComponents} instance. This method uses UTF-8 to encode.
	 *
	 * @return the encoded uri components
	 */
    public UriComponents encode() {
        try {
            return encode(DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException e) {
            throw new InternalError("\"" + DEFAULT_ENCODING + "\" not supported");
        }
    }

    /**
     * Encodes all URI components using their specific encoding rules, and returns the result as a new
     * {@code UriComponents} instance.
     *
     * @param encoding the encoding of the values contained in this map
     * @return the encoded uri components
     * @throws UnsupportedEncodingException if the given encoding is not supported
     */
    public UriComponents encode(String encoding) throws UnsupportedEncodingException {
        Assert.hasLength(encoding, "'encoding' must not be empty");

        if (encoded) {
            return this;
        }

		String encodedScheme = encodeUriComponent(this.scheme, encoding, Type.SCHEME);
		String encodedUserInfo = encodeUriComponent(this.userInfo, encoding, Type.USER_INFO);
		String encodedHost = encodeUriComponent(this.host, encoding, Type.HOST);
		List<String> encodedPathSegments = new ArrayList<String>(this.pathSegments.size());
		for (String pathSegment : this.pathSegments) {
			String encodedPathSegment = encodeUriComponent(pathSegment, encoding, Type.PATH_SEGMENT);
			encodedPathSegments.add(encodedPathSegment);
		}
		MultiValueMap<String, String> encodedQueryParams =
				new LinkedMultiValueMap<String, String>(this.queryParams.size());
		for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
			String encodedName = encodeUriComponent(entry.getKey(), encoding, Type.QUERY_PARAM);
			List<String> encodedValues = new ArrayList<String>(entry.getValue().size());
			for (String value : entry.getValue()) {
				String encodedValue = encodeUriComponent(value, encoding, Type.QUERY_PARAM);
				encodedValues.add(encodedValue);
			}
			encodedQueryParams.put(encodedName, encodedValues);
		}
		String encodedFragment = encodeUriComponent(this.fragment, encoding, Type.FRAGMENT);

		return new UriComponents(encodedScheme, encodedUserInfo, encodedHost, this.port, encodedPathSegments,
				encodedQueryParams, encodedFragment, true);
    }

    /**
     * Encodes the given source into an encoded String using the rules specified by the given component and with the
     * given options.
     *
     * @param source the source string
     * @param encoding the encoding of the source string
     * @param type the URI component for the source
     * @return the encoded URI
     * @throws IllegalArgumentException when the given uri parameter is not a valid URI
     */
	static String encodeUriComponent(String source, String encoding, Type type)
			throws UnsupportedEncodingException {
		if (source == null) {
			return null;
		}
		
		Assert.hasLength(encoding, "'encoding' must not be empty");

		byte[] bytes = encodeBytes(source.getBytes(encoding), type);
		return new String(bytes, "US-ASCII");
	}

	private static byte[] encodeBytes(byte[] source, Type type) {
		Assert.notNull(source, "'source' must not be null");
        Assert.notNull(type, "'type' must not be null");

        ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
        for (int i = 0; i < source.length; i++) {
            int b = source[i];
            if (b < 0) {
                b += 256;
            }
            if (type.isAllowed(b)) {
                bos.write(b);
            }
            else {
                bos.write('%');

                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));

                bos.write(hex1);
                bos.write(hex2);
            }
        }
        return bos.toByteArray();
    }

	// expanding

	/**
	 * Replaces all URI template variables with the values from a given map. The map keys represent
	 * variable names; the values variable values. The order of variables is not significant.

	 * @param uriVariables the map of URI variables
	 * @return the expanded uri components
	 */
	public UriComponents expand(Map<String, ?> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");

		String expandedScheme = expandUriComponent(this.scheme, uriVariables);
		String expandedUserInfo = expandUriComponent(this.userInfo, uriVariables);
		String expandedHost = expandUriComponent(this.host, uriVariables);
		List<String> expandedPathSegments = new ArrayList<String>(this.pathSegments.size());
		for (String pathSegment : this.pathSegments) {
			String expandedPathSegment = expandUriComponent(pathSegment, uriVariables);
			expandedPathSegments.add(expandedPathSegment);
		}
		MultiValueMap<String, String> expandedQueryParams =
				new LinkedMultiValueMap<String, String>(this.queryParams.size());
		for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
			String expandedName = expandUriComponent(entry.getKey(), uriVariables);
			List<String> expandedValues = new ArrayList<String>(entry.getValue().size());
			for (String value : entry.getValue()) {
				String expandedValue = expandUriComponent(value, uriVariables);
				expandedValues.add(expandedValue);
			}
			expandedQueryParams.put(expandedName, expandedValues);
		}
		String expandedFragment = expandUriComponent(this.fragment, uriVariables);

		return new UriComponents(expandedScheme, expandedUserInfo, expandedHost, this.port, expandedPathSegments,
				expandedQueryParams, expandedFragment, false);
	}

	private String expandUriComponent(String source, Map<String, ?> uriVariables) {
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
			Object variableValue = uriVariables.get(variableName);
			String uriVariableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(uriVariableValueString);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Replaces all URI template variables with the values from a given array. The array represent variable values.
	 * The order of variables is significant.

	 * @param uriVariableValues URI variable values
	 * @return the expanded uri components
	 */
	public UriComponents expand(Object... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");

		Iterator<Object> valueIterator = Arrays.asList(uriVariableValues).iterator();

		String expandedScheme = expandUriComponent(this.scheme, valueIterator);
		String expandedUserInfo = expandUriComponent(this.userInfo, valueIterator);
		String expandedHost = expandUriComponent(this.host, valueIterator);
		List<String> expandedPathSegments = new ArrayList<String>(this.pathSegments.size());
		for (String pathSegment : this.pathSegments) {
			String expandedPathSegment = expandUriComponent(pathSegment, valueIterator);
			expandedPathSegments.add(expandedPathSegment);
		}
		MultiValueMap<String, String> expandedQueryParams =
				new LinkedMultiValueMap<String, String>(this.queryParams.size());
		for (Map.Entry<String, List<String>> entry : this.queryParams.entrySet()) {
			String expandedName = expandUriComponent(entry.getKey(), valueIterator);
			List<String> expandedValues = new ArrayList<String>(entry.getValue().size());
			for (String value : entry.getValue()) {
				String expandedValue = expandUriComponent(value, valueIterator);
				expandedValues.add(expandedValue);
			}
			expandedQueryParams.put(expandedName, expandedValues);
		}
		String expandedFragment = expandUriComponent(this.fragment, valueIterator);

		return new UriComponents(expandedScheme, expandedUserInfo, expandedHost, this.port, expandedPathSegments,
				expandedQueryParams, expandedFragment, false);
	}

	private String expandUriComponent(String source, Iterator<Object> valueIterator) {
		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		Matcher matcher = NAMES_PATTERN.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			if (!valueIterator.hasNext()) {
				throw new IllegalArgumentException("Not enough variable values available to expand [" + source + "]");
			}
			Object variableValue = valueIterator.next();
			String uriVariableValueString = getVariableValueAsString(variableValue);
			String replacement = Matcher.quoteReplacement(uriVariableValueString);
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}


	private String getVariableName(String match) {
		int colonIdx = match.indexOf(':');
		return colonIdx == -1 ? match : match.substring(0, colonIdx);
	}

	protected String getVariableValueAsString(Object variableValue) {
		return variableValue != null ? variableValue.toString() : "";
	}





	// other functionality

    /**
     * Returns a URI string from this {@code UriComponents} instance.
     *
     * @return the URI string
     */
    public String toUriString() {
        StringBuilder uriBuilder = new StringBuilder();

        if (scheme != null) {
            uriBuilder.append(scheme);
            uriBuilder.append(':');
        }

        if (userInfo != null || host != null) {
            uriBuilder.append("//");
            if (userInfo != null) {
				uriBuilder.append(userInfo);
                uriBuilder.append('@');
            }
            if (host != null) {
				uriBuilder.append(host);
            }
            if (port != -1) {
                uriBuilder.append(':');
				uriBuilder.append(port);
            }
        }

		String path = getPath();
		if (path != null) {
            uriBuilder.append(path);
        }

		String query = getQuery();
		if (query != null) {
            uriBuilder.append('?');
            uriBuilder.append(query);
        }

        if (fragment != null) {
            uriBuilder.append('#');
			uriBuilder.append(fragment);
        }

        return uriBuilder.toString();
    }

    /**
     * Returns a {@code URI} from this {@code UriComponents} instance.
     *
     * @return the URI
     */
    public URI toUri() {
        try {
            if (encoded) {
                return new URI(toUriString());
            }
            else {
				return new URI(getScheme(), getUserInfo(), getHost(), getPort(), getPath(), getQuery(),
						getFragment());
            }
        }
        catch (URISyntaxException ex) {
            throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
        }
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof UriComponents) {
			UriComponents other = (UriComponents) o;

			if (scheme != null ? !scheme.equals(other.scheme) : other.scheme != null) {
				return false;
			}
			if (userInfo != null ? !userInfo.equals(other.userInfo) : other.userInfo != null) {
				return false;
			}
			if (host != null ? !host.equals(other.host) : other.host != null) {
				return false;
			}
			if (port != other.port) {
				return false;
			}
			if (!pathSegments.equals(other.pathSegments)) {
				return false;
			}
			if (!queryParams.equals(other.queryParams)) {
				return false;
			}
			if (fragment != null ? !fragment.equals(other.fragment) : other.fragment != null) {
				return false;
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int result = scheme != null ? scheme.hashCode() : 0;
		result = 31 * result + (userInfo != null ? userInfo.hashCode() : 0);
		result = 31 * result + (host != null ? host.hashCode() : 0);
		result = 31 * result + port;
		result = 31 * result + pathSegments.hashCode();
		result = 31 * result + queryParams.hashCode();
		result = 31 * result + (fragment != null ? fragment.hashCode() : 0);
		return result;
	}

	@Override
    public String toString() {
		return toUriString();
    }

    // inner types

    /**
     * Enumeration used to identify the parts of a URI.
     * <p/>
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
