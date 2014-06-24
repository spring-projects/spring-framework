/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.resource;

import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A {@code VersionStrategy} that handles unique, static, application-wide version strings
 * as prefixes in the request path.
 *
 * <p>Enables inserting a unique and static version String (e.g. reduced SHA, version name,
 * release date) at the beginning of resource paths so that when a new version of the application
 * is released, clients are forced to reload application resources.
 *
 * <p>This is useful when changing resource names is not an option (e.g. when
 * using JavaScript module loaders). If that's not the case, the use of
 * {@link ContentBasedVersionStrategy} provides more optimal performance since
 * version is generated on a per-resource basis: only actually modified resources are reloaded
 * by the client.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.1
 * @see VersionResourceResolver
 */
public class FixedVersionStrategy extends AbstractVersionStrategy {

	private final String version;

	/**
	 * Create a new FixedVersionStrategy with the given version string.
	 * @param fixedVersion the fixed version string to use
	 */
	public FixedVersionStrategy(String fixedVersion) {
		Assert.hasText(fixedVersion, "version must not be null or empty");
		this.version = fixedVersion.endsWith("/") ? fixedVersion : fixedVersion + "/";
	}

	/**
	 * Create a new FixedVersionStrategy and get the version string to use by
	 * calling the given {@code Callable} instance.
	 */
	public FixedVersionStrategy(Callable<String> versionInitializer) throws Exception {
		String fixedVersion = versionInitializer.call();
		Assert.hasText(fixedVersion, "version must not be null or empty");
		this.version = fixedVersion.endsWith("/") ? fixedVersion : fixedVersion + "/";
	}

	@Override
	public String extractVersionFromPath(String requestPath) {

		return extractVersionAsPrefix(requestPath, this.version);
	}

	@Override
	public String deleteVersionFromPath(String requestPath, String candidateVersion) {
		return deleteVersionAsPrefix(requestPath, candidateVersion);
	}

	@Override
	public boolean resourceVersionMatches(Resource baseResource, String candidateVersion) {
		return this.version.equals(candidateVersion);
	}

	@Override
	public String addVersionToUrl(String baseUrl, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return addVersionAsPrefix(baseUrl, this.version);
	}

}
