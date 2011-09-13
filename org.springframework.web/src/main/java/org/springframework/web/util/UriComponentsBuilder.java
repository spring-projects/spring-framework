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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Builder for {@link UriComponents}.
 * <p/>
 * Typical usage involves:
 * <ol>
 *     <li>Create a {@code UriComponentsBuilder} with one of the static factory methods (such as
 *     {@link #fromPath(String)} or {@link #fromUri(URI)})</li>
 *     <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 *     {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 *     {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and
 *     {@link #fragment(String)}.</li>
 *     <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 * @since 3.1
 */
public class UriComponentsBuilder {

    private static final char PATH_DELIMITER = '/';

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)=?([^&=]+)?");

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


    private String scheme;

    private String userInfo;

    private String host;

    private int port = -1;

    private final List<String> pathSegments = new ArrayList<String>();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();

    private String fragment;

	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
    }

    // Factory methods

    /**
     * Returns a new, empty builder.
     *
     * @return the new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder newInstance() {
        return new UriComponentsBuilder();
    }

    /**
     * Returns a builder that is initialized with the given path.
     *
     * @param path the path to initialize with
     * @return the new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromPath(String path) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.path(path);
        return builder;
    }

    /**
     * Returns a builder that is initialized with the given {@code URI}.
     *
     * @param uri the URI to initialize with
     * @return the new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromUri(URI uri) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.uri(uri);
        return builder;
    }

	/**
	 * Returns a builder that is initialized with the given URI string.
	 *
	 * @param uri the URI string to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.hasLength(uri, "'uri' must not be empty");
		Matcher m = URI_PATTERN.matcher(uri);
		if (m.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();

			builder.scheme(m.group(2));
			builder.userInfo(m.group(5));
			builder.host(m.group(6));
			String port = m.group(8);
			if (StringUtils.hasLength(port)) {
				builder.port(Integer.parseInt(port));
			}
			builder.path(m.group(9));
			builder.query(m.group(11));
			builder.fragment(m.group(13));

			return builder;
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
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "'httpUrl' must not be null");
		Matcher m = HTTP_URL_PATTERN.matcher(httpUrl);
		if (m.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();

			builder.scheme(m.group(1));
			builder.userInfo(m.group(4));
			builder.host(m.group(5));
			String port = m.group(7);
			if (StringUtils.hasLength(port)) {
				builder.port(Integer.parseInt(port));
			}
			builder.path(m.group(8));
			builder.query(m.group(10));

			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}



    // build methods

    /**
     * Builds a {@code UriComponents} instance from the various components contained in this builder.
     *
     * @return the URI components
     */
    public UriComponents build() {
		return build(false);
	}

    /**
     * Builds a {@code UriComponents} instance from the various components contained in this builder.
     *
	 * @param encoded whether all the components set in this builder are encoded ({@code true}) or not ({@code false}).
	 * @return the URI components
     */
    public UriComponents build(boolean encoded) {
		return new UriComponents(scheme, userInfo, host, port, pathSegments, queryParams, fragment, encoded);
    }

    // URI components methods

    /**
     * Initializes all components of this URI builder with the components of the given URI.
     *
     * @param uri the URI
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder uri(URI uri) {
        Assert.notNull(uri, "'uri' must not be null");
        Assert.isTrue(!uri.isOpaque(), "Opaque URI [" + uri + "] not supported");

        this.scheme = uri.getScheme();

        if (uri.getUserInfo() != null) {
            this.userInfo = uri.getUserInfo();
        }
        if (uri.getHost() != null) {
            this.host = uri.getHost();
        }
        if (uri.getPort() != -1) {
            this.port = uri.getPort();
        }
        if (StringUtils.hasLength(uri.getPath())) {
            this.pathSegments.clear();
			path(uri.getPath());
        }
        if (StringUtils.hasLength(uri.getQuery())) {
			this.queryParams.clear();
			query(uri.getQuery());
        }
        if (uri.getFragment() != null) {
            this.fragment = uri.getFragment();
        }
        return this;
    }

    /**
     * Sets the URI scheme. The given scheme may contain URI template variables, and may also be {@code null} to clear the
     * scheme of this builder.
     *
     * @param scheme the URI scheme
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * Sets the URI user info. The given user info may contain URI template variables, and may also be {@code null} to
     * clear the user info of this builder.
     *
     * @param userInfo the URI user info
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder userInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    /**
     * Sets the URI host. The given host may contain URI template variables, and may also be {@code null} to clear the host
     * of this builder.
     *
     * @param host the URI host
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder host(String host) {
        this.host = host;
        return this;
    }

    /**
     * Sets the URI port. Passing {@code -1} will clear the port of this builder.
     *
     * @param port the URI port
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder port(int port) {
        Assert.isTrue(port >= -1, "'port' must not be < -1");
        this.port = port;
        return this;
    }

    private String portAsString() {
        return this.port != -1 ? Integer.toString(this.port) : null;
    }

    /**
     * Appends the given path to the existing path of this builder. The given path may contain URI template variables.
     *
     * @param path the URI path
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder path(String path) {
		if (path != null) {
	        String[] pathSegments = StringUtils.tokenizeToStringArray(path, "/");
    	    pathSegment(pathSegments);
		} else {
			pathSegments.clear();
		}
		return this;
    }

    /**
     * Appends the given path segments to the existing path of this builder. Each given path segments may contain URI
     * template variables.
     *
     * @param segments the URI path segments
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder pathSegment(String... segments) throws IllegalArgumentException {
        Assert.notNull(segments, "'segments' must not be null");
        Collections.addAll(this.pathSegments, segments);

        return this;
    }

	/**
	 * Appends the given query to the existing query of this builder. The given query may contain URI template variables.
	 *
	 * @param query the URI path
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder query(String query) {
		if (query != null) {
			Matcher m = QUERY_PARAM_PATTERN.matcher(query);
			while (m.find()) {
				String name = m.group(1);
				String value = m.group(2);
				queryParam(name, value);
			}
		}
		else {
			queryParams.clear();
		}
		return this;
	}


    /**
     * Appends the given query parameter to the existing query parameters. The given name or any of the values may contain
     * URI template variables. If no values are given, the resulting URI will contain the query parameter name only (i.e.
     * {@code ?foo} instead of {@code ?foo=bar}.
     *
     * @param name   the query parameter name
     * @param values the query parameter values
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "'name' must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = value != null ? value.toString() : null;
				queryParams.add(name, valueAsString);
			}
		}
		else {
			queryParams.add(name, null);
		}
		return this;
	}

    /**
     * Sets the URI fragment. The given fragment may contain URI template variables, and may also be {@code null} to clear
     * the fragment of this builder.
     *
     * @param fragment the URI fragment
     * @return this UriComponentsBuilder
     */
    public UriComponentsBuilder fragment(String fragment) {
        if (fragment != null) {
            Assert.hasLength(fragment, "'fragment' must not be empty");
            this.fragment = fragment;
        }
        else {
            this.fragment = null;
        }
        return this;
    }

}
