/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link UriTemplateHandler} based on the use of
 * {@link UriComponentsBuilder} for expanding and encoding variables.
 *
 * <p>There are also several properties to customize how URI template handling
 * is performed, including a {@link #setBaseUrl baseUrl} to be used as a prefix
 * for all URI templates and a couple of encoding related options &mdash;
 * {@link #setParsePath parsePath} and {@link #setStrictEncoding strictEncoding}
 * respectively.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {

	private boolean parsePath;

	private boolean strictEncoding;


	/**
	 * Whether to parse the path of a URI template string into path segments.
	 * <p>If set to {@code true} the URI template path is immediately decomposed
	 * into path segments any URI variables expanded into it are then subject to
	 * path segment encoding rules. In effect URI variables in the path have any
	 * "/" characters percent encoded.
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

	/**
	 * Whether to encode characters outside the unreserved set as defined in
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 Section 2</a>.
	 * This ensures a URI variable value will not contain any characters with a
	 * reserved purpose.
	 * <p>By default this is set to {@code false} in which case only characters
	 * illegal for the given URI component are encoded. For example when expanding
	 * a URI variable into a path segment the "/" character is illegal and
	 * encoded. The ";" character however is legal and not encoded even though
	 * it has a reserved purpose.
	 * <p><strong>Note:</strong> this property supersedes the need to also set
	 * the {@link #setParsePath parsePath} property.
	 * @param strictEncoding whether to perform strict encoding
	 * @since 4.3
	 */
	public void setStrictEncoding(boolean strictEncoding) {
		this.strictEncoding = strictEncoding;
	}

	/**
	 * Whether to strictly encode any character outside the unreserved set.
	 */
	public boolean isStrictEncoding() {
		return this.strictEncoding;
	}


	@Override
	protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	@Override
	protected URI expandInternal(String uriTemplate, Object... uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	/**
	 * Create a {@code UriComponentsBuilder} from the URI template string.
	 * This implementation also breaks up the path into path segments depending
	 * on whether {@link #setParsePath parsePath} is enabled.
	 */
	protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		if (shouldParsePath() && !isStrictEncoding()) {
			List<String> pathSegments = builder.build().getPathSegments();
			builder.replacePath(null);
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		return builder;
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Map<String, Object> encodedUriVars = new HashMap<String, Object>(uriVariables.size());
			for (Map.Entry<String, ?> entry : uriVariables.entrySet()) {
				encodedUriVars.put(entry.getKey(), applyStrictEncoding(entry.getValue()));
			}
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Object[] encodedUriVars = new Object[uriVariables.length];
			for (int i = 0; i < uriVariables.length; i++) {
				encodedUriVars[i] = applyStrictEncoding(uriVariables[i]);
			}
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	private String applyStrictEncoding(Object value) {
		String stringValue = (value != null ? value.toString() : "");
		try {
			return UriUtils.encode(stringValue, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			// Should never happen
			throw new IllegalStateException("Failed to encode URI variable", ex);
		}
	}

	private URI createUri(UriComponents uriComponents) {
		try {
			// Avoid further encoding (in the case of strictEncoding=true)
			return new URI(uriComponents.toUriString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

}
