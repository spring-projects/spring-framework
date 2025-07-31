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

package org.springframework.web.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default implementation of {@link ApiVersionInserter}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see DefaultApiVersionInserterBuilder
 */
final class DefaultApiVersionInserter implements ApiVersionInserter {

	private final @Nullable String header;

	private final @Nullable String queryParam;

	private final @Nullable String mediaTypeParam;

	private final @Nullable Integer pathSegmentIndex;

	private final ApiVersionFormatter versionFormatter;


	DefaultApiVersionInserter(
			@Nullable String header, @Nullable String queryParam, @Nullable String mediaTypeParam,
			@Nullable Integer pathSegmentIndex, @Nullable ApiVersionFormatter formatter) {

		Assert.isTrue(header != null || queryParam != null || mediaTypeParam != null || pathSegmentIndex != null,
				"Expected 'header', 'queryParam', 'mediaTypeParam', or 'pathSegmentIndex' to be configured");

		this.header = header;
		this.queryParam = queryParam;
		this.mediaTypeParam = mediaTypeParam;
		this.pathSegmentIndex = pathSegmentIndex;
		this.versionFormatter = (formatter != null ? formatter : Object::toString);
	}


	@Override
	public URI insertVersion(Object version, URI uri) {
		if (this.queryParam == null && this.pathSegmentIndex == null) {
			return uri;
		}
		String formattedVersion = this.versionFormatter.formatVersion(version);
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
		if (this.queryParam != null) {
			builder.queryParam(this.queryParam, formattedVersion);
		}
		if (this.pathSegmentIndex != null) {
			List<String> pathSegments = new ArrayList<>(builder.build().getPathSegments());
			assertPathSegmentIndex(this.pathSegmentIndex, pathSegments.size(), uri);
			pathSegments.add(this.pathSegmentIndex, formattedVersion);
			builder.replacePath(null);
			pathSegments.forEach(builder::pathSegment);
		}
		return builder.build().toUri();
	}

	private void assertPathSegmentIndex(Integer index, int pathSegmentsSize, URI uri) {
		Assert.state(index <= pathSegmentsSize,
				"Cannot insert version into '" + uri.getPath() + "' at path segment index " + index);
	}

	@Override
	public void insertVersion(Object version, HttpHeaders headers) {
		if (this.header != null) {
			String formattedVersion = this.versionFormatter.formatVersion(version);
			headers.set(this.header, formattedVersion);
		}
		if (this.mediaTypeParam != null) {
			MediaType contentType = headers.getContentType();
			if (contentType != null) {
				Map<String, String> params = new LinkedHashMap<>(contentType.getParameters());
				params.put(this.mediaTypeParam, this.versionFormatter.formatVersion(version));
				contentType = new MediaType(contentType, params);
				headers.setContentType(contentType);
			}
		}
	}

}
