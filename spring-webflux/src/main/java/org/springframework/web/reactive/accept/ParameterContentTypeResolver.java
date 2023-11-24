/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver that checks a query parameter and uses it to look up a matching
 * MediaType. Lookup keys can be registered or as a fallback
 * {@link MediaTypeFactory} can be used to perform a lookup.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ParameterContentTypeResolver implements RequestedContentTypeResolver {

	/** Primary lookup for media types by key (e.g. "json" -> "application/json") */
	private final Map<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);

	private String parameterName = "format";


	public ParameterContentTypeResolver(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((key, value) -> this.mediaTypes.put(formatKey(key), value));
	}

	private static String formatKey(String key) {
		return key.toLowerCase(Locale.ENGLISH);
	}


	/**
	 * Set the name of the parameter to use to determine requested media types.
	 * <p>By default this is set to {@literal "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		String key = exchange.getRequest().getQueryParams().getFirst(getParameterName());
		if (!StringUtils.hasText(key)) {
			return MEDIA_TYPE_ALL_LIST;
		}
		key = formatKey(key);
		MediaType match = this.mediaTypes.get(key);
		if (match == null) {
			match = MediaTypeFactory.getMediaType("filename." + key)
					.orElseThrow(() -> {
						List<MediaType> supported = new ArrayList<>(this.mediaTypes.values());
						return new NotAcceptableStatusException(supported);
					});
		}
		this.mediaTypes.putIfAbsent(key, match);
		return Collections.singletonList(match);
	}

}
