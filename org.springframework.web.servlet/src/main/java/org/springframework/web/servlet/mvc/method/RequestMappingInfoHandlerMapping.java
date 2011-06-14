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
import java.util.Iterator;
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
		return mapping.getPatterns();
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
		return mapping.getMatchingRequestMapping(lookupPath, request, getPathMatcher());
	}

	/**
	 * Returns a {@link Comparator} that can be used to sort and select the best matching {@link RequestMappingInfo}.
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(String lookupPath, HttpServletRequest request) {
		return new RequestMappingInfoComparator(lookupPath, request);
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

		String pattern = info.getPatterns().iterator().next();
		Map<String, String> uriTemplateVariables = getPathMatcher().extractUriTemplateVariables(pattern, lookupPath);
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);

		if (!info.getProduces().isEmpty()) {
			Set<MediaType> mediaTypes = info.getProduces().getMediaTypes();
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
			for (String pattern : info.getPatterns()) {
				if (getPathMatcher().match(pattern, lookupPath)) {
					if (!info.getMethods().match(request)) {
						for (RequestMethod method : info.getMethods().getMethods()) {
							allowedMethods.add(method.name());
						}
					}
					if (!info.getConsumes().match(request)) {
						consumableMediaTypes.addAll(info.getConsumes().getMediaTypes());
					}
					if (!info.getProduces().match(request)) {
						producibleMediaTypes.addAll(info.getProduces().getMediaTypes());
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
	 * A comparator for {@link RequestMappingInfo}s. Effective comparison can only be done in the context of a specific
	 * request. For example not all {@link RequestMappingInfo} patterns may apply to the current request. Therefore an
	 * HttpServletRequest is required as input.
	 *
	 * <p>Furthermore, the following assumptions are made about the input RequestMappings: <ul><li>Each RequestMappingInfo
	 * has been fully matched to the request <li>The RequestMappingInfo contains matched patterns only <li>Patterns are
	 * ordered with the best matching pattern at the top </ul>
	 *
	 * @see RequestMappingInfoHandlerMapping#getMatchingMapping(RequestMappingInfo, String, HttpServletRequest)
	 */
	private class RequestMappingInfoComparator implements Comparator<RequestMappingInfo> {

		private Comparator<String> patternComparator;

		private List<MediaType> requestAcceptHeader;

		public RequestMappingInfoComparator(String lookupPath, HttpServletRequest request) {
			this.patternComparator = getPathMatcher().getPatternComparator(lookupPath);
			String acceptHeader = request.getHeader("Accept");
			this.requestAcceptHeader = MediaType.parseMediaTypes(acceptHeader);
			MediaType.sortByQualityValue(this.requestAcceptHeader);
		}

		public int compare(RequestMappingInfo mapping, RequestMappingInfo otherMapping) {
			int result = comparePatterns(mapping.getPatterns(), otherMapping.getPatterns());
			if (result != 0) {
				return result;
			}
			result = mapping.getParams().compareTo(otherMapping.getParams());
			if (result != 0) {
				return result;
			}
			result = mapping.getHeaders().compareTo(otherMapping.getHeaders());
			if (result != 0) {
				return result;
			}
			result = mapping.getConsumes().compareTo(otherMapping.getConsumes());
			if (result != 0) {
				return result;
			}
			result = mapping.getProduces().compareTo(otherMapping.getProduces(), this.requestAcceptHeader);
			if (result != 0) {
				return result;
			}
			result = mapping.getMethods().compareTo(otherMapping.getMethods());
			if (result != 0) {
				return result;
			}
			return 0;
		}

		private int comparePatterns(Set<String> patterns, Set<String> otherPatterns) {
			Iterator<String> iterator = patterns.iterator();
			Iterator<String> iteratorOther = otherPatterns.iterator();
			while (iterator.hasNext() && iteratorOther.hasNext()) {
				int result = patternComparator.compare(iterator.next(), iteratorOther.next());
				if (result != 0) {
					return result;
				}
			}
			if (iterator.hasNext()) {
				return -1;
			}
			else if (iteratorOther.hasNext()) {
				return 1;
			}
			else {
				return 0;
			}
		}

	}

}
