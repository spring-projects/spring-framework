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

import org.springframework.util.Assert;
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

    private String scheme;

    private String userInfo;

    private String host;

    private int port = -1;

    private final List<String> pathSegments = new ArrayList<String>();

    private final StringBuilder queryBuilder = new StringBuilder();

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
     * Returns a new, empty URI builder.
     *
     * @return the new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder newInstance() {
        return new UriComponentsBuilder();
    }

    /**
     * Returns a URI builder that is initialized with the given path.
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
     * Returns a URI builder that is initialized with the given {@code URI}.
     *
     * @param uri the URI to initialize with
     * @return the new {@code UriComponentsBuilder}
     */
    public static UriComponentsBuilder fromUri(URI uri) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.uri(uri);
        return builder;
    }

    // build methods

    /**
     * Builds a {@code UriComponents} instance from the various components contained in this builder.
     *
     * @return the URI components
     */
    public UriComponents build() {
        String port = portAsString();
        String path = pathAsString();
        String query = queryAsString();
        return UriComponents.fromUriComponents(scheme, null, userInfo, host, port, path, query, fragment, false);
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
            String[] pathSegments = StringUtils.tokenizeToStringArray(uri.getPath(), "/");

            this.pathSegments.clear();
            Collections.addAll(this.pathSegments, pathSegments);
        }
        if (StringUtils.hasLength(uri.getQuery())) {
            this.queryBuilder.setLength(0);
            this.queryBuilder.append(uri.getQuery());
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
        Assert.notNull(path, "path must not be null");

        String[] pathSegments = StringUtils.tokenizeToStringArray(path, "/");
        return pathSegment(pathSegments);
    }

    private String pathAsString() {
        if (!pathSegments.isEmpty()) {
            StringBuilder pathBuilder = new StringBuilder();
            for (String pathSegment : pathSegments) {
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
            return pathBuilder.toString();
        }
        else {
            return null;
        }
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

        if (ObjectUtils.isEmpty(values)) {
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
        return this;
    }

    private String queryAsString() {
        return queryBuilder.length() != 0 ? queryBuilder.toString() : null;
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
