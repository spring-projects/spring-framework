/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.reactive.accept.ApiVersionResolver;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.accept.DefaultApiVersionStrategy;
import org.springframework.web.reactive.accept.PathApiVersionResolver;

/**
 * Configure API versioning.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ApiVersionConfigurer {

	private final List<ApiVersionResolver> versionResolvers = new ArrayList<>();

	private @Nullable ApiVersionParser<?> versionParser;

	private boolean versionRequired = true;

	private @Nullable String defaultVersion;

	private final Set<String> supportedVersions = new LinkedHashSet<>();

	private boolean detectSupportedVersions = true;


	/**
	 * Add a resolver that extracts the API version from a request header.
	 * @param headerName the header name to check
	 */
	public ApiVersionConfigurer useRequestHeader(String headerName) {
		this.versionResolvers.add(exchange -> exchange.getRequest().getHeaders().getFirst(headerName));
		return this;
	}

	/**
	 * Add a resolver that extracts the API version from a request parameter.
	 * @param paramName the parameter name to check
	 */
	public ApiVersionConfigurer useRequestParam(String paramName) {
		this.versionResolvers.add(exchange -> exchange.getRequest().getQueryParams().getFirst(paramName));
		return this;
	}

	/**
	 * Add a resolver that extracts the API version from a path segment.
	 * @param index the index of the path segment to check; e.g. for URL's like
	 * "/{version}/..." use index 0, for "/api/{version}/..." index 1.
	 */
	public ApiVersionConfigurer usePathSegment(int index) {
		this.versionResolvers.add(new PathApiVersionResolver(index));
		return this;
	}

	/**
	 * Add custom resolvers to resolve the API version.
	 * @param resolvers the resolvers to use
	 */
	public ApiVersionConfigurer useVersionResolver(ApiVersionResolver... resolvers) {
		this.versionResolvers.addAll(Arrays.asList(resolvers));
		return this;
	}

	/**
	 * Configure a parser to parse API versions with.
	 * <p>By default, {@link SemanticApiVersionParser} is used.
	 * @param versionParser the parser to user
	 */
	public ApiVersionConfigurer setVersionParser(@Nullable ApiVersionParser<?> versionParser) {
		this.versionParser = versionParser;
		return this;
	}

	/**
	 * Whether requests are required to have an API version. When set to
	 * {@code true}, {@link org.springframework.web.accept.MissingApiVersionException}
	 * is raised, resulting in a 400 response if the request doesn't have an API
	 * version. When set to false, a request without a version is considered to
	 * accept any version.
	 * <p>By default, this is set to {@code true} when API versioning is enabled
	 * by adding at least one {@link ApiVersionResolver}). When a
	 * {@link #setDefaultVersion defaultVersion} is also set, this is
	 * automatically set to {@code false}.
	 * @param required whether an API version is required.
	 */
	public ApiVersionConfigurer setVersionRequired(boolean required) {
		this.versionRequired = required;
		return this;
	}

	/**
	 * Configure a default version to assign to requests that don't specify one.
	 * @param defaultVersion the default version to use
	 */
	public ApiVersionConfigurer setDefaultVersion(@Nullable String defaultVersion) {
		this.defaultVersion = defaultVersion;
		return this;
	}

	/**
	 * Add to the list of supported versions to check against before raising
	 * {@link InvalidApiVersionException} for unknown versions.
	 * <p>By default, actual version values that appear in request mappings are
	 * used for validation. Therefore, use of this method is optional. However,
	 * if you prefer to use explicitly configured, supported versions only, then
	 * set {@link #detectSupportedVersions} to {@code false}.
	 * <p>Note that the initial API version, if not explicitly declared in any
	 * request mappings, may need to be declared here instead as a supported
	 * version.
	 * @param versions supported version values to add
	 */
	public ApiVersionConfigurer addSupportedVersions(String... versions) {
		Collections.addAll(this.supportedVersions, versions);
		return this;
	}

	/**
	 * Whether to use versions from mappings for supported version validation.
	 * <p>By default, this is {@code true} in which case mapped versions are
	 * considered supported versions. Set this to {@code false} if you want to
	 * use only explicitly configured {@link #addSupportedVersions(String...)
	 * supported versions}.
	 * @param detect whether to use detected versions for validation
	 */
	public ApiVersionConfigurer detectSupportedVersions(boolean detect) {
		this.detectSupportedVersions = detect;
		return this;
	}

	protected @Nullable ApiVersionStrategy getApiVersionStrategy() {
		if (this.versionResolvers.isEmpty()) {
			return null;
		}

		DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(this.versionResolvers,
				(this.versionParser != null ? this.versionParser : new SemanticApiVersionParser()),
				this.versionRequired, this.defaultVersion, this.detectSupportedVersions);

		this.supportedVersions.forEach(strategy::addSupportedVersion);

		return strategy;
	}

}
