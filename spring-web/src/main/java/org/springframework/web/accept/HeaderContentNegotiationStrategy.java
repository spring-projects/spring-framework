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

package org.springframework.web.accept;

import java.util.Collections;
import java.util.List;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A ContentNegotiationStrategy that parses the 'Accept' header of the request.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class HeaderContentNegotiationStrategy implements ContentNegotiationStrategy {

	private static final String ACCEPT_HEADER = "Accept";

	/**
	 * {@inheritDoc}
	 * @throws HttpMediaTypeNotAcceptableException if the 'Accept' header cannot be parsed.
	 */
	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {
		String acceptHeader = webRequest.getHeader(ACCEPT_HEADER);
		try {
			if (StringUtils.hasText(acceptHeader)) {
				List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
				MediaType.sortBySpecificityAndQuality(mediaTypes);
				return mediaTypes;
			}
		}
		catch (InvalidMediaTypeException ex) {
			throw new HttpMediaTypeNotAcceptableException(
					"Could not parse accept header [" + acceptHeader + "]: " + ex.getMessage());
		}
		return Collections.emptyList();
	}

}
