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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * Request condition to map based on the API version of the request.
 * Versions can be fixed (e.g. "1.2") or baseline (e.g. "1.2+") as described
 * in {@link RequestMapping#version()}.
 *
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class VersionRequestCondition extends AbstractRequestCondition<VersionRequestCondition> {

	private final @Nullable String versionValue;

	private final @Nullable Object version;

	private final boolean baselineVersion;

	private final Set<String> content;


	/**
	 * Constructor with the version, if set on the {@code @RequestMapping}, and
	 * the {@code ApiVersionStrategy}, if API versioning is enabled.
	 */
	public VersionRequestCondition(@Nullable String version, @Nullable ApiVersionStrategy strategy) {
		if (StringUtils.hasText(version)) {
			Assert.isTrue(strategy != null, "ApiVersionStrategy is required for mapping by version");
			this.baselineVersion = version.endsWith("+");
			this.versionValue = updateVersion(version, this.baselineVersion);
			this.version = strategy.parseVersion(this.versionValue);
			this.content = Set.of(version);
		}
		else {
			this.versionValue = null;
			this.version = null;
			this.baselineVersion = false;
			this.content = Collections.emptySet();
		}
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

	/**
	 * Return the raw version value.
	 */
	public @Nullable String getVersion() {
		return this.versionValue;
	}

	@Override
	public VersionRequestCondition combine(VersionRequestCondition other) {
		return (other.version != null ? other : this);
	}

	@Override
	public @Nullable VersionRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		Comparable<?> requestVersion = exchange.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);

		if (this.version == null || requestVersion == null) {
			return this;
		}

		// Always use a baseline match here in order to select the highest version (baseline or fixed)
		// The fixed version match is enforced at the end in handleMatch()

		int result = compareVersions(this.version, requestVersion);
		return (result <= 0 ? this : null);
	}

	@SuppressWarnings("unchecked")
	private <V extends Comparable<V>> int compareVersions(Object v1, Object v2) {
		return ((V) v1).compareTo((V) v2);
	}

	@Override
	public int compareTo(VersionRequestCondition other, ServerWebExchange exchange) {
		Object otherVersion = other.version;
		if (this.version == null && otherVersion == null) {
			return 0;
		}
		else if (this.version != null && otherVersion != null) {
			// make higher version bubble up
			return (-1 * compareVersions(this.version, otherVersion));
		}
		else {
			// Prefer mappings with a version unless the request is without a version
			int result = this.version != null ? -1 : 1;
			Comparable<?> version = exchange.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
			return (version == null ? -1 * result : result);
		}
	}

	/**
	 * Perform a final check on the matched request mapping version.
	 * <p>In order to ensure baseline versions are properly capped by higher
	 * fixed versions, initially we match all versions as baseline versions in
	 * {@link #getMatchingCondition(ServerWebExchange)}. Once the highest of
	 * potentially multiple matches is selected, we enforce the strict match
	 * for fixed versions.
	 * <p>For example, given controller methods for "1.2+" and "1.5", and
	 * a request for "1.6", both are matched, allowing "1.5" to be selected, but
	 * that is then rejected as not acceptable since it is not an exact match.
	 * @param exchange the current exchange
	 * @throws NotAcceptableApiVersionException if the matched condition has a
	 * fixed version that is not equal to the request version
	 */
	public void handleMatch(ServerWebExchange exchange) {
		if (this.version != null && !this.baselineVersion) {
			Comparable<?> version = exchange.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
			if (version != null && !this.version.equals(version)) {
				throw new NotAcceptableApiVersionException(version.toString());
			}
		}
	}

}
