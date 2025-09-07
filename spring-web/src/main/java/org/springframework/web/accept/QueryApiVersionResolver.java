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


import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ApiVersionResolver} that extract the version from a query parameter.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class QueryApiVersionResolver implements ApiVersionResolver {

	private final String queryParamName;


	public QueryApiVersionResolver(String queryParamName) {
		this.queryParamName = queryParamName;
	}


	@Override
	public @Nullable String resolveVersion(HttpServletRequest request) {
		String query = request.getQueryString();
		if (StringUtils.hasText(query)) {
			UriComponents uri = UriComponentsBuilder.fromUriString("?" + query).build();
			return uri.getQueryParams().getFirst(this.queryParamName);
		}
		return null;
	}

}
