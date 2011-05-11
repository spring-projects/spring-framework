/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Factory for {@link RequestCondition} objects.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class RequestConditionFactory {

	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private static final String ACCEPT_HEADER = "Accept";

	/**
	 * Parses the given request methods, and returns them as a single request condition.
	 *
	 * @param methods the methods
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#method()
	 */
	public static RequestMethodsRequestCondition parseMethods(RequestMethod... methods) {
		return methods != null ? new RequestMethodsRequestCondition(methods) : new RequestMethodsRequestCondition();
	}

	/**
	 * Parses the given parameters, and returns them as a single request condition.
	 *
	 * @param params the parameters
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public static ParamsRequestCondition parseParams(String... params) {
		return params != null ? new ParamsRequestCondition(params) : new ParamsRequestCondition();
	}

	/**
	 * Parses the given headers, and returns them as a single request condition.
	 *
	 * @param headers the headers
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#headers()
	 */
	public static HeadersRequestCondition parseHeaders(String... headers) {
		if (headers == null) {
			return new HeadersRequestCondition();
		}
		HeadersRequestCondition headersCondition = new HeadersRequestCondition(headers);

		// filter out Accept and Content-Type headers, they are dealt with by produces and consumes respectively
		Set<HeadersRequestCondition.HeaderRequestCondition> filteredConditions =
				new LinkedHashSet<HeadersRequestCondition.HeaderRequestCondition>(headersCondition.getConditions());

		for (Iterator<HeadersRequestCondition.HeaderRequestCondition> iterator = filteredConditions.iterator();
				iterator.hasNext();) {
			HeadersRequestCondition.HeaderRequestCondition headerCondition = iterator.next();
			if (ACCEPT_HEADER.equalsIgnoreCase(headerCondition.name) ||
					CONTENT_TYPE_HEADER.equalsIgnoreCase(headerCondition.name)) {
				iterator.remove();
			}
		}
		return new HeadersRequestCondition(filteredConditions);
	}

	/**
	 * Parses the given consumes, and returns them as a single request condition.
	 *
	 * @param consumes the consumes
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#consumes()
	 */
	public static ConsumesRequestCondition parseConsumes(String... consumes) {
		return new ConsumesRequestCondition(consumes);
	}

	public static ConsumesRequestCondition parseConsumes(String[] consumes, String[] headers) {

		List<ConsumesRequestCondition.ConsumeRequestCondition> allConditions = parseContentTypeHeaders(headers);

		// ignore the default consumes() value if any content-type headers have been set
		boolean headersHasContentType = !allConditions.isEmpty();
		boolean consumesHasDefaultValue = consumes.length == 1 && consumes[0].equals("*/*");
		if (!headersHasContentType || !consumesHasDefaultValue) {
			for (String consume : consumes) {
				allConditions.add(new ConsumesRequestCondition.ConsumeRequestCondition(consume));
			}
		}
		return new ConsumesRequestCondition(allConditions);
	}

	private static List<ConsumesRequestCondition.ConsumeRequestCondition> parseContentTypeHeaders(String[] headers) {
		List<ConsumesRequestCondition.ConsumeRequestCondition> allConditions =
				new ArrayList<ConsumesRequestCondition.ConsumeRequestCondition>();
		HeadersRequestCondition headersCondition = new HeadersRequestCondition(headers);
		for (HeadersRequestCondition.HeaderRequestCondition headerCondition : headersCondition.getConditions()) {
			if (CONTENT_TYPE_HEADER.equalsIgnoreCase(headerCondition.name)) {
				List<MediaType> mediaTypes = MediaType.parseMediaTypes(headerCondition.value);
				for (MediaType mediaType : mediaTypes) {
					allConditions.add(new ConsumesRequestCondition.ConsumeRequestCondition(mediaType,
							headerCondition.isNegated));
				}
			}
		}
		return allConditions;
	}

}
