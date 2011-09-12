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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
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
public final class UriComponents implements Map<UriComponents.Type, String> {

    /**
     * The default encoding used for various encode methods.
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static final String SCHEME_PATTERN = "([^:/?#]+):";

    private static final String HTTP_PATTERN = "(http|https):";

    private static final String USERINFO_PATTERN = "([^@/]*)";

    private static final String HOST_PATTERN = "([^/?#:]*)";

    private static final String PORT_PATTERN = "(\\d*)";

    private static final String PATH_PATTERN = "([^?#]*)";

    private static final String QUERY_PATTERN = "([^#]*)";

    private static final String LAST_PATTERN = "(.*)";

    // Regex patterns that matches URIs. See RFC 3986, appendix B
    private static final Pattern URI_PATTERN = Pattern.compile(
            "^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
                    ")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
            "^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
                    PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");


    private static final String PATH_DELIMITER = "/";

    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)=?([^&=]+)?");

    private final Map<Type, String> uriComponents;

    private final boolean encoded;

    private UriComponents(Map<Type, String> uriComponents, boolean encoded) {
        Assert.notEmpty(uriComponents, "'uriComponents' must not be empty");
        this.uriComponents = Collections.unmodifiableMap(uriComponents);
        this.encoded = encoded;
    }

    /**
     * Creates a new {@code UriComponents} object from the given string URI.
     *
     * @param uri the source URI
     * @return the URI components of the URI
     */
    public static UriComponents fromUriString(String uri) {
        Assert.notNull(uri, "'uri' must not be null");
        Matcher m = URI_PATTERN.matcher(uri);
        if (m.matches()) {
            Map<UriComponents.Type, String> result = new EnumMap<UriComponents.Type, String>(UriComponents.Type.class);

            result.put(UriComponents.Type.SCHEME, m.group(2));
            result.put(UriComponents.Type.AUTHORITY, m.group(3));
            result.put(UriComponents.Type.USER_INFO, m.group(5));
            result.put(UriComponents.Type.HOST, m.group(6));
            result.put(UriComponents.Type.PORT, m.group(8));
            result.put(UriComponents.Type.PATH, m.group(9));
            result.put(UriComponents.Type.QUERY, m.group(11));
            result.put(UriComponents.Type.FRAGMENT, m.group(13));

            return new UriComponents(result, false);
        }
        else {
            throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
        }
    }

    /**
     * Creates a new {@code UriComponents} object from the string HTTP URL.
     *
     * @param httpUrl the source URI
     * @return the URI components of the URI
     */
    public static UriComponents fromHttpUrl(String httpUrl) {
        Assert.notNull(httpUrl, "'httpUrl' must not be null");
        Matcher m = HTTP_URL_PATTERN.matcher(httpUrl);
        if (m.matches()) {
            Map<UriComponents.Type, String> result = new EnumMap<UriComponents.Type, String>(UriComponents.Type.class);

            result.put(UriComponents.Type.SCHEME, m.group(1));
            result.put(UriComponents.Type.AUTHORITY, m.group(2));
            result.put(UriComponents.Type.USER_INFO, m.group(4));
            result.put(UriComponents.Type.HOST, m.group(5));
            result.put(UriComponents.Type.PORT, m.group(7));
            result.put(UriComponents.Type.PATH, m.group(8));
            result.put(UriComponents.Type.QUERY, m.group(10));

            return new UriComponents(result, false);
        }
        else {
            throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
        }
    }

    /**
     * Creates a new {@code UriComponents} object from the given {@code URI}.
     *
     * @param uri the URI
     * @return the URI components of the URI
     */
    public static UriComponents fromUri(URI uri) {
        Assert.notNull(uri, "'uri' must not be null");

        Map<Type, String> uriComponents = new EnumMap<Type, String>(Type.class);
        if (uri.getScheme() != null) {
            uriComponents.put(Type.SCHEME, uri.getScheme());
        }
        if (uri.getRawAuthority() != null) {
            uriComponents.put(Type.AUTHORITY, uri.getRawAuthority());
        }
        if (uri.getRawUserInfo() != null) {
            uriComponents.put(Type.USER_INFO, uri.getRawUserInfo());
        }
        if (uri.getHost() != null) {
            uriComponents.put(Type.HOST, uri.getHost());
        }
        if (uri.getPort() != -1) {
            uriComponents.put(Type.PORT, Integer.toString(uri.getPort()));
        }
        if (uri.getRawPath() != null) {
            uriComponents.put(Type.PATH, uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            uriComponents.put(Type.QUERY, uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            uriComponents.put(Type.FRAGMENT, uri.getRawFragment());
        }
        return new UriComponents(uriComponents, true);
    }


    /**
     * Creates an instance of the {@code UriComponents} object from the given components. All the given arguments
     * can be {@code null} and are considered to be unencoded.
     */
    public static UriComponents fromUriComponents(String scheme,
                         String authority,
                         String userInfo,
                         String host,
                         String port,
                         String path,
                         String query,
                         String fragment) {
        return fromUriComponents(scheme, authority, userInfo, host, port, path, query, fragment, false);
    }

    /**
     * Creates an instance of the {@code UriComponents} object from the given components. All the given arguments
     * can be {@code null}.
     *
     * @param encoded {@code true} if the arguments are encoded; {@code false} otherwise
     */
    public static UriComponents fromUriComponents(String scheme,
                                                  String authority,
                                                  String userInfo,
                                                  String host,
                                                  String port,
                                                  String path,
                                                  String query,
                                                  String fragment,
                                                  boolean encoded) {
        Map<Type, String> uriComponents = new EnumMap<Type, String>(Type.class);
        if (scheme != null) {
            uriComponents.put(Type.SCHEME, scheme);
        }
        if (authority != null) {
            uriComponents.put(Type.AUTHORITY, authority);
        }
        if (userInfo != null) {
            uriComponents.put(Type.USER_INFO, userInfo);
        }
        if (host != null) {
            uriComponents.put(Type.HOST, host);
        }
        if (port != null) {
            uriComponents.put(Type.PORT, port);
        }
        if (path != null) {
            uriComponents.put(Type.PATH, path);
        }
        if (query != null) {
            uriComponents.put(Type.QUERY, query);
        }
        if (fragment != null) {
            uriComponents.put(Type.FRAGMENT, fragment);
        }
        return new UriComponents(uriComponents, encoded);
    }

    /**
     * Creates an instance of the {@code UriComponents} object that contains the given components map.
     *
     * @param uriComponents the component to initialize with
     */
    public static UriComponents fromUriComponentMap(Map<Type, String> uriComponents) {
        boolean encoded;
        if (uriComponents instanceof UriComponents) {
            encoded = ((UriComponents) uriComponents).encoded;
        }
        else {
            encoded = false;
        }
        return new UriComponents(uriComponents, encoded);
    }

    /**
     * Creates an instance of the {@code UriComponents} object that contains the given components map.
     *
     * @param uriComponents the component to initialize with
     * @param encoded whether the components are encpded
     */
    public static UriComponents fromUriComponentMap(Map<Type, String> uriComponents, boolean encoded) {
        return new UriComponents(uriComponents, encoded);
    }

    // component getters

    /**
     * Returns the scheme.
     *
     * @return the scheme. Can be {@code null}.
     */
    public String getScheme() {
        return get(Type.SCHEME);
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
     * Returns the user info.
     *
     * @return the user info. Can be {@code null}.
     */
    public String getUserInfo() {
        return get(Type.USER_INFO);
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
     * Returns the port as string.
     *
     * @return the port as string. Can be {@code null}.
     */
    public String getPort() {
        return get(Type.PORT);
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
     * Returns the path.
     *
     * @return the path. Can be {@code null}.
     */
    public String getPath() {
        return get(Type.PATH);
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
     * Returns the query.
     *
     * @return the query. Can be {@code null}.
     */
    public String getQuery() {
        return get(Type.QUERY);
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
     * Returns the fragment.
     *
     * @return the fragment. Can be {@code null}.
     */
    public String getFragment() {
        return get(Type.FRAGMENT);
    }

    // other functionality

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

        final Map<Type, String> encoded = new EnumMap<Type, String>(Type.class);
        for (Entry<Type, String> entry : uriComponents.entrySet()) {
            Type key = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                value = encodeUriComponent(value, encoding, key);
            }
            encoded.put(key, value);
        }
        return new UriComponents(encoded, true);
    }

    /**
     * Encodes the given source into an encoded String using the rules specified by the given component and with the
     * given options.
     *
     * @param source the source string
     * @param encoding the encoding of the source string
     * @param uriComponent the URI component for the source
     * @param encodingOptions the options used when encoding. May be {@code null}.
     * @return the encoded URI
     * @throws IllegalArgumentException when the given uri parameter is not a valid URI
     * @see EncodingOption
     */
    static String encodeUriComponent(String source,
                                            String encoding,
                                            UriComponents.Type uriComponent) throws UnsupportedEncodingException {
        Assert.hasLength(encoding, "'encoding' must not be empty");

        byte[] bytes = encodeInternal(source.getBytes(encoding), uriComponent);
        return new String(bytes, "US-ASCII");
    }

    private static byte[] encodeInternal(byte[] source,
                                         UriComponents.Type uriComponent) {
        Assert.notNull(source, "'source' must not be null");
        Assert.notNull(uriComponent, "'uriComponent' must not be null");

        ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
        for (int i = 0; i < source.length; i++) {
            int b = source[i];
            if (b < 0) {
                b += 256;
            }
            if (uriComponent.isAllowed(b)) {
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


    /**
     * Returns a URI string from this {@code UriComponents} instance.
     *
     * @return the URI created from the given components
     */
    public String toUriString() {
        StringBuilder uriBuilder = new StringBuilder();

        if (getScheme() != null) {
            uriBuilder.append(getScheme());
            uriBuilder.append(':');
        }

        if (getUserInfo() != null || getHost() != null || getPort() != null) {
            uriBuilder.append("//");
            if (getUserInfo() != null) {
                uriBuilder.append(getUserInfo());
                uriBuilder.append('@');
            }
            if (getHost() != null) {
                uriBuilder.append(getHost());
            }
            if (getPort() != null) {
                uriBuilder.append(':');
                uriBuilder.append(getPort());
            }
        }
        else if (getAuthority() != null) {
            uriBuilder.append("//");
            uriBuilder.append(getAuthority());
        }

        if (getPath() != null) {
            uriBuilder.append(getPath());
        }

        if (getQuery() != null) {
            uriBuilder.append('?');
            uriBuilder.append(getQuery());
        }

        if (getFragment() != null) {
            uriBuilder.append('#');
            uriBuilder.append(getFragment());
        }

        return uriBuilder.toString();
    }

    /**
     * Returns a {@code URI} from this {@code UriComponents} instance.
     *
     * @return the URI created from the given components
     */
    public URI toUri() {
        try {
            if (encoded) {
                return new URI(toUriString());
            }
            else {
                if (getUserInfo() != null || getHost() != null || getPort() != null) {
                    return new URI(getScheme(), getUserInfo(), getHost(), getPortAsInteger(), getPath(), getQuery(),
                            getFragment());
                }
                else {
                    return new URI(getScheme(), getAuthority(), getPath(), getQuery(), getFragment());
                }
            }
        }
        catch (URISyntaxException ex) {
            throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
        }
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
