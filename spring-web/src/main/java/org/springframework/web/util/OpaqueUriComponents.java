/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Extension of {@link UriComponents} for opaque URIs.
 *
 * @author Arjen Poutsma
 * @since 3.2
 */
final class OpaqueUriComponents extends UriComponents {

	private static final MultiValueMap<String, String> QUERY_PARAMS =
			new LinkedMultiValueMap<String, String>(0);

	private final String ssp;

	OpaqueUriComponents(String scheme, String schemeSpecificPart, String fragment) {
		super(scheme, fragment);
		this.ssp = schemeSpecificPart;
	}

	@Override
	public String getSchemeSpecificPart() {
		return ssp;
	}

	@Override
	public String getUserInfo() {
		return null;
	}

	@Override
	public String getHost() {
		return null;
	}

	@Override
	public int getPort() {
		return -1;
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public List<String> getPathSegments() {
		return Collections.emptyList();
	}

	@Override
	public String getQuery() {
		return null;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return QUERY_PARAMS;
	}

	@Override
	public UriComponents encode(String encoding) throws UnsupportedEncodingException {
		return this;
	}

	@Override
	protected UriComponents expandInternal(UriTemplateVariables uriVariables) {
		String expandedScheme = expandUriComponent(this.getScheme(), uriVariables);
		String expandedSSp = expandUriComponent(this.ssp, uriVariables);
		String expandedFragment = expandUriComponent(this.getFragment(), uriVariables);

		return new OpaqueUriComponents(expandedScheme, expandedSSp, expandedFragment);
	}

	@Override
	public String toUriString() {
		StringBuilder uriBuilder = new StringBuilder();

		if (getScheme() != null) {
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}
		if (ssp != null) {
			uriBuilder.append(ssp);
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
			return new URI(getScheme(), ssp, getFragment());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(
					"Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	public UriComponents normalize() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof OpaqueUriComponents) {
			OpaqueUriComponents other = (OpaqueUriComponents) o;

			if (getScheme() != null ? !getScheme().equals(other.getScheme()) :
					other.getScheme() != null) {
				return false;
			}
			if (ssp != null ?
					!ssp.equals(other.ssp) :
					other.ssp != null) {
				return false;
			}
			if (getFragment() != null ? !getFragment().equals(other.getFragment()) :
					other.getFragment() != null) {
				return false;
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int result = getScheme() != null ? getScheme().hashCode() : 0;
		result = 31 * result +
				(ssp != null ? ssp.hashCode() : 0);
		result = 31 * result + (getFragment() != null ? getFragment().hashCode() : 0);
		return result;
	}
}
