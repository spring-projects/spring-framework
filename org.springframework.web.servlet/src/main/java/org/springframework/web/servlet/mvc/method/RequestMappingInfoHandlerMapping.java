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

package org.springframework.web.servlet.mvc.method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * An {@link AbstractHandlerMethodMapping} variant that uses {@link RequestMappingInfo} to represent request 
 * mapping conditions.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1.0
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	@Override
	protected void handlerMethodsInitialized(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
		List<RequestMappingInfo> mappings = new ArrayList<RequestMappingInfo>(handlerMethods.keySet());
		while (mappings.size() > 1) {
			RequestMappingInfo mapping = mappings.remove(0);
			for (RequestMappingInfo otherMapping : mappings) {
				// further validate mapping conditions
			}
		}
	}

	@Override
	protected Set<String> getMappingPaths(RequestMappingInfo mapping) {
		return mapping.getPatternsCondition().getPatterns();
	}

	/**
	 * Returns a new {@link RequestMappingInfo} with attributes matching to the current request or {@code null}.
	 *
	 * @see RequestMappingInfo#getMatchingRequestMapping(String, HttpServletRequest, PathMatcher)
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo mapping,
													String lookupPath,
													HttpServletRequest request) {
		return mapping.getMatchingRequestMapping(request);
	}

	/**
	 * Returns a {@link Comparator} that can be used to sort and select the best matching {@link RequestMappingInfo}.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(String lookupPath, HttpServletRequest request) {
		return new RequestMappingInfoComparator(request);
	}

	/**
	 * Exposes URI template variables and producible media types as request attributes.
	 * 
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		String pattern = info.getPatternsCondition().getPatterns().iterator().next();
		Map<String, String> uriTemplateVariables = getPathMatcher().extractUriTemplateVariables(pattern, lookupPath);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		if (!info.getProducesCondition().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProducesCondition().getMediaTypes();
			request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
		}
	}

	/**
	 * Iterates all {@link RequestMappingInfo}s looking for mappings that match by URL but not by HTTP method.
	 *
	 * @throws HttpRequestMethodNotSupportedException if there are matches by URL but not by HTTP method
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos,
										  String lookupPath,
										  HttpServletRequest request) throws ServletException {
		Set<String> allowedMethods = new HashSet<String>(6);
		Set<MediaType> consumableMediaTypes = new HashSet<MediaType>();
		Set<MediaType> producibleMediaTypes = new HashSet<MediaType>();
		for (RequestMappingInfo info : requestMappingInfos) {
			for (String pattern : info.getPatternsCondition().getPatterns()) {
				if (getPathMatcher().match(pattern, lookupPath)) {
					if (info.getMethodsCondition().getMatchingCondition(request) == null) {
						for (RequestMethod method : info.getMethodsCondition().getMethods()) {
							allowedMethods.add(method.name());
						}
					}
					if (info.getConsumesCondition().getMatchingCondition(request) == null) {
						consumableMediaTypes.addAll(info.getConsumesCondition().getMediaTypes());
					}
					if (info.getProducesCondition().getMatchingCondition(request) == null) {
						producibleMediaTypes.addAll(info.getProducesCondition().getMediaTypes());
					}
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

	/**
	 * A comparator for {@link RequestMappingInfo}s. Effective comparison can only be done in the context 
	 * of a specific request. For example only a subset of URL patterns may apply to the current request.
	 */
	private class RequestMappingInfoComparator implements Comparator<RequestMappingInfo> {

		private final HttpServletRequest request;

		public RequestMappingInfoComparator(HttpServletRequest request) {
			this.request = request;
		}

		public int compare(RequestMappingInfo mapping, RequestMappingInfo otherMapping) {
			int result = mapping.getPatternsCondition().compareTo(otherMapping.getPatternsCondition(), request);
			if (result != 0) {
				return result;
			}
			result = mapping.getParamsCondition().compareTo(otherMapping.getParamsCondition(), request);
			if (result != 0) {
				return result;
			}
			result = mapping.getHeadersCondition().compareTo(otherMapping.getHeadersCondition(), request);
			if (result != 0) {
				return result;
			}
			result = mapping.getConsumesCondition().compareTo(otherMapping.getConsumesCondition(), request);
			if (result != 0) {
				return result;
			}
			result = mapping.getProducesCondition().compareTo(otherMapping.getProducesCondition(), request);
			if (result != 0) {
				return result;
			}
			result = mapping.getMethodsCondition().compareTo(otherMapping.getMethodsCondition(), request);
			if (result != 0) {
				return result;
			}
			return 0;
		}
	}

}
