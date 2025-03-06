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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Request condition to map based on the API version of the request.
 * Versions can be fixed (e.g. "1.2") or baseline (e.g. "1.2+") as described
 * in {@link RequestMapping#version()}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class VersionRequestCondition extends AbstractRequestCondition<VersionRequestCondition> {

	private static final String VERSION_ATTRIBUTE_NAME = VersionRequestCondition.class.getName() + ".VERSION";

	private static final String NO_VERSION_ATTRIBUTE = "NO_VERSION";

	private static final ApiVersionStrategy NO_OP_VERSION_STRATEGY = new NoOpApiVersionStrategy();


	private final @Nullable String versionValue;

	private final @Nullable Object version;

	private final boolean baselineVersion;

	private final ApiVersionStrategy versionStrategy;

	private final Set<String> content;


	public VersionRequestCondition() {
		this.versionValue = null;
		this.version = null;
		this.baselineVersion = false;
		this.versionStrategy = NO_OP_VERSION_STRATEGY;
		this.content = Collections.emptySet();
	}

	public VersionRequestCondition(String configuredVersion, ApiVersionStrategy versionStrategy) {
		this.baselineVersion = configuredVersion.endsWith("+");
		this.versionValue = updateVersion(configuredVersion, this.baselineVersion);
		this.version = versionStrategy.parseVersion(this.versionValue);
		this.versionStrategy = versionStrategy;
		this.content = Set.of(configuredVersion);
	}

	private static String updateVersion(String version, boolean baselineVersion) {
		return (baselineVersion ? version.substring(0, version.length() - 1) : version);
	}


	@Override
	protected Collection<String> getContent() {
		return this.content;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	public @Nullable String getVersion() {
		return this.versionValue;
	}

	@Override
	public VersionRequestCondition combine(VersionRequestCondition other) {
		return (other.version != null ? other : this);
	}

	@Override
	public @Nullable VersionRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (this.version == null) {
			return this;
		}

		Comparable<?> version = (Comparable<?>) request.getAttribute(VERSION_ATTRIBUTE_NAME);
		if (version == null) {
			String value = this.versionStrategy.resolveVersion(request);
			version = (value != null ? parseVersion(value) : this.versionStrategy.getDefaultVersion());
			this.versionStrategy.validateVersion(version, request);
			version = (version != null ? version : NO_VERSION_ATTRIBUTE);
			request.setAttribute(VERSION_ATTRIBUTE_NAME, (version));
		}

		if (version == NO_VERSION_ATTRIBUTE) {
			return this;
		}

		// At this stage, match all versions as baseline versions.
		// Strict matching for fixed versions is enforced at the end in handleMatch.

		int result = compareVersions(this.version, version);
		return (result <= 0 ? this : null);
	}

	private Comparable<?> parseVersion(String value) {
		try {
			return this.versionStrategy.parseVersion(value);
		}
		catch (Exception ex) {
			throw new InvalidApiVersionException(value, null, ex);
		}
	}

	@SuppressWarnings("unchecked")
	private <V extends Comparable<V>> int compareVersions(Object v1, Object v2) {
		return ((V) v1).compareTo((V) v2);
	}

	@Override
	public int compareTo(VersionRequestCondition other, HttpServletRequest request) {
		Object otherVersion = other.version;
		if (this.version == null && otherVersion == null) {
			return 0;
		}
		else if (this.version != null && otherVersion != null) {
			// make higher version bubble up
			return (-1 * compareVersions(this.version, otherVersion));
		}
		else {
			return (this.version != null ? -1 : 1);
		}
	}

	/**
	 * Perform a final check on the matched request mapping version.
	 * <p>In order to ensure baseline versions are properly capped by higher
	 * fixed versions, initially we match all versions as baseline versions in
	 * {@link #getMatchingCondition(HttpServletRequest)}. Once the highest of
	 * potentially multiple matches is selected, we enforce the strict match
	 * for fixed versions.
	 * <p>For example, given controller methods for "1.2+" and "1.5", and
	 * a request for "1.6", both are matched, allowing "1.5" to be selected, but
	 * that is then rejected as not acceptable since it is not an exact match.
	 * @param request the current request
	 * @throws NotAcceptableApiVersionException if the matched condition has a
	 * fixed version that is not equal to the request version
	 */
	public void handleMatch(HttpServletRequest request) {
		if (this.version != null && !this.baselineVersion) {
			Comparable<?> version = (Comparable<?>) request.getAttribute(VERSION_ATTRIBUTE_NAME);
			Assert.state(version != null, "No API version attribute");
			if (!this.version.equals(version)) {
				throw new NotAcceptableApiVersionException(version.toString());
			}
		}
	}


	private static final class NoOpApiVersionStrategy implements ApiVersionStrategy {

		@Override
		public @Nullable String resolveVersion(HttpServletRequest request) {
			return null;
		}

		@Override
		public String parseVersion(String version) {
			return version;
		}

		@Override
		public void validateVersion(@Nullable Comparable<?> requestVersion, HttpServletRequest request) {
		}

		@Override
		public @Nullable Comparable<?> getDefaultVersion() {
			return null;
		}
	}

}
