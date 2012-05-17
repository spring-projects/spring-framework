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

package org.springframework.web.servlet.mvc.method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * Abstract base class for classes for which {@link RequestMappingInfo} defines
 * the mapping between a request and a handler method.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1.0
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	/**
	 * Get the URL path patterns associated with this {@link RequestMappingInfo}.
	 */
	@Override
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternsCondition().getPatterns();
	}

	/**
	 * Check if the given RequestMappingInfo matches the current request and
	 * return a (potentially new) instance with conditions that match the
	 * current request -- for example with a subset of URL patterns.
	 * @return an info in case of a match; or {@code null} otherwise.
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
		return info.getMatchingCondition(request);
	}

	/**
	 * Provide a Comparator to sort RequestMappingInfos matched to a request.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
		return new Comparator<RequestMappingInfo>() {
			public int compare(RequestMappingInfo info1, RequestMappingInfo info2) {
				return info1.compareTo(info2, request);
			}
		};
	}

	/**
	 * Expose URI template variables and producible media types in the request.
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		Set<String> patterns = info.getPatternsCondition().getPatterns();
		String bestPattern = patterns.isEmpty() ? lookupPath : patterns.iterator().next();
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);

		Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
		Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedVars);

		if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
			request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	/**
	 * Iterate all RequestMappingInfos once again, look if any match by URL at
	 * least and raise exceptions accordingly.
	 *
	 * @throws HttpRequestMethodNotSupportedException
	 * 		if there are matches by URL but not by HTTP method
	 * @throws HttpMediaTypeNotAcceptableException
	 * 		if there are matches by URL but not by consumable media types
	 * @throws HttpMediaTypeNotAcceptableException
	 * 		if there are matches by URL but not by producible media types
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos,
										  String lookupPath,
										  HttpServletRequest request) throws ServletException {
		Set<String> allowedMethods = new HashSet<String>(6);
		Set<MediaType> consumableMediaTypes = new HashSet<MediaType>();
		Set<MediaType> producibleMediaTypes = new HashSet<MediaType>();
		for (RequestMappingInfo info : requestMappingInfos) {
			if (info.getPatternsCondition().getMatchingCondition(request) != null) {
				if (info.getMethodsCondition().getMatchingCondition(request) == null) {
					for (RequestMethod method : info.getMethodsCondition().getMethods()) {
						allowedMethods.add(method.name());
					}
				}
				if (info.getConsumesCondition().getMatchingCondition(request) == null) {
					consumableMediaTypes.addAll(info.getConsumesCondition().getConsumableMediaTypes());
				}
				if (info.getProducesCondition().getMatchingCondition(request) == null) {
					producibleMediaTypes.addAll(info.getProducesCondition().getProducibleMediaTypes());
				}
			}
		}
		if (!allowedMethods.isEmpty()) {
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), allowedMethods);
		}
		else if (!consumableMediaTypes.isEmpty()) {
			MediaType contentType = null;
			if (StringUtils.hasLength(request.getContentType())) {
				contentType = MediaType.parseMediaType(request.getContentType());
			}
			throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<MediaType>(consumableMediaTypes));
		}
		else if (!producibleMediaTypes.isEmpty()) {
			throw new HttpMediaTypeNotAcceptableException(new ArrayList<MediaType>(producibleMediaTypes));
		}
		else {
			return null;
		}
	}

}
