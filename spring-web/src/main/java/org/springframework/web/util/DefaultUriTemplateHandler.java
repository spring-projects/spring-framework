/*
 * Copyright 2002-2015 the original author or authors.
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

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link UriTemplateHandler} that relies on
 * {@link UriComponentsBuilder} internally.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultUriTemplateHandler implements UriTemplateHandler {

	private boolean parsePath;


	/**
	 * Whether to parse the path of a URI template string into path segments.
	 * <p>If set to {@code true} the path of parsed URI templates is decomposed
	 * into path segments so that URI variables expanded into the path are
	 * treated according to path segment encoding rules. In effect that means the
	 * "/" character is percent encoded.
	 * <p>By default this is set to {@code false} in which case the path is kept
	 * as a full path and expanded URI variables will preserve "/" characters.
	 * @param parsePath whether to parse the path into path segments
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * Whether the handler is configured to parse the path into path segments.
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		UriComponentsBuilder builder = initBuilder(uriTemplate);
		return builder.build().expand(uriVariables).encode().toUri();
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariableValues) {
		UriComponentsBuilder builder = initBuilder(uriTemplate);
		return builder.build().expand(uriVariableValues).encode().toUri();
	}

	protected UriComponentsBuilder initBuilder(String uriTemplate) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		if (shouldParsePath()) {
			List<String> pathSegments = builder.build().getPathSegments();
			builder.replacePath(null);
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		return builder;
	}

}
