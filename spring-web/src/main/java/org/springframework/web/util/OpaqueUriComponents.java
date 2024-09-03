/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link UriComponents} for opaque URIs.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @since 3.2
 * @see <a href="https://tools.ietf.org/html/rfc3986#section-1.2.3">Hierarchical vs Opaque URIs</a>
 */
@SuppressWarnings("serial")
final class OpaqueUriComponents extends UriComponents {

	private static final MultiValueMap<String, String> QUERY_PARAMS_NONE = new LinkedMultiValueMap<>();

	@Nullable
	private final String ssp;


	OpaqueUriComponents(@Nullable String scheme, @Nullable String schemeSpecificPart, @Nullable String fragment) {
		super(scheme, fragment);
		this.ssp = schemeSpecificPart;
	}


	@Override
	@Nullable
	public String getSchemeSpecificPart() {
		return this.ssp;
	}

	@Override
	@Nullable
	public String getUserInfo() {
		return null;
	}

	@Override
	@Nullable
	public String getHost() {
		return null;
	}

	@Override
	public int getPort() {
		return -1;
	}

	@Override
	@Nullable
	public String getPath() {
		return null;
	}

	@Override
	public List<String> getPathSegments() {
		return Collections.emptyList();
	}

	@Override
	@Nullable
	public String getQuery() {
		return null;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return QUERY_PARAMS_NONE;
	}

	@Override
	public UriComponents encode(Charset charset) {
		return this;
	}

	@Override
	protected UriComponents expandInternal(UriTemplateVariables uriVariables) {
		String expandedScheme = expandUriComponent(getScheme(), uriVariables);
		String expandedSsp = expandUriComponent(getSchemeSpecificPart(), uriVariables);
		String expandedFragment = expandUriComponent(getFragment(), uriVariables);
		return new OpaqueUriComponents(expandedScheme, expandedSsp, expandedFragment);
	}

	@Override
	public UriComponents normalize() {
		return this;
	}

	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();

		if (getScheme() != null) {
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}
		if (this.ssp != null) {
			uriBuilder.append(this.ssp);
		}
		if (getFragment() != null) {
			uriBuilder.append('#');
			uriBuilder.append(getFragment());
		}

		return uriBuilder.toString();
	}

	@Override
	public URI toUri() {
		try {
			return new URI(getScheme(), this.ssp, getFragment());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		if (getScheme() != null) {
			builder.scheme(getScheme());
		}
		if (getSchemeSpecificPart() != null) {
			builder.schemeSpecificPart(getSchemeSpecificPart());
		}
		if (getFragment() != null) {
			builder.fragment(getFragment());
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof OpaqueUriComponents that &&
				ObjectUtils.nullSafeEquals(getScheme(), that.getScheme()) &&
				ObjectUtils.nullSafeEquals(this.ssp, that.ssp) &&
				ObjectUtils.nullSafeEquals(getFragment(), that.getFragment())));
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(getScheme());
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.ssp);
		result = 31 * result + ObjectUtils.nullSafeHashCode(getFragment());
		return result;
	}

}
