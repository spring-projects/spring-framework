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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ApiVersionStrategy} that delegates to the
 * configured version resolvers and version parser.
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


	/**
	 * Create an instance.
	 * @param versionResolvers one or more resolvers to try; the first non-null
	 * value returned by any resolver becomes the resolved used
	 * @param versionParser parser for to raw version values
	 * @param versionRequired whether a version is required; if a request
	 * does not have a version, and a {@code defaultVersion} is not specified,
	 * validation fails with {@link MissingApiVersionException}
	 * @param defaultVersion a default version to assign to requests that
	 * don't specify one
	 */
	public DefaultApiVersionStrategy(
			List<ApiVersionResolver> versionResolvers, ApiVersionParser<?> versionParser,
			boolean versionRequired, @Nullable String defaultVersion) {

		Assert.notEmpty(versionResolvers, "At least one ApiVersionResolver is required");
		Assert.notNull(versionParser, "ApiVersionParser is required");

		this.versionResolvers = new ArrayList<>(versionResolvers);
		this.versionParser = versionParser;
		this.versionRequired = (versionRequired && defaultVersion == null);
		this.defaultVersion = (defaultVersion != null ? versionParser.parseVersion(defaultVersion) : null);
	}


	@Override
	public @Nullable Comparable<?> getDefaultVersion() {
		return this.defaultVersion;
	}

	/**
	 * Add to the list of known, supported versions to check against in
	 * {@link ApiVersionStrategy#validateVersion}. Request versions that are not
	 * in the supported result in {@link InvalidApiVersionException}
	 * in {@link ApiVersionStrategy#validateVersion}.
	 * @param versions the versions to add
	 */
	public void addSupportedVersion(String... versions) {
		for (String version : versions) {
			this.supportedVersions.add(parseVersion(version));
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

		if (!this.supportedVersions.contains(requestVersion)) {
			throw new InvalidApiVersionException(requestVersion.toString());
		}
	}

	@Override
	public String toString() {
		return "DefaultApiVersionStrategy[supportedVersions=" + this.supportedVersions +
				", versionRequired=" + this.versionRequired + ", defaultVersion=" + this.defaultVersion + "]";
	}

}
