/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ApiVersionStrategy} that delegates to the
 * configured version resolvers, version parser, and deprecation handler.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class DefaultApiVersionStrategy implements ApiVersionStrategy {

	private final List<ApiVersionResolver> versionResolvers;

	private final ApiVersionParser<?> versionParser;

	private final boolean versionRequired;

	private final @Nullable Comparable<?> defaultVersion;

	private final Set<Comparable<?>> supportedVersions = new TreeSet<>();

	private final boolean detectSupportedVersions;

	private final Set<Comparable<?>> detectedVersions = new TreeSet<>();

	private final @Nullable ApiVersionDeprecationHandler deprecationHandler;


	/**
	 * Create an instance.
	 * @param versionResolvers one or more resolvers to try; the first non-null
	 * value returned by any resolver becomes the value used
	 * @param versionParser parser for raw version values
	 * @param versionRequired whether a version is required; if a request
	 * does not have a version, and a {@code defaultVersion} is not specified,
	 * validation fails with {@link MissingApiVersionException}
	 * @param defaultVersion a default version to assign to requests that
	 * don't specify one
	 * @param detectSupportedVersions whether to use API versions that appear in
	 * mappings for supported version validation (true), or use only explicitly
	 * configured versions (false).
	 * @param deprecationHandler handler to send hints and information about
	 * deprecated API versions to clients
	 */
	public DefaultApiVersionStrategy(
			List<ApiVersionResolver> versionResolvers, ApiVersionParser<?> versionParser,
			boolean versionRequired, @Nullable String defaultVersion, boolean detectSupportedVersions,
			@Nullable ApiVersionDeprecationHandler deprecationHandler) {

		Assert.notEmpty(versionResolvers, "At least one ApiVersionResolver is required");
		Assert.notNull(versionParser, "ApiVersionParser is required");

		this.versionResolvers = new ArrayList<>(versionResolvers);
		this.versionParser = versionParser;
		this.versionRequired = (versionRequired && defaultVersion == null);
		this.defaultVersion = (defaultVersion != null ? versionParser.parseVersion(defaultVersion) : null);
		this.detectSupportedVersions = detectSupportedVersions;
		this.deprecationHandler = deprecationHandler;
	}


	@Override
	public @Nullable Comparable<?> getDefaultVersion() {
		return this.defaultVersion;
	}

	/**
	 * Whether the strategy is configured to detect supported versions.
	 * If this is set to {@code false} then {@link #addMappedVersion} is ignored
	 * and the list of supported versions can be built explicitly through calls
	 * to {@link #addSupportedVersion}.
	 */
	public boolean detectSupportedVersions() {
		return this.detectSupportedVersions;
	}

	/**
	 * Add to the list of supported versions to check against in
	 * {@link ApiVersionStrategy#validateVersion} before raising
	 * {@link InvalidApiVersionException} for unknown versions.
	 * <p>By default, actual version values that appear in request mappings are
	 * considered supported, and use of this method is optional. However, if you
	 * prefer to use only explicitly configured, supported versions, then set
	 * {@code detectSupportedVersions} flag to {@code false}.
	 * @param versions the supported versions to add
	 * @see #addMappedVersion(String...)
	 */
	public void addSupportedVersion(String... versions) {
		for (String version : versions) {
			this.supportedVersions.add(parseVersion(version));
		}
	}

	/**
	 * Internal method to add to the list of actual version values that appear in
	 * request mappings, which allows supported versions to be discovered rather
	 * than {@link #addSupportedVersion(String...) configured}.
	 * <p>If you prefer to use explicitly configured, supported versions only,
	 * set the {@code detectSupportedVersions} flag to {@code false}.
	 * @param versions the versions to add
	 * @see #addSupportedVersion(String...)
	 */
	public void addMappedVersion(String... versions) {
		for (String version : versions) {
			this.detectedVersions.add(parseVersion(version));
		}
	}


	@Override
	public @Nullable String resolveVersion(HttpServletRequest request) {
		for (ApiVersionResolver resolver : this.versionResolvers) {
			String version = resolver.resolveVersion(request);
			if (version != null) {
				return version;
			}
		}
		return null;
	}

	@Override
	public Comparable<?> parseVersion(String version) {
		return this.versionParser.parseVersion(version);
	}

	public void validateVersion(@Nullable Comparable<?> requestVersion, HttpServletRequest request)
			throws MissingApiVersionException, InvalidApiVersionException {

		if (requestVersion == null) {
			if (this.versionRequired) {
				throw new MissingApiVersionException();
			}
			return;
		}

		if (!isSupportedVersion(requestVersion)) {
			throw new InvalidApiVersionException(requestVersion.toString());
		}
	}

	private boolean isSupportedVersion(Comparable<?> requestVersion) {
		return (this.supportedVersions.contains(requestVersion) ||
				this.detectSupportedVersions && this.detectedVersions.contains(requestVersion));
	}

	@Override
	public void handleDeprecations(Comparable<?> version, HttpServletRequest request, HttpServletResponse response) {
		if (this.deprecationHandler != null) {
			this.deprecationHandler.handleVersion(version, request, response);
		}
	}

	@Override
	public String toString() {
		return "DefaultApiVersionStrategy[" +
				"supportedVersions=" + this.supportedVersions + ", " +
				"mappedVersions=" + this.detectedVersions + ", " +
				"detectSupportedVersions=" + this.detectSupportedVersions + ", " +
				"versionRequired=" + this.versionRequired + ", " +
				"defaultVersion=" + this.defaultVersion + "]";
	}

}
